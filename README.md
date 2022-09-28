# DataONE Indexer

This component provides index task processing of index tasks created by other components. It consists of
three main subsystems, each defined by it's own helm subsystem chart:

- index-worker: a subsystem implementing a Worker class to process index jobs in parallel
- rabbitmq: a deplyment of the RabbitMQ queue management system
- solr: a deployment of the SOLR full text search system

Clients are expected to register index task messages in the RabbitMQ queue to be processed. Upon startup, the RabbitMQ workers
register themselves as handlers of the index task messages. As messages enter the queue, RabbitMQ dispatches these to 
registered workers in parallel, and workers in turn process the associated object and insert a new index entry into SOLR.

See LICENSE.txt for the details of distributing this software.

## Building Docker image

The image can be built with either `docker` or `nerdctl` depending on which container environment you have installed. Here I show the example using Racher Desktop configured to use `nerdctl`.

```bash
mvn clean package -DskipTests
nerdctl build -t dataone-index-worker:2.4.0 -f docker/Dockerfile --build-arg TAG=2.4.0 .
```

If you are building locally for Kubernetes on, for example, rancher-desktop, you'll need to set the namespace to `k8s.io` using a build command such as:

```bash
mvn clean package -DskipTests
nerdctl build -t dataone-index-worker:2.4.0 -f docker/Dockerfile --build-arg TAG=2.4.0 --namespace k8s.io .
```

## Publish the image to GHCR

For the built image to be accessible for kubernetes, it needs to be published to an image registry that is visible to Kubernetes. For example, we can make the published image available via the Github Container Registry (ghcr.io) so that it can be pulled by Kubernetes. For this to work, one must first tag the image with the ghcr.io URL so that it can be published. Then, after logging in to the registry with a suitable GIHUB PAT, one can push the image to the registry.  

Note that, for the image to be associated with a particular github repository, a metadata LABEL can be added to the image that associates it when it is built.  To locally connect the image to a repository, add this line to your Dockerfile:

```
LABEL org.opencontainers.image.source https://github.com/dataoneorg/dataone-indexer
```

Commands for pushing the built image:
```
nerdctl tag dataone-index-worker:2.4.0 ghcr.io/dataoneorg/dataone-index-worker:2.4.0
echo $GITHUB_PAT | nerdctl login ghcr.io -u DataONEorg --password-stdin
nerdctl push ghcr.io/dataoneorg/dataone-index-worker:2.4.0
```

Once the image has been pushed, it may be private and will need to be made public and assigned to the `dataone-indexer` repository if the LABEL wasn't set as described above.

## Deploying the application via Helm

Helm provides a simple mechanism to install all application dependencies and configure the application in a single command. To deploy using helm to a release named `d1index` and also in a namespace named `d1index`, and then view the deployed pods and services, use a sequence like:

```bash
kubectl create namespace d1index
helm install -n d1index d1index ./helm
kubectl -n d1index get all
```

and to uninstall the helm, use:

```bash
helm -n d1index uninstall d1index
```

Note that this helm chart also installs rabbitmq, which can be partially configured through the values.yaml file in the parent chart through exported child properties.

### Authentication note

The username and password under which the rabbitmq service runs are set in the values.yaml file. It appears that this information is cached on a PersistentVolumeClaim that is created automatically by rabbitmq. If the credentials are changed in the helm `values.yaml` file, authentication will fail because they will conflict with the cached values in the PVC. If you are just testing, the problem can be resolved by deleting the PVC. In production, the PVC would also be used for maintaining durable queues, and so it may not be reasonable to delete the PVC.  You can get the name and identifiers of the PVCs with `kubectl -n d1index get pvc`.

## Running the IndexWorker in the docker container

The docker image assumes that the deployment configuration file exists to configure endpoint addresses and credentials. To run the indexer, ensure that the 
`DATAONE_INDEXER_CONFIG` is set in the environment and contains the absolute path to the configuration file for the indexer. This path must be accessible in the container, so you will likely want to mount a volume to provide the edited properties file. You can then run it using a command like:

```bash
nerdctl run -it \
    -e DATAONE_INDEXER_CONFIG=/var/lib/dataone-indexer/dataone-indexer.properties \
    -v `pwd`/helm/config/dataone-indexer.properties:/var/lib/dataone-indexer/dataone-indexer.properties \
    dataone-index-worker:2.4.0
```

## Manual steps to customize the Solr servers

After installing the standard solr servers by helm, some manual steps are needed to make it work against the DataONE schema.
- Copy the local `schema.xml` and `solrconfig.xml` files which are located at `src/main/resources/solr-conf` directory to all Solr pods
```
kubectl cp -n d1index schema.xml  d1index-solr-0:/bitnami/solr/server/solr/configsets/.
kubectl cp -n d1index solrconfig.xml  d1index-solr-0:/bitnami/solr/server/solr/configsets/.
```
- Creat the configuration directory for the collection of `dataone-index` in all Solr pods
```
kubectl exec -n d1index -it d1index-solr-2 -- /bin/bash
d1index-solr-2:/bitnami/solr/server/solr/configsets$ cp -R sample_techproducts_configs dataone_index_configs
d1index-solr-2:/bitnami/solr/server/solr/configsets$ cp schema.xml dataone_index_configs/conf/.
d1index-solr-2:/bitnami/solr/server/solr/configsets$ cp solrconfig.xml dataone_index_configs/conf/solrconfig.xml 
```
- Delete the dummy `dataone-one` collection installed in the helm installation process
```
/opt/bitnami/solr/bin/solr delete -c collection_name
```
- Create a new `dataone-one` collection based on the customized configuration. Note: this command only needs to run once on a Solr pod.
```
/opt/bitnami/solr/bin/solr create -c dataone-index -d /bitnami/solr/server/solr/configsets/dataone_index_configs/ -rf 3
```

## History

This is a refactored version of the original DataONE [d1_cn_index_processor](https://github.com/DataONEorg/d1_cn_index_processor) that runs completely independently of other
DataONE Coordinating Node services. It is intended to be deployed in a Kubernetes cluster environment, but is written such 
that it can be deployed in other environments as well as needed.

