[TOC]
# General

The Workflow API is available since API version 1.1.0.

# Workflow instances

### GET /api/workflows

Returns a list of workflow instances.

The following query string parameters are supported to filter, sort and pagingate the returned list:

Query String Parameter | Type                        | Description
:----------------------|:----------------------------|:-----------
`filter`               | [`string`](types.md#basic)  | A comma-separated list of filters to limit the results with (see [Filtering](usage.md#filtering)). See the below table for the list of available filters
`sort`                 | [`string`](types.md#basic)  | A comma-separated list of sort criteria (see [Sorting](usage.md#sorting)).  See the below table for the list of available sort criteria
`limit`                | [`integer`](types.md#basic) | The maximum number of results to return (see [Pagination](usage.md#pagination))
`offset`               | [`integer`](types.md#basic) | The index of the first result to return (see [Pagination](usage.md#pagination))

The following filters are available:

Filter Name                      | Description
:--------------------------------|:-----------
`state`                          | Workflow instances that are in this state. Can be included multiple times (inclusive or).
`state_not`                      | Workflow instances that are not in this state. Can be included multiple times (inclusive or).
`current_operation`              | Workflow instances that are currently executing this operation. Can be included multiple times (inclusive or).
`current_operation_not`          | Workflow instances that are currently not executing this operation. Can be included multiple times (inclusive or).
`workflow_definition_identifier` | Workflow instances that use this workflow definition
`event_identifier`               | Workflow instances where the identifier of the processed event matches
`event_title`                    | Workflow instances where the title of the processed event matches
`event_created`                  | Workflow instances where the processed event was created between two dates
`event_creator`                  | Workflow instances where the creator of the processed event matches
`event_contributor`              | Workflow instances where the contributor of the processed event matches
`event_language`                 | Workflow instances where the language of the processed event matches
`event_license`                  | Workflow instances where the license of the processed event matches
`event_subject`                  | Workflow instances where the subject of the processed event matches
`series_identifier`              | Workflow instances where the identifier of series of the processed event matches
`series_title`                   | Workflow instances where the title of series of the processed event matches
`textFilter`                     | Workflow instances where any part of the Workflow definition's metadata fields match this value

Note:

The filters `event_created` expect the following value:
[`datetime`](types.md#date-and-time) + '/' + [`datetime`](types.md#date-and-time)

The list can be sorted by the following criteria:

Sort Criteria                    | Description
:--------------------------------|:-----------
`event_identifier`               | By the event identifier of the workflow instance
`event_title`                    | By the event title of the workflow instance
`event_created`                  | By the event creation date of the workflow instance
`event_creator`                  | By the event creator of the workflow instance
`event_contributor`              | By the event contributor of the workflow instance
`event_language`                 | By the event language of the workflow instance
`event_license`                  | By the event license of the workflow instance
`event_subject`                  | By the event subject of the workflow instance
`series_identifier`              | By the series identifier of the workflow instance
`series_title`                   | By the series title of the workflow instance
`workflow_definition_identifier` | By the workflow definition identifier of the workflow instance

This request additionally supports the following query string parameters to include additional information directly in
the response:

Query String Parameter     | Type                        | Description
:--------------------------|:----------------------------|:-----------
`withoperations`           | [`boolean`](types.md#basic) | Whether the workflow operations should be included in the response
`withconfiguration`        | [`boolean`](types.md#basic) | Whether the workflow configuration should be included in the response

__Sample request__

```xml
https://opencast.example.org/api/workflow-definitions?sort=event_created:DESC&limit=5&offset=1&filter=workflow_definition_identifier:fast
```

__Response__

`200 (OK)`: A (potentially empty) list of workflow instances is returned. The list is represented as JSON array where
each element is a JSON object with the following fields:

Field                            | Type                                                       | Description
:--------------------------------|:-----------------------------------------------------------|:-----------
`identifier`                     | [`integer`](types.md#basic)                                | The unique identifier of this workflow instance
`title`                          | [`string`](types.md#basic)                                 | The title of this workflow instance
`description`                    | [`string`](types.md#basic)                                 | The description of this workflow instance
`state`                          | [`workflow_state`](types.md#workflow_state)                | The state of this workflow instance
`operations`                     | [`array[operation_instance]`](types.md#operation_instance) | The list of operations of this workflow instance
`configuration`                  | [`property`](types.md#property)                            | The configuration for this workflow instance
`workflow_definition_identifier` | [`string`](types.md#basic)                                 | The template of this workflow instance (i.e. the unique identifier of the workflow definition)
`event_identifier`               | [`string`](types.md#basic)                                 | The id of the event this workflow instance belongs to
`creator`                        | [`string`](types.md#basic)                                 | The name of the creator of this workflow instance

`400 (BAD REQUEST)`: The request is invalid or inconsistent.

__Example__

```
[
  {
    "workflow_definition_identifier": "fast",
    "identifier": 1603,
    "creator": "Opencast Project Administrator",
    "operations": [
      {
        "identifier": 1604,
        "completion": "2018-08-08T08:46:57.3Z",
        "configuration": {
          "publishLive": "false",
          "uploadedSearchPreview": "false",
          "publishToOaiPmh": "true",
          "comment": "false",
          "publishToMediaModule": "true"
        },
        "time_in_queue": 0,
        "failed_attempts": 0,
        "start": "2018-08-08T08:46:57.201Z",
        "description": "Applying default configuration values",
        "fail_workflow_on_error": true,
        "unless": "",
        "max_attempts": 1,
        "host": "http:\/\/localhost:8080",
        "state": "succeeded",
        "operation": "defaults",
        "if": "",
        "retry_strategy": "none",
        "error_handler_workflow": ""
      },
      {
        "identifier": 1605,
        "completion": "",
        "configuration": {
          "apply-acl": "true"
        },
        "time_in_queue": 0,
        "failed_attempts": 0,
        "start": "2018-08-08T08:47:02.209Z",
        "description": "Applying access control entries from series",
        "fail_workflow_on_error": true,
        "unless": "",
        "max_attempts": 1,
        "host": "http:\/\/localhost:8080",
        "state": "skipped",
        "operation": "series",
        "if": "",
        "retry_strategy": "none",
        "error_handler_workflow": "partial-error"
      }
      ...
    ],
    "configuration": {
      "publishLive": "false",
      "workflowDefinitionId": "fast",
      "uploadedSearchPreview": "false",
      "publishToOaiPmh": "true",
      "comment": "false",
      "publishToMediaModule": "true"
    },
    "description": "\n    A minimal workflow that transcodes the media into distribution formats, then\n    sends the resulting distribution files, along with their associated metadata,\n    to the distribution channels.\n  ",
    "state": "succeeded",
    "title": "Fast Testing Workflow",
    "event_identifier": "f41e4417-b841-4d20-a466-f98ddfbe4c2a"
  }
]
```

### POST /api/workflows

Creates a workflow instance.

Form Parameters                  | Required |Type                             | Description
:--------------------------------|:---------|:--------------------------------|:-----------
`event_identifier`               | yes      | [`string`](types.md#basic)      | The event identifier this workflow should run against
`workflow_definition_identifier` | yes      | [`string`](types.md#basic)      | The identifier of the workflow definition to use
`configuration`                  | no       | [`property`](types.md#property) | The optional configuration for this workflow

This request additionally supports the following query string parameters to include additional information directly in
the response:

Query String Parameter     | Type                        | Description
:--------------------------|:----------------------------|:-----------
`withoperations`           | [`boolean`](types.md#basic) | Whether the workflow operations should be included in the response
`withconfiguration`        | [`boolean`](types.md#basic) | Whether the workflow configuration should be included in the response

__Example__

event\_identifier:
```
f41e4417-b841-4d20-a466-f98ddfbe4c2a
```

workflow\_definition\_identifier:
```
fast
```

configuration:
```
{
  "publishLive": "false",
  "uploadedSearchPreview": "false",
  "publishToOaiPmh": "true",
  "comment": "false",
  "publishToMediaModule": "true"
}
```

__Response__

`201 (CREATED)`: A new workflow is created and its identifier is returned in the Location header.

```xml
Location: http://api.opencast.org/api/workflows/3170
```

The workflow instance is returned as JSON object with the following fields:

Field                 | Type                                                       | Description
:---------------------|:-----------------------------------------------------------|:-----------
`identifier`          | [`string`](types.md#basic)                                 | The unique identifier of this workflow instance
`title`               | [`string`](types.md#basic)                                 | The title of this workflow instance
`description`         | [`string`](types.md#basic)                                 | The description of this workflow instance
`tags`                | [`array[string]`](types.md#array)                          | The (potentially empty) list of workflow tags of this workflow instance
`configuration_panel` | [`string`](types.md#basic)                                 | The configuration panel of this workflow instance
`operations`          | [`array[operation_instance]`](types.md#operation_instance) | The list of operations of this workflow instance

`400 (BAD REQUEST)`: The request is invalid or inconsistent.<br/>
`404 (NOT FOUND)`: The specified workflow instance does not exist.

### GET /api/workflows/{workflow_instance_id}

Returns a single workflow instance.

This request supports the following query string parameters to include additional information directly in the response:

Query String Parameter     | Type                        | Description
:--------------------------|:----------------------------|:-----------
`withoperations`           | [`boolean`](types.md#basic) | Whether the workflow operations should be included in the response
`withconfiguration`        | [`boolean`](types.md#basic) | Whether the workflow configuration should be included in the response

__Response__

`200 (OK)`: The workflow instance is returned as JSON object with the following fields:

Field                 | Type                                                       | Description
:---------------------|:-----------------------------------------------------------|:-----------
`identifier`          | [`string`](types.md#basic)                                 | The unique identifier of this workflow instance
`title`               | [`string`](types.md#basic)                                 | The title of this workflow instance
`description`         | [`string`](types.md#basic)                                 | The description of this workflow instance
`tags`                | [`array[string]`](types.md#array)                          | The (potentially empty) list of workflow tags of this workflow instance
`configuration_panel` | [`string`](types.md#basic)                                 | The configuration panel of this workflow instance
`operations`          | [`array[operation_instance]`](types.md#operation_instance) | The list of operations of this workflow instance

`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.<br/>
`404 (NOT FOUND)`: The event or workflow definition could not be found.

__Example__

```
{
  "workflow_definition_identifier": "fast",
  "identifier": 1603,
  "creator": "Opencast Project Administrator",
  "operations": [
    {
      "identifier": 1604,
      "completion": "2018-08-08T08:46:57.3Z",
      "configuration": {
        "publishLive": "false",
        "uploadedSearchPreview": "false",
        "publishToOaiPmh": "true",
        "comment": "false",
        "publishToMediaModule": "true"
      },
      "time_in_queue": 0,
      "failed_attempts": 0,
      "start": "2018-08-08T08:46:57.201Z",
      "description": "Applying default configuration values",
      "fail_workflow_on_error": true,
      "unless": "",
      "max_attempts": 1,
      "host": "http:\/\/localhost:8080",
      "state": "succeeded",
      "operation": "defaults",
      "if": "",
      "retry_strategy": "none",
      "error_handler_workflow": ""
    },
    {
      "identifier": 1605,
      "completion": "",
      "configuration": {
        "apply-acl": "true"
      },
      "time_in_queue": 0,
      "failed_attempts": 0,
      "start": "2018-08-08T08:47:02.209Z",
      "description": "Applying access control entries from series",
      "fail_workflow_on_error": true,
      "unless": "",
      "max_attempts": 1,
      "host": "http:\/\/localhost:8080",
      "state": "skipped",
      "operation": "series",
      "if": "",
      "retry_strategy": "none",
      "error_handler_workflow": "partial-error"
    }
    ...
  ],
  "configuration": {
    "publishLive": "false",
    "workflowDefinitionId": "fast",
    "uploadedSearchPreview": "false",
    "publishToOaiPmh": "true",
    "comment": "false",
    "publishToMediaModule": "true"
  },
  "description": "\n    A minimal workflow that transcodes the media into distribution formats, then\n    sends the resulting distribution files, along with their associated metadata,\n    to the distribution channels.\n  ",
  "state": "running",
  "title": "Fast Testing Workflow",
  "event_identifier": "f41e4417-b841-4d20-a466-f98ddfbe4c2a"
}
```

### PUT /api/workflows/{workflow_instance_id}

Updates a workflow instance.

Form Parameters         | Required |Type                                         | Description
:-----------------------|:---------|:--------------------------------------------|:-----------
`state`                 | no       | [`workflow_state`](types.md#workflow_state) | The optional state transition for this workflow
`configuration`         | no       | [`property`](types.md#property)             | The optional configuration for this workflow

This request additionally supports the following query string parameters to include additional information directly in
the response:

Query String Parameter     | Type                        | Description
:--------------------------|:----------------------------|:-----------
`withoperations`           | [`boolean`](types.md#basic) | Whether the workflow operations should be included in the response
`withconfiguration`        | [`boolean`](types.md#basic) | Whether the workflow configuration should be included in the response

__Example__

state:
```
paused
```

configuration:
```
{
  "publishLive": "false",
  "uploadedSearchPreview": "false",
  "publishToOaiPmh": "true",
  "comment": "false",
  "publishToMediaModule": "true"
}
```

__Allowed workflow state transitions__

The following workflow state transitions are allowed:

Current state     | Allowed new state
:-----------------|:-----------------
`instantiated`    | `paused`, `stopped`, `running`
`running`         | `paused`, `stopped`
`failing`         | `paused`, `stopped`
`paused`          | `paused`, `stopped`, `running`
`succeeded`       | `paused`, `stopped`
`stopped`         | `paused`, `stopped`
`failed`          | `paused`, `stopped`

__Response__

`200 (OK)`: The workflow instance is updated and returned as JSON object with the following fields:

Field                 | Type                                                       | Description
:---------------------|:-----------------------------------------------------------|:-----------
`identifier`          | [`string`](types.md#basic)                                 | The unique identifier of this workflow instance
`title`               | [`string`](types.md#basic)                                 | The title of this workflow instance
`description`         | [`string`](types.md#basic)                                 | The description of this workflow instance
`tags`                | [`array[string]`](types.md#array)                          | The (potentially empty) list of workflow tags of this workflow instance
`configuration_panel` | [`string`](types.md#basic)                                 | The configuration panel of this workflow instance
`operations`          | [`array[operation_instance]`](types.md#operation_instance) | The list of operations of this workflow instance

`400 (BAD REQUEST)`: The request is invalid or inconsistent.<br/>
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.<br/>
`404 (NOT FOUND)`: The workflow instance could not be found.<br/>
`409 (CONFLICT)`: The workflow instance cannot transition to this state.

### DELETE /api/workflows/{workflow_instance_id}

Deletes a workflow instance.

__Response__

`204 (NO CONTENT)`: The workflow instance has been deleted.<br/>
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.<br/>
`404 (NOT FOUND)`: The specified workflow instance does not exist.<br/>
`409 (CONFLICT)`: The workflow instance cannot be deleted in this state.

# Workflow definitions

### GET /api/workflow-definitions

Returns a list of workflow definitions.

The following query string parameters are supported to filter, sort and pagingate the returned list:

Query String Parameter | Type                        | Description
:----------------------|:----------------------------|:-----------
`filter`               | [`string`](types.md#basic)  | A comma-separated list of filters to limit the results with (see [Filtering](usage.md#filtering)). See the below table for the list of available filters
`sort`                 | [`string`](types.md#basic)  | A comma-separated list of sort criteria (see [Sorting](usage.md#sorting)).  See the below table for the list of available sort criteria
`limit`                | [`integer`](types.md#basic) | The maximum number of results to return (see [Pagination](usage.md#pagination))
`offset`               | [`integer`](types.md#basic) | The index of the first result to return (see [Pagination](usage.md#pagination))

The following filters are available:

Filter Name    | Description
:--------------|:-----------
`tag`          | Workflow definitions where the tag is included

The list can be sorted by the following criteria:

Sort Criteria       | Description
:-------------------|:-----------
`identifier`        | By the identifier of the workflow definition
`title`             | By the title of the workflow definition
`displayorder`      | By the display order of the workflow definition

This request additionally supports the following query string parameters to include additional information directly in
the response:

Query String Parameter     | Type                        | Description
:--------------------------|:----------------------------|:-----------
`withoperations`           | [`boolean`](types.md#basic) | Whether the workflow operations should be included in the response
`withconfigurationpanel`   | [`boolean`](types.md#basic) | Whether the workflow configuration panel should be included in the response

__Sample request__

```xml
https://opencast.example.org/api/workflow-definitions?sort=title:DESC&limit=5&offset=1&filter=tag:archive
```

__Response__

`200 (OK)`: A (potentially empty) list of workflow definitions is returned. The list is represented as JSON array where
each element is a JSON object with the following fields:

Field                 | Type                                                           | Description
:---------------------|:---------------------------------------------------------------|:-----------
`identifier`          | [`string`](types.md#basic)                                     | The unique identifier of this workflow definition
`title`               | [`string`](types.md#basic)                                     | The title of this workflow definition
`description`         | [`string`](types.md#basic)                                     | The description of this workflow definition
`tags`                | [`array[string]`](types.md#array)                              | The (potentially empty) list of workflow tags of this workflow definition
`configuration_panel` | [`string`](types.md#basic)                                     | The configuration panel of this workflow definition
`operations`          | [`array[operation_definition]`](types.md#operation_definition) | The list of operations of this workflow definition

`400 (BAD REQUEST)`: The request is invalid or inconsistent.

__Example__

```
[
  {
    "id": "fast",
    "title": "Fast Testing Workflow",
    "description": "\n    A minimal workflow that transcodes the media into distribution formats, then\n    sends the resulting distribution files, along with their associated metadata,\n    to the distribution channels.\n  ",
    "tags": [
      "schedule",
      "upload"
    ],
    "configuration_panel": "\n    \n      <div id=\"workflow-configuration\">\n        <fieldset>\n          <legend>Add a comment that the recording needs:</legend>\n          <ul>\n            <li>\n              <input id=\"comment\" name=\"comment\" type=\"checkbox\" class=\"configField\" value=\"true\" />\n              <label for=\"comment\">Review / Cutting</label>\n            </li>\n          </ul>\n        </fieldset>\n        <fieldset>\n          <legend>Immediately distribute the recording to:</legend>\n          <ul>\n            <li>\n              <input id=\"publishToMediaModule\" name=\"publishToMediaModule\" type=\"checkbox\" class=\"configField\" value=\"true\" checked=checked />\n              <label for=\"publishToMediaModule\">Opencast Media Module</label>\n            </li>\n            <li>\n              <input id=\"publishToOaiPmh\" name=\"publishToOaiPmh\" type=\"checkbox\" class=\"configField\" value=\"true\" checked=checked />\n              <label for=\"publishToOaiPmh\">Default OAI-PMH Repository</label>\n            </li>\n          </ul>\n        </fieldset>\n        <fieldset>\n          <legend>Publish live stream:</legend>\n          <ul>\n            <li>\n              <input id=\"publishLive\" name=\"publishLive\" type=\"checkbox\" class=\"configField\" value=\"false\" />\n              <label for=\"publishLive\">Add live event to Opencast Media Module</label>\n            </li>\n          </ul>\n        </fieldset>\n      </div>\n    \n  ",
    "operations": [
      {
        "id": "defaults",
        "description": "Applying default configuration values",
        "configuration": {
          "publishLive": "false",
          "publishToOaiPmh": "true",
          "comment": "false",
          "publishToMediaModule": "true",
          "uploadedSearchPreview": "false"
        },
        "unless": "",
        "if": "",
        "fail_workflow_on_error": "true",
        "error_handler_workflow": "",
        "retry_strategy": "none",
        "max_attempts": "1"
      },
      {
        "id": "series",
        "description": "Applying access control entries from series",
        "configuration": {
          "apply-acl": "true"
        },
        "unless": "",
        "if": "",
        "fail_workflow_on_error": "true",
        "error_handler_workflow": "partial_error",
        "retry_strategy": "none",
        "max_attempts": "1"
      }
      ...
    ]
  }
]
```

### GET /api/workflow-definitions/{workflow_definition_id}

Returns a single workflow definition.

This request supports the following query string parameters to include additional information directly in the response:

Query String Parameter     | Type                        | Description
:--------------------------|:----------------------------|:-----------
`withoperations`           | [`boolean`](types.md#basic) | Whether the workflow operations should be included in the response
`withconfigurationpanel`   | [`boolean`](types.md#basic) | Whether the workflow configuration panel should be included in the response

__Response__

`200 (OK)`: The workflow definition is returned as JSON object with the following fields:

Field                 | Type                                                           | Description
:---------------------|:---------------------------------------------------------------|:-----------
`identifier`          | [`string`](types.md#basic)                                     | The unique identifier of this workflow definition
`title`               | [`string`](types.md#basic)                                     | The title of this workflow definition
`description`         | [`string`](types.md#basic)                                     | The description of this workflow definition
`tags`                | [`array[string]`](types.md#array)                              | The (potentially empty) list of workflow tags of this workflow definition
`configuration_panel` | [`string`](types.md#basic)                                     | The configuration panel of this workflow definition
`operations`          | [`array[operation_definition]`](types.md#operation_definition) | The list of operations of this workflow definition


`404 (NOT FOUND)`: The specified workflow definition does not exist.

__Example__

```
{
  "id": "fast",
  "title": "Fast Testing Workflow",
  "description": "\n    A minimal workflow that transcodes the media into distribution formats, then\n    sends the resulting distribution files, along with their associated metadata,\n    to the distribution channels.\n  ",
  "tags": [
    "schedule",
    "upload"
  ],
  "configuration_panel": "\n    \n      <div id=\"workflow-configuration\">\n        <fieldset>\n          <legend>Add a comment that the recording needs:</legend>\n          <ul>\n            <li>\n              <input id=\"comment\" name=\"comment\" type=\"checkbox\" class=\"configField\" value=\"true\" />\n              <label for=\"comment\">Review / Cutting</label>\n            </li>\n          </ul>\n        </fieldset>\n        <fieldset>\n          <legend>Immediately distribute the recording to:</legend>\n          <ul>\n            <li>\n              <input id=\"publishToMediaModule\" name=\"publishToMediaModule\" type=\"checkbox\" class=\"configField\" value=\"true\" checked=checked />\n              <label for=\"publishToMediaModule\">Opencast Media Module</label>\n            </li>\n            <li>\n              <input id=\"publishToOaiPmh\" name=\"publishToOaiPmh\" type=\"checkbox\" class=\"configField\" value=\"true\" checked=checked />\n              <label for=\"publishToOaiPmh\">Default OAI-PMH Repository</label>\n            </li>\n          </ul>\n        </fieldset>\n        <fieldset>\n          <legend>Publish live stream:</legend>\n          <ul>\n            <li>\n              <input id=\"publishLive\" name=\"publishLive\" type=\"checkbox\" class=\"configField\" value=\"false\" />\n              <label for=\"publishLive\">Add live event to Opencast Media Module</label>\n            </li>\n          </ul>\n        </fieldset>\n      </div>\n    \n  ",
  "operations": [
    {
      "id": "defaults",
      "description": "Applying default configuration values",
      "configuration": {
        "publishLive": "false",
        "publishToOaiPmh": "true",
        "comment": "false",
        "publishToMediaModule": "true",
        "uploadedSearchPreview": "false"
      },
      "unless": "",
      "if": "",
      "fail_workflow_on_error": "true",
      "error_handler_workflow": "",
      "retry_strategy": "none",
      "max_attempts": "1"
    },
    {
      "id": "series",
      "description": "Applying access control entries from series",
      "configuration": {
        "apply-acl": "true"
      },
      "unless": "",
      "if": "",
      "fail_workflow_on_error": "true",
      "error_handler_workflow": "partial_error",
      "retry_strategy": "none",
      "max_attempts": "1"
    }
    ...
  ]
}
```
