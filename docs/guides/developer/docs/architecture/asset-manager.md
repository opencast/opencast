Asset Manager
=============

Architecture
------------

### Modules

The AssetManager consists of the following modules:

* `asset-manager-api`
An API module defining the core AssetManager functions and properties.
* `asset-manager-impl`
The default implementation of the AssetManager as an OSGi service, containing the storage API for pluggable asset stores.
* `asset-manager-storage-fs`
An implementation of the AssetStore that archives to the local filesystem. Depends on asset-manager-impl.
* `asset-manager-storage-aws`
An implementation of the AssetStore that archives to an S3 bucket, with its own small database mapping stored assets
to S3 objects. Depends on asset-manager-impl.
* `asset-manager-static-file-authorization`
Authorizes direct, static access to archived files by checking the requesting user's roles against the ACL a
snapshot's elements were archived with, stored as properties.
* `asset-manager-workflowoperation`
Workflow operation handlers to take a snapshot, move it between stores, delete it, or select a specific version,
all from inside a running workflow.
* `shared-filesystem-utils`
Utilities that build on the AssetManager, such as starting a workflow on the latest archived snapshot of each of a
given list of episodes.

### High Level View

The AssetManager is a facade (`AssetManager`, implemented by the OSGi service `AssetManagerImpl`) in front of two
separate concerns: a database, and one or more [asset stores](#assetstore) that hold media package elements'
actual bytes. An element's content always goes to an asset store, never the database — but the database is not
just bookkeeping about the stores, either: each snapshot's full media package manifest is kept as XML directly in a
database column (`SnapshotDto.mediaPackageXml`), duplicating the `manifest.xml` archived separately to the asset
store, and [properties](#working-with-properties) are typed values stored entirely in the database, with no asset
store involved at all. For elements themselves, what the database consistently tracks is which snapshot each one
belongs to, its checksum, and which store currently holds it — tracking that makes it possible to move a snapshot
between stores without the `AssetManager` API consumer ever needing to know where a given version's bytes actually
live. Running more than one store side by side is a real, supported deployment: alongside the default local
filesystem store, `asset-manager-storage-aws` can archive to S3, and the admin guide's [move storage workflow
operation](https://docs.opencast.org/stable/admin/#workflowoperationhandlers/move-storage-woh/) is built specifically
to move snapshots to that kind of colder storage — the admin guide's own example is moving the raw input media there
once initial processing is done.

Taking a [snapshot](#taking-snapshots) with `takeSnapshot()` walks a media package's elements and, for each one,
asks the [workspace](filesystem.md#the-workspace) for the bytes and hands them to the local `AssetStore`
(`getLocalAssetStore().put()`) under a `StoragePath` keyed by organization, media package ID, version, and element
ID — the same archiving path [filesystem.md](filesystem.md#from-workspace-to-the-archive) already describes for the
filesystem-backed store. Before writing fresh content, though, `AssetManagerImpl` checks the database for an asset
with the same checksum already archived anywhere in that store; if one exists, it copies (hard-links, where the
store supports it) rather than storing the bytes again. This is a second, independent form of deduplication from
the workspace's hard-linking: it applies across the whole archive and all episodes, not just within one node's
local cache.

Every snapshot is versioned and immutable — a version, once archived, is never rewritten, only superseded by a new
one or deleted outright. Read and write access to a media package's snapshots is checked on every operation
(`isAuthorized()`) against the media package's [access control list](#security); that same ACL is also copied into
the episode's properties at snapshot time, which lets other components — the [static file authorization
module](#modules) among them — check access without going through the full `AssetManager` API.

### Classes

* `AssetManager` / `AssetManagerImpl`
  The facade described [above](#high-level-view); the entry point for every AssetManager operation.
* `Snapshot` / `SnapshotImpl`
  One archived, immutable version of a media package: its `Version`, organization, owner, `Availability`, the ID of
  the asset store holding it, and the media package itself. At archive time, each element's URI is rewritten to a
  storage-neutral `urn:matterhorn:...` identifier rather than a live URL, since the snapshot's storage location can
  change later; `HttpAssetProvider` rewrites these back into real, fetchable URLs whenever a snapshot is retrieved
  through the API.
* `Asset` / `AssetImpl`
  One archived media package element's content: its checksum, MIME type, size, `Availability`, and the ID of the
  asset store holding it. A snapshot owns many assets, one per element.
* `AssetId`
  Identifies an asset as the triple {`Version`, media package ID, media package element ID}, used at the
  `AssetManager` API level, for example by `getAsset()`. Distinct from `StoragePath` below.
* `StoragePath`
  The coordinate an `AssetStore` actually keys a stored element's location by: {organization ID, media package ID,
  `Version`, element ID}. It carries the organization ID that `AssetId` leaves out, since a store can be shared
  across organizations even though `AssetId` values are already unique without it.
* `Version` / `VersionImpl`
  An ordered, comparable version number, unique per media package. Claimed via `Database.claimVersion()`, which
  increments the last-claimed value for that media package ID or starts at `VersionImpl.FIRST` for a new episode.
* `Property` / `PropertyId` / `Value`
  A typed key-value pair scoped to an episode by `PropertyId`'s {media package ID, namespace, name}, not to a single
  snapshot. Covered in detail under [Working with Properties](#working-with-properties) below.
* `AssetStore` / `RemoteAssetStore`
  The pluggable storage interface described under [AssetStore](#assetstore); `RemoteAssetStore` is the sub-interface
  additional, non-local stores implement so more than one can be bound at once, as [described
  above](#high-level-view).
* `Database`
  The persistence layer, in `asset-manager-impl`'s `persistence` package. Its DTOs (`SnapshotDto`, `AssetDto`,
  `PropertyDto`, `VersionClaimDto`) map onto the four tables described under [Database](#database) below.

Default Implementation
----------------------

### AssetStore

Assets are stored in the following directory structure.

    $BASE_PATH
     |— <organization_id>
         |— <media_package_id>
             |— <version>
                 |— manifest.xml
                 |— <media_package_element_id>.<ext>

### Database
--------

The asset manager uses four tables

* `oc_assets_snapshot`
  Manages snapshots. Each snapshot may be linked to zero or more assets.
* `oc_assets_asset`
  Manages the assets of a snapshot.
* `oc_assets_properties`
  Manages the properties. This table is indirectly linked to the snapshot table via column `mediapackage_id`.
* `oc_assets_version_claim`
  Manages the next free version number per episode.

### Security

Every `AssetManager` read or write method checks `isAuthorized(mediaPackageId, action)` first, where `action` is
either `READ_ACTION` or `WRITE_ACTION`. What that check does depends on the caller:

* A global administrator is always granted access.
* An organization administrator is granted access as long as a snapshot for that media package exists in their own
  organization — no further per-episode check.
* Any other user must first belong to the same organization, and then either hold the synthetic role
  `ROLE_EPISODE_<mediaPackageId>_<ACTION>` — the same scoped-access mechanism used elsewhere in Opencast to grant
  access without an actual ACL role, for example to a signed URL's session — or have a role that matches one of the
  media package's archived ACL entries.

The *active ACL* of a media package is whichever access control list currently applies to it: its own episode-level
XACML attachment if it has one, else its series' XACML attachment, else the organization's global default. It is
resolved fresh, on demand, via `AuthorizationService.getActiveAcl()`, and it can change over time — for instance
when an editor changes an event's access rights.

The ACL check against an archived snapshot's roles does not resolve the active ACL this way. Instead,
`takeSnapshot()` resolves it once, at the moment the snapshot is taken, and copies each of its entries into a
property under the `org.opencastproject.assetmanager.security` namespace, keyed by `"<role> | <action>"`. From then
on, that snapshot's authorization check reads only this stored copy — it is never re-resolved or kept in sync with
the active ACL afterward. If the active ACL changes later, only snapshots taken after that change reflect it; older
snapshots keep the access rules that were active when *they* were archived. This also lets other components check
access without going through the `AssetManager` API at all: [`asset-manager-static-file-authorization`](#modules)
queries these stored properties directly to gate static file access.

`AssetManagerImpl`'s check reads that stored copy. The general-purpose `AuthorizationService.hasPermission()`
implements the same episode-role-then-ACL pattern, but does not read a stored copy at all — it resolves the active
ACL fresh on every call instead. The two are separate, duplicate implementations of the same logic against two
different sources of truth, and the code carries its own acknowledgment that unifying them is not safe to do
casually.

One thing the ACL check above does *not* cover: `takeSnapshot()` still tags every snapshot with an owner
(`Snapshot.getOwner()`), but that ownership is no longer enforced as an access boundary anywhere. Deleting a
snapshot (`deleteSnapshots()`, `deleteAllButLatestSnapshot()`) checks only `isAuthorized(mpId, WRITE_ACTION)` — any
user with write access to the media package can delete any of its snapshots, regardless of who owns them.


Usage
-----

### Taking Snapshots

`takeSnapshot(MediaPackage mp)` archives a versioned copy of a media package, as described under [High Level
View](#high-level-view). It reuses the owner of the media package's existing latest snapshot, or
`AssetManager.DEFAULT_OWNER` if this is the first snapshot of the episode; `takeSnapshot(String owner, MediaPackage
mp)` lets a caller set the owner explicitly instead. As covered under [Security](#security), that owner is recorded
but no longer enforced as an access boundary.

### Working with Properties

Properties are associated with an episode, not a single snapshot. They act as annotations helping services to work
with saved media packages without having to implement their own storage layer. Properties are typed, and identified
by a `PropertyId`: a media package ID, a namespace, and a name.

#### Getting Started

Let's start with an fictious example of an ApprovalService. The approval service keeps track of approvals given by an
editor to publish a media package. Only approved media packages may be published and the editor should also be able to
leave a comment defining a publication as prohibited. Here, three properties are needed, an approval flag, a text field
for comments and a time stamp for the date of approval. The following code snippet sets a property on an episode, with
am referring to the AssetManager and mp the media package id of type String of the episode.

    AssetManager am = …;
    String mp = …; // a media package id
    am.setProperty(Property.mk(PropertyId.mk(
      mp, "org.opencastproject.approval", "approval"),
      Value.mk(true)));

It is recommended to use namespace names after the service's package name, in the example:
`org.opencastproject.approval`. Beyond that, there is no dedicated helper class for grouping related properties —
namespaces and names are just plain strings, so services typically define their own constants for them rather than
repeating literals.

#### Reading and Deleting Properties

`selectProperties(mediaPackageId, namespace)` returns every property in that namespace for a given media package.

    List<Property> approvalProperties = am.selectProperties(mp, "org.opencastproject.approval");

There is no query language for combining a property lookup with a snapshot lookup in a single call, and no way to
filter episodes by a property's value across the whole archive either — a caller has to already have a candidate
list of media package IDs from somewhere else (the search index, for instance), then call `getLatestSnapshot(mpId)`
and `selectProperties(mpId, namespace)` separately for each one and correlate the two itself. This isn't a pattern
used anywhere in the current codebase; it's simply what combining the two calls would require.

`deleteProperties(mediaPackageId)` deletes every property for a media package; `deleteProperties(mediaPackageId,
namespace)` restricts that to one namespace. `deletePropertiesWithCurrentUser(mediaPackageId, namespace)` does the
same but is further scoped by [the current user's authorization](#security).

#### Value Types
The following type are available for properties:

* Long
* String
* Date
* Boolean
* Version
  Version is the AssetManager type that abstracts a snapshot version.

#### Decomposing properties
Since properties are type safe they cannot be accessed directly.
If you know the type of the property you can access its value using a type evidence constant.

    String string = p.getValue().get(Value.STRING);
    Boolean bool = p.getValue().get(Value.BOOLEAN);

Type evidence constants are defined in class `Value`. If the type is unknown since you are iterating a mixed collection
of values, for example if you need to decompose the value. Decomposition is the act of pattern matching against the
value's type. Each case is handled by a different function, all returning the same type. Let's say you are iterating
over a collection of values and want to print them, formatted, to the console. All `handle*` parameters are functions of
type `Fn` taking the raw value as input and returning a String.

    List<Value> vs = …;
    for (Value v : vs) {
      String f = v.decompose(
        handleStringFn,
        handleDateFn,
        handleLongFn,
        handleBooleanFn,
        handleVersionFn);
      System.out.println(f);
    }

The class `org.opencastproject.assetmanager.api.fn.Properties` contains various utility functions to help extracting
values from properties.

### Retrieving and Deleting Snapshots

Beyond `getLatestSnapshot(mediaPackageId)`, already used under [Working with Properties](#working-with-properties),
`AssetManager` offers a family of purpose-built getters rather than a general query language:
`getSnapshotsById`, `getSnapshotsByIdAndVersion`, `getSnapshotsByDateOrderedById`, `getLatestSnapshotsBySeriesId`,
and others — see the interface's own Javadoc for the full list.

    List<Snapshot> versions = am.getSnapshotsById(mpId);

Deleting works the same way: `deleteSnapshots(mediaPackageId)` removes every snapshot of a media package, and
`deleteAllButLatestSnapshot(mediaPackageId)` removes every version but the newest — the more common case, for
reclaiming disk space without losing the current version.

    int removed = am.deleteAllButLatestSnapshot(mpId);

As covered under [Security](#security), deletion is not scoped by owner: either call requires only write access to
the media package as a whole.
