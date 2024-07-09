# dataone-indexer Release Notes

## dataone-indexer version 3.0.1 & helm chart version 1.0.1

* Release date: 2024-07-08
* helm chart version 1.0.1
  * Change `.Values.idxworker.cn_url` to `.Values.global.d1ClientCnUrl`
  * Get `fullname` from metacat chart or provide in values.yaml
  * Add simple 'exec' liveness probe. Remove readiness probe
* dataone-indexer version 3.0.1
  * Bump rmq amqp client to 5.21.0
  * Add healthcheck code
  * Exit app if unrecoverable exception occurs when started from `main()` method

## dataone-indexer version 3.0.0 & helm chart version 1.0.0

* Release date: 2024-04-30
* helm chart version 1.0.0 -- first release of helm chart
* dataone-indexer version 3.0.0 -- first release of dataone-indexer
