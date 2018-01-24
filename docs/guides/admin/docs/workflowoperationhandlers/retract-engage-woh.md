# RetractEngageWorkflowOperationHandler


## Description

The RetractEngageWorkflowOperationHandler retracts the published elements from the local Opencast Media Module.

There are no configuration keys at this time.

## Operation Examples

####Retract
    <!-- Retract from engage player -->

    <operation
      id="retract-engage"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Retract recording from Engage">
    </operation>
