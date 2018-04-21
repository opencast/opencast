Overview
========
The AssetManager is the successor of the Archive. The AssetManager resolves some of the shortcomings of the Archive,
such as

- **Complexity**
  The Archive architecture, with its implementation specific metadata schemas, turned out to be too complex and not used
  in practice
- **Offline storage**
  Adopters requested the need to move assets offline which cannot be handled with the Archive. The AssetManager lays the
  ground for this, with futher improvements and features to follow.
- **Properties**
  Services often need to store some properties along with a media package to accomplish their tasks. This leads to
  services creating their own persistence layer which increases the overall complexity of the system. The AssetManager
  provides a simple yet powerful annotation API to set key/value properties on media packages.
- **Queries**
  The Archive only provided a very simple query interface supporting boolean AND operations. The AssetManager, equipped
  with a comprehensive query language, supporting complex SQL-like queries.
- **Bandwidth**
  To reduce database IO, the AssetManager’s query language supports the selection of only parts of a media package.
- **Name**
  The name Archive did not properly describe its purpose. It has been used as an archive but also as a hub for starting
  workflows and managing assets. This may have caused confusion here and there which should be replaced by the new name
  AssetManager.

Please read on to learn about the complete feature set.

Terms
-----
Before we move on to the list of features let’s clarify some terms. Media package The media package is the central data
structure within Opencast that refers to  a collection of different types of elements, e.g. video tracks, metadata
catalogs, attachments that belong together. See also ⇒episode.

- **Media package element**
  Each element contained in a media package, like video tracks and metadata catalogs, are referred to as a media package
  element.
- **Asset**
  This is a media package element under the control of the AssetManager. This implies that each asset, unlike a raw
  media package element, has a version.
- **Property**
  Key/value metadata stored with an ⇒episode.
- **Snapshot**
  A snapshot is the immutable, versioned “copy” that the AssetManager takes of a media package.
- **Episode**
  An episode is the complete history of snapshots of a media package. It can be associated with ⇒properties.
- **Availability**
  A flag that indicates if an ⇒asset is online or offline. Availability is managed per ⇒episode.
- **Target**
  A target defines the data that should be fetched from the database and is used to reduce database IO by fetching only
  what’s actually needed.

Features
--------

### General
This section describes the features defined by the AssetManager API.

- **Versioned storage of media packages**
  Media package snapshots are stored in an immutable, versioned manner. Each snapshot operation creates a new single
  version of the media package and all of its assets.
- **Properties**
  Properties of various data types can be saved along with an episode. This frees services from implementing their own
  persistence layer if they need to store metadata along with an episode. Properties are a key/value store. The key is a
  tuple consisting of a namespace name and the key name, which allows properties to be grouped.
- **Query language**
  SQL-like query language.

### Default Implementation
Find below the features of the default implementation.

- **Asset store API**
  This is an API for pluggable storage backends, supporting operations to store, retrieve and delete assets. The default
  implementation of the store saves assets in the file system leveraging the Workspace service in order to reduce data
  transfers.
- **REST endpoint**
  Provides support for the most basic operations of the AssetManager.
- **Disc space management**
  If an asset has not been modified between two snapshots, the AssetManager is able to save disk space by creating hard
  links. The underlying file system needs to support this feature.
- **HttpAssetProvider API**
  The central transport protocol of Opencast is HTTP, with media package elements accessible via URLs. When a saved
  media package is being delivered, the HttpAssetProvider rewrite the URLs of media package elements to point to a valid
  HTTP endpoint. The bundled REST endpoint implements this interface.
- **Security**
  Access to the AssetManager is secured based on the organization, user roles and media package ACLs.
