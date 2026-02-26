Matrix Notification Workflow Operation
================================

ID: `matrix-notify`

Description
-----------

The matrix-notify operation sends a message to a specified Matrix room using the Matrix Client-Server API. It is useful to send notifications when some operation(s) have been completed or some error(s) have occurred in a workflow.

Parameter Table
-------------

|configuration keys | description
|---------------- --| ----------------------------------------------
|room-id            | The Matrix room ID to send the notification to
|message            | The message body to send

Operation Example
-----------

```yaml
  - id: matrix-notify
    description: Notify Matrix about workflow completion
    configurations:
      - room-id: !room-id!
      - message: Workflow has been completed successfully
```

Configuration
------

The Matrix Notification Workflow Operation Handler requires the following configuration properties to be set in `etc/org.opencastproject.workflow.handler.matrix.MatrixNotificationWorkflowOperationHandler.cfg`:

- `matrix.server.url` - The Matrix server URL (default: https://matrix.org)
- `matrix.access.token` - The Matrix access token for authentication (required)
- `matrix.max.retry` - Maximum number of retry attempts (default: 5)
- `matrix.timeout` - Request timeout in seconds (default: 10)

The `room-id` and `message` parameters are specified in the workflow configuration and are required for the operation to function.

The Matrix access token must be obtained from the Matrix server and should have appropriate permissions to send messages to the specified room.

Note: The Matrix server URL can be set to a custom Matrix server (e.g., https://matrix.example.com) if using a self-hosted Matrix server instead of the default matrix.org server.
