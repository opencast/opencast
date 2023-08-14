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
The default  implementation of the AssetStore. Depends on asset-manager-impl.
* `asset-manager-util`
Additional functionality for the AssetManager providing utilities such as starting workflows on archived snapshots, etc.
* `asset-manager-workflowoperation`
A workflow operation handler to take media package snapshots of a media package from inside a running workflow.

### High Level View

TODO Describes components and how they relate.

### Classes

TODO Most important classes and how they relate

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
