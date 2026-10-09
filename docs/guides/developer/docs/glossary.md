Glossary
========

Short definitions of terms used throughout the architecture documentation, especially ones that are easy to
confuse with one another.


Asset
-----

One archived media package element's content — its checksum, MIME type, size, and which asset store holds it.
Distinct from a *snapshot*, which is the whole archived media package a given asset belongs to. See [Asset
Manager: Classes](architecture/asset-manager.md#classes).


Capture Agent
-------------

Hardware or software that records at a scheduled time and place and uploads the result to Opencast. See the
[capture agent guide](architecture/capture-agent/capture-agent.md).


Element (Media Package Element)
-----------------------------------

One piece of a media package: a track, catalog, attachment, or publication, tagged with a *flavor* and addressed
by its own element ID. See [Architecture Overview: Media Package](architecture/overview.md#media-package).


Episode
-------

The set of all snapshots of a given media package — effectively its full history of archived versions.
Confusingly close to, but distinct from, both *media package* (a single version's content) and *event* (the index's
read model of it); "episode" mostly surfaces in the asset manager, where properties are associated with an episode
rather than any one snapshot. See [Asset Manager: Working with Properties](architecture/asset-manager.md#working-with-properties).


Event
-----

Not a separate object Opencast processes — it is what the search index, and through it the admin UI and the
External API, call a media package once it has been scheduled, recorded, or ingested, assembled from the
scheduler, workflow service, and asset manager. A workflow operation works with a *media package*; the event
shown afterward is a read model built from that, not something manipulated directly. See [Architecture Overview:
Events](architecture/overview.md#events).


Flavor
------

A `type/subtype` pair, such as `presenter/source` or `dublincore/episode`, that identifies what role a media
package element plays. Workflow operations and services use flavors, not file names or ordering, to find the
elements they are supposed to work on. See [Architecture Overview: Media Package](architecture/overview.md#media-package).


Job
---

A unit of asynchronous work — for example, "encode this track with this profile" — created by a workflow
operation and handed to the service registry for dispatch to a service instance. See [Job
Dispatch](architecture/job-dispatch.md).


Load
----

A float describing how much of a node's capacity a job is expected to consume. A node's own capacity is its *max
load*; dispatching tries to keep the sum of a node's running jobs' loads under that limit. See [Job Dispatch:
Jobs and Load](architecture/job-dispatch.md#jobs-and-load).


Media Package
-------------

The unit of content Opencast processes: a container for a set of elements plus an identifier and an optional
series reference. A media package is not itself a store of files — its elements point to files kept by the
working file repository or the asset manager. See [Architecture Overview: Media
Package](architecture/overview.md#media-package).


Service
-------

A component registered with the service registry to process jobs of a particular type. Several instances
of the same service can run on different cluster nodes. See [Architecture Overview: Jobs and
Services](architecture/overview.md#jobs-and-services).


Snapshot
--------

One archived, immutable version of a media package, including all of its assets. See [Asset Manager:
Classes](architecture/asset-manager.md#classes).


Workflow, Workflow Instance, Workflow Operation
---------------------------------------------------

A *workflow* is a `WorkflowDefinition` listing a sequence of operations; a `WorkflowInstance` is one run of such a
definition against one media package. Each step is a `WorkflowOperationInstance`, backed by a
`WorkflowOperationHandler` that does the actual work. See [Architecture Overview: Workflows and
Operations](architecture/overview.md#workflows-and-operations).


Working File Repository, Workspace
---------------------------------------

The *working file repository* is the HTTP-exposed store a media package element lives in while being actively
processed. The *workspace* is a separate, per-node cache in front of it, used to avoid downloading the same file
more than once; almost everything, including writes, goes through the workspace rather than the working file
repository directly. See [Filesystem](architecture/filesystem.md).
