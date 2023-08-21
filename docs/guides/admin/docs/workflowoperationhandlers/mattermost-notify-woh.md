Mattermost Notification Workflow Operation
==========================================

ID: `mattermost-notify`

Description
-----------

The Mattermost notify operation sends a notification to a channel of Mattermost or similar applications,
like Slack, with the chosen parameters provided. It is useful to send such notifications when some operation(s) have
been completed or some error has occurred in a workflow.

The notification message can be freely chosen. You can use different parameters which will be replaced with the
corresponding metadata of the current workflow instance (see List of parameters).


Parameter Table
---------------

|configuration keys | description                                           |default    |
|------------------ | ----------------------------------------------------- | --------- |
|url                | URL of the mattermost webhook                         | EMPTY     |
|message            | Message that will be send                             | EMPTY     |
|method             | HTTP method that will be used                         | post      |
|max-retry          | Value for the number of attempts for a request        | 5         |
|timeout            | Maximum time to wait for client to execute a request  | 10 * 1000 |


Operation Example
-----------------

```xml
<operation
    id="mattermost-notify"
    description="Notify Mattermost about error">
  <configurations>
    <configuration key="url">insert-url-of-mattermost-webhook</configuration>
    <configuration key="message">Error at Workflow %i (%t) State: %s</configuration>
    <configuration key="method">post</configuration>
    <configuration key="max-retry">3</configuration>
    <configuration key="timeout">5</configuration>
  </configurations>
</operation>
```

Message Variables
-----------------

All message variables (`%<letter>`) will be substituted with corresponding metadata of the current workflow instance
when the message is being sent.

|Parameter  | Metadata                         |
|---------- | -------------------------------- |
|%t         | Title of workflow                |
|%i         | ID of workflow                   |
|%s         | State of workflow                |
|%o         | ID of current workflow operation |
|%I         | ID of media package              |
|%T         | Title of media package           |
|%C         | Creators of media package        |
|%c         | Contributors of media package    |
|%D         | Date of media package            |
|%d         | Duration of media package        |
|%L         | License of media package         |
|%l         | Language of media package        |
|%S         | Series-Title of media package    |
