Upgrading Opencast 2.2 To 2.3
=============================

This guide describes how to upgrade Opencast 2.2.x to 2.3.x. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).


How to Upgrade
--------------

1. Download Opencast 2.3
2. Stop your current Opencast instance
3. Back-up Opencast files and database (optional)
4. [Upgrade the database](#database-migration)
5. [Upgrade the message broker configuration](#upgrade-message-broker-configuration)
7. Update the third party tools
5. Replace Opencast 2.2 with 2.3
6. Review the configuration changes and adjust your configuration accordingly
8. [Re-build the search indexes](#re-build-search-indexes)


Database Migration
------------------

Opencast 2.3 comes with a slightly modified database scheme compared to Opencast 2.2, essentially allowing comments to
be longer than 255 characters. While this might seem a rather unimportant change, we highly recommend to apply this
change to your existing database to stay in sync with the community versions, avoiding problems with further migrations.

It should be needless to say that this migration should not take a lot of time and should be safe. Nevertheless, as with
all database migrations, we recommend to make a database backup before attempting the upgrade.

You can find the database upgrade script at `…/docs/upgrade/2.1_to_2.2/mysql5.sql`.


Upgrade Message Broker Configuration
------------------------------------

The message broker configuration has been changed and must be updated. You can find the latest configuration at
`docs/scripts/activemq/activemq.xml`.

Please make sure to adapt the configuration to your needs, adding access control rules and configuring the hosts to
listen to as necessary. For more information, have a look at the [message broker configuration guide
](configuration/message-broker.md).


Re-Build Search Indexes
-----------------------

### Search-Service Solr Index

A security issue in the search service, which could potentially allow some
users to view private recordings without the proper access rights, was revealed
and fixed in 2.2.4.

If you update from a version lower than 2.2.4, we recommend to re-build the
search index. An automatic rebuild can be triggered by just deleting the Solr
search index directory at `…/data/solr-indexes/search`. The index will populate
itself again from the database the next time Opencast is started.

If you have already upgraded to 2.2.4 and you rebuilt the search index then,
you need not do it again.


### Elasticsearch Index

The introduction of the new external API requires an update to the Elasticsearch index:

1. Delete the index directory at `…/data/index`.
2. Restart Opencast and wait until the system is fully started.
3. Use one of the following methods to recreate the index:

    - Make an HTTP GET request to `/admin-ng/index/recreateIndex` using your browser or an alternative HTTP client.
    - Open the REST documentation, which can be found under the “Help” section in the Admin UI (by clicking on the “?”
      symbol at the top right corner). Then go to the “Admin UI - Index Endpoint” section and use the testing form on
      `/recreateIndex`.

    In both cases, the resulting page is empty but should return a HTTP status 200.
