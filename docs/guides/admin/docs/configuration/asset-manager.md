Asset Manager Configuration
===========================

### How can I use a different storage backend?

Configure an alternate storage backend, and then either use the REST endpoints or the
[Move Storage](../workflowoperationhandlers/move-storage-woh.md) workflow operation as part of a workflow.  Note that
the REST endpoints trigger workflows, the workflow operation handlers are generally only useful as part of an automated
storage tiering system.


### REST Endpoints

The REST endpoints can be accessed from `$server_url/assets/docs`. The value of `$server_url` is set during
[basic configuration](basic.md). There is no other current user interface for storage tiering at this time.

Config Options
--------------

### File System Based Asset Store

Configure the file system based asset store in `custom.properties`.

- `org.opencastproject.episode.rootdir`
   The path where the file system based asset store of the default implementation stores the assets. This key is optional.
- `org.opencastproject.storage.dir`
  This is Opencast’s general config key to configure the base path of everything storage related.
  If no storage directory is configured explicitly, the file system based asset store will use
  `${org.opencastproject.storage.dir}/archive` as its base path.

