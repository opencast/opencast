Upgrading Opencast from 11.x to 12.x
====================================

This guide describes how to upgrade Opencast 11.x to 12.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Read the release notes (especially the section of behaviour changes)
4. Review the configuration changes and adjust your configuration accordingly
5. Do the [database migration](#database-migration)
6. Update MariaDB connection configuration
7. Clean up old indexes
8. Remove ActiveMQ
9. Start Opencast if you haven't already done so

Database Migration
------------------

Upgrading to Opencast 12 requires a database migration, as some tables have changed.
Migration scripts can be found in `doc/upgrade/11_to_12/`.
There are separate scripts for MariaDB and PostgreSQL.

<div class=warn>
TODO: Explain how to use the Python scripts.
</div>


MariaDB Connection Configuration
--------------------------------

This only applies if you use MariaDB or MySQL.

The syntax oof the JDBC connection configuration for MariaDB has slightly changed to an update of the MariaDB
Connector/J in `etc/custom.properties`, please use `jdbc:mariadb:` instead of `jdbc:mysql:`:

```properties
org.opencastproject.db.jdbc.url=jdbc:mariadb://localhost/opencast?useMysqlMetadata=true
```


Optional: Delete Workflow Service Solr Index
-----------------------

Starting with Opencast 12, the workflow and series services do not store data in Solr anymore.
Therefore, the index can be deleted to save on disk space.
Unless you setup Solr yourself, the index can likely be found under
`/var/lib/opencast/solr-indexes/workflows/` and
`/var/lib/opencast/solr-indexes/series/`.

<div class=warn>
TODO: Explain where (on which nodes) these are located exactly.
</div>


Optional: Remove ActiveMQ
-------------------------

Starting with version 12, Opencast does no longer require ActiveMQ.
You can stop and remove the service from your Opencast cluster unless you need it for anything else.
