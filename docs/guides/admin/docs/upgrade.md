Upgrading Opencast from 10.x to 11.x
===================================

This guide describes how to upgrade Opencast 10.x to 11.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Read the release notes (especially the section of behaviour changes)
4. Review the configuration changes and adjust your configuration accordingly
5. Do the [database migration](#database-migration)
6. [Migrate, rebuild or rename Elasticsearch index](#elasticsearch-migration)
7. Start Opencast if you haven't already done so

Database Migration
------------------

Upgrading to Opencast 11 requires a DB migration, as some tables have changed slightly.
Migration scripts can be found in `doc/upgrade/10_to_11/`.
There are separate scripts for MariaDB/MySQL (`mysql5.sql`) and PostgreSQL (`postgresql.sql`).

Elasticsearch Migration
-----------------------

In Opencast 11, instead of having two separate Elasticsearch indices for the Admin UI and the External API that share
most of their content, there will be only one index supporting both. Since this index will have a new name, this index
will either have to be rebuilt from scratch ([Option 1](#option-1-rebuild)), or you can migrate your Admin UI index
([Option 2](#option-2-migration)), since its structure is completely identical to the new index.
However, if you don't care about accuracy, you could just keep using the Admin UI index
([Option 3](#option-3-keep-the-admin-ui-index)).

Please read all options and commit to one before taking any action.

### Option 1: Rebuild

Start your new Opencast and make an HTTP POST request to `/index/rebuild`.

Example (using cURL):

    curl -i -u <admin_user>:<password> -s -X POST https://example.opencast.org/index/rebuild

You can also just open the REST documentation, which can be found under the “Help” section in the admin interface (the
“?” symbol at the top right corner). Then go to the “Index Endpoint” section and use the testing form on
`/rebuild` to issue a POST request.

In both cases you should get a 200 HTTP status.

### Option 2: Migration

**Some important notices before you start:**

- Please take a quick look at the scripts before using them and adjust them if needed.
- Do not start your new Opencast before or during migration of the index (at least on the Admin node)!
  Starting Opencast before migration will create a new empty index which will later cause the cloning process to fail.
  (To fix this, delete all subindices of the new index - `opencast_event`, `opencast_series`, `opencast_theme`,
  `opencast_version` - before attempting migration again. You could use a **modified** version of
  `delete-old-indices.sh` to do this or pick out the relevant commands and do it manually.
  *Make sure you remove the correct subindices for the new index, not the old ones!*)
  Running Opencast during migration (whether with the new or old indices) can result in inconsistent data during
  cloning. Do not do this!
- By default the cloning process will not copy over the index metadata and the two index settings `number_of_replicas`
  and `auto_expand_replicas`. The script will set `number_of_replicas` to 0 (assuming you have a single node) and
  `auto_expand_replicas` to false (the default). This should be fine for most people, but if you have a more intricate
  setup, you might need to change these.
- The cleanup script will also attempt to delete the group indices that are no longer used since OC 10. If you started
with OC 10 or already removed them, this will fail, but that's okay.
- **Before removing the old indices, please start Opencast and test the new index first!**

To migrate your index, you can use the `migrate-indices.sh` bash script contained in
[`docs/upgrade/10_to_11/`](https://github.com/opencast/opencast/blob/develop/docs/upgrade/10_to_11/).
This will clone the Admin UI index to an index with the new name. If your storage supports hardlinks, the cloning
process should be pretty quick.

If you then want to clean up the old indices, you can use `delete-old-indices.sh` for this.

### Option 3: Keep the Admin UI index

If you don't have time for either and if you don't care about having an exact index identifier in your Elasticsearch,
you could also just set `index.identifier` in `org.opencastproject.elasticsearch.index.ElasticsearchIndex` to "adminui"
to keep using the old admin ui index. Do this before starting Opencast to avoid creating a new index.

The external API index can then be removed. (Please be aware that the External API index cannot be used any longer since
it doesn't contain the themes.) You can use a **modified** version of `delete-old-indices.sh` to do this (make sure
to remove only the External API sub indices and _not_ the Admin UI ones you're currently using!)
