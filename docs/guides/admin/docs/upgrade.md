Upgrading Opencast from 6.x to 7.x
==================================

This guide describes how to upgrade Opencast 6.x to 7.x. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).

How to Upgrade
--------------

1. Stop your current Opencast instance
2. Replace Opencast 6.x with 7.x
3. Back-up Opencast files and database (optional)
4. [Upgrade the database](#database-migration)
5. [Upgrade the ActiveMQ configuration](#activemq-migration)
6. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly


Database Migration
------------------

As part of performance optimizations, a foreign key constraint was added to one table. This requires a database schema
update. As with all database migrations, we recommend to create a database backup before attempting the upgrade.

You can find the database upgrade script in `docs/upgrade/6_to_7/`. This script is suitable for both, MariaDB and
MySQL.


ActiveMQ Migration
------------------

*So gar, this is not required*


Configuration Changes
---------------------

- *TODO*
