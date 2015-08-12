DDL Scripts
===========

Opencast can automatically generate all necessary tables in your database.
You *should*, however, use the optimized ddl scripts located in this directory
instead.

For more information about database configuration for Opencast have a look
at the database configuration guide in our Administration Guide:

  http://docs.opencast.org/

or local in:

  ../../guides/admin/docs/configuration/database.md


Generating new DDL scripts
--------------------------

*This section is for developers only*

The recommended method of obtaining DDLs is to run Opencast to automatically
generate tables by setting the following property in config.properties:

	org.opencastproject.db.ddl.generation=true

This way Opencast will create the database structure automatically which then
can be exported and edited for performance tuning.
