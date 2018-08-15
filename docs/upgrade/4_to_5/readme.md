SQL Upgrade Scripts for migration from Opencast 4.x to Opencast 5.x
===================================================================

Both `mysql5.sql` and `mariadb10.sql` essentially do the same thing and you should be able to use the first on MariaDB
as well. The difference is that the latter contains a set of additional checks, trying to deal with possible database
inconsistencies during the update. For that, `DROP INDEX IF EXISTS` statements are used which are not supported by
MySQL. Hence, if you are using MySQL, use the `mariadb10.sql` script and watch out for errors yourself when running the
script.
