# AssetManagerDeleteWorkflowOperationHandler

## Description
The delete handler is responsible for deleting an episode, identified by the workflow’s current media package, from the asset manager.

## Parameter Table
The handler does not take any parameters. The episode to delete is determined from the current media package of the workflow. 

## Operation Example

    <operation
      id="asset-delete"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Delete from AssetManager">
    </operation>
