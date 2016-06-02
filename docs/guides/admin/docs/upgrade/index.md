# Upgrading Opencast to 2.2

## Database Migration
You will find the database migration script in /docs/upgrade/2.1_to_2.2/<vendor>.sql

*Notice:* We highly recommend to use MariaDB 10.0+ instead of MySQL 5.5+, as the migration scripts for MariaDB are much more robust. 

Switch to the directory that contains the mysql5.sql or mariadb10.sql file and run the MySQL/MariaDB client with the 
database user and switch to the database you want to use (`opencast`):

    mysql -u opencast -p opencast

Run the ddl script:

    mysql> source mariadb10.sql;

or 

    mysql> source mysql5.sql;

With the mysql5 script you might encouter errors in the logs about missing INDEXes and non existing FOREIGN KEYS. You can probably ignore these errors. If your MySQL database is not working afterwards we can only recommend to try MariaDB. You should not use the shell import of the SQL script for MySQL, as unlike with the SOURCE command, the processing of the script in the shell will stop, as soon as an error is encountered!

## Distribution artifacts migration
With the introduction of stream security, there was the need to be able to prevent cross-tenants access on the download and streaming distribution artifacts. To reach that the download and streaming distribution service has been adjusted to generate their artifact URL including the tenant. Because of this new URL's all existing artifact URL's need a migration and the artifacts in the file systems need to be moved to a new location.

To update the distribution artifacts:

1. Install Opencast 2.2
2. Start Matterhorn
3. Log-in to the Karaf console on your node where the search service is running (usually presentation node) and install the opencast-migration feature by entering: `feature:install opencast-migration`
4. Check the logs for errors!
5. Restart Matterhorn.

## Configuration changes
