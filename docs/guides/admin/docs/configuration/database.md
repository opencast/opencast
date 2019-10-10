Database Configuration
======================

Opencast ships with embedded JDBC drivers for the H2, MySQL and MariaDB databases. The built-in H2 database is used by
default and needs no configuration, but it is strongly recommended to use MariaDB for production.
performance gain.

> **Notice:** H2 is neither supported for updates, nor for distributed systems. Use it for testing only!


### Other databases

Running Opencast with PostgreSQL should be possible and there is some community support for this. While it should work,
the support for this is unofficial and we cannot guarantee that every new feature is well tested on that platform.

The EclipseLink JPA implementation which is used in Opencast supports other databases as well and it should be
possible to attach other database engines.

Setting up MariaDB/MySQL
------------------------

### Requirements

Before following this guide, you should have:

* Installed the Opencast Core System
* Followed the [Basic Configuration instructions](basic.md)


### Step 0: Set-up MariaDB/MySQL

This step is not Opencast-specific and may be different depending on your scenario (e.g. if you want to have a dedicated
database server). It shall only be a guide for people with no experience setting up MariaDB/MySQL to help them get
started.  MariaDB is used for this guide but if your distribution includes MySQL instead, the installation should be
very much the same.

First, install the MariaDB server. On RedHat-based systems, use:

    yum install mariadb mariadb-server

Afterward, start the server and set it up to start automatically after each reboot:

    systemctl start mariadb.service
    systemctl enable mariadb.service

Now you have MariaDB running, but without a properly configured root account (no password, etc.) which might pose a
security risk. MariaDB includes a useful tool to secure your database server. You can launch it by executing (yes, it is
still called mysql…):

    mysql_secure_installation

It will guide you through the steps of setting up a root account with password, etc.


### Step 1: Create an Opencast Database

The first step, if you have not already done this, is to create a database for Opencast. You can use the following SQL
code to to that. For executing the SQL, use the MariaDB/MySQL client (run `mysql` from your shell) or use a graphical
tool like phpMyAdmin. For now, we will use the MySQL shell client and the default administrative (root) user. Launch the
client with:

    mysql -u root -p

You will be asked for the password of the user root. When logged in, you will end up in the MariaDB/MySQL shell.  Next,
create a database called `opencast` by executing:

    CREATE DATABASE opencast CHARACTER SET utf8 COLLATE utf8_general_ci;

Then create a user `opencast` with a password and grant it all necessary rights:

    GRANT SELECT,INSERT,UPDATE,DELETE,CREATE TEMPORARY TABLES ON opencast.*
      TO 'opencast'@'localhost' IDENTIFIED BY 'opencast_password';

The rights granted here are all that is needed to *run* Opencast. To execute the migration scripts
used to initialize (see next section) and upgrade the database schema upon releases of new versions
of Opencast, you need more. If you don't want to do this using the `root` user (which normally
can do anything), but with a dedicated user called `admin` for the sake of the example,
you should grant that user the following rights:

    GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,DROP,INDEX,TRIGGER,CREATE TEMPORARY TABLES,REFERENCES ON opencast.*
      TO 'admin'@'localhost' IDENTIFIED BY 'opencast_admin_password';

You can choose other names for the users and the database, and you **should** use a different password.

In a distributed system, apart from `'username'@'localhost'` (which would allow access from the local machine only),
you should grant a external user access to the database by running the same command for a user like
`'username'@'10.0.1.%'`, where the `10.0.1.%` specifies the IP range allowed to access the server with `%` being a
wildcard for "anything". For more details on MariaDB/MySQL user creation have a look at any of the following links:

* [MariaDB Reference Manual :: `GRANT` statement](https://mariadb.com/kb/en/mariadb/grant/)
* [MySQL Reference Manual :: Adding User Accounts](http://mysql.com/doc/en/adding-users.html).

Finally, leave the client and restart the database server to enable the new user(s):

    systemctl restart mariadb.service


### Step 2: Set up the Database Structure

To set up the database structure you can (and should!) use the Opencast ddl scripts. You can find them in
`…/docs/scripts/ddl/mysql5.sql` or download them from GitHub.

To import the database structure using the MariaDB client, switch to the directory that contains the `mysql5.sql` file,
run the client with a user priviledged to create the database structure and switch to the database you want to use
(e.g. `opencast`):

    mysql -u root -p opencast

Run the ddl script:

    mysql> source mysql5.sql;

Alternatively, you can import the script directly from the command line:

    mysql -u root -p opencast < …/docs/scripts/ddl/mysql5.sql

Now, ensure the MariaDB [`wait_timeout`](https://mariadb.com/kb/en/library/server-system-variables/) in `mariadb.cnf`
or `mysql.cnf` is bigger than `org.opencastproject.db.jdbc.pool.max.idle.time` in Opencast's `custom.properties`.
Raising the `max_connections` in `mariadb.cnf` parameter might be required, too, depending on your installation's size.
Reload the configuration into MariaDB, then connect to your database as user `opencast` and verify the values by
executing `SHOW  VARIABLES LIKE %_timeout;`. A `MySQLNonTransientConnectionException`, for instance “A PooledConnection
that has already signaled a Connection error is still in use”, in your Opencast logs might indicate a problem with this
configuration.

### Step 3: Configure Opencast

The following changes must be made in `…/etc/custom.properties` (`/etc/opencast/custom.properties` in a package
installation).

1. Change the following configuration key (uncomment if necessary):

        org.opencastproject.db.ddl.generation=false

    If set to true, the database structure will be generated automatically. It works, but without all the database
    optimizations implemented in the DDL scripts used in the step 2. While convenient for development, you should never
    set this to `true` in a production environment.

2. Configure Opencast to use MariaDB/MySQL:

        org.opencastproject.db.vendor=MySQL

3. Configure Opencast to use the JDBC driver for MariaDB/MySQL:

        org.opencastproject.db.jdbc.driver=com.mysql.jdbc.Driver

4. Configure the host where Opencast should find the database (`localhost`) and the database name (`opencast`). Adjust
the names in this example to match your configuration:

        org.opencastproject.db.jdbc.url=jdbc:mysql://localhost/opencast

5. Configure the username and password which Opencast should use to access the database:

        org.opencastproject.db.jdbc.user=opencast
        org.opencastproject.db.jdbc.pass=opencast_password
