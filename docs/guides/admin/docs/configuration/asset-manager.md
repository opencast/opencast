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

### Prefix Directories

Within a storage directory, assets are stored as `<root>/<organization>/<media package>/<version>/<element>`.
On large installations, the organization directory ends up containing hundreds of thousands of media package
directories, which makes listing it, e.g. for backups, very slow.

To avoid this, Opencast can group media packages in subdirectories named after the first characters of their
identifier. Configure the number of characters to use by appending `.prefix.length` to the configuration key of a
storage directory:

```properties
# Existing storage, flat layout (prefix length defaults to 0)
org.opencastproject.episode.rootdir=/srv/opencast/archive

# New storage: <root>/<organization>/<first 2 characters>/<media package>/…
org.opencastproject.episode.rootdir.1=/mnt/archive-share-1
org.opencastproject.episode.rootdir.1.prefix.length=2
```

`org.opencastproject.episode.rootdir.prefix.length` configures the base directory.
It also applies if `org.opencastproject.episode.rootdir` is not set and the default directory is used.
It is not a default for the other directories.
Each directory has its own setting, so you can add a new directory using prefix directories while keeping an existing
directory unchanged.

The prefix is derived from the media package identifier as follows:

- Take the first characters of the identifier and convert them to lower case.
- Replace every character which is not an ASCII letter or digit (`a-z`, `0-9`) with `_`.
- If the identifier is shorter than the prefix length, pad the prefix with `_`.

Examples for a prefix length of 2:

| Media package identifier               | Prefix |
|----------------------------------------|--------|
| `3f6e3c1a-8d0b-4b0e-9a64-0c2f0e8f7d21` | `3f`   |
| `Lecture-2026`                         | `le`   |
| `..abc`                                | `__`   |
| `x`                                    | `x_`   |

Please note:

- The prefix length of a directory must not be changed once it contains data.
  Opencast records the prefix length in a file called `.opencast-asset-store` in each storage directory when the
  directory is first used and refuses to start if the configuration does not match.
  To change the layout, add a new directory with the desired prefix length instead.
- A directory without this file which already contains data is assumed to use the flat layout.
  It can only be configured with a prefix length of 0.
  Make sure to include this hidden file when backing up or copying a storage directory.
- Older Opencast versions cannot read directories using prefix directories.
  Once a directory uses them, downgrading is not possible without moving the data.
- The prefix length can be at most 32.
- Configure the same prefix lengths on all nodes.
  Nodes with a different configuration will not read assets from the affected directory directly but download them.
- Prefix directories are never deleted, even if they are empty.
