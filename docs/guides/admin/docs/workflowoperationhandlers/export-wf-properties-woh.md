# ExportWfPropertiesWorkflowOperationHandler

## Description

The ExportWfPropertiesWorkflowOperation can be used to export workflow properties to a Java properties file. Those
properties can later be imported using the [ImportWfPropertiesWorkflowOperation](import-wf-properties-woh.md).

## Parameter Table

|Configuration Key|Example                     |Description                                            |
|-----------------|----------------------------|-------------------------------------------------------|
|target-flavor*   |processing/defaults         |The flavor to apply to the exported workflow properties|
|target-tags      |archive                     |The tags to apply to the exported workflow properties  |
|keys             |variableName1, variableName2|The workflow property keys that need to be persisted. If the option is not specified, all defined properties should be persisted.|

\* mandatory configuration key

##Operation Example

    <operation
      id="export-wf-properties"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Export workflow settings to Java properties file">
      <configurations>
        <configuration key="target-flavor">processing/defaults</configuration>
        <configuration key="target-tags">archive</configuration>
      </configurations>
    </operation>

