# dataone-indexer Release Notes

## dataone-indexer version 3.1.3 & helm chart version 1.3.0

### Release date: 2025-04-28

### dataone-indexer version 3.1.3
* This is a patch release to update logging dependencies and fix a connectivity issue:
* [Restore disconnected RabbitMQ connections](https://github.com/DataONEorg/dataone-indexer/issues/176) -
  The fix in [version 3.1.2](#dataone-indexer-version-312--helm-chart-version-120) failed to fully
  resolve RabbitMQ disconnects under some circumstances, leading to the indexer failing to dequeue
  new jobs. We believe version 3.1.3 corrects the problem.
* Bump the log4j version from 2.17.1 to 2.24.3
* Bump slf4j-api from 1.6.1 to 2.0.17
* Bump slf4j-reload4j from 1.7.36 to 2.0.17

### helm chart version 1.3.0

* Update Bitnami RabbitMQ subchart to version 14.7.0 (RabbitMQ app version 3.13.7)
* [Issue 208: Introduce nameOverride and
  fullnameOverride for rabbitmq](https://github.com/DataONEorg/dataone-indexer/pull/208). These
  changes also enabled removal of the lifecycle postStart hook introduced in
  [chart 1.2.0](#dataone-indexer-version-312--helm-chart-version-120), and resolved the issue:
  * [Reduce RabbitMQ Startup Time](https://github.com/DataONEorg/dataone-indexer/issues/202)

> [!CAUTION]
> **ENSURE THAT THE RABBITMQ QUEUE IS EMPTY**, before upgrading or installing a new chart version
> for the first time, because each new chart version will store the queue on a newly-created PV/PVC!
>
> This applies only the initial installation or upgrade. After this, RabbitMQ will continue to use
> the same newly-created PV/PVC, and the queue will not be lost. The new PVC will be named:
> `data-[release-name]-rabbitmq-[rmq-version]-[idx]`, where `[rmq-version]` is the rabbitmq app
> version (not the chart version), with periods replaced by dashes, and `[idx]` is the statefulset
> ordinal index; e.g.: `data-metacatarctic-rabbitmq-3-13-7-0`
>
> When upgrade or installation is complete, you can then safely `kubectl delete` both the old PVC
> and the old PV (provided you're certain the queue was empty).

> [!NOTE]
> This behavior can be overridden by setting `.Values.rabbitmq.nameOverride` to the same name as the
> previous version, but this is **NOT recommended**, since the RabbitMQ installation then becomes an
> upgrade instead of a fresh install, and may require significant manual intervention; see:
> https://www.rabbitmq.com/docs/feature-flags#version-compatibility


* Reverted changes that previously [set k8s container resources requests & limits for index
  workers, and all subcharts](https://github.com/DataONEorg/dataone-indexer/issues/182), based on
 feedback and helm chart conventions.
  * Explicitly set `solr.javaMem: "-Xms512m -Xmx2g"` in `values.yaml`
* Bump indexer App version to 3.1.3
* [Ensure indexer subcharts use Bitnami Common > 2.9](https://github.com/DataONEorg/dataone-indexer/issues/206)


## dataone-indexer version 3.1.2 & helm chart version 1.2.0

* Release date: 2025-03-27
* **dataone-indexer version 3.1.2**
  * This is a patch release to update solr and fix some issues:
    * [Restore disconnected RabbitMQ connections](https://github.com/DataONEorg/dataone-indexer/issues/176)
      - resolves issue where RabbitMQ connections were not being restored after a disconnect,
        leading to the indexer failing to dequeue new jobs.
    * [Update solr library to v9.8.0](https://github.com/DataONEorg/dataone-indexer/issues/169)
    * [Increase solrVerConflictMaxTries](https://github.com/DataONEorg/dataone-indexer/issues/158)
      - increases the likelihood of recovery from version conflict issues
* **helm chart version 1.2.0**
  * Bump indexer App version to 3.1.2
  * Update base image to `eclipse-temurin:17.0.14_7-jre-noble`
    - uid `1000` already in use on this new image, so Dockerfile now creates and runs as uid/gid
      `59997` to match permissions on the shared metacat volume.
  * Update Bitnami Solr subchart to version 9.5.5 (Solr app version 9.8.1)
  * Update Bitnami RabbitMQ subchart to version 11.16.2 (RabbitMQ app version 3.11.18)
  * Add readiness probe that monitors RabbitMQ connection status
  * Add lifecycle postStart hook to enable all RabbitMQ feature flags, since this is now needed for
    future upgrades
    - see https://www.rabbitmq.com/blog/2022/07/20/required-feature-flags-in-rabbitmq-3.11
  * [Address deprecated rabbitmq health checks](https://github.com/DataONEorg/dataone-indexer/issues/163)
  * [Consolidate the 2 RMQ and Solr initContainers into
    one](https://github.com/DataONEorg/dataone-indexer/issues/183) for faster startup and less
    resource usage
  * [Set k8s container resources requests & limits for index workers, and all
    subcharts](https://github.com/DataONEorg/dataone-indexer/issues/182)
  * [Enable indexer-metacat-pv to Mount a Host Path](https://github.com/DataONEorg/dataone-indexer/issues/162)
  * Add `fsGroupChangePolicy: OnRootMismatch` to `podSecurityContext`


## dataone-indexer helm chart version 1.1.2

* Release date: 2025-02-24
  * Add [support for hostPath volume](https://github.com/DataONEorg/dataone-indexer/pull/164)
  * Increase `.Values.idxworker.solrVerConflictMaxTries` from 50 to 25000 to resolve solr
    version-conflict errors
  * (Application software version remains unchanged at 3.1.1)

## dataone-indexer version 3.1.1 & helm chart version 1.1.1

* Release date: 2024-12-17
* **dataone-indexer version 3.1.1**
  * This is a minor release to fix logging issues. Changes were made to the `slf4j` and `log4j`
    dependencies to ensure that log messages are written correctly
    (see [PR #155](https://github.com/DataONEorg/dataone-indexer/pull/155)).
* **helm chart version 1.1.1**
  * Bump Application version to 3.1.1
  * Change log4j properties configMap mount-point from `log4j.properties` to `log4j2.properties`

## dataone-indexer version 3.1.0 & helm chart version 1.1.0

* Release date: 2024-11-21
* **dataone-indexer version 3.1.0**
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
* **helm chart version 1.1.0**
  * Bump Application version to 3.1.0
  * Add `storage` to values.yaml for new hashstore integration

## dataone-indexer version 3.0.2 & helm chart version 1.0.2

* Release date: 2024-07-29
* **dataone-indexer version 3.0.2**
  * Bug fix - RabbitMQ Channel timeouts (PR #119)
* **helm chart version 1.0.2**
  * Bump Application version to 3.0.2
  * Make .Values.rabbitmq.auth.existingPasswordSecret a required value

## dataone-indexer version 3.0.1 & helm chart version 1.0.1

* Release date: 2024-07-08
* **dataone-indexer version 3.0.1**
  * Bump rmq amqp client to 5.21.0
  * Add healthcheck code
  * Exit app if unrecoverable exception occurs when started from `main()` method
* **helm chart version 1.0.1**
  * Change `.Values.idxworker.cn_url` to `.Values.global.d1ClientCnUrl`
  * Get `fullname` from metacat chart or provide in values.yaml
  * Add simple 'exec' liveness probe. Remove readiness probe

## dataone-indexer version 3.0.0 & helm chart version 1.0.0

* Release date: 2024-04-25
* **dataone-indexer version 3.0.0** -- first release of dataone-indexer
* **helm chart version 1.0.0** -- first release of helm chart
