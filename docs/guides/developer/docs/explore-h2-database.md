Explore H2 Database
===================

By default, Opencast uses an internal H2 database.
While this cannot be accessed as easily as PoostgreSQL or MariaDB databases,
it can still be accessed for debugging purposes.

<div class=warn>
H2 only allows one process to have access to the database.
Make sure Opencast is stopped when trying to access the database.
</div>

Before we start, we need to find the H2 module.
If you built Opencast once, you should have it in your local Maven cache.
If you downloaded Opencast as tarball, you will find it somewhere in the `system` folder.

To locate it, run:

```
❯ find ~/.m2/repository -name 'h2-*.jar'
/home/lars/.m2/repository/com/h2database/h2/1.3.176/h2-1.3.176.jar
```

Next, find the database.
For this, go to the Opencast data directory (e.g. `data/opencast`).
In here you should find the database file:

```
❯ ls -lh db.h2.db
-rw-r--r--. 1 lars lars 1.6M May  4 13:06 db.h2.db
```


Dumping Database
----------------

You can dump the H2 database into an SQL file to make it readable or to import it into a different database.

```
❯ CLASSPATH=/home/lars/.m2/repository/com/h2database/h2/1.3.176/h2-1.3.176.jar java org.h2.tools.Script \
    -url jdbc:h2:./db -user sa -password sa
❯ ls -l backup.sql
-rw-r--r--. 1 lars lars 91058 May  4 13:20 backup.sql
❯ head -n4 backup.sql
CREATE USER IF NOT EXISTS SA SALT '3004d37cddfd5449' HASH '146885fd9173fca50cd6c9449a5943a124d63f1febefe8bb1ed66ded5bd685c0' ADMIN;
CREATE CACHED TABLE PUBLIC.OC_BUNDLEINFO(
    ID BIGINT NOT NULL,
    BUILD_NUMBER VARCHAR(128),
```


Database Explorer
-----------------

You can also launch the database explorer to access the database through a web interface:

```
❯ CLASSPATH=/home/lars/.m2/repository/com/h2database/h2/1.3.176/h2-1.3.176.jar java org.h2.tools.Console
```

Usually, you can now access the web interface on [localhost:8082](http://localhost:8082).
If now, run `ps` and `ss` to determine the port:

```
❯ ps ax|grep org.h2.tools.Console
  85047 pts/2    Sl+    0:05 java org.h2.tools.Console
❯ ss -tlpn | grep pid=85047
LISTEN 0      50                      *:8082             *:*    users:(("java",pid=85047,fd=6))
LISTEN 0      50                      *:5435             *:*    users:(("java",pid=85047,fd=22))
LISTEN 0      50                      *:9092             *:*    users:(("java",pid=85047,fd=20))
```
