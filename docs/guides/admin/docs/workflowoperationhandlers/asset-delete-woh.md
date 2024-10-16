Asset Manager Delete Workflow Operation
=======================================

ID: `asset-delete`

Description
-----------

The delete handler can be used for deleting snapshotted versions of an episode, identified by the workflow’s current
media package, from the asset manager.

If no parameter is given, the all snapshots are deleted.


Parameter Table
---------------

|Configuration Key  |Example |Description                                                           |
|-------------------|--------|----------------------------------------------------------------------|
|keep-last-snapshot |true    |Deletes every snapshot except the last one.                           |
|roll-back-to       |5       |Rolls back to the specified snapshot version, deleting all newer ones.|


Operation Example
-----------------

```yaml
- id: asset-delete
  description: Delete all but latest snapshot
  configurations:
    - keep-last-snapshot: true
```
