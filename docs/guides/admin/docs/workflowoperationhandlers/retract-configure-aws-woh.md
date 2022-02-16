# ConfigurableAWSS3RetractWorkflowOperationHandler


## Description

The ConfigurableAWSS3RetractWorkflowOperationHandler retracts the published elements from a configured publication.

If the elements have been added to the Publication using "with-published-elements", as in the case with the external
api, they haven't actually been published so it is unnecessary to have a retract-configuration. Adding a retraction
won't cause any errors, it will just skip those elements.

## Parameters

These are the keys that can be configured for the worklow operation in the workflow definition.  The `channel-id` is
mandatory.

|Key                    |Description                                          |Example    |Default  |
|-----------------------|-----------------------------------------------------|-----------|---------|
|channel-id             |The id of the channel to retract from                |`internal` |         |

## Operation Examples

#### Retract from Internal Channel
    <!-- Remove the internal publication if the mediapackage is being deleted. -->
    <operation
      id="retract-configure-aws"
      exception-handler-workflow="partial-error"
      description="Retract from internal publication channel using AWS S3">
      <configurations>
        <configuration key="channel-id">internal</configuration>
      </configurations>
    </operation>

#### Retract from External API

    <operation
      id="retract-configure-aws"
      exception-handler-workflow="partial-error"
      description="Retract from external api publication channel using AWS S3">
      <configurations>
        <configuration key="channel-id">api</configuration>
      </configurations>
    </operation>
