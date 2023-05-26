Upgrading Opencast from 13.x to 14.x
====================================

This guide describes how to upgrade Opencast 13.x to 14.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Read the [release notes](releasenotes.md) (especially the section of behaviour changes)
4. [Review the configuration changes and adjust your configuration accordingly](#configuration-changes)
5. [Migrate the database](#database-migration)
6. Start Opencast
7. [Rebuild the Elasticsearch indexes](#rebuild-the-elasticsearch-indexes)

Configuration Changes
---------------------


Workflow changes:


Database Migration
------------------

You can find database upgrade scripts in `docs/upgrade/13_to_14/`. These scripts are suitable for both, MariaDB and
PostgreSQL. Changes include DB schema optimizations as well as fixes for the new workflow tables.

Rebuild the Elasticsearch Indexes
----------------------------------

The 14.0 release contains multiple changes to the Elasticsearch indexes an requires a rebuild.

Start your new Opencast and make an HTTP POST request to `/index/rebuild`.

Example (using cURL):

    curl -i -u <admin_user>:<password> -s -X POST https://example.opencast.org/index/rebuild

You can also just open the REST documentation, which can be found under the “Help” section in the admin interface (the
“?” symbol at the top right corner). Then go to the “Index Endpoint” section and use the testing form on
`/rebuild` to issue a POST request.

In both cases you should get a 200 HTTP status.
