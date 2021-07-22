Upgrading Opencast from 10.x to 11.x
===================================

This guide describes how to upgrade Opencast 10.x to 11.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Review the configuration changes and adjust your configuration accordingly
4. [Migrate (or rebuild) Elasticsearch index](#migrate-elasticsearch)
5. Start Opencast

Elasticsearch Migration
-----------------------

In Opencast 11, instead of having two separate Elasticsearch indices for the Admin UI and the External API that share
most of their content, there will be only one index supporting both. Since this index will have a new name, this index
will either have to be rebuilt from scratch, or you can migrate your Admin UI index, since its structure is completely
identical to the new index. However, if you don't care about accuracy, you could just keep using the Admin UI index.

### Option 1: Rebuild

Start your new Opencast and make an HTTP POST request to `/index/rebuild`.

Example (using cURL):

    curl -i --digest -u <digest_user>:<digest_password> -H "X-Requested-Auth: Digest" -s -X POST \
      https://example.opencast.org/index/rebuild

You can also just open the REST documentation, which can be found under the “Help” section in the admin interface (the
“?” symbol at the top right corner). Then go to the “Index Endpoint” section and use the testing form on
`/rebuild` to issue a POST request.

In both cases you should get a 200 HTTP status.

### Option 2: Migration

To migrate your index, you can use the `migrate-indices.sh` bash script contained in
[`docs/upgrade/10_to_11/`](https://github.com/opencast/opencast/blob/develop/docs/upgrade/10_to_11/).
This will clone the Admin UI index to an index with the new name. If your storage supports hardlinks, the cloning
process should be pretty quick.

If you want to clean up the old indices afterwards, you can use `delete-indices.sh` for this.

#### Some important notices:

- Please take a quick look at the scripts before using them and adjust them if needed.
- Do not start your new Opencast before migrating the index! This will create a new empty index which will cause the
  cloning process to fail. To fix this, delete all subindices of the new index (event, series, theme, version) before
  attempting migration again.
- The old indices cannot be used during migration, so at least the Admin node shouldn't be running.
- By default the cloning process will not copy over the index metadata and the two index settings `number_of_replicas`
  and `auto_expand_replicas`. The script will set `number_of_replicas` to 0 (assuming you have a single node) and
  `auto_expand_replicas` to false (the default). This should be fine for most people, but if you have a more intricate
  setup, you might need to change these.
- The cleanup script will also attempt to delete the group indices that are no longer used since OC 10. If you started
with OC 10 or already removed them, this will fail, but that's okay.
- **Before removing the old indices, please start Opencast and test the new index first!**

### Option 3: Keep the Admin UI index

If you don't have time for either and if you don't care about having an exact index identifier in your Elasticsearch, 
you could also just set `index.identifier` in `org.opencastproject.elasticsearch.index.ElasticsearchIndex` to "adminui" 
to keep using the old admin ui index. Do this before starting Opencast to avoid creating a new index.

The external API index can then be removed. (Please be aware that the External API index cannot be used any longer since
it doesn't contain the themes.)
