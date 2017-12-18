# RetractOaiPmhWorkflowOperation


## Description

The RetractOaiPmhWorkflowOperationHandler retracts the published elements from a OAI-PMH repository.

## Parameter Table

|Configuration Keys |Description                                                                                   |
|-------------------|----------------------------------------------------------------------------------------------|
|repository         |The name of the OAI-PMH repository where the media should be retracted from                   |

## Operation Examples


    <operation
        id="retract-oaipmh"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Retract event from the OAI-PMH repository">
        <configurations>
            <configuration key="repository">default</configuration>
        </configurations>
    </operation>
