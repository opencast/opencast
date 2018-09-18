# Mattermost Notification
## Description
The MattermostNotificationOperationHander sends a notification to a channel of Mattermost or similar applications,
like Slack, with the chosen parameters provided. It is useful to send such notifications when some operation(s) have
been completed or some error has occurred in a workflow.
The notification message can be freely chosen. You can use different parameters which will be replaced with the
corresponding metadata of the current workflow instance (see List of parameters).

## List of configuration options

|configuration keys | description                                           |default    |
|------------------ | ----------------------------------------------------- | --------- |
|url                | URL of the mattermost webhook                         | EMPTY     |
|message            | Message that will be send                             | EMPTY     |
|method             | HTTP method that will be used                         | post      |
|max-retry          | Value for the number of attempts for a request        | 5         |
|timeout            | Maximum time to wait for client to excecute a request | 10 * 1000 |


## Example for mattermost-notify operation

```XML
  <operation
      id="mattermost-notify"
      fail-on-error="false"
      exception-handler-workflow="error"
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

## List of parameters
All parameters (%<letter>) will be substituted with corresponding metadata of the current workflow instance.

|Parameter  | Metadata                         |
|---------- | -------------------------------- |
|%t         | Title of workflow                |
|%i         | ID of workflow                   |
|%s         | State of workflow                |
|%o         | ID of current workflow operation |
|%I         | ID of Mediapackage               |
|%T         | Title of Mediapackage            |
|%C         | Creators of Mediapackage         |
|%c         | Contributors of Mediapackage     |
|%D         | Date of Mediapackage             |
|%d         | Duration of Mediapackage         |
|%L         | License of Mediapackage          |
|%l         | Language of Mediapackage         |
|%S         | Series-Title of Mediapackage     |
