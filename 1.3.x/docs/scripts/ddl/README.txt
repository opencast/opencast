The recommended method of obtaining DDLs is to run Matterhorn to autogenerate 
tables by setting the following property in config.properties:

org.opencastproject.db.ddl.generation=true

The SQL files in this directory are only for creating indicies for performance
tuning.
