Filesystem
==========

Opencast moves a lot of media between services, often across multiple nodes, without necessarily copying the
underlying bytes more than once. This page covers the three storage layers involved — the working file repository,
the workspace, and the asset manager's archive — and how a file can travel from ingest to permanent storage via
nothing but hard links, if the deployment allows it.

For a walkthrough with diagrams, see [this webinar on storage management and snapshots](https://explore.opencast.org/webinars/v/K0McuVkZs2X).


The Working File Repository
-----------------------------

The *working file repository* (WFR) is where a media package element lives while it is being actively processed. It
is an HTTP-exposed store (`/files/...`), so any node can read any other node's files by URL regardless of where they
physically sit. On disk, it organizes files under its root in two ways:

    <root>/mediapackage/<media_package_id>/<element_id>/<filename>
    <root>/collection/<collection_id>/<filename>

The `mediapackage` tree holds media package elements: the tracks, catalogs, attachments and publications that make up
a media package's manifest, each tagged with a flavor and addressed by its own element ID. The `collection` tree is
untyped scratch space, keyed by an arbitrary string a service picks for itself — the composer service stages its
encoded output there under `"composer"` before a workflow operation handler decides what flavor to give it and
formally adds it as an element, ingest stages uploaded files there before unpacking them, and so on. A file only
becomes an element — addressable by flavor, part of the manifest — once something calls
`WorkingFileRepository.put()` with a media package and element ID; before that, it is just a collection item with no
identity beyond its own filename.


The Workspace
-------------

The *workspace* is a per-node cache: code asks it for a `java.io.File` given some URI, and it decides how to get it
there. Its purpose is to avoid downloading the same remote file more than once: several services on the same node
asking for the same URI all get the same locally cached copy, and when storage is shared between nodes, that
avoidance extends across the whole cluster too.

Concretely, `WorkspaceImpl` keeps its own root directory as a local cache. The first time something asks for a given
URI, it is fetched — by HTTP if the source is remote, or without any network request at all if the source turns out
to be locally reachable — and kept under the workspace root; later requests for the same URI reuse that local copy.

Whether "fetched" means an actual copy or something cheaper depends on hard links. At startup, `WorkspaceImpl` runs a
real test: it writes a file into the working file repository's root and tries to hard link it into the workspace
root. If that succeeds — which it will whenever both roots sit on the same volume, including a shared NFS mount
across multiple nodes — `linkingEnabled` is set for the life of the service, and every later "copy" from the working
file repository into the workspace is a hard link instead: a new directory entry pointing at the same data, created
instantly and using no extra disk space. If the roots are on different volumes, the workspace falls back to a real
copy (or a download, for anything not locally reachable at all).


Workspace and the Working File Repository
--------------------------------------------

Almost nothing accesses the working file repository directly — including for writes: `Workspace.put()` and
`.putInCollection()` simply delegate to the working file repository's own methods internally, so going through the
workspace costs nothing. The one exception in the codebase is ingest, which writes a file and then reads it straight
back from the working file repository itself moments later to sniff its content; there is nothing to cache when you
already have what you just wrote.


From Workspace to the Archive
-------------------------------

Archiving a snapshot follows the same pattern one level up. `AbstractFileSystemAssetStore.put()` — the shared base of
the [asset store implementations](asset-manager.md#assetstore) — does not read the source file itself; it asks the
workspace for a local copy first, then hard links (or, failing that, copies) *that* file into its own archive
directory.

The result is that when the working file repository, the workspace, and the asset store's root are all configured
onto the same volume — the common case for an allinone node, or a cluster sharing storage over NFS — a file can go
from "just ingested" to "permanently archived" without its bytes ever being duplicated: only new directory entries
are created, each time pointing at the same inode. Workflow operations can still read and write through the
workspace as if everything were a plain local file, because as far as the filesystem is concerned, it is.

The workspace has one more shortcut in the same spirit: if the asset manager's own storage path is known to be
locally reachable (again, typically because of shared storage), the workspace can resolve a URI to an already
archived file directly from that path too, skipping the HTTP round trip entirely even for content that was archived
long ago.


Cleaning Up
-----------

None of this is meant to be permanent outside the archive. Once a workflow no longer needs a media package's working
copies, the `cleanup` workflow operation — typically the last operation in a workflow — removes the matching files
from both the workspace and the working file repository, except for any flavor listed in its `preserve-flavors`
configuration. This is the primary way storage use stays bounded during normal operation.

Two periodic cleanups exist as a safety net for what that misses. `WorkspaceCleaner`
(`org.opencastproject.workspace.cleanup.period`/`.max.age`) purges any locally cached file past its max age,
regardless of why it is still there. The working file repository's own cleanup
(`org.opencastproject.working.file.repository.cleanup.period`/`.max.age`) is narrower: it only ever touches
collections explicitly listed in `org.opencastproject.working.file.repository.cleanup.collections`, which by default
is just `failed.zips` — abandoned ingest uploads, not general working data.
