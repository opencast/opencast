Upgrading Opencast from 11.x to 11.x
===================================

This guide describes how to upgrade Opencast 11.x to 12.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Read the release notes (especially the section of behaviour changes)
4. Review the configuration changes and adjust your configuration accordingly
5. Do the [database migration](#database-migration)
7. Start Opencast if you haven't already done so

Database Migration
------------------

Upgrading to Opencast 12 requires a DB migration, as some tables have changed slightly.
Migration scripts can be found in `doc/upgrade/11_to_12/`.
There are separate scripts for MariaDB/MySQL (`mysql5.sql`) and PostgreSQL (`postgresql.sql`).

Optional: Delete Workflow Service Solr Index
-----------------------

Starting with Opencast 12, the Workflow Service does not store data in its Solr Index anymore. Therefore, the index can
be deleted to save on disk space. Unless you setup Solr yourself, the index can likely be found under
 `/var/lib/opencast/solr-indexes/workflows/`.
