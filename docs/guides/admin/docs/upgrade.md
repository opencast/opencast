Upgrading Opencast 2.3 To 2.4
=============================

This guide describes how to upgrade Opencast 2.3.x to 2.4.x. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).


How to Upgrade
--------------

1. Download Opencast 2.4
2. Stop your current Opencast instance
3. Back-up Opencast files and database (optional)
4. [Upgrade the database](#database-migration)
5. Update the third party tools
6. Replace Opencast 2.3 with 2.4
7. Review the configuration changes and adjust your configuration accordingly


Database Migration
------------------

Opencast 2.4 includes the following database changes:

1. Support for OAI-PMH (MH-12013)
2. Fix for a mis-named role name for the External API (MH-12015)

It should be needless to say that this migration should not take a lot of time and should be safe. Nevertheless, as with
all database migrations, we recommend to make a database backup before attempting the upgrade.

You can find the database upgrade script at `…/docs/upgrade/2.3_to_2.4/mysql5.sql`.

