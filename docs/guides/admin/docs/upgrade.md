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
7. Review the configuration changes and adjust your configuration accordingly
8. [Re-build the search indexes](#re-build-search-indexes)


Database Migration
------------------

Opencast 4.0 includes the following database changes:

1. AssetManager migration (MH-12082)

It should be needless to say that this migration should not take a lot of time and should be safe. Nevertheless, as with
all database migrations, we recommend to make a database backup before attempting the upgrade.

You can find the database upgrade script at `…/docs/upgrade/3.0_to_4.0/mysql5.sql`.
