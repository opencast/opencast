Upgrading Opencast from 11.x to 12.x
====================================

This guide describes how to upgrade Opencast 11.x to 12.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Read the release notes (especially the section of behaviour changes)
4. Review the configuration changes and adjust your configuration accordingly
5. [Migrate the database](#database-migration)
6. [Update MariaDB connection configuration](#mariadb-connection-configuration)
7. [Clean up old indexes](#optional-delete-solr-index)
8. [Remove ActiveMQ](#optional-remove-activemq)
9. Start Opencast

Database Migration
------------------

Upgrading to Opencast 12 requires a database migration as some tables have changed.
Migration scripts can be found in
[`doc/upgrade/11_to_12/`](https://github.com/opencast/opencast/tree/r/12.x/docs/upgrade/11_to_12).
There are separate scripts for MariaDB and PostgreSQL.

<div class=warn>
Back-up your database before starting the migration.
</div>

To start the migration, first make sure you have Python ≥ 3.6 installed on your system.
You can run these scripts from any of your systems as long as it can establish a connection to your database.
You only have to run these scripts once

To upgrade, choose the script for the database type you are running,
either `workflows_postgresql.py` or `workflows_mariadb.py`
and open the file with an editor. Adjust the connection settings located near the top of the file:

```py
# Vars
user = "opencast"
password = "dbpassword"
host = "127.0.0.1"
database = "opencast"
```

Next, install the Python dependencies listed at the top of the migration script you want to use.
You can do this in a virtual Python environment if you do not want to install them globally:

```sh
python3 -m venv venv
. ./venv/bin/activate
# you might need to have a MariaDB/PostgrSQL client installed
pip install psycopg2-binary
# or for MariaDB
pip install mysql-connector-python
```

Alternatively to using `pip`, use your system's package manager, e.g.:

```
dnf install python3-psycopg2
```

Finally, run the script to start the migration.
Depending on the amount of workflows in your system, this might take some time:

```sh
# PostgreSQL
python3 workflows_postgresql.py
# MariaDB
python3 workflows_mariadb.py
```

Wait for this to finish before starting Opencast.


MariaDB Connection Configuration
--------------------------------

This only applies if you use MariaDB or MySQL.

The syntax of the JDBC connection configuration for MariaDB has slightly changed to an update of the MariaDB
Connector/J in `etc/custom.properties`, please use `jdbc:mariadb:` instead of `jdbc:mysql:`:

```properties
org.opencastproject.db.jdbc.url=jdbc:mariadb://localhost/opencast?useMysqlMetadata=true
```


Optional: Delete Solr Index
---------------------------

Starting with Opencast 12, the workflow and series services do not store data in Solr anymore.
Therefore, the index can be deleted to save on disk space.
Do _not_ delete the search Solr index.
Unless you setup Solr yourself, the index should be located on the following nodes and paths:

- The workflow Solr index was located on the admin or allinone nodes at
  `/var/lib/opencast/solr-indexes/workflows/`
- The series Solr index was located on the admin or allinone nodes at
  `/var/lib/opencast/solr-indexes/series/`.


Optional: Remove ActiveMQ
-------------------------

Starting with version 12, Opencast does no longer require ActiveMQ.
You can stop and remove the service from your Opencast cluster unless you need it for anything else.

For example, to remove this on Debian or CentOS, run something like:

```sh
# Debian, Ubuntu, …
apt purge activemq-dist

# RHEL, CentOS, …
dnf remove activemq-dist

# Old RHEL, CentOS, …
yum remove activemq-dist
```

You can also potentially remove old data from:

```
/etc/activemq*
/var/lib/activemq
```
