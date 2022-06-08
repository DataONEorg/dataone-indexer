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

```bash
nerdctl build -t dataone-index-worker:2.4.0 -f docker/Dockerfile --build-arg TAG=2.4.0 .
```

## History

This is a refactored version of the original DataONE [d1_cn_index_processor](https://github.com/DataONEorg/d1_cn_index_processor) that runs completely independently of other
DataONE Coordinating Node services. It is intended to be deployed in a Kubernetes cluster environment, but is written such 
that it can be deployed in other environments as well as needed.

