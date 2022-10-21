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

After installing the standard solr servers by helm, we need to create the DataONE collections with the specific DataONE schema. A shell script `/solrconfig/config-solr.sh` is provided that authenticates against solr, and creates the new configuration. After logging into the solr-0 pod, this script can be run to create the configuration. It only needs to be run on one node, as the config values are created in zookeeper.

## Checking if SOLR is configured

Logging in using the SOLR_AUTHENTICATION_OPTS and SOLR_AUTH_TYPE env variables allows the `solr` command to be executed to check the server status:

```bash
$ export SOLR_AUTH_TYPE=basic
$ export SOLR_AUTHENTICATION_OPTS="-Dbasicauth=${SOLR_ADMIN_USERNAME}:${SOLR_ADMIN_PASSWORD}" 
$ solr status -z ${SOLR_ZK_HOSTS} -c ${SOLR_COLLECTION}

Found 1 Solr nodes:

Solr process 8 running on port 8983
{
  "solr_home":"/opt/bitnami/solr/server/solr",
  "version":"9.0.0 a4eb7aa123dc53f8dac74d80b66a490f2d6b4a26 - janhoy - 2022-05-05 01:00:08",
  "startTime":"2022-10-11T07:08:50.155Z",
  "uptime":"0 days, 0 hours, 21 minutes, 52 seconds",
  "memory":"70.9 MB (%13.8) of 512 MB",
  "cloud":{
    "ZooKeeper":"d1index-zookeeper:2181/solr",
    "liveNodes":"3",
    "collections":"1"}}
```

# SOLR Dashboard

Once the SOLR server is up and running, connect to the SOLR Dashboard by creating a kube proxy, and then browse to the local address:

```
k8 port-forward -n d1index service/d1index-solr 8983:8983 & 
echo "Solr URL: 127.0.0.1:8983/solr/"
```

You'll need to login with the helm-configured SOLR admin user and password.

Once the proxy is set up, you can also run API calls from the [ConfigSet API](https://solr.apache.org/guide/6_6/configsets-api.html) and [Collections API](https://solr.apache.org/guide/6_6/collections-api.html).

```
curl -u ${SOLR_ADMIN_USERNAME}:${SOLR_ADMIN_PASSWORD} http://localhost:8983/solr/admin/configs?action=CREATE\&name=dataone_index --header "Content-Type:text/xml" -X POST -d @dataone_index.zip
{
  "responseHeader":{
    "status":0,
    "QTime":5974}}
curl -u ${SOLR_ADMIN_USERNAME}:${SOLR_ADMIN_PASSWORD} http://localhost:8983/solr/admin/configs?action=list
curl -u ${SOLR_ADMIN_USERNAME}:${SOLR_ADMIN_PASSWORD} http://localhost:8983/solr/admin/collections?action=list
```


## History

This is a refactored version of the original DataONE [d1_cn_index_processor](https://github.com/DataONEorg/d1_cn_index_processor) that runs completely independently of other
DataONE Coordinating Node services. It is intended to be deployed in a Kubernetes cluster environment, but is written such 
that it can be deployed in other environments as well as needed.

