# RetractAWSWorkflowOperationHandler


## Description

The RetractAWSWorkflowOperationHandler retracts the published elements from Amazon S3.

There are no configuration keys at this time.

## Operation Examples

####Retract
    <!-- Retract from AWS -->

    <operation
      id="retract-aws"
      fail-on-error="true"
      exception-handler-workflow="ng-partial-error"
      description="Retract recording from AWS">
    </operation>
