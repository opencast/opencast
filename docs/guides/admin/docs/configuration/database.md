Database Configuration
======================

Opencast ships with embedded JDBC drivers for the H2, MySQL, MariaDB and PostgreSQL databases.
The built-in H2 database is used by default and needs no configuration,
but is not suited for production.

> __H2__ is not supported for updates or distributed systems. Use it for testing only!


### Requirements

Before following this guide, you should have:

- [Installed Opencast](../installation/index.md)
- Followed the [Basic Configuration instructions](basic.md)


Step 1: Select a Database
-------------------------

The EclipseLink JPA implementation which is used in Opencast supports several different databases, although
some databases might require additional drivers.
Official support only exists for MariaDB, MySQL, PostgreSQL and H2.
Other database engines are not tested and specific issues will likely not be addressed.

- __MariaDB__ is the recommended database engine.
  It is used by most adopters and is well tested.
- __MySQL__ is supported but tested less than MariaDB.
- __PostgreSQL__ support is experimental.
- __H2__ is not suitable for anything but testing and development.
  It cannot be used in distributed environments.

Step 2: Set up the Database
---------------------------

This step is not Opencast-specific and may be different depending on your scenario and system.
The following is an example of database setup using MariaDB, followed by an example for PostgreSQL, and is assuming CentOS 8 as Linux distribution.
Look at your distribution's documentation for setting up a database.

### MariaDB

Install and start MariaDB:

```sh
% dnf install mariadb mariadb-server
% systemctl start mariadb.service
% systemctl enable mariadb.service
```

Finally, set root user credentials by running

```sh
% mysql_secure_installation
```



The first step is to create a database for Opencast.
You can use any other database client, e.g. phpMyAdmin, for this as well.

```sh
% mysql -u root -p
```

You will be asked for the password of the user root.
Next, create a database called `opencast` by executing:

```sql
CREATE DATABASE opencast CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Then create a user `opencast` with a password and grant it all necessary rights:

```sql
GRANT ALL PRIVILEGES ON opencast.* TO 'opencast'@'localhost' IDENTIFIED BY 'opencast_password';
```

<details>

<summary>Limiting the granted privileges</summary>

You can limit the granted privileges further if you want to.
The rights granted here are sufficient to run Opencast:

```sql
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,DROP,INDEX,TRIGGER,CREATE TEMPORARY TABLES,REFERENCES ON opencast.*
  TO 'admin'@'localhost' IDENTIFIED BY 'opencast_password';
```
</details>

You can choose other names for the users and the database, and you should use a different password.

In a distributed system, apart from `'username'@'localhost'` (which would allow access from the local machine only),
you should grant a external user access to the database by running the same command for a user like
`'username'@'10.0.1.%'`, where the `10.0.1.%` specifies the IP range allowed to access the server.
For more details on MariaDB user creation, have a look at [MariaDB Reference Manual :: `GRANT` statement
](https://mariadb.com/kb/en/mariadb/grant/)

Finally, leave the client and restart the database server to enable the new user(s):

```sh
% systemctl restart mariadb.service
```

### PostgreSQL

Opencast's official PostgreSQL support is still marked as experimental.

Install PostgreSQL, create a database and a user.
You may need to enable password authentication in your `pg_hba.conf` first.
Please refer to the PostgreSQL documentation for more details.

```
sudo -u postgres psql
postgres=# create database opencast;
postgres=# create user opencast with encrypted password 'opencast_password';
postgres=# grant all privileges on database opencast to opencast;
```


Step 4: Configure Opencast
--------------------------

The following changes must be made in `etc/custom.properties`.
Examples are provided for MariaDB/MySQL and PostgreSQL.

1. Configure Opencast to use the JDBC driver for MariaDB or PostgreSQL.
   The MariaDB driver will also work for MySQL.

        # MariaDB/MySQL
        org.opencastproject.db.jdbc.driver=org.mariadb.jdbc.Driver
        # PostgreSQL
        org.opencastproject.db.jdbc.driver=org.postgresql.Driver

2. Configure the host where Opencast will find the database (`127.0.0.1`) and the database name (`opencast`).

        # MariaDB/MySQL
        org.opencastproject.db.jdbc.url=jdbc:mysql://127.0.0.1/opencast?useMysqlMetadata=true
        # PostgreSQL
        org.opencastproject.db.jdbc.url=jdbc:postgresql://127.0.0.1/opencast


3. Configure the database username and password.

        org.opencastproject.db.jdbc.user=opencast
        org.opencastproject.db.jdbc.pass=opencast_password


Step 5: OAI-PMH Database (optional)
-----------------------------------

The database tables are automatically generated by Opencast when they are needed.
One exception to this is the OAI-PMH publication database which requires an additional trigger.
Trying to generate the schema automatically will most likely fail.

If you want to use OAI-PMH, you must create the necessary table manually.

Use the following code to generate the OAI-PMH database table on MariaDB/MySQL.
PostgreSQL is not yet supported.

```sql
CREATE TABLE oc_oaipmh (
  mp_id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  repo_id VARCHAR(255) NOT NULL,
  series_id VARCHAR(128),
  deleted tinyint(1) DEFAULT '0',
  modification_date DATETIME DEFAULT NULL,
  mediapackage_xml TEXT(65535) NOT NULL,
  PRIMARY KEY (mp_id, repo_id, organization),
  CONSTRAINT UNQ_oc_oaipmh UNIQUE (modification_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_oc_oaipmh_modification_date ON oc_oaipmh (modification_date);

-- set to current date and time on insert
CREATE TRIGGER oc_init_oaipmh_date BEFORE INSERT ON `oc_oaipmh`
FOR EACH ROW SET NEW.modification_date = NOW();

-- set to current date and time on update
CREATE TRIGGER oc_update_oaipmh_date BEFORE UPDATE ON `oc_oaipmh`
FOR EACH ROW SET NEW.modification_date = NOW();
```
