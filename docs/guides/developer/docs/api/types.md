# Data Types

This page defines data types that are commonly used by many External API requests. The page is divided in several
sections that start with general information about how the types are supposed to work.

[TOC]

## Basic

This section defines basic data types used in the External API specification.

Type                | Description
:-------------------|:-----------
[`string`](#basic)  | An UTF-8 encoded string
[`boolean`](#basic) | `true` or `false`
[`integer`](#basic) | An integer number, i.e. [-][0-9]+[0-9]*

## Extended

This section describes extended data types used in the External API specification.

### array

The External API makes use of JSON arrays often. We indicate the element type in the brackets, e.g.
[`array[string]`](#array).

The empty array [] is allowed.

__Examples__

```
["String1", "String2"]
```

```
[]
```

### file

This data type indicates that a file is needed as parameter.

### flavor

Opencast uses flavors to locate tracks, attachments and catalogs associated to events. Flavors have a type and a
subtype and are written in the form `type` + "/" + `subtype`.

[`flavor`](#flavor) ::= `type` + "/" + `subtype` whereas both `type` and `subtype` consist of ([a-z][A-Z][1-9])+([a-z][A-Z][1-9][+-])*

__Example__

```
dublincore/episode
```

### property

Opencast often uses sets of key-value pairs to associate properties to objects. The External API uses
JSON objects to represent those properties. Both the name of the JSON object field and its value are of type `string`.

__Example__

```
{
  "key": "value",
  "live": "true"
}
```

## Date and Time

Type                         |Encoding      | Description
:----------------------------|:-------------|:-----------
[`date`](#date-and-time)     |[ISO 8601][4] | Date
[`datetime`](#date-and-time) |[ISO 8601][4] | Date and Time

__Examples__

[`date`](#date-and-time):

```
2018-03-11
```

[`datetime`](#date-and-time):

```
2018-03-11T13:23:51Z
```

[4]: http://en.wikipedia.org/wiki/ISO_8601

## Recurrence Rule

To define a set of recurring events within a given time period, Opencast uses recurrence rules.

For more details about reccurrence rules, please refer to the
[Internet Calendaring and Scheduling Core Object Specification (iCalendar)](https://tools.ietf.org/html/rfc5545).

__Example__

```
"rrule":"FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=16;BYMINUTE=0"
```

Please note that `BYHOUR` is specified in UTC.

## Metadata Catalogs

The External API is designed to take full advantage of the powerful metadata facilities of Opencast.

Opencast distinguishes between bibliographic metadata and technical metadata:

* *Bibliographic metadata* is supposed to describe the associated objects and to be used to present those objects to
    endusers (e.g. in a video portal).
* *Technical metadata* is used by Opencast to manage objects (e.g. permissions, processing, scheduling)

For events and series, Opencast manages bibliographic metadata in metadata catalogs. There are two kind of metadata
catalogs:

* The default metadata catalogs for events (`dublincore/episode`) and series (`dublincore/series`)
* An arbitrary number of configurable extended metadata catalogs can be configured for both events and series

While the extended metadata can be fully configured, the default metadata catalogs are supposed to hold a minimum
set of defined metadata fields.

As the metadata catalogs are configurable, the External API provides means of gathering the metadata catalog
configuration.

This is done by ["/api/events/{event_id}/metadata"](events-api.md#metadata) and
["/api/series/{series_id}/metdata"](series-api.md#metadata). Those requests return self-describing metadata catalogs
that do not just contain the values of all metadata fields but also a list of available metadata fields and their
configuration.

Note that the Opencast configuration defines which metadata catalogs are available for events and series.

The following sections define data types that are used to manage metadata catalogs.

### fields

Each metadata catalogs has a list of metadata fields that is described as array of JSON objects with the following
fields:

Field          | Optional | Type                | Description
:--------------|:---------|:--------------------|:-----------
`id`           | no       | [`string`](#basic)  | Technical identifier of the metadata field
`value`        | no       | [`string`](#basic)  | Current value of the metadata field
`label`        | no       | [`string`](#basic)  | Displayable name of the metadata field (i18n)
`type`         | no       | [`string`](#basic)  | Type of the metadata field
`readOnly`     | no       | [`boolean`](#basic) | Whether this metadata field is read-only
`required`     | no       | [`boolean`](#basic) | Whether this metadata field is mandatory
`collection`   | yes      | [`string`](#basic)  | Pre-defined list of values as JSON object
`translatable` | yes      | [`boolean`](#basic) | Whether the field `value` supports i18n

__Example__

```
[
  {
    "readOnly": false,
    "id": "title",
    "label": "EVENTS.EVENTS.DETAILS.METADATA.TITLE",
    "type": "text",
    "value": "Test Event Title",
    "required": true
  },
  {
    "translatable": true,
    "readOnly": false,
    "id": "language",
    "label": "EVENTS.EVENTS.DETAILS.METADATA.LANGUAGE",
    "type": "text",
    "value": "",
    "required": false
  },
  {
    "translatable": true,
    "readOnly": false,
    "id": "license",
    "label": "EVENTS.EVENTS.DETAILS.METADATA.LICENSE",
    "type": "text",
    "value": "",
    "required": false
  },
  [...]
]
```

### values

To modifiy values of metadata catalogs, a JSON array with JSON objects contained the following fields is used:

Field   | Required | Description
:-------|:---------|:-----------
`id`    | yes      | The technical identifier of the metadata field
`value` | yes      | The value of the metadata field

Notes:

* Fields which are not included in `catalog_values` will not be updated
* Attempting to write readonly fields will result in error
* Attempting to write empty values to a required field will result in error

```
[
  {
    "id": "title",
    "value": "Captivating title - edited"
  },
  {
    "id": "creator",
    "value": ["John Clark", "Thiago Melo Costa"]
  },
  {
    "id": "description",
    "value": "A great description - edited"
  }
]
```

### catalog

Besides the metadata configuration, the full metadata catalog configuration includes some additional fields
describing the catalog itself:

Field  | Type                | Description
:------|:--------------------|:-----------
label  | [`string`](#basic)  | Displayable name of the metadata catalog
flavor | [`string`](#basic)  | The flavor of the metadata catalog
fields | [`fields`](#fields) | An array of JSON objects describing the metadata field configurations of the metadata catalogs

__Example__


```
  {
    "flavor": "dublincore/episode",
    "title": "EVENTS.EVENTS.DETAILS.CATALOG.EPISODE",
    "fields": [
      {
        "readOnly": false,
        "id": "title",
        "label": "EVENTS.EVENTS.DETAILS.METADATA.TITLE",
        "type": "text",
        "value": "Test 1",
        "required": true
      },
      [...]
    ]
  }
]

```

### catalogs

The metadata configuration including all metadata catalogs of a given objects is returned as JSON array whereas
its elements are of type [`catalog`](#catalog).

__Example__


```
[
  {
    "flavor": "dublincore/episode",
    "title": "EVENTS.EVENTS.DETAILS.CATALOG.EPISODE",
    "fields": [
      {
        "readOnly": false,
        "id": "title",
        "label": "EVENTS.EVENTS.DETAILS.METADATA.TITLE",
        "type": "text",
        "value": "Test 1",
        "required": true
      },
      {
        "readOnly": false,
        "id": "subjects",
        "label": "EVENTS.EVENTS.DETAILS.METADATA.SUBJECT",
        "type": "text",
        "value": [],
        "required": false
      },
      {
        "readOnly": false,
        "id": "description",
        "label": "EVENTS.EVENTS.DETAILS.METADATA.DESCRIPTION",
        "type": "text_long",
        "value": "",
        "required": false
      },
      [...]
    ]
  }
]

```

## Access Control Lists

Opencast uses access control lists (ACL) to manage permissions of objects. Each access control list is associated to
exactly one object and consists of a list of access control entries (ACE).
The access control entries are a list of triples &lt;`role`, `action`, `allow`&gt; which read like "Users with role
`role` are allowed to perform action `action` on the associate object if `allow` is true".

Opencast defines the following ACL actions:

Action | Description
:------|:-----------
`read` | Read access
`write`| Write access

Depending on the configuration of Opencast, there can be additional ACL actions.

### ace

The access control entries are represented as JSON objects with the following fields:

ACE field  | Required | Type                | Description
:----------|:---------|:--------------------|:-----------
`role`     | yes      | [`string`](#basic)  | The role this ACE affects
`action`   | yes      | [`string`](#basic)  | The actions this ACE affects
`allow`    | yes      | [`boolean`](#basic) | Whether role is allowed to perform the action

__Example__

```
  {
    "allow": true,
    "action": "write",
    "role": "ROLE_ADMIN"
  }
```

### acl

The access control lists are represented as JSON arrays with element type [`ace`](#ace).

__Example__

```
[
  {
    "allow": true,
    "action": "write",
    "role": "ROLE_ADMIN"
  },
  {
    "allow": true,
    "action": "read",
    "role": "ROLE_USER"
  }
]
```

## Workflow

### workflow_retry_strategy

The retry strategy of a workflow operation definition in case of an error. The following values are possible:

Value   | Description
:-------|:-----------
`none`  | no retry
`retry` | restart the operation
`hold`  | keep the operation in hold state

### workflow_state

The state the workflow instance is currently in. The following values are possible:

Value          | Description
:--------------|:-----------
`instantiated` | The workflow instance was instantiated, but is not yet running
`running`      | The workflow instance is running
`stopped`      | The workflow instance was stopped
`paused`       | The workflow instance was paused
`succeeded`    | The workflow instance has succeeded
`failed`       | The workflow instance has failed
`failing`      | The workflow instance has failed and is currently running the error handling workflow

### workflow_operation_state

The state the workflow operation instance is currently in. The following values are possible:

Value          | Description
:--------------|:-----------
`instantiated` | The workflow operation instance was instantiated, but is not yet running
`running`      | The workflow operation instance is running
`paused`       | The workflow operation instance was paused
`succeeded`    | The workflow operation instance has succeeded
`failed`       | The workflow operation instance has failed
`skipped`      | The workflow operation instance was skipped
`retry`        | The workflow operation instance has failed and a retry is currently running

### operation_definition

The workflow operation_definition entries are represented as JSON objects with the following fields:

Field                    | Type                                                  | Description
:------------------------|:------------------------------------------------------|:-----------
`operation`              | [`string`](#basic)                                    | The operation of this workflow operation definition
`description`            | [`string`](#basic)                                    | The description of this workflow operation definition
`configuration`          | [`property`](#property)                               | The configuration for this workflow operation definition
`if`                     | [`string`](#basic)                                    | The condition of this workflow operation definition that is required to be true for this operation to be executed
`unless`                 | [`string`](#basic)                                    | The condition of this workflow operation definition that is required to be false for this operation to be executed
`fail_workflow_on_error` | [`boolean`](#basic)                                   | Whether an error during this workflow operation definition fails the workflow
`error_handler_workflow` | [`string`](#basic)                                    | The identifier of the exception handler workflow of this workflow operation definition
`retry_strategy`         | [`workflow_retry_strategy`](#workflow_retry_strategy) | The retry strategy of this workflow operation definition in case of an error
`max_attempts`           | [`integer`](#basic)                                   | The number of attempts made to execute this workflow operation definition

### operation_instance

The workflow operation_instance entries are represented as JSON objects with the following fields:

Field                    | Type                                                    | Description
:------------------------|:--------------------------------------------------------|:-----------
`identifier`             | [`integer`](#basic)                                     | The unique identifier of this workflow operation instance (equals the job identifier)
`operation`              | [`string`](#basic)                                      | The operation of this workflow operation instance
`description`            | [`string`](#basic)                                      | The description of this workflow operation instance
`configuration`          | [`property`](#property)                                 | The configuration for this workflow operation instance
`state`                  | [`workflow_operation_state`](#workflow_operation_state) | The state of this workflow operation instance
`start`                  | [`datetime`](#date-and-time)                            | The start date and time of this workflow operation instance
`completion`             | [`datetime`](#date-and-time)                            | The completion date and time of this workflow operation instance
`time_in_queue`          | [`integer`](#basic)                                     | The number of milliseconds this workflow operation instance waited in a service queue
`host`                   | [`string`](#basic)                                      | The host that executed this workflow operation instance
`if`                     | [`string`](#basic)                                      | The condition of this workflow operation instance that is required to be true for this operation to be executed
`unless`                 | [`string`](#basic)                                      | The condition of this workflow operation instance that is required to be false for this operation to be executed
`fail_workflow_on_error` | [`boolean`](#basic)                                     | Whether an error during this workflow operation instance fails the workflow
`error_handler_workflow` | [`string`](#basic)                                      | The identifier of the exception handler workflow of this workflow operation instance
`retry_strategy`         | [`workflow_retry_strategy`](#workflow_retry_strategy)   | The retry strategy of this workflow operation instance in case of an error
`max_attempts`           | [`integer`](#basic)                                     | The number of attempts made to execute this workflow operation instance
`failed_attempts`        | [`integer`](#basic)                                     | The number of failed attempts to execute this workflow operation instance
