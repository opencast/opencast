# DefaultsWorkflowOperation

##Description
The DefaultsWorkflowOperationHandler is used to define default workflow configuration values that are in effect in cases where a workflow instance is started without the user interface being invoked, with the result that no configuration of the workflow instance has taken place. The defaults specified by this handler will be applied for configuration keys that have not been specified but won't overwrite existing values.

##Parameter Table
Tags and flavors can be used in combination.

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|key|hello world|This would set the workflow configuration "key" to the value "hello world" if - and only if - the key is undefined.|-|

## Operation Example

    <operation
      id="defaults"
      description="Applying default values">
      <configurations>
        <configuration key="key">hello world</configuration>
      </configurations>
    </operation>
