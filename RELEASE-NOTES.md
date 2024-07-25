# dataone-indexer Release Notes

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
