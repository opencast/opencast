Database Configuration
======================

Opencast ships with embedded JDBC drivers for the H2 (HSQL), MySQL and MariaDB databases. The built-in H2 database is
used by default, and needs no configuration, but it is strongly recommended to use MariaDB 10.0 instead as there will be
a huge performance gain.

> **Notice:** Opencast 2.2 is not compatible with MySQL 5.7 (and probably MariaDB 10.1). **Please use MariaDB 10.0 or MySQL
5.5 or 5.6.**

> **Notice:** In general, it is recommended to use MariaDB 10.0 instead of MySQL 5.5, as MariaDB is fully compatible with
MySQL 5.5 but not vice versa. MariaDB's extra features allow for robust migration scripts than with MySQL.

> **Notice:** For a distributed setup of Opencast, you cannot use the internal H2 database.


### Other databases

Running Opencast with PostgreSQL should be possible and there is some community support for this. While it should work,
the support for this is unofficial and we cannot guarantee that every new feature is well tested on that platform.

The EclipseLink JPA implementation which is used in Opencast supports other databases as well and it should be
possible to attach other database engines.

Setting up MariaDB/MySQL
------------------------

### Requirements

Before following this guide, you should have:

 - Installed the Opencast Core System
 - Followed the [Basic Configuration instructions](basic.md)


### Step 0: Set-up MariaDB/MySQL

This step is not Opencast-specific and may be different depending on your scenario (e.g. if you want to have a dedicated
database server). It shall only be a guide for people with no experience setting up MariaDB/MySQL to help them get the
database running.

> **Notice:** If your distribution includes MySQL instead of MariaDB, the installation should still be very much the
same.

First you have to install the MariaDB server. Usually you would do that by using the package management tool of you
distribution. On RedHat-based systems (CentOS, Scientific Linux, etc.) this should be:

    yum install mariadb mariadb-server

After the installation, you can start the server and set it up to start automatically after each reboot with the
following commands:

    # If you are using Systemd
    systemctl start mariadb.service
    systemctl enable mariadb.service
    # If you are using SysV-Init
    service mariadb start
    chkconfig --level 345 mariadb on

Now you have a MariaDB server running, but without a properly configured root account (no password, etc.) which might 
pose a security risk. MariaDB includes a useful tool to secure your database server (it is also included in MySQL).
You can launch this tool by executing (yes, it is still called mysql_…):

    mysql_secure_installation

It will guide you through the steps of setting up a root account with password, etc.


### Step 1: Create an Opencast Database

The first step, if you have not already done this, is obviously to create a database for Opencast. You can use the
following SQL code to to that. For executing the SQL, use the MariaDB/MySQL client (run the mysql program from your
shell) or use a graphical tool like phpMyAdmin. For now, we will use the MySQL shell client and the default
administrative (root) user. Launch the client with:

    mysql -u root -p

You will be asked for the password of the user root. After entering it, you will end up in the MariaDB/MySQL shell.
Next, create a database called `opencast` by executing:

    CREATE DATABASE opencast CHARACTER SET utf8 COLLATE utf8_general_ci;

Then create a user `opencast` with the password `opencast_password` and grant him all necessary rights:

    GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,INDEX,TRIGGER ON opencast.*
      TO 'opencast'@'localhost' IDENTIFIED BY 'opencast_password';

> **Notice:** You can choose another name for the user and database and we strongly recommend that you use a different
password.

In a distributed system, apart from `'username'@'localhost'` (which would allow access from the local machine only),
you should grant a external user access to the database by running the same command for a user like
`'username'@'10.0.1.%'`, where the `10.0.1.%` specifies the IP range allowed to access the server with `%` being a
wildcard for "anything". For more details on MariaDB/MySQL user creation have a look at any of the following links:

* [MariaDB Reference Manual :: `GRANT` statement](https://mariadb.com/kb/en/mariadb/grant/)
* [MySQL Reference Manual :: Adding User Accounts](http://mysql.com/doc/en/adding-users.html).

Finally, leave the MariaDB/MySQL client shell and restart the database server to enable the new user(s) with:

    service mysqld restart

or, if you have a systemd based system:

    systemctl restart mariadb.service


### Step 2: Set up the Database Structure

To set up the database structure you can (and should!) use the Opencast ddl scripts. You can find them in the 
installation document folder `.../docs/scripts/ddl/mysql5.sql`. You can also download the script from BitBucket.

To import the database structure using the MariaDB/MySQL client, switch to the directory that contains the `mysql5.sql` 
file and run the MariaDB/MySQL client with the user you created in the previous step (`-u opencast`) and switch to 
the database you want to use (`opencast`):

    mysql -u opencast -p opencast

Run the ddl script:

    mysql> source mysql5.sql;

Alternatively, you can import the script directly from the command line:

    mysql -u opencast -p opencast < .../docs/scripts/ddl/mysql5.sql

Instead of using the MariaDB/MySQL Client, you can also use other methods for executing SQL code like phpMyAdmin or 
MySQL-Workbench.

### Step 3: Configure Opencast

The following changes must be made in the `.../etc/custom.properties` file (`/etc/opencast/custom.properties` in an RPM
installation).

1. Use the editor of your choice to open the file, e.g.:

        vim etc/opencast/custom.properties

2. Change the following configuration key (uncomment if necessary):

        org.opencastproject.db.ddl.generation=false

    If set to true, the database structure will be generated automatically. It works, but without all the database
    optimizations implemented in the DDL scripts used in the step 2. While convenient for development, you should never
    set this to `true` in a production environment.

3. Configure Opencast to use MariaDB/MySQL:

        org.opencastproject.db.vendor=MySQL

4. Configure Opencast to use the JDBC driver for MariaDB/MySQL:

        org.opencastproject.db.jdbc.driver=com.mysql.jdbc.Driver

5. Configure the host where Opencast should find the database (`localhost`) and the database name (`opencast`). Adjust
the names in this example to match your configuration:

        org.opencastproject.db.jdbc.url=jdbc:mysql://localhost/opencast

6. Configure the username and password which Opencast should use to access the database:

        org.opencastproject.db.jdbc.user=opencast
        org.opencastproject.db.jdbc.pass=opencast_password

    **Notice:** The user specified here must have the necessary rights to the database, as explained in the Step 1.
