Asset Manager Delete Workflow Operation
=======================================

ID: `asset-delete`

<div class=warn>
This operation will permanently delete internal Opencast data.
This can result in data loss.
Make sure to carefully read the documentation and understand the side-effects.
This especially means the publication storage mentioned below.
</div>

Description
-----------

The delete handler can be used for deleting snapshotted versions of an episode, identified by the workflow’s current
media package, from the asset manager.

The media package resulting from this operation is the one from the latest snapshot after the deletion, but with the
publication section from the current workflow from when the operation started.

Parameter Table
---------------

|Configuration Key  |Example |Description                                                           |
|-------------------|--------|----------------------------------------------------------------------|
|keep-last-snapshot |true    |Deletes every snapshot except the last one.                           |
|roll-back-to       |5       |Rolls back to the specified snapshot version, deleting all newer ones.|


Publications
------------

Opencast stores the current version of a publication in the latest snapshot.
This means that rolling back and thus deleting newer snapshots may result in the loss of the current publication data.
If that is what you need or if the publications were not modified in the snapshots you deleted, that's fine.

In case you do need to keep the current list of publications, however, the operation will return the media package of
the version you rolled back to, but with the publication section from when the operation started.
This allows you to easily create a new snapshot with the media from the version you rolled back to,
but the current list of publications.
You just need to add a `snapshot` operation after this operation.


Operation Example
-----------------

```yaml
- id: asset-delete
  description: Delete all but latest snapshot
  configurations:
    - keep-last-snapshot: true
```
