Database Configuration
======================

Opencast ships with embedded JDBC drivers for the H2 (HSQL), MySQL/MariaDB databases. The built in H2 databased is used
by default, and needs no configuration, but it is strongly recommended to use MariaDB 10.0 instead as there will be a
huge performance gain, especially if more data are in that database.

*Notice:* Opencast 2.2 is not compatible wit MySQL 5.7 (and probaly MariaDB 10.1). **Please use MariaDB 10.0 or MySQL 5.5 or 5.6.**

*Notice:* In general we recomment to use MariaDB 10.0 instead of MySQL 5.5, as the MariaDB is fully compatible with MySQL 5.5 but not vice versa. The enhancements of MariaDB, i.e. allowed more robust migration scripts than MySQL.

*Notice:* For a distributed set-up of Opencast, you cannot use the internal H2 database.


### Other databases

Running Opencast with PostgreSQL should be possible and there is some community support for this. While it should work,
the support for this is unofficial and we cannot guarantee that every new feature is well tested on that platform.

The EclipseLink JPA implementation which is used in Opencast supports other databases as well and it should be
possible to attach other database engines.

Setting up MySQL/MariaDB
------------------------

### Requirements

Before following this guide, you should have:

 - Installed the Opencast Core System
 - Followed the [Basic Configuration instructions](basic.md)


### Step 0: Set-up MySQL/MariaDB

This step is not Opencast specific and may be different for your needs (e.g.  if you want to have a dedicated database
server). It shall only be a guide for people with no experience setting up MySQL/MariaDB and to help them get things
running.

*Notice:* If your distribution includes MySQL instead of MariaDB, the installation should still be very much the
same.

First you have to install the MariaDB server. Usually you would do that by using the package management tool of you
distribution. On RedHat based systems (CentOS, Scientific Linux, …) this should be:

    yum install mariadb mariadb-server

After the installation you can start the server and set it up to start automatically after each reboot with the
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
following SQL code to to that. For executing the SQL, use the MySQL/MariaDB client (run the mysql program from your
shell) or use a graphical tool like phpMyAdmin. For now, we will use the MySQL shell client and the default
administrative (root) user. Launch the client with:

    mysql -u root -p

You will be asked for the password of the user root. After entering it, you will end up in the MySQL/MariaDB shell.
Next, create a database called `opencast` by executing:

    CREATE DATABASE opencast CHARACTER SET utf8 COLLATE utf8_general_ci;

Then create a user `opencast` with the password `opencast_password` and grant him all necessary rights:

    GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,INDEX ON opencast.*
      TO 'opencast'@'localhost' IDENTIFIED BY 'opencast_password';

*Notice:* You can choose another name for the user and database and we would recommend that you use a different password.


On Distributed Systems, additionally to `'username'@'localhost'` which would allow access from the local machine only,
for a distributed system you would also create a user like `'username'@'10.0.1.%'` and grant the necessary rights to
this user as well. The `10.0.1.%` specifies the IP range which gets access to the server with `%` being a wildcard for
anything.  For more details on MySQL user creation have a look at [MySQL Reference Manual :: Adding User Accounts](http://mysql.com/doc/en/adding-users.html).

Finally, leave the MySQL/MariaDB client shell and restart the database server to enable the user with:

    service mysqld restart

or if you have a systemd based system:

    systemctl restart mariadb.service


### Step 2: Set up the Database Structure

To set up the database structure you can (and should!) use the Opencast ddl scripts. You can find the script in the 
installations document folder `.../docs/scripts/ddl/mysql5.sql`. You can also download the script from BitBucket.

To import the database structure from the MySQL/MariaDB Client, switch to the directory that contains the `mysql5.sql` 
file and run the MySQL/MariaDB client with the user you created in the previous step (`-u opencast`) and switch to 
the database you want to use (`opencast`):

    mysql -u opencast -p opencast

Run the ddl script:

    mysql> source mysql5.sql;

To import the script directly from the command line:

    mysql -u opencast -p opencast < .../docs/scripts/ddl/mysql5.sql

Instead of using the MySQL/MariaDB Client, you can also use other methods for executing SQL code like phpMyAdmin or 
MySQL-Workbench.

### Step 3: Configure Opencast

The following settings are made in the `.../etc/custom.properties` file (often `/etc/opencast/custom.properties`). 
Use the editor of your choice to open it, e.g.:

    vim etc/opencast/custom.properties

Now change the following configuration keys (uncomment where necessary):

    org.opencastproject.db.ddl.generation=false

If set to true, the database structure will be generated automatically. It works, but all database optimizations are
lost. You should never do this, unless you need it for development purposes.

Tell Opencast to use MySQL:

    org.opencastproject.db.vendor=MySQL

Tell Opencast to use the JDBC driver for MySQL:

    org.opencastproject.db.jdbc.driver=com.mysql.jdbc.Driver

Tell Opencast where to find the database and the name of the database (Replace “localhost” and “opencast” if necessary):

    org.opencastproject.db.jdbc.url=jdbc:mysql://localhost/opencast

Tell Opencast which username and password to use for accessing the database:

    org.opencastproject.db.jdbc.user=opencast
    org.opencastproject.db.jdbc.pass=opencast_password

*Notice:* This user needs to have the necessary rights to the database, similar to the user that was created in Step 1.
