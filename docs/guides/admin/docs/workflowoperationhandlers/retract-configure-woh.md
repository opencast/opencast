# ConfigurableRetractWorkflowOperationHandler


## Description

The ConfigurableRetractWorkflowOperationHandler retracts the published elements from a configured publication.

If the elements have been added to the Publication using "with-published-elements", as in the case with the external api, they haven't actually been published so it is unnecessary to have a retract-configuration. Adding a retraction won't cause any errors, it will just skip those elements.

There is only one configuration key "channel-id". This is the channel to remove the published elements from. 

## Operation Examples

####Retract from Internal Channel
    <!-- Remove the internal publication if the mediapackage is being deleted. -->
    <operation
      id="retract-configure"
      exception-handler-workflow="partial-error"
      description="Retract from internal publication channel">
      <configurations>
        <configuration key="channel-id">internal</configuration>
      </configurations>
    </operation>

####Retract from External API

    <operation
      id="retract-configure"
      exception-handler-workflow="partial-error"
      description="Retract from external api publication channel">
      <configurations>
        <configuration key="channel-id">api</configuration>
      </configurations>
    </operation>
