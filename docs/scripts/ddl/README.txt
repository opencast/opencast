DDL Scripts
===========

Matterhorn can automatically generate all necessary tables in your database.
You *should*, however, use the optimized ddl scripts located in this directory
instead.

For more information about database configuration for Matterhorn have a look
at the project wiki:

	https://opencast.jira.com/wiki/display/MHTRUNK/Database+Configuration


Generating new DDL scripts
--------------------------

*This section is for developers only*

The recommended method of obtaining DDLs is to run Matterhorn to automatically
generate tables by setting the following property in config.properties:

	org.opencastproject.db.ddl.generation=true

This way Matterhorn will create the database structure automatically which then
can be exported and edited for performance tuning.
