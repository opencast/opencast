Upgrading Opencast 3.0 To 4.0
=============================

This guide describes how to upgrade Opencast 3.0.x to 4.0.x. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).


How to Upgrade
--------------

1. Download Opencast 4.0
2. Stop your current Opencast instance
3. Back-up Opencast files and database (optional)
4. [Upgrade the database](#database-migration)
5. Update the third party tools
6. Replace Opencast 3.0 with 4.0
7. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
8. \* [Migrate the scheduler service](#migrate-scheduler-service)
9. Drop the old scheduler DB table `DROP TABLE mh_scheduled_event;` and delete the old scheduler solr data by removing the scheduler solr directory.
10. [Re-build the search indexes](#re-build-search-indexes)

\* This step is optional and only needed if you want to migrate old scheduler data.


Database Migration
------------------

Opencast 4.0 includes the following database changes:

1. AssetManager and Scheduler migration (MH-12082)

It should be needless to say that this migration should not take a lot of time and should be safe. Nevertheless, as with
all database migrations, we recommend to make a database backup before attempting the upgrade.

You can find the database upgrade script at `…/docs/upgrade/3.0_to_4.0/mysql5.sql`.

Standalone scheduler migration
------------------------------

The new scheduler service adds support for extended metadata and is bound to the new AssetManager.

If you're setting up Opencast from scratch you can safely skip this document but if you plan to upgrade your system
and continue to use your data you should read on.

Configuration Changes
---------------------

Opencast 4.0 has following configuration changes:

1. The Working File Repository URL can be set per tenant (due to [MH-12104](https://opencast.jira.com/browse/MH-12104))

The Configuration key `org.opencastproject.file.repo.url` was moved from `etc/custom.properties` to
`etc/org.opencastproject.organization-mh_default_org.cfg` as `prop.org.opencastproject.file.repo.url`.
The fallback value is same as before (set to `${org.opencastproject.server.url}`).
On a multiple server setup the value should be same on all nodes.
For more information read the [Configure Opencast](installation/multiple-servers/#step-5-configure-opencast) section.

2. WOH snapshot vs archive (due to [MH-12082](https://opencast.jira.com/browse/MH-12082))

The workflow operation handler *archive* has been renamed to *snapshot*. Besides its new name, the workflow operation
handler *snapshot* is fully compatible to *archive*. You will need, however, to adapt the name in your workflow
definition.

3. Asset Manager Configuration (due to [MH-12082](https://opencast.jira.com/browse/MH-12082))

There are two new optional configuration properties `org.opencastproject.episode.rootdir` and
`org.opencastproject.storage.dir` in `custom.properties` that you might want to have a look at.
Those properties are described in the [Asset Manager Configuration](configuration/asset-manager/)

4. ActiveMQ (due to [MH-12082](https://opencast.jira.com/browse/MH-12082))

Please don't forget to update your activemq.xml to the version provided with this release.

Migrate Scheduler Service
-------------------------

The migration of the scheduler service uses the data of the old scheduling database table which hasn't been deleted by the DB migration script.

1. Configure the destination organization by adding the [migration configuration](#migration-configuration) to the `custom.properties`.
2. Start Opencast using the interactive script in `bin/start-opencast`
3. Log-in to the Karaf console on your node where the scheduler service is running (usually admin node) and install the opencast-migration feature by entering: `feature:install opencast-migration`
4. Check the logs for errors!
5. Restart Opencast service - you do not need to use the interactive start script.

#### Migration Configuration
```
# Organizational context in which the harvest should run.
organization=mh_default_org
```


Re-Build Search Indexes
-----------------------

### Elasticsearch Index

The introduction of the new scheduler service requires an update to the Elasticsearch index:

1. Delete the index directory at `…/data/index`.
2. Restart Opencast and wait until the system is fully started.
3. Use one of the following methods to recreate the index:

    - Make an HTTP GET request to `/admin-ng/index/recreateIndex` using your browser or an alternative HTTP client.
    - Open the REST documentation, which can be found under the “Help” section in the Admin UI (by clicking on the “?”
      symbol at the top right corner). Then go to the “Admin UI - Index Endpoint” section and use the testing form on
      `/recreateIndex`.

    In both cases, the resulting page is empty but should return a HTTP status 200.
