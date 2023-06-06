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

Configuration Changes
---------------------


Workflow changes:


Database Migration
------------------

You can find database upgrade scripts in `docs/upgrade/13_to_14/`. These scripts are suitable for both, MariaDB and
PostgreSQL. Changes include DB schema optimizations as well as fixes for the new workflow tables.

