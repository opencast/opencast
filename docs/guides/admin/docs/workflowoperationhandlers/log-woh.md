LoggingWorkflowOperationHandler
===============================


Description
-----------

The LoggingWorkflowOperationHandler is primarily meant for testing and debugging purposes. It allows to log the current
state of of a workflow and/or its media package.

|Name                |Default|Description                                                  |
|--------------------|-------|-------------------------------------------------------------|
|directory           |       |If set, write the logs to this directory                     |
|workflowinstance-xml|`false`|Log the current state of the workflow as XML                 |
|mediapackage-xml    |`false`|Log the state of the current workflow's media package as XML |
|mediapackage-json   |`true` |Log the state of the current workflow's media package as JSON|

Setting any output configuration (`*-xml` and `*-json` options) will overwrite all defaults and only the formats
explicitly enabled will be logged.


Operation Example
-----------------

```xml
<operation
  id="log"
  description="Log to system logger">
</operation>
```

```xml
<operation
  id="log"
  description="Log to file">
  <configurations>
    <configuration key="directory">/tmp/logtest</configuration>
  </configurations>
</operation>
```
