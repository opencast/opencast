# RetractYoutubeWorkflowOperation


## Description

The RetractYoutubeWorkflowOperationHandler retracts the published elements from YouTube.

There are no configuration keys at this time.

## Operation Examples

####Retract

    <!-- Retract from YouTube -->

    <operation
      id="retract-youtube"
      if="${retractFromYouTube}"
      fail-on-error="true"
      exception-handler-workflow="ng-partial-error"
      description="Retract recording from YouTube">
    </operation>
