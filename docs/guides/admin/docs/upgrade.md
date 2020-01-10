
Upgrading Opencast from 8.x to 9.x
==================================

This guide describes how to upgrade Opencast 8.x to 9.x. In case you need information about how to upgrade older
versions of Opencast, please refer to [older release notes](https://docs.opencast.org).

Configuration Changes
---------------------

1. The dispatch interval property is now called `dispatch.interval` and expects seconds instead of milliseconds
   `etc/org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.cfg`.
2. The file `etc/elasticsearch.yml` was removed as it is no longer required due to the removal of the embedded
   Elasticsearch node.
3. The file `etc/custom.properties` was changed:
   - The property `org.opencastproject.elasticsearch.server.address` was renamed
   to `org.opencastproject.elasticsearch.server.hostname` and now defaults to `localhost` (was: `127.0.0.1`).
   - The property `org.opencastproject.elasticsearch.server.port` now defaults to `9200` (was: `9300`).
   - A new property `org.opencastproject.elasticsearch.server.scheme` was added and defaults to `http`.

How to Upgrade
--------------
1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Back-up Opencast files and database (optional)
4. Upgrade the database
5. [Install and configure a standlone Elasticsearch node](#install-and-confifure-a-standalone-elasticsearch-node)
6. [Review the configuration changes and adjust your configuration accordingly](#configuration-changes)
7. Remove search index data folder
8. Start Opencast
9. [Rebuild the Elasticsearch indexes](#rebuild-the-elasticsearch-indexes)

Install and configure a standalone Elasticsearch node
-----------------------------------------------------

In the past, Opencast came with its own integrated Elasticsearch node. However, recent versions of Elasticsearch no longer
support to be embedded in applications. Since the Elasticsearch client was updated to version 7, Opencast now requires an
external Elasticsearch node of the same version to be present. This means, that all Opencast adopters now have to run
Elasticsearch.

Please check [the documentation](modules/searchindex/elasticsearch.md) for information about how to setup an external node.

If you already used an external Elasticsearch node in the past, please update your node to version 7. Since the index
schema has changed, you will need to drop you indices and [rebuild them](#rebuild-the-elasticsearch-indexes).

Rebuild the Elasticsearch Indexes
----------------------------------

In order to populate the external Elasticsearch index, an index rebuild is necessary.

### Admin Interface

Stop Opencast, delete the index directory at `data/index`, restart Opencast and make an HTTP POST request to
`/admin-ng/index/recreateIndex`.

Example (using cURL):

    curl -i --digest -u <digest_user>:<digest_password> -H "X-Requested-Auth: Digest" -s -X POST \
      https://example.opencast.org/admin-ng/index/recreateIndex

You can also just open the REST documentation, which can be found under the “Help” section in the admin interface (the
“?” symbol at the top right corner). Then go to the “Admin UI - Index Endpoint” section and use the testing form on
`/recreateIndex` to issue a POST request.

In both cases you should get a 200 HTTP status.

### External API

If you are using the External API, then also trigger a rebuilt of its index by sending an HTTP POST request to
`/api/recreateIndex`.

Example (using cURL):

    curl -i --digest -u <digest_user>:<digest_password> -H "X-Requested-Auth: Digest" -s -X POST \
      https://example.opencast.org/api/recreateIndex

You can also just open the REST documentation, which can be found under the “Help” section in the admin interface (the
“?” symbol at the top right corner). Then go to the “External API - Base Endpoint” section and use the testing form on
`/recreateIndex`.

In both cases you should again get a 200 HTTP status.
