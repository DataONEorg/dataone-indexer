# dataone-indexer Release Notes

## dataone-indexer version 3.1.0 & helm chart version 1.1.0

* Release date: 2024-11-21
* dataone-indexer version 3.1.0
  * Integrate with the new Dataone hash-based storage library
    [`hashstore-java`](https://github.com/DataONEorg/hashstore-java).
    * Indexer no longer needs access to an aut token in order to index private datasets.
  * Update Docker base image to eclipse-temurin:17.0.12_7-jre-jammy
  * Upgrade log4j-core to 2.24.0 to fix "method can't be found" issue
  * Bump dependencies:
    * org.apache.commons:commons-lang3 from 3.4 to 3.17.0
    * org.slf4j:slf4j-api from 1.7.36 to 2.0.16
    * org.springframework.data:spring-data-commons from 1.6.5.RELEASE to 3.3.4
    * org.apache.maven.plugins:maven-compiler-plugin from 2.0.1 to 3.13.0
    * com.coderplus.maven.plugins:copy-rename-maven-plugin from 1.0 to 1.0.1
    * org.apache.logging.log4j:log4j-jcl from 2.17.1 to 2.24.0
    * org.apache.maven.plugins:maven-clean-plugin from 3.2.0 to 3.4.0
    * com.fasterxml.jackson.core:jackson-annotations from 2.13.3 to 2.18.0
* helm chart version 1.0.2
  * Bump Application version to 3.1.0
  * Add `storage` to values.yaml for new hashstore integration

## dataone-indexer version 3.0.2 & helm chart version 1.0.2

* Release date: 2024-07-29
* dataone-indexer version 3.0.2
  * Bug fix - RabbitMQ Channel timeouts (PR #119)
* helm chart version 1.0.2
  * Bump Application version to 3.0.2
  * Make .Values.rabbitmq.auth.existingPasswordSecret a required value

## dataone-indexer version 3.0.1 & helm chart version 1.0.1

* Release date: 2024-07-08
* dataone-indexer version 3.0.1
  * Bump rmq amqp client to 5.21.0
  * Add healthcheck code
  * Exit app if unrecoverable exception occurs when started from `main()` method
* helm chart version 1.0.1
  * Change `.Values.idxworker.cn_url` to `.Values.global.d1ClientCnUrl`
  * Get `fullname` from metacat chart or provide in values.yaml
  * Add simple 'exec' liveness probe. Remove readiness probe

## dataone-indexer version 3.0.0 & helm chart version 1.0.0

* Release date: 2024-04-25
* dataone-indexer version 3.0.0 -- first release of dataone-indexer
* helm chart version 1.0.0 -- first release of helm chart
