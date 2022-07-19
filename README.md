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

The image can be built with either `docker` or `nerdctl` depending on which container environment you have installed. Here I show the example using
Racher Desktop configured to use `nerdctl`.

```bash
mvn clean package -DskipTests
nerdctl build -t dataone-index-worker:2.4.0 -f docker/Dockerfile --build-arg TAG=2.4.0 .
```

## Running the IndexWorker in the docker container

The docker image assumes that the deployment configuration file exists to configure endpoint addresses and credentials. To run the indexer, ensure that the 
`DATAONE_INDEXER_CONFIG` is set in the environment and contains the absolute path to the configuration file for the indexer. This path must be accessible in the container, so you will likely want to mount a volume to provide the edited properties file. You can then run it using a command like:

```bash
nerdctl run -it \
    -e DATAONE_INDEXER_CONFIG=/var/lib/dataone-indexer/dataone-indexer.properties \
    -v `pwd`/helm/config/dataone-indexer.properties:/var/lib/dataone-indexer/dataone-indexer.properties \
    dataone-index-worker:2.4.0
```

## History

This is a refactored version of the original DataONE [d1_cn_index_processor](https://github.com/DataONEorg/d1_cn_index_processor) that runs completely independently of other
DataONE Coordinating Node services. It is intended to be deployed in a Kubernetes cluster environment, but is written such 
that it can be deployed in other environments as well as needed.

