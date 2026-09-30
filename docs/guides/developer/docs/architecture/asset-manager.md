Asset Manager
=============

Architecture
------------

### Modules

The AssetManager consists of the following modules:

* `asset-manager-api`
An API module defining the core AssetManager functions, properties and the query language.
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

TODO


Usage
-----

### Taking Snapshots

TODO

### Working with Properties

Properties are associated with an episode, not a single snapshot. They act as annotations helping services to work with
saved media packages without having to implement their own storage layer. Properties are typed and can be used to create
queries.

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
`org.opencastproject.approval`. This code looks overly verbose. Also you need to deal with namespace names and property
names directly. That's cumbersome and error prone even though you might intoduce constants for them. To help remedy this
situation a little helper class class `PropertySchema` exists. It is strongly recommended to make use of it. Here's how
it goes.

    static class ApprovalPops extends PropertySchema {
     public ApprovalProps(AQueryBuilder q) {
       super(q, "org.opencastproject.approval");
     }

     public PropertyField<Boolean> approved() {
       return booleanProp("approved");
     }

     public PropertyField<String> comment() {
       return stringProp("comment");
     }

     public PropertyField<Date> date() {
       return dateProp("date");
     }
    }

Now you can set properties like this.

    am.setProperty(p.approved().mk(mp, false));
    am.setProperty(p.comment().mk(mp, "Audio quality is too poor!"));
    am.setProperty(p.date().mk(mp, new Date());

Now, if you want to find all episodes that have been rejected you need to create and run the following query.

    AQueryBuilder q = am.createQuery();
    AResult r = q.select(q.snapshot()).where(p.approved().eq(true)).run();

This query yields all snapshots of all episodes that have been approved. But that's not exactly what we want as we are
only interested in the latest snapshot generated when we re-run the approval process, and resetting all previous
approvals.

    q.select(q.snapshot())
      .where(p.approved().eq(true).and(q.version().isLatest())
      .run();

This will only return the latest version of each episode. However, along with the information of the approved
episodes,we want to display when they were approved. Looking at the AResult and ARecord interfaces it seems that
properties need to be selected in order to fetch them.

    q.select(q.snapshot(), q.properties())
      .where(p.approved().eq(true).and(q.version().isLatest())
      .run();

Here we go. Now we can access all properties stored with the returned snapshots. Now, let's assume other services make
heavy use of properties too. This may cause serious database IO if we always select all properties like we did using the
q.properties() target. Let's do better.

    q.select(q.snapshot(), q.propertiesOf("org.opencastproject.approval"))
      .where(p.approved().eq(true).and(q.version().isLatest())
      .run();

This will return only the properties of our service's namespace. But do we have to deal with namespace strings again?
No.

    q.select(q.snapshot(), q.propertiesOf(p.allProperties()))
      .where(p.approved().eq(true).and(q.version().isLatest())
      .run();

Our implementation of `PropertySchema` provides as with a ready to use target for the properties of our namespace only.
In our use case we could reduce IO even further since we're only interested in the date property.

    q.select(q.snapshot(), q.propertiesOf(p.date().target()))
      .where(p.approved().eq(true).and(q.version().isLatest())
      .run();

This is the query returns only the latest snapshots of all episodes being approved together with the date of approval.
Now that you've seen how to create properties let's move on to delete them again.

#### Deleting Properties
Properties are deleted pretty much like they are queried, using a delete query.

    q.delete(q.propertiesOf(p.allProperties())).run();

The above query deletes all properties that belong to schema p from all episodes. If you want to restrict deletion to a
single episode, add an id predicate to the where clause.

    q.delete(q.propertiesOf(p.allProperties()))
      .where(q.mediaPackageId(mpId))
      .run();

Deleting just a single property from all episodes is also possible.

    q.delete(p.approved()).run();

Or multiple properties at once.

    q.delete(p.approved(), p.comment()).run();

Please see the query API documentation for further information.

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

#### Using PropertySchema
You've already seen that a property is constructed from a media package id, a namespace, a property name and a value.
Since this is a bit cumbersome, the API features an abstract base class to construct property schemas. The resulting
schema implementations encapsulate all the string constants so that you don't have to deal with them manually. Please
see the example in the _Getting Started_ section. It is strongly recommended to work with schemas as much as possible.

### Creating and Running Queries

Creating and running a query is a two step process. First, you create a new `AQueryBuilder`.

    AQueryBuilder q = am.createQuery();

Next, you build a query like this.

    ASelectQuery s = q.select(q.snapshot())
      .where(q.mediaPackageId(mpId).and(q.version().isLatest());

Now it's time to actually run the query against the database.

    AResult r = s.run();

All this can, of course, be done in a single statement, but it has been broken up in several steps  to show you the
intermediate types.

    am.createQuery()
      .select(q.snapshot())
      .where(q.mediaPackageId(mpId).and(q.version().isLatest())
      .run();

The result set `r` contains the retrieved data encapsulated in stream of `ARecord` objects. If nothing matched the given
predicates then a call to r.getRecords() yields an empty stream. Please note that even though a `Stream` is returned, it
does not mean that the result set is actually streamed—or lazily loaded—from the database. The `Stream` interface is
just far more powerful than the collection types from JCL.

#### A note on immutability

Please note that all classes of the query API are immutable and therefore safe to be used in a concurrent environment.
Whenever you call a factory method on an instance of one of the query classes a new instance is yielded. They never
mutate state.

### Accessing Query Results

Running a query yields an object of type `AResult` which in turn yields the found result records. Besides it also
provides some general result metadata like the set limit, offset etc. An `ARecord` holds the found snapshots and
properties, depending on the select targets and the predicates. If no snapshots have been selected then, none will be
returned here. The same holds true for properties. However, an `ARecord` instance holding the media package id is
created regardless of the requested targets. The typical pattern to access query results is to iterate over the stream
of records. This can be accomplished using a simple for loop or one of the functional methods that the `Stream` type
provides, e.g. map over the elements of a stream to create a new one. For easy access to fetched resources you may wrap
the result in an enrichment.

    AResult r = …;
    RichAResult rr = Enrichments.enrich(r);

`RichAResult` features methods to directly access all fetched snapshots and properties.

### Deleting Snapshots

This works exactly like deleting properties, except that you need to specify snapshots instead of properties.
Please note that it's also possible to specify snapshots and properties simultanously.

    q.delete("owner", q.snapshot()).where(q.version().isLatest().not()).run();

The above query deletes all snapshots but the latest. This is a good query to free up some disc space.

Snapshots can only be deleted per owner.

### Query Language Reference

The query API features

* select clause and targets
* where clause with boolean and relational operations, nesting of boolean operations
* selecting by properties
* order-by clause
* querying and deleting

Please see the API doc for further information about the various elements and how to create them.
