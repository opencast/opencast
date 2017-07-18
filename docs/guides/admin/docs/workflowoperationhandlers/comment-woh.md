# CommentWorkflowOperationHandler

## Description
The CommentWorkflowOperationHandler can be used to create, resolve or delete comments for events within workflows.

## Parameter Table

|Configuration Key|Example                         |Description                                       |Default|
|-----------------|--------------------------------|--------------------------------------------------|-------|
|action           |create                          |Action to be performed: create, resolve or delete |create |
|description*     |Recording has not been cut yet. |The description text to add to the comment        |       |
|reason           |EVENTS.EVENTS.DETAILS.COMMENTS.REASONS.CUTTING |The optional comment reason's i18n id. You can find the id in etc/listprovides/event.comment.reasons.properties|

\* mandatory configuration key

Notes:

* *create* will not create duplicate comments, i.e. if there is already a comment with the same reason and description, no additional comment will be created
* *resolve* and *delete* will perform no action in case that no comment with the specified reason and description exists

##Operation Example

    <operation
      id="comment"
      description="Mark the recording for cutting">
      <configurations>
        <configuration key="description">Recording has not been cut yet.</configuration>
        <configuration key="reason">EVENTS.EVENTS.DETAILS.COMMENTS.REASONS.CUTTING</configuration>
        <configuration key="action">create</configuration>
      </configurations>
    </operation>

