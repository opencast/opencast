# ImportWfPropertiesWorkflowOperationHandler

## Description
The ImportWfPropertiesWorkflowOperationHandler loads workflow properties from a Java properties file and sets
the corresponding workflow instance variables so that the properties can be use to control workflow execution.

In case that no properties are found in `source-flavor`, the workflow operation will just skip.

Note that the [ExportWfPropertiesWorkflowOperationHandler](export-wf-properties-woh.md) can be used to export workflow properties to a Java properties file.

## Parameter Table

|Configuration Key|Example                      |Description                                                          |
|-----------------|-----------------------------|---------------------------------------------------------------------|
|source-flavor*   |processing/defaults          |Flavor of the attachment that contains the serialized workflow instance properties|
|keys             |variableName1, variableName2 |The workflow property keys to retrieve (comma separated list). If the option has not been specified, all keys will be retrieved|

\* mandatory configuration key

##Operation Example

    <operation
      id="import-wf-properties"
      fail-on-error="true"
      exception-handler-workflow="partial-error"
      description="Load processing settings">
      <configurations>
        <configuration key="source-flavor">processing/defaults</configuration>
      </configurations>
    </operation>

