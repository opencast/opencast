H2 Migration
==============
Opencast ships with embedded JDBC drivers for the H2, MySQL, MariaDB and PostgreSQL databases. The built-in H2 database is used by default and needs no configuration, but it is strongly recommended to use MariaDB for production.

H2 is a relational database management system written in Java that uses ANSI SQL standard, neverthless, although MariaDB and MySQL Server follow the ANSI SQL standard and the ODBC SQL standard, it is not possible to directly export data from H2 to Maria DB and MySQL.

As a workaround we wrote a couple of SQL scripts, one to export all tables to CSV files from H2 and another one to import them into MySQL or MariaDB. The use of the scripts are responsability of the user and should always peform a backup before working with any database

# Instructions
Step 1:  Install MariaDB or MySQL 
-----------------------------------

Install and configure MariaDB or MySQL as per [Database Configuration](https://docs.opencast.org/r/8.x/admin/#configuration/database/) document instructions.

Step 2:  Dump H2 data into CSV's
---------------------------------
To dump H2 data into CSV files use(under your own risk) the script CreateCSV.sql found in the [H2-Migration](https://github.com/opencast/helper-scripts/H2-Migration/) folder. Run the script directly from the console by using:

```sh
% java -cp h2*.jar org.h2.tools.RunScript -url "jdbc:h2:.../build/opencast-dist-develop-8.5/data/opencast/db;MODE=MySQL;DATABASE_TO_LOWER=TRUE" -user "sa" -pass "sa" -script ".../CreateCSV.sql"
```
Alternatively it is possible to Download and install the [H2 console installer](http://www.h2database.com/html/download.html) and run the script from the H2 console. When connecting the H2 database use  `jdbc:h2:.../build/opencast-dist-develop-8.5/data/opencast/db;MODE=MySQL;DATABASE_TO_LOWER=TRUE`. Notice the `MODE=MySQL;DATABASE_TO_LOWER=TRUE` used to maximaze the compatibility between H2 and MySQL or MariaDB. (Default username and password to connect to H2 is `sa`)

Step 3:  Import data from CSV to MariaDB or MySQL
------------------------------
Connect to the MariaDB or MySQL. 
```sh
% mysql -u root -p
```

Use the script [ImportCSV.sql](https://github.com/opencast/helper-scripts/H2-Migration/) to import all data from the CSV files to the MariaDB or MySQL: 
```
mysql> source ImportCSV.sql;
```

# SQL Scripts
## H2 Queries
Copies the data into a CSV file, there is no specific order to run the each to the queries to import the data from H2 to the CSV files. 

```sql
--TO MOVE DATA FROM oc_user TABLE  

--Creates a temp table that matches the MySQL structure
CREATE CACHED TABLE "public"."oc_user_temp"(
    "id" BIGINT NOT NULL,
    "username" VARCHAR(128),
    "password" LONGVARCHAR,
    "name" VARCHAR,
    "email" VARCHAR,
    "organization" VARCHAR(128),
    "manageable" INTEGER
); 

--Move the instances from the H2 relation to the recently created cache relation
INSERT INTO "public"."oc_user_temp" (id, username, password, name, email, organization, manageable)
select id, username, password, name, email, organization, manageable
FROM oc_user;


--Move the instances to a CSV file that we will use later on
CALL CSVWRITE('.../oc_user.csv', 'SELECT * FROM "public"."oc_user_temp"', 'charset=UTF-8  null=NULL');

--Deletes temp table
drop table "public"."oc_user_temp";
```

## MySQL/MariaDB Queries
In the case of the queries to import the data from the CSV files into the MariaDB or MySQL database, an order should be followed, the criteria for such order is the dependencies between tables. 

```sql
--Load oc_series_elements
LOAD DATA INFILE ".../oc_user.csv"  
IGNORE --ignore fields with duplicated unique keys
INTO TABLE oc_series_elements  
    FIELDS TERMINATED BY ','  
    OPTIONALLY ENCLOSED BY '"'  
    LINES TERMINATED BY '\n'
IGNORE 1 LINES;
```
The function for the first `IGNORE` is to dismiss any errors related to duplicated unique keys or missing foreign keys. If the order of importing the CSV files is not right then it will ignore those errors. To troubleshoot why data is not being imported it is possible to get rid of `IGNORE` line. 

MySQL/MariaDB dependencies
----------------------------

To troubleshoot the dependencies between tables, you can use the following table as a guide:

| Table                             | Dependencies            |
|-----------------------------------|-------------------------|
| SEQUENCE                          |                         |
| oc_acl_managed_acl                |                         |
| oc_annotation                     |                         |
| oc_assets_asset                   | oc_assets_snapshot      |
| oc_assets_properties              |                         |
| oc_assets_snapshot                |                         |
| oc_assets_version_claim           |                         |
| oc_aws_asset_mapping              |                         |
| oc_bundleinfo                     |                         |
| oc_capture_agent_role             |                         |
| oc_capture_agent_state            |                         |
| oc_event_comment                  |                         |
| oc_event_comment_reply            |                         |
| oc_group                          |                         |
| oc_group_member                   |                         |
| oc_group_role                     | oc_role                 |
| oc_host_registration              |                         |
| oc_incident                       | oc_job                  |
| oc_incident_text                  |                         |
| oc_job                            | oc_service_registration |
| oc_job_argument                   | oc_job                  |
| oc_job_context                    |                         |
| oc_job_oc_service_registration    | oc_service_registration |
|                                   | oc_job                  |
| oc_oaipmh                         |                         |
| oc_oaipmh_elements                |                         |
| oc_oaipmh_harvesting              |                         |
| oc_organization                   |                         |
| oc_organization_node              | oc_organization         |
| oc_organization_property          | oc_organization         |
| oc_role                           | oc_organization         |
| oc_scheduled_extended_event       | oc_organization         |
| oc_scheduled_last_modified        |                         |
| oc_search                         | oc_organization         |
| oc_series                         | oc_organization         |
| oc_series_elements                |                         |
| oc_series_property                | oc_series               |
| oc_service_registration           | oc_host_registration    |
| oc_themes                         |                         |
| oc_transcription_service_job      |                         |
| oc_transcription_service_provider |                         |
| oc_user                           | oc_organization         |
| oc_user_action                    |                         |
| oc_user_ref                       | oc_organization         |
| oc_user_ref_role                  | oc_role                 |
|                                   | oc_user_ref             |
| oc_user_role                      | oc_role                 |
|                                   | oc_user                 |
| oc_user_session                   |                         |
| oc_user_settings                  |                         |
| oc_series_elements                |                         |
| oc_series_property                | oc_series               |
| oc_service_registration           | oc_host_registration    |
| oc_themes                         |                         |
| oc_transcription_service_job      |                         |
| oc_transcription_service_provider |                         |
| oc_user                           | oc_organization         |
| oc_user_action                    |                         |
| oc_user_ref                       | oc_organization         |
| oc_user_ref_role                  | oc_role                 |
|                                   | oc_user_ref             |
| oc_user_role                      | oc_role                 |
|                                   | oc_user                 |
| oc_user_session                   |                         |
| oc_user_settings                  |                         |



