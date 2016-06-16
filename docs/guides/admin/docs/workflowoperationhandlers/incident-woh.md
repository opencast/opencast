# IncidentCreatorWorkflowOperationHandler

## Description
The IncidentCreatorWorkflowOperationHandler creates an incident on a dummy job used for integration testing.

## Parameter Table
|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|code|2|The code number of the incident to produce.|1|
|severity|WARNING	|The severity. See Incident.Severity enum.|INFO|
|details	|"tagged,+rss" / "-rss,+tagged"| Some details: title=content;title=content;...|EMPTY|
|params|"presentation/tagged"|Some params: key=value;key=value;...|EMPTY|

##Operation Example

    <operation
      id="incident"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Provoke a job incident">
      <configurations>
        <configuration key="code">3</configuration>
        <configuration key="severity">INFO</configuration>
        <configuration key="details">exception=content;id=325</configuration>
        <configuration key="params">track=track-1;profile=full</configuration>
      </configurations>
    </operation>
