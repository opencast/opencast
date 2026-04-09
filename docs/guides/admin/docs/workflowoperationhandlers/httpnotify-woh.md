HTTP Notification Workflow Operation
====================================

ID: `http-notify`

Description
-----------

Opencast can through this operation notify any HTTP endpoint about the process of the workflow.

Parameter Table
---------------

Parameters that are always posted include the workflow instance identifier in the parameter named
**workflowInstanceId** containing the current workflow’s identifier, and the mediapackage identifier
in the parameter named **mediaPackageId** containing the current mediapackage’s identifier.

|Key       |Required |Description                                                                              |Example|
|----------|---------|-------------------------------------------------------------------------------------------|-----|
|url       |**true** |The target URL to notify                                                                   |http://test.ch|
|subject   |false    |The name of the event to notify from. The following events are planned: importing\_started, imported, prepared, processing\_started, published|importing\_started|
|message   |false    |Data supporting the notification. Think of this as the body of an e-mail                   |internal::25|
|method    |false    |Supported methods are "put", "post". If no method is specified, "post" is used by default  |post|
|max-retry |false    |The maximal number of notification attempts. The default value is 5                        |5|
|timeout   |false    |The timeout in seconds for the notification request: The default value is 10               |10|

Operation Example
-----------------

```yaml
  - id: http-notify
    description: Notify test
    configurations:
      - url: http://www.test.ch
      - subject: importing-started
      - message: internal::25
      - method: put
      - max-retry: 3
      - timeout: 5
```
