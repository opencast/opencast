AssetManagerDeleteWorkflowOperationHandler
==========================================

Description
-----------

The delete handler is responsible for deleting an episode, identified by the workflow’s current media package, from the
asset manager.

If no parameter is given, the whole episode and all of its snapshots are deleted.
If the keep-last-snapshot parameter is used, it is advised to use the *ingest-download* workflow before *asset-delete*.
Otherwise there will be logged a lot of errors for unreferenced snapshots.


## Parameter Table

|Configuration Key         |Example           |Description                                       |
|--------------------------|------------------|--------------------------------------------------|
|keep-last-snapshot        |true              |Deletes every snapshot except the last one.        |


Operation Example
-----------------

    <operation
      id="asset-delete"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Delete from AssetManager">
      <configurations>
        <configuration key="keep-last-snapshot">true</configuration>
      </configurations>
    </operation>
