Comment Workflow Operation
==========================

ID: `comment`

Description
-----------

The comment operation can be used to create, resolve or delete comments for events within workflows.

Parameter Table
---------------

|Configuration Key|Example                         |Description                                       |
|-----------------|--------------------------------|--------------------------------------------------|
|action           |create                          |Action to be performed: create, resolve or delete. Default value is create.|
|reason           |EVENTS.EVENTS.DETAILS.<br>COMMENTS.REASONS.CUTTING |The comment reason's i18n id. You can find the id in etc/listproviders/<br>event.comment.reasons.properties|
|description      |Recording has not been cut yet. |The description text to add to the comment.       |

Notes:

* reason and description must be provided for the *create* action.
* *create* will not create duplicate comments: if there is already a comment with the same reason and description,
  a new comment will not be created.
* *resolve* and *delete* will perform no action if no comment matches the provided parameters (reason, description,
   or reason and description). If more than one comment matches the parameters, only the first matching comment will be
   resolved or deleted.

Operation Example
-----------------

Create a comment:

```yaml
  - id: comment
    description: Mark the recording for cutting
    configurations:
      - action: create
      - reason: EVENTS.EVENTS.DETAILS.COMMENTS.REASONS.CUTTING
      - description: Recording has not been cut yet.
```

Resolve a comment:

```yaml
  - id: comment
    description: Resolve the cutting flag
    configurations:
      - action: resolve
      - reason: EVENTS.EVENTS.DETAILS.COMMENTS.REASONS.CUTTING

```
