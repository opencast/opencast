Database Configuration
======================

Matterhorn ships with embedded JDBC drivers for the H2 (HSQL), MySQL/MariaDB databases. The built in H2
databased is used by default and needs no configuration, but it is strongly recommended to use MySQL or MariaDB instead
as there will be a huge performance gain, especially if more data are in that database.

*Notice:* For a distributed set-up of Matterhorn, you cannot use the internal H2 database.


### Other databases

Running Matterhorn with PostgreSQL should be possible and there is some community support for this. While it should
work, the support for this is unofficial and we cannot guarantee that every new feature is well tested on that platform.

The EclipseLink JPA implementation which is used in Matterhorn supports other databases as well and it should be
possible to attach other database engines.

Setting up MySQL/MariaDB
------------------------

### Requirements

Before following this guide you should have:

 - Installed the Matterhorn Core System
 - Followed the [Basic Configuration instructions](basic.md)


### Step 0: Set-up MySQL/MariaDB

This step is not Matterhorn specific and may be different for your needs (e.g.  if you want to have a dedicated database
server). It shall only be a guide for people with no experience setting up MySQL/MariaDB and to help them get things
running.

*Notice:* If your distribution still shipps MySQL instead of MariaDB, the installation should still be very much the
same. Only the names will of course change.

First you have to install the MariaDB server. Usually you would do that by using the package management tool of you
distribution. On RedHat based systems (CentOS, Scientific Linux, …) this should be:

    yum install mariadb mariadb-server

After the installation you can start the server and set it up to start automatically after each reboot with the
following commands:

    ä If you are using Systemd
    systemctl start mariadb.service
    systemctl enable mariadb.service
    # If you are using SysV-Init
    service mariadb start
    chkconfig --level 345 mariadb on

Now you have a MariaDB server running, but without a properly set up root account (no password, etc.) which might pose a
security risk. To create this initial configuration, there is a convenient tool that comes which MariaDB and which will
help. You can launch this tool by executing (yes, it is still called mysql_…):

    mysql_secure_installation

It will guide you through the steps of setting up a root account with password, etc.


### Step 1: Create a Matterhorn Database

The first step, if you have not already done this, is obviously to create a database for Matterhorn. You can use the
following SQL code to to that. For executing the SQL, use the MySQL/MariaDB client (run the mysql program from your
shell) or use a graphical tool like phpMyAdmin. For now, we will use the MySQL shell client and the default
administrative (root) user. Launch the client with:

    mysql -u root -p

You will be asked for the password of the user root. After entering it, you will end up in the MySQL/MariaDB shell.
Next, create a database called “matterhorn” by executing:

    CREATE DATABASE matterhorn CHARACTER SET utf8 COLLATE utf8_general_ci;

Then create a user “matterhorn” with the password “opencast_password” and grant him all necessary rights:

    GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,INDEX ON matterhorn.*
      TO 'matterhorn'@'localhost' IDENTIFIED BY 'opencast_password';

*Notice:* Of cause you may use other names for user or database and should use a different password.


On Distributed Systems, additionally to 'username'@'localhost' which would allow access from the local machine only, for
a distributed system you would also create a user like 'username'@'10.0.1.%' and grant the necessary rights to this user
as well. The '10.0.1.%' specifies the IP range which gets access to the server with '%' being a wildcard for anything.
For more details on MySQL user creation have a look at MySQL Reference Manual :: 6.3.2 Adding User Accounts.

Finally, leave the MySQL/MariaDB client shell and restart the database server to enable the user with:

    service mysqld restart

or if you have a systemd based system:

    systemctl restart mariadb.service


### Step 2: Set up the Database Structure

To set up the database structure you can (and should!) use the Matterhorn ddl scripts. You can find the script either at
`/usr/share/matterhorn/docs/scripts/ddl/mysql5.sql` or download it from BitBucket.

Switch to the directory that contains the mysql5.sql file and run the MySQL/MariaDB client with the user you created in
the previous step (-u matterhorn) and switch to the database you want to use (matterhorn):

    mysql -u matterhorn -p matterhorn

Run the ddl script:

    mysql> source mysql5.sql;

Instead of using the MySQL/MariaDB Client, you can, of cause, also use every other method for executing SQL code like
phpMyAdmin or MySQL-Workbench…


### Step 3: Configure Matterhorn

The following settings are made in the `<MH_CONFIG_DIR>/config.properties` file (often
`/etc/matterhorn/config.properties`). Use the editor of your choice to open it, e.g.:

    vim /etc/matterhorn/config.properties

Now change the following configuration keys:

    org.opencastproject.db.ddl.generation=false

If set to true, the database structure will be generated automatically. It works, but all database optimizations are
lost. You should never do this, unless you need it for development purposes.

    org.opencastproject.db.vendor=MySQL

Tell Matterhorn that you use MySQL. 

    org.opencastproject.db.jdbc.driver=com.mysql.jdbc.Driver

Tell Matterhorn to use the JDBC driver for MySQL.

    org.opencastproject.db.jdbc.url=jdbc:mysql://localhost/matterhorn

Tell Matterhorn where to find the database and the name of the database. Replace “localhost” and “matterhorn” if necessary.

    org.opencastproject.db.jdbc.user=matterhorn

Tell Matterhorn which username to use for accessing the database. This user need to have the rights to read from and
write to the database.

    org.opencastproject.db.jdbc.pass=opencast_password

Tell Matterhorn which password to use for accessing the database. This must obviously fit the username.
