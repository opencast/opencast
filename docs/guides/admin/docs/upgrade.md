# Upgrading Opencast from 17.x to 18.x

This guide describes how to upgrade Opencast 17.x to 18.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

1. Read the [release notes](releasenotes.md)
2. Stop your current Opencast instance
3. Replace Opencast with the new version
4. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
5. [Migrate the database and extract Elasticsearch/Opensearch data](#database-migration)
6. Start Opencast
7. [Rebuild the index](#index-rebuild)

## Configuration Changes

Check for changes in the configuration and apply those relevant to your setup to your files. You can use the following
command to list all changes:
```
git diff origin/r/{{ opencast_major_version() | int - 1 }}.x origin/r/{{ opencast_major_version() }}.x -- etc/
```

The most important changes are:

- Series, Editor and Login-Buttons in the PaellaPlayer are now disabled by default.
  [[#6479](https://github.com/opencast/opencast/pull/6479)] [[#6480](https://github.com/opencast/opencast/pull/6480)]
  [[#6481](https://github.com/opencast/opencast/pull/6481)]
- The feature "Episode ID Roles" is now a core feature: enabled by default with no option to disable.
  If you have not used this feature before, you'll need to perform an index rebuild. See
  [Rebuild the index](#index-rebuild) for details. [[#6696](https://github.com/opencast/opencast/pull/6696)]
- The default workflows partial-publish and partial-preview have been modified to enable audio-only processing.
  If you want to use this feature and use your own workflows, have a look at the required changes in the PR.
  [[#5856](https://github.com/opencast/opencast/pull/5856)]

## Database Migration and Elasticsearch Data Extraction

### 1. Database Migration
Before running the data extraction script, you **must** apply the database migration `mariadb.sql` or `postgres.sql`.
This step ensures that the necessary schema changes are applied before updating the records. You will find database
upgrade scripts in `docs/upgrade/17_to_18/`.

### 2. Elasticsearch Data Extraction and SQL Update Script Generation
After completing the database migration, run the provided Bash script `extract_es_data.sh` to extract data from
Elasticsearch and generate an SQL update script.

#### Steps to Execute the Script:
1. **Install Required Dependencies**
    - Ensure that `jq` is installed, as it is required to parse Elasticsearch responses.
    - You can install `jq` using the following command:
```sh
      # Debian/Ubuntu
      sudo apt-get install jq
 
      # macOS (using Homebrew)
      brew install jq
 
      # Red Hat/Fedora
      sudo dnf install jq
```

2. **Configure the Shell Script**
   Update the following parameters in the script to match your environment:
    - `ES_HOST` (Elasticsearch host)
    - `ES_USER` (Elasticsearch username)
    - `ES_PASS` (Elasticsearch password)

3. **Run the Script**
   Execute the script using the following command:
```sh
   ./extract_es_data.sh
```
This script:
- Fetches records from Elasticsearch (`opencast_series` index)
- Generates an SQL update script (`update_statements.sql`)

4. **Apply the SQL Update Statements**
   After running the script, apply the generated SQL statements to the database:

```sh
   # PostgreSQL
   psql -U <db_user> -d <db_name> -f update_statements.sql

   # MariaDB
   mysql -u <db_user> -p <db_name> < update_statements.sql
```
Replace `<db_user>` and `<db_name>` with your actual database credentials.

### 3. Error Handling
- Any errors encountered while generating the SQL script will be logged in `error_log.txt`.
- Review this file for potential issues before applying the SQL updates.

### Important Notes
- The shell script processes Elasticsearch records in batches of **1000**.
- If the database migration script is not executed first, applying the SQL update script may fail due to missing schema
  changes.

## Index Rebuild

[[#6498](https://github.com/opencast/opencast/pull/6498)] requires an update of the index settings, mappings and text
fields, for which there is a migration script `optimize_search.sh`  in `docs/upgrade/17_to_18/`.

[[#6696](https://github.com/opencast/opencast/pull/6696)] only requires an update to the indexed Elasticsearch/Opensearch
ACLs if you haven't used this feature before. **If this feature was already enabled, no further actions are required.**
If it was not enabled before upgrading, you can start the rebuild by calling the `/index/rebuild/AssetManager/ACL` and
`/index/rebuild/Search` endpoints. These endpoints will reindex the event ACLs in both the AssetManager index and the
Search index. For more information, see [here](configuration/episode-id-roles.md).
