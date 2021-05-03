# RetractEngageAWSS3WorkflowOperationHandler


## Description

The RetractEngageAWSS3WorkflowOperationHandler retracts the published elements from Amazon S3.

There are no configuration keys at this time.

## Operation Examples

#### Retract
    <!-- Retract from AWS -->

    <operation
      id="retract-engage-aws"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Retract recording from AWS">
    </operation>
