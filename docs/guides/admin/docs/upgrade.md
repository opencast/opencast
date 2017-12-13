Upgrading Opencast 3.0 To 4.0
=============================

This guide describes how to upgrade Opencast 3.0.x to 4.0.x. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).


How to Upgrade
--------------

1. Stop your current Opencast instance
2. Back-up Opencast files and database (optional)
3. [Upgrade the database](#database-migration)
4. Replace Opencast 3.0 with 4.0
6. Review the [configuration changes](releasenotes#working-file-repository-configuration) and adjust your configuration accordingly
7. [Upgrade the ActiveMQ configuration](#activemq-migration)
8. [Migrate the scheduler service](#scheduler-migration)
9. [Re-build ElasticSearch index](#re-build-elasticsearch-index)
10. Drop the old scheduler database table: `DROP TABLE mh_scheduled_event;`
11. Delete the old scheduler Solr data directory (`data/solr-indexes/scheduler`)


Database Migration
------------------

Opencast 4.0 includes database changes for the new asset manager and scheduler.  As with all database migrations, we
recommend to make a database backup before attempting the upgrade.

You can find the database upgrade script at `…/docs/upgrade/3.0_to_4.0/mysql5.sql`.

Additionally, you need to ensure your Opencast user is granted all necessary rights. Additional to previous
versions, `GRANT CREATE TEMPORARY TABLES` is required as well. You can simply re-set your users following the user
set-up step from the [database configuration guide](configuration/database/#step-1-create-an-opencast-database)


ActiveMQ Migration
------------------

Opencast 4.0 needs a new ActiveMQ message broker configuration. Please follow the steps of the [message broker
configuration guide](configuration/message-broker/) to deploy a new configuration. No data migration is required for
this since the message broker only contains temporary data.


Scheduler Migration
-------------------

The new scheduler service adds support for extended metadata and is bound to the new asset manager. Data from the old
scheduler need to be migrated if Opencast contains upcoming, scheduled events. If you do not have any upcoming,
scheduled events in your system, you can safely skip this step.

The migration of the scheduler service uses the data of the old scheduling database table which therefore is not deleted
by the database migration script.

To start the migration follow these steps:

1. Delete the existing indexes in `data/index` and `data/solr-indexes/scheduler`
2. Configure the destination organization for the migration by configuring the organizational context in the
   `custom.properties`. Do that by adding a configuration key like:
   `org.opencastproject.migration.organization=mh_default_org`.
3. Start Opencast
4. Check the logs for errors!
5. After the migration is done, remove `org.opencastproject.migration.organization` again from `custom.properties´ to
   avoid further migration attempts.


Re-Build ElasticSearch Index
----------------------------

The introduction of the new scheduler service requires an update to the ElasticSearch index:

1. If you did not do that during the scheduler migration, delete the index directory at `data/index`
2. Start Opencast if you have not already, waiting until it has started completely.
3. Use one of the following methods to recreate the index:

    - Make an HTTP POST request to `/admin-ng/index/recreateIndex` using your browser or an alternative HTTP client.
    - Open the REST documentation, which can be found under the “Help” section in the Admin UI (by clicking on the “?”
      symbol at the top right corner). Then go to the “Admin UI - Index Endpoint” section and use the testing form on
      `/recreateIndex`.

    In both cases, the resulting page is empty but should return a HTTP status 200.

4. If you are going to use the External API, then the corresponding ElasticSearch index must also be recreated:

    - Make an HTTP POST request to `/api/recreateIndex` using your browser or an alternative HTTP client.
    - Open the REST documentation, which can be found under the “Help” section in the Admin UI (by clicking on the “?”
      symbol at the top right corner). Then go to the “External API - Base Endpoint” section and use the testing form on
      `/recreateIndex`.

