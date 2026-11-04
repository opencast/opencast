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


### Multiple Storage Directories

The file system based asset store can spread its data over multiple directories, e.g. to use several NFS shares.
Configure additional directories in `custom.properties` next to the base directory configured above:

```properties
org.opencastproject.episode.rootdir=/srv/opencast/archive
org.opencastproject.episode.rootdir.1=/mnt/archive-share-1
org.opencastproject.episode.rootdir.2=/mnt/archive-share-2
```

The numbers start at `1` and must be consecutive.
Opencast stops reading additional directories at the first missing number.

Opencast decides where to store a media package when its first snapshot is archived:

- A new media package is stored in the directory with the most usable free space at that time.
- All later snapshots of that media package, e.g. after metadata, access control or media changes, are stored in the
  same directory as the existing ones.
  Opencast never stores parts of a media package in different directories.
- Media packages are never moved between directories automatically, and Opencast does not balance their distribution.
  If you add a new, empty directory, most new media packages will go there until its free space drops to the level of
  the other directories.

When reading or deleting assets, Opencast looks for the media package in all configured directories.

Please note:

- All directories must be writable.
  Opencast checks write access to each directory on startup and the asset store will not start if it fails.
  Since new snapshots of existing media packages are written to the directory the media package already lives in,
  there is no way to configure a read-only directory for older content.
  Use [storage tiering](#how-can-i-use-a-different-storage-backend) to move content to a different storage instead.
- Directories must not be nested inside each other.
- If a directory is temporarily unavailable, e.g. because of a failed mount, Opencast will treat the media packages
  stored in it as missing.
- Configure the same directories on all nodes which have the asset manager storage mounted.
  The workspace uses this configuration to access assets directly from the file system instead of downloading them.
