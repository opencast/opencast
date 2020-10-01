Database Configuration
======================

Opencast ships with embedded JDBC drivers for the H2, MySQL, MariaDB and PostgreSQL databases.
The built-in H2 database is used by default and needs no configuration,
but it is strongly recommended to use MariaDB for production.

> __H2__ is not supported for updates or distributed systems. Use it for testing only!


### Other databases

Running Opencast with PostgreSQL should be possible and there is some community support for this.
The support for this is unofficial and we cannot guarantee that every new feature is well tested on that platform.

The EclipseLink JPA implementation which is used in Opencast supports other databases as well and it should be
possible to attach other database engines.


Setting up MariaDB
------------------

Before following this guide, you should have:

- [Installed Opencast](../installation/index.md)
- Followed the [Basic Configuration instructions](basic.md)


### Installation

This step is not Opencast-specific and may be different depending on your scenario and system.
This shall act as an example and is assuming CentOS 8 as Linux distribution.
Look at your distributions documentation for setting up a database.

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


### Creating a Database

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
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE TEMPORARY TABLES ON opencast.*
  TO 'opencast'@'localhost' IDENTIFIED BY 'opencast_password';
```

These privileges are often not sufficient for running the scripts used to initialize and upgrade the database.
For this, fall back to using the root user or grant a user slightly more privileges:

```sql
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,DROP,INDEX,TRIGGER,CREATE TEMPORARY TABLES,REFERENCES ON opencast.*
  TO 'admin'@'localhost' IDENTIFIED BY 'opencast_admin_password';
```
</details>

You can choose other names for the users and the database, and you **should** use a different password.

In a distributed system, apart from `'username'@'localhost'` (which would allow access from the local machine only),
you should grant a external user access to the database by running the same command for a user like
`'username'@'10.0.1.%'`, where the `10.0.1.%` specifies the IP range allowed to access the server.
For more details on MariaDB user creation, have a look at [MariaDB Reference Manual :: `GRANT` statement
](https://mariadb.com/kb/en/mariadb/grant/)

Finally, leave the client and restart the database server to enable the new user(s):

```sh
% systemctl restart mariadb.service
```


### Set up the Database Structure

To set up the database structure you can (and should!) use the Opencast ddl scripts. You can find them in
`…/docs/scripts/ddl/mysql5.sql` or download them from GitHub.

To import the database structure using the MariaDB client, switch to the directory that contains the `mysql5.sql` file,
run the client with a user privileged to create the database structure and switch to the database you want to use
(e.g. `opencast`):

```sh
% mysql -u root -p opencast
```

Run the ddl script:

```
mysql> source mysql5.sql;
```

Now, ensure the MariaDB [`wait_timeout`](https://mariadb.com/kb/en/library/server-system-variables/) in `mariadb.cnf`
or `mysql.cnf` is bigger than `org.opencastproject.db.jdbc.pool.max.idle.time` in Opencast's `custom.properties`.
Raising the `max_connections` in `mariadb.cnf` parameter might be required, too, depending on your installation's size.
Reload the configuration into MariaDB, then connect to your database as user `opencast` and verify the values by
executing `SHOW VARIABLES LIKE %_timeout;`. A `MySQLNonTransientConnectionException`, for instance “A PooledConnection
that has already signaled a Connection error is still in use”, in your Opencast logs might indicate a problem with this
configuration.


### Configure Opencast

The following changes must be made in `etc/custom.properties`:

1. Change the following configuration key (uncomment if necessary):

        org.opencastproject.db.ddl.generation=false

    If set to true, the database structure will be generated automatically. It works, but without all the database
    optimizations implemented in the DDL scripts used in the step 2. While convenient for development, you should never
    set this to `true` in a production environment.

2. Configure Opencast to use MariaDB/MySQL:

        org.opencastproject.db.vendor=MySQL

3. Configure Opencast to use the JDBC driver for MariaDB/MySQL:

        org.opencastproject.db.jdbc.driver=com.mysql.jdbc.Driver

4. Configure the host where Opencast will find the database (`localhost`) and the database name (`opencast`). Adjust
the names in this example to match your configuration:

        org.opencastproject.db.jdbc.url=jdbc:mysql://localhost/opencast

5. Configure the username and password with which to access the database:

        org.opencastproject.db.jdbc.user=opencast
        org.opencastproject.db.jdbc.pass=opencast_password
