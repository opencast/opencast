#HttpNotificationWorkflowOperation

##Description
Matterhorn can through this operation notify any HTTP endpoint about the process of the workflow.

##Parameter Table
A parameter that is always posted is the workflow instance identifier in the parameter named **workflowInstanceId** containing the current workflow’s identifier.

|Key|Required|Description|Example|
|---|--------|-----------|-------|
|url|**true**	|The target url to notify|http://test.ch|
|subject	|false	|The name of the event to notify from. The following events are planned: importing_started, imported, prepared, processing_started, published|importing_started|
|message	|false	|Data supporting the notification. Think of this as the body of an e-mail	|internal::25|
|method	|false	|Supported methods are "put", "post". If no method is specified, "post" is used by default	|post|
|max-retry	|false	|The maximal number of notification attempts. The default value is 5|5|
|timeout	|false	|The timeout in seconds for the notification request: The default value is 10|10|

##Operation Example
 
    <operation
      id="http-notify"
      fail-on-error="false"
      exception-handler-workflow="error"
      description="Notify test">
      <configurations>
        <configuration key="url">http://www.test.ch</configuration>
        <configuration key="subject">importing-started</configuration>
        <configuration key=“message”>internal::25</configuration>
        <configuration key=“method”>put</configuration>
        <configuration key="max-retry">3</configuration>
        <configuration key="timeout">5</configuration>
      </configurations>
    </operation>
