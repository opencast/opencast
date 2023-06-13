[TOC]
# General

### GET /api/events

Returns a list of events.

The following query string parameters are supported to filter, sort and paginate the returned list:

Query String Parameter |Type                         | Description
:----------------------|:----------------------------|:-----------
`filter`               | [`string`](types.md#basic)  | A comma-separated list of filters to limit the results with (see [Filtering](usage.md#filtering)). See the below table for the list of available filters. Version 1.5.0 and newer support comma separated use of the `filter` keyword, creating a logical OR.
`sort`                 | [`string`](types.md#basic)  | A comma-separated list of sort criteria (see [Sorting](usage.md#sorting)).  See the below table for the list of available sort criteria
`limit`                | [`integer`](types.md#basic) | The maximum number of results to return (see [Pagination](usage.md#pagination))
`offset`               | [`integer`](types.md#basic) | The index of the first result to return (see [Pagination](usage.md#pagination))

The following filters are available:

Filter Name       | Description
:-----------------|:-----------
`contributors`    | Events where the contributors match. Can occur multiple times
`location`        | Events based upon the location it is scheduled in
`series`          | Events based upon which series they are a part of. Use the series identifier here. If using version 1.1.0 or higher, please use `is_part_of` instead
`subject`         | Filters events based upon which subject they are a part of
`textFilter`      | Filters events where any part of the event's metadata fields match this value
`identifier`      | Filters events whose identifiers match this value. Can occur multiple times (version 1.1.0 and higher)
`title`           | Filters events whose title match this value (version 1.1.0 and higher)
`description`     | Filters events whose description match this value (version 1.1.0 and higher)
`series_name`     | Filters events that belong to series with the given name (version 1.1.0 or higher)
`language`        | Filters events whose language match this value (version 1.1.0 or higher)
`created`         | Filters events whose created match this value (version 1.1.0 or higher)
`license`         | Filters events whose license match this value (version 1.1.0 or higher)
`rightsholder`    | Filters events whose rights holder matches this value (version 1.1.0 or higher)
`status`          | Filters events based on their status (version 1.1.0 or higher)
`is_part_of`      | Events based upon which series they are a part of. Use the series identifier here (version 1.1.0 or higher)
`source`          | Filter events whose source match this value (version 1.1.0 or higher)
`agent_id`        | Filter events based on the capture agent id (version 1.1.0 or higher)
`start`           | Filter events based on start date (version 1.1.0 or higher)
`technical_start` | Filter events based on the technical start date (version 1.1.0 or higher)

Note:
The filters `start` and `technical_start` expect the following value:

[`datetime`](types.md#date-and-time) + '/' + [`datetime`](types.md#date-and-time)

The list can be sorted by the following criteria:

Sort Criteria       | Description
:-------------------|:-----------
`title`             | By the title of the event
`presenter`         | By the presenter of the event
`start_date`        | By the start date of the event
`end_date`          | By the end date of the event
`review_status`     | By whether the event has been reviewed and approved or not [DEPRECATED]
`workflow_state`    | By the current processing state of the event. Is it scheduled to be recorded (INSTANTIATED), currently processing (RUNNING), paused waiting for a resource or user paused (PAUSED), cancelled (STOPPED), currently failing (FAILING), already failed (FAILED), or finally SUCCEEDED
`scheduling_status` | By the current scheduling status of the event [DEPRECATED]
`series_name`       | By the series name of the event
`location`          | By the location (capture agent) that the event will be or has been recorded on

This request additionally supports the following query string parameters to include additional information directly in
the response:

Query String Parameter     |Type                         | Description
:--------------------------|:----------------------------|:-----------
`sign`                     | [`boolean`](types.md#basic) | Whether publication urls (version 1.7.0 or higher) and distribution urls should be pre-signed.
`withacl`                  | [`boolean`](types.md#basic) | Whether the acl metadata should be included in the response.
`withmetadata`             | [`boolean`](types.md#basic) | Whether the metadata catalogs should be included in the response.
`withpublications`         | [`boolean`](types.md#basic) | Whether the publication ids and urls should be included in the response.
`withscheduling`           | [`boolean`](types.md#basic) | Whether the scheduling information should be included in the response (version 1.1.0 and higher).

By setting the optional `sign` parameter to `true`, the method will pre-sign distribution urls if URL signing is turned
on in Opencast. Starting with version 1.7.0, publication urls will also be signed if configured. Remember to consider
the [maximum validity of signed URLs](security-api.md#Introduction) when caching this response.

__Sample request__

```xml
https://opencast.example.org/api/events?sort=title:DESC&limit=5&offset=1&filter=location:ca-01
```

__Response__

`200 (OK)`: A (potentially empty) list of events is returned. The list is represented as JSON array where each element
is a JSON object with the following fields:

Field                | Type                                 | Description
:--------------------|:-------------------------------------|:-----------
`identifier`         | [`string`](types.md#basic)           | The unique identifier of the event
`creator`            | [`string`](types.md#basic)           | The technical creator of this event
`presenter`\*        | [`array[string]`](types.md#array)    | The presenters of this event
`created`\*          | [`datetime`](types.md#date-and-time) | The date and time this event was created
`subjects`\*         | [`array[string]`](types.md#array)    | The subjects of this event
`start`              | [`datetime`](types.md#date-and-time) | The technical (version < 1.4.0) or bibliographic (version >= 1.4.0) start date and time of this event
`description`\*      | [`string`](types.md#basic)           | The description of this event
`title`\*            | [`string`](types.md#basic)           | The title of this event
`processing_state`   | [`string`](types.md#basic)           | The current processing state of this event
`duration`           | [`string`](types.md#basic)           | The bibliographic duration of this event
`archive_version`    | [`string`](types.md#basic)           | The current version of this event
`contributor`\*      | [`array[string]`](types.md#array)    | The contributors of this event
`has_previews`       | [`boolean`](types.md#basic)          | Whether this event can be opened with the video editor
`location`\*         | [`string`](types.md#basic)           | The bibliographic location of this event
`publication_status` | [`array[string]`](types.md#array)    | The publications available for this event
`language`\*         | [`string`](types.md#basic)           | The language of this event (version 1.1.0 and higher)
`rightsholder`\*     | [`string`](types.md#basic)           | The rights holder of this event (version 1.1.0 and higher)
`license`\*          | [`string`](types.md#basic)           | The license of this event (version 1.1.0 and higher)
`is_part_of`\*       | [`string`](types.md#basic)           | The technical identifier of the series this event belongs to (version 1.1.0 and higher)
`series`             | [`string`](types.md#basic)           | The title of the series this event belongs to (version 1.1.0 and higher)
`source`\*           | [`string`](types.md#basic)           | The source of this event (version 1.1.0 and higher)
`status`             | [`string`](types.md#basic)           | The technical status of this event (version 1.1.0 and higher)


\* Metadata fields of metadata catalog `dublincore/episode`

__Example__

```
[
  {
    "identifier": "776d4970-bc2d-45c4-900a-1f80b7990cb8",
    "creator": "Opencast Project Administrator",
    "presenter": [
      "Prof. X",
      "Dr. Who"
    ],
    "created": "2018-03-19T16:11:48Z",
    "subjects": [
      "Mathematics",
      "Basics"
    ],
    "start": "2018-03-19T16:11:48Z",
    "description": "This lecture is about the very basics",
    "title": "Lecture 1 - The Basics",
    "processing_state": "SUCCEEDED",
    "duration": 0,
    "archive_version": 3,
    "contributor": [
      "Prof. X"
    ],
    "has_previews": true,
    "location": "",
    "publication_status": [
      "internal",
      "engage-player",
      "api",
      "oaipmh-default"
    ]
  },
  {
    "identifier": "2356d450-b42d-94xd-2a0d-3fa04745c0cb8",
    "creator": "Opencast Project Administrator",
    "presenter": [
    ],
    "created": "2018-04-20T09:38:42Z",
    "subjects": [
      "Physics"
    ],
    "start": "2018-04-23T08:00:00Z",
    "description": "The forces explained",
    "title": "Lecture 8 - Advanced stuff",
    "processing_state": "SUCCEEDED",
    "duration": 0,
    "archive_version": 2,
    "contributor": [
    ],
    "has_previews": true,
    "location": "",
    "publication_status": [
      "internal",
      "engage-player",
      "api",
      "oaipmh-default"
    ]
  }
]
```

### POST /api/events

Creates an event by sending metadata, access control list, processing instructions and files in a [multipart request](http://www.w3.org/Protocols/rfc1341/7_2_Multipart.html).

Multipart Form Parameters  | Type                            | Description
:--------------------------|:--------------------------------|:-----------
`acl`                      | [`acl`](types.md#acl)           | A collection of roles with their possible action
`metadata`                 | [`catalogs`](types.md#catalogs) | Event metadata as Form param
`presenter`                | [`file`](types.md#file)         | Presenter movie track
`presentation`             | [`file`](types.md#file)         | Presentation movie track
`audio`                    | [`file`](types.md#file)         | Audio track
`processing`               | [`string`](types.md#basic)      | Processing instructions task configuration
`scheduling`               | [`string`](types.md#basic)      | Scheduling information (version 1.1.0 and higher)

__Scheduling__

Field      | Required | Type                                 | Description
:----------|:---------|:-------------------------------------|:-----------
`agent_id` | Yes      | [`string`](types.md#basic)           | The technical identifier of the capture agent
`start`    | Yes      | [`datetime`](types.md#date-and-time) | The date and time (UTC) when the recording shall start
`end`      | Yes\*    | [`datetime`](types.md#date-and-time) | The date and time (UTC) when the recording shall end
`duration` | Yes\*    | [`integer`](types.md#basic)          | The duration of the recording in milliseconds
`rrule`    | Yes\*    | [`rrule`](types.md#recurrence-rule)  | The recurrence rule used to create multiple scheduled events

\* When creating a single scheduled event, either `end` or `duration` must be specified. When creating multiple
scheduled events, the fields `end`, `duration` and `rrule` are required

To create a single scheduled event, omit the field `rrule`. You can specify the start and end of the recording by
using either the fields `start` and `end` or the fields `start` and `duration`.
Please note that specifying both `end` and `duration` is not valid.

To create multiple scheduled events, the field `rrule` is used. This field contains the recurrence rule used to
determine the dates and times the scheduled events shall be created within the time period specified by the fields
`start` and `end` + `duration`.
Please note that `duration` is a mandatory field when creating multiple scheduled events.

__Additional Notes__

- Both `start` and `end` must be specified in UTC

__Sample__

metadata:
```
[
  {
    "flavor": "dublincore/episode",
    "fields": [
      {
        "id": "title",
        "value": "Captivating title"
      },
      {
        "id": "subjects",
        "value": ["John Clark", "Thiago Melo Costa"]
      },
      {
        "id": "description",
        "value": "A great description"
      },
      {
        "id": "startDate",
        "value": "2016-06-22"
      },
      {
        "id": "startTime",
        "value": "13:30:00Z"
      }
    ]
  }
]
```

processing:
```
{
  "workflow": "schedule-and-upload",
  "configuration": {
    "flagForCutting": "false",
    "flagForReview": "false",
    "publishToEngage": "true",
    "publishToHarvesting": "true",
    "straightToPublishing": "true"
  }
}
```

acl:
```
[
  {
    "action": "write",
    "role": "ROLE_ADMIN"
  },
  {
    "action": "read",
    "role": "ROLE_USER"
  }
]
```

scheduling a single event:
```
{
  "agent_id": "ca24",
  "start": "2018-03-27T16:00:00Z",
  "end": "2018-03-27T19:00:00Z",
  "inputs": ["default"]
}
```

scheduling multiple events:
```
{
  "agent_id": "ca24",
  "start": "2019-05-20T16:00:00Z",
  "end": "2019-06-10T19:00:00Z",
  "inputs": ["default"],
  "rrule":"FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=16;BYMINUTE=0",
  "duration":1080000;
}
```

__Response__

`200 (OK)`: Multiple new events were created (scheduling with `rrule`).<br/>
`201 (CREATED)`: A new event is created and its identifier is returned in the `Location` header.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.<br/>
`409 (CONFLICT)`: The event could not be created due to a scheduling conflict. A list of conflicting events is returned.

In case of success, when scheduling multiple events (200):
```
[
  {
    "identifier": "e6aeb8df-a852-46cd-8128-b89de696f20e"
  },
  {
    "identifier": "90e93bf6-ed95-483b-8e57-3e2de77a786f"
  }
]
```

In case of success (201):
```xml
Location: http://api.opencast.org/api/events/e6aeb8df-a852-46cd-8128-b89de696f20e
```
```
{
  "identifier": "e6aeb8df-a852-46cd-8128-b89de696f20e"
}
```

In case of a conflict (409):
```
[
  {
    "start":"2018-03-21T14:00:00Z",
    "end":"2018-03-21T16:00:00Z",
    "title":"My Event 06"
  }
]
```


### GET /api/events/{event_id}

Returns a single event.

By setting the optional `sign` parameter to `true`, the method will pre-sign distribution urls if signing is turned on
in Opencast. Starting with version 1.7.0, publication urls will also be signed if configured. Remember to consider the
[maximum validity of signed URLs](security-api.md#Introduction) when caching this response.

Query String Parameter | Type                        | Description
:----------------------|:----------------------------|:-----------
`sign`                 | [`boolean`](types.md#basic) | Whether publication urls (version 1.7.0 or higher) and distribution urls should be pre-signed.
`withacl`              | [`boolean`](types.md#basic) | Whether the acl metadata should be included in the response.
`withmetadata`         | [`boolean`](types.md#basic) | Whether the metadata catalogs should be included in the response.
`withpublications`     | [`boolean`](types.md#basic) | Whether the publication ids and urls should be included in the response.
`withscheduling`       | [`boolean`](types.md#basic) | Whether the scheduling information should be included in the response (version 1.1.0 and higher).


__Response__

`200 (OK)`: The event is returned as JSON object with the following fields:

Field                | Type                                 | Description
:--------------------|:-------------------------------------|:-----------
`identifier`         | [`string`](types.md#basic)           | The unique identifier of the event
`creator`            | [`string`](types.md#basic)           | The technical creator of this event
`presenter`\*        | [`array[string]`](types.md#array)    | The presenters of this event
`created`\*          | [`datetime`](types.md#date-and-time) | The date and time this event was created
`subjects`\*         | [`array[string]`](types.md#array)    | The subjects of this event
`start`              | [`datetime`](types.md#date-and-time) | The technical (version < 1.4.0) or bibliographic (version >= 1.4.0) start date and time of this event
`description`\*      | [`string`](types.md#basic)           | The description of this event
`title`\*            | [`string`](types.md#basic)           | The title of this event
`processing_state`   | [`string`](types.md#basic)           | The current processing state of this event
`duration`           | [`string`](types.md#basic)           | The bibliographic duration of this event
`archive_version`    | [`string`](types.md#basic)           | The current version of this event
`contributor`\*      | [`array[string]`](types.md#array)    | The contributors of this event
`has_previews`       | [`boolean`](types.md#basic)          | Whether this event can be opened with the video editor
`location`\*         | [`string`](types.md#basic)           | The bibliographic location of this event
`publication_status` | [`array[string]`](types.md#array)    | The publications available for this event
`language`\*         | [`string`](types.md#basic)           | The language of this event (version 1.1.0 and higher)
`rightsholder`\*     | [`string`](types.md#basic)           | The rights holder of this event (version 1.1.0 and higher)
`license`\*          | [`string`](types.md#basic)           | The license of this event (version 1.1.0 and higher)
`is_part_of`\*       | [`string`](types.md#basic)           | The technical identifier of the series this event belongs to (version 1.1.0 and higher)
`series`             | [`string`](types.md#basic)           | The title of the series this event belongs to (version 1.1.0 and higher)
`source`\*           | [`string`](types.md#basic)           | The source of this event (version 1.1.0 and higher)
`status`             | [`string`](types.md#basic)           | The technical status of this event (version 1.1.0 and higher)

\* Metadata fields of metadata catalog `dublincore/episode`

`404 (NOT FOUND)`: The specified event does not exist.

```
{
  "identifier": "776d4970-bc2d-45c4-900a-1f80b7990cb8",
  "creator": "Opencast Project Administrator",
  "presenter": [
    "Prof. X",
    "Dr. Who"
  ],
  "created": "2018-03-19T16:11:48Z",
  "subjects": [
    "Mathematics",
    "Basics"
  ],
  "start": "2018-03-19T16:11:48Z",
  "description": "This lecture is about the very basics",
  "title": "Lecture 1 - The Basics",
  "processing_state": "SUCCEEDED",
  "duration": 0,
  "archive_version": 3,
  "contributor": [
    "Prof. X"
  ],
  "has_previews": true,
  "location": "",
  "publication_status": [
    "internal",
    "engage-player",
    "api",
    "oaipmh-default"
  ]
}
```

### POST /api/events/{event_id}

Updates an event.

Multipart Form Parameters  |Type                             | Description
:--------------------------|:--------------------------------|:-----------
`acl`                      | [`acl`](types.md#acl)           | A collection of roles with their possible action
`metadata`                 | [`catalogs`](types.md#catalogs) | Event metadata as Form param
`presenter`                | [`file`](types.md#file)         | Presenter movie track
`presentation`             | [`file`](types.md#file)         | Presentation movie track
`audio`                    | [`file`](types.md#file)         | Audio track
`processing`               | [`string`](types.md#basic)      | Processing instructions task configuration
`scheduling`               | [`string`](types.md#basic)      | Scheduling information (version 1.1.0 and higher)

__Sample__

This sample request will update the Dublin Core metadata section of the event only.

metadata:
```
[
  {
    "flavor": "dublincore/episode",
    "fields": [
      {
        "id": "title",
        "value": "Captivating title"
      },
      {
        "id": "subjects",
        "value": ["Space", "Final Frontier"]
      },
      {
        "id": "description",
        "value": "A great description"
      }
    ]
  }
]
```

__Response__

`204 (NO CONTENT)`: The event has been updated.<br/>
`404 (NOT FOUND)`: The specified event does not exist.<br/>
`409 (CONFLICT)`: The event could not be updated due to a scheduling conflict. A list of conflicting events is returned.

In case of a conflict (when updating `scheduling`):
```
[
  {
    "start":"2018-03-21T14:00:00Z",
    "end":"2018-03-21T16:00:00Z",
    "title":"My Event 06"
  }
]
```

### DELETE /api/events/{event_id}

Retracts possible publications and deletes an event. Since version 1.6.0 published events will be retracted by this
endpoint, if you use a version previous to 1.6.0 don't call this endpoint before retracting published events.

__Response__

`202 (ACCEPTED)`: The retraction of publications has started.<br/>
`204 (NO CONTENT)`: The event has been deleted.<br/>
`404 (NOT FOUND)`: The specified event does not exist.


# Access Policy

Most events in Opencast come with an access control list (ACL), containing entries that map actions to roles, either
allowing or denying that action.

The section on roles in the chapter on [Authorization](authorization.md#AccessControl) will help shed some light on what
kind of roles are available and how they are assigned to the current user.

For more information about access control lists, please refer to [Access Control Lists](types.md#access-control-lists).

### GET /api/events/{event_id}/acl

Returns an event's access policy.

__Response__

`200 (OK)`: The access control list for the specified event is returned as [`acl`](types.md#acl).<br/>
`404 (NOT FOUND)`: The specified event does not exist.

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

### PUT /api/events/{event_id}/acl

Update an event's access policy.

Form Parameters |Type                   | Description
:---------------|:----------------------|:-----------
`acl`           | [`acl`](types.md#acl) | Access policy

Note that the existing access control list will be overwritten.

__Sample__

acl:
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

__Response__

`204 (NO CONTENT)`: The access control list for the specified event is updated.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

### POST /api/events/{event_id}/acl/{action}

Grants permission to execute `action` on the specified event to any user with role `role`. Note that this is a
convenience method to avoid having to build and post a complete access control list.

Path Parameters |Type                        | Description
:---------------|:---------------------------|:-----------
`event_id`      | [`string`](types.md#basic) | Event identifier
`action`        | [`string`](types.md#basic) | The action that is allowed to be executed

Form Parameters |Type                        | Description
:---------------|:---------------------------|:-----------
`role`          | [`string`](types.md#basic) | The role that is granted permission


Note that other access control lists entries will not be affected by this request.

__Sample__

```
role: "ROLE_STUDENT"
```

__Response__

`204 (NO CONTENT)`: The permission has been created in the access control list of the specified event.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

### DELETE /api/events/{event_id}/acl/{action}/{role}

Revokes permission to execute `action` on the specified event from any user with role `role`.

Path Parameters |Type                        | Description
:---------------|:---------------------------|:-----------
`event_id`      | [`string`](types.md#basic) | Event identifier
`action`        | [`string`](types.md#basic) | The action that is no longer allowed to be executed
`role`          | [`string`](types.md#basic) | The role that is no longer granted permission

Note that other access control lists entries will not be affected by this request.

__Response__

`204 (NO CONTENT)`: The permission has been revoked from the access control list of the specified event.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

# Media

### GET /api/events/{event_id}/media

Returns the complete set of media tracks.

__Response__

`200 (OK)`: The list of media tracks is returned.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

```
[
  {
    "duration": 184669,
    "flavor": "presenter\/source",
    "identifier": "ea9a6cd8-85d2-4fb5-8571-29dc8b9c5940",
    "mimetype": "video\/mp4",
    "size": 65072115,
    "streams": {
      "video-1": {
        "identifier": "video-1",
        "framecount": 4430,
        "framewidth": 2048,
        "framerate": 24.0,
        "format": "H.264 \/ AVC \/ MPEG-4 AVC \/ MPEG-4 part 10",
        "bitrate": 2685332.0,
        "frameheight": 858
      },
      "audio-1": {
        "identifier": "audio-1",
        "channels": 2,
        "framecount": 7954,
        "format": "AAC (Advanced Audio Coding)",
        "bitrate": 128878.0,
        "samplingrate": 44100
      }
    },
    "checksum": "ac0e3138be69927c0fb6ff4ea7465df0 (md5)",
    "description": "",
    "has_audio": true,
    "has_video": true,
    "is_master_playlist": false,
    "is_live": false,
    "element-description": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/ea9a6cd8-85d2-4fb5-8571-29dc8b9c5940\/5\/presenter.mp4",
    "uri": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/ea9a6cd8-85d2-4fb5-8571-29dc8b9c5940\/5\/presenter.mp4",
    "tags": [
      "archive"
    ]
  },
  {
    "duration": 184606,
    "flavor": "presenter\/delivery",
    "identifier": "28fba154-cdbd-4bd8-b6b7-829ad1ea6746",
    "mimetype": "video\/mp4",
    "size": 16513961,
    "streams": {
      "video-1": {
        "identifier": "video-1",
        "framecount": 4429,
        "framewidth": 854,
        "framerate": 24.0,
        "format": "H.264 \/ AVC \/ MPEG-4 AVC \/ MPEG-4 part 10",
        "bitrate": 582517.0,
        "frameheight": 358
      },
      "audio-1": {
        "identifier": "audio-1",
        "channels": 2,
        "format": "AAC (Advanced Audio Coding)",
        "bitrate": 128965.0,
        "samplingrate": 44100
      }
    },
    "checksum": "e6a5fad8100fd5b09920e6a6dddc44dc (md5)",
    "description": "",
    "has_audio": true,
    "has_video": true,
    "is_master_playlist": false,
    "is_live": false,
    "element-description": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/28fba154-cdbd-4bd8-b6b7-829ad1ea6746\/5\/84066_segment_0_480p.mp4",
    "uri": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/28fba154-cdbd-4bd8-b6b7-829ad1ea6746\/5\/84066_segment_0_480p.mp4",
    "tags": [
      "480p-quality",
      "archive"
    ]
  },
  {
    "duration": 184606,
    "flavor": "presenter\/delivery",
    "identifier": "3f359b10-da82-4834-8e65-741bc62dcdaf",
    "mimetype": "video\/mp4",
    "size": 27365772,
    "streams": {
      "video-1": {
        "identifier": "video-1",
        "framecount": 4429,
        "framewidth": 1280,
        "framerate": 24.0,
        "format": "H.264 \/ AVC \/ MPEG-4 AVC \/ MPEG-4 part 10",
        "bitrate": 1052783.0,
        "frameheight": 536
      },
      "audio-1": {
        "identifier": "audio-1",
        "channels": 2,
        "format": "AAC (Advanced Audio Coding)",
        "bitrate": 128965.0,
        "samplingrate": 44100
      }
    },
    "checksum": "6f547c80f9cc4166ccea52192ce82045 (md5)",
    "description": "",
    "has_audio": true,
    "has_video": true,
    "is_master_playlist": false,
    "is_live": false,
    "element-description": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/3f359b10-da82-4834-8e65-741bc62dcdaf\/5\/84066_segment_1_720p.mp4",
    "uri": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/3f359b10-da82-4834-8e65-741bc62dcdaf\/5\/84066_segment_1_720p.mp4",
    "tags": [
      "720p-quality",
      "archive"
    ]
  },
  {
    "duration": 185000,
    "flavor": "presenter\/delivery",
    "identifier": "db9e1496-5044-4451-9070-18399e20791a",
    "mimetype": "application\/x-mpegURL",
    "size": 462,
    "streams": {
      "video-1": {
        "identifier": "video-1",
        "framecount": 4440,
        "framewidth": 854,
        "framerate": 24.0,
        "format": "H.264 \/ AVC \/ MPEG-4 AVC \/ MPEG-4 part 10",
        "bitrate": 201482.0,
        "frameheight": 358
      },
      "audio-1": {
        "identifier": "audio-1",
        "channels": 2,
        "format": "AAC (Advanced Audio Coding)",
        "bitrate": 130322.0,
        "samplingrate": 44100
      },
      "video-2": {
        "identifier": "video-2",
        "framecount": 4440,
        "framewidth": 1280,
        "framerate": 24.0,
        "format": "H.264 \/ AVC \/ MPEG-4 AVC \/ MPEG-4 part 10",
        "bitrate": 386268.0,
        "frameheight": 536
      },
      "audio-2": {
        "identifier": "audio-2",
        "channels": 2,
        "format": "AAC (Advanced Audio Coding)",
        "bitrate": 130322.0,
        "samplingrate": 44100
      }
    },
    "checksum": "053aaad6a0ada363e17c787e78a2b349 (md5)",
    "description": "",
    "has_audio": true,
    "has_video": true,
    "is_master_playlist": true,
    "is_live": false,
    "element-description": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/db9e1496-5044-4451-9070-18399e20791a\/5\/presenter.m3u8",
    "uri": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/db9e1496-5044-4451-9070-18399e20791a\/5\/presenter.m3u8",
    "tags": [
      "archive"
    ]
  },
  {
    "duration": 185000,
    "flavor": "presenter\/delivery",
    "identifier": "dfd0a8a6-be37-46fa-993e-997b0aad064f",
    "mimetype": "application\/x-mpegURL",
    "size": 3991,
    "streams": {
      "video-1": {
        "identifier": "video-1",
        "framecount": 4440,
        "framewidth": 854,
        "framerate": 24.0,
        "format": "H.264 \/ AVC \/ MPEG-4 AVC \/ MPEG-4 part 10",
        "bitrate": 201482.0,
        "frameheight": 358
      },
      "audio-1": {
        "identifier": "audio-1",
        "channels": 2,
        "format": "AAC (Advanced Audio Coding)",
        "bitrate": 130322.0,
        "samplingrate": 44100
      }
    },
    "checksum": "d7109c109cf955b2d6e9ebdf0ad07f6c (md5)",
    "description": "",
    "has_audio": true,
    "has_video": true,
    "is_master_playlist": false,
    "is_live": false,
    "element-description": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/dfd0a8a6-be37-46fa-993e-997b0aad064f\/5\/84066_variant_0.m3u8",
    "uri": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/dfd0a8a6-be37-46fa-993e-997b0aad064f\/5\/84066_variant_0.m3u8",
    "tags": [
      "archive"
    ]
  },
  {
    "duration": 185000,
    "flavor": "presenter\/delivery",
    "identifier": "d94e2fbc-8f65-4b21-b7b9-cf2dcee43eeb",
    "mimetype": "application\/x-mpegURL",
    "size": 4009,
    "streams": {
      "video-1": {
        "identifier": "video-1",
        "framecount": 4440,
        "framewidth": 1280,
        "framerate": 24.0,
        "format": "H.264 \/ AVC \/ MPEG-4 AVC \/ MPEG-4 part 10",
        "bitrate": 386268.0,
        "frameheight": 536
      },
      "audio-1": {
        "identifier": "audio-1",
        "channels": 2,
        "format": "AAC (Advanced Audio Coding)",
        "bitrate": 130322.0,
        "samplingrate": 44100
      }
    },
    "checksum": "720c93ec46bba9fa14cf1b5761b0458e (md5)",
    "description": "",
    "has_audio": true,
    "has_video": true,
    "is_master_playlist": false,
    "is_live": false,
    "element-description": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/d94e2fbc-8f65-4b21-b7b9-cf2dcee43eeb\/5\/84066_variant_1.m3u8",
    "uri": "https:\/\/admin.opencast.org\/assets\/assets\/3627bd37-7681-4e4b-bff1-a7d299be38cd\/d94e2fbc-8f65-4b21-b7b9-cf2dcee43eeb\/5\/84066_variant_1.m3u8",
    "tags": [
      "archive"
    ]
  }
]
```

### POST /api/events/{event_id}/track

Adds the given track to the given flavor in the event. 

Path Parameters |Type                        | Description
:---------------|:---------------------------|:-----------
`event_id`      | [`string`](types.md#basic) | Event identifier

Multipart Form Parameters  |Type                             | Description
:--------------------------|:--------------------------------|:-----------
`flavor`                   | [`string`](types.md#basic)      | Denotes type and subtype, e.g. 'captions/source+en'
`overwriteExisting`        | [`boolean`](types.md#basic)     | If true, all other tracks in the specified flavor are REMOVED
`track`                    | [`file`](types.md#file)         | The track file

__Response__

`200 (OK)`: The track was added successfully.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.<br/>
`404 (NOT FOUND)`: The specified event does not exist.<br/>


# Metadata

This section describes how to use the External API to work with metadata catalogs associated to events.

Opencast manages the bibliographic metadata of series using metadata catalogs which are identified by flavors.
The default metadata catalog for Opencast events has the flavor `dublincore/episode`. Opencast additionally supports
extended metadata catalogs for series that can be configured.

The External API supports both the default event metadata catalog and events extended metadata catalogs.
For the default event metadata catalog, the metadata is directly returned in the responses.

Since the metadata catalogs can be configured, the External API provides a facility to retrieve the catalog
configuration of series metadata catalogs. For more details about this mechanism, please refer to
["Metadata Catalogs"](types.md#metadata-catalogs).

### GET /api/events/{event_id}/metadata

Returns the complete set of metadata.

__Response__

`200 (OK)`: The metadata collection is returned as [`catalogs`](types.md#catalogs).<br/>
`404 (NOT FOUND)`: The specified event does not exist.

```
[
  {
    "label": "EVENTS.EVENTS.DETAILS.CATALOG.EPISODE",
    "flavor": "dublincore/episode",
    "fields": [
      {
        "id": "title",
        "readOnly": false,
        "value": "Captivating title",
        "label": "EVENTS.EVENTS.DETAILS.METADATA.TITLE",
        "type": "text",
        "required": true
      },
      {
        "id": "description",
        "readOnly": false,
        "value": "A great description",
        "label": "EVENTS.EVENTS.DETAILS.METADATA.DESCRIPTION",
        "type": "text_long",
        "required": false
      }
    ]
  },
  {
    "label": "EVENTS.EVENTS.DETAILS.CATALOG.LICENSE",
    "flavor": "license/episode",
    "fields": [
      {
        "id": "license",
        "readOnly": false,
        "value": "CCND",
        "label": "EVENTS.EVENTS.DETAILS.METADATA.LICENSE",
        "collection": {
          "BSD": "EVENTS.LICENSE.BSD",
          "GPL3": "EVENTS.LICENSE.GPL",
          "CCND": "EVENTS.LICENSE.CCND"
        },
        "type": "text",
        "required": false
      }
    ]
  }
]
```

### GET /api/events/{event_id}/metadata

Returns the event's metadata of the specified type. For a metadata catalog there is the flavor such as
`dublincore/episode` and this is the unique type.

Query String Parameters |Type                         | Description
:-----------------------|:----------------------------|:-----------
`type`                  | [`flavor`](types.md#flavor) | The type of metadata to get

__Response__

`200 (OK)`: The metadata collection is returned as [`fields`](types.md#fields).<br/>
`404 (NOT FOUND)`: The specified event does not exist.

```
[
  {
    "id": "title",
    "readOnly": false,
    "value": "Captivating title",
    "label": "EVENTS.EVENTS.DETAILS.METADATA.TITLE",
    "type": "text",
    "required": true
  },
  {
    "id": "description",
    "readOnly": false,
    "value": "A great description",
    "label": "EVENTS.EVENTS.DETAILS.METADATA.DESCRIPTION",
    "type": "text_long",
    "required": false
  }
]
```

### PUT /api/events/{event_id}/metadata

Update the metadata with the matching type of the specified event. For a metadata catalog there is the flavor such as
`dublincore/episode` and this is the unique type.

Query String Parameters |Type                         | Description
:-----------------------|:----------------------------|:-----------
`type`                  | [`flavor`](types.md#flavor) | The type of metadata to update

Form Parameters |Type                         | Description
:---------------|:----------------------------|:-----------
`metadata`      | [`values`](types.md#values) | Metadata fields and values to be updated

Note that metadata fields not included in the form parameter `metadata` will not be updated.

__Sample__

metadata:
```
[
  {
    "id": "title",
    "value": "Captivating title"
  },
  {
    "id": "subjects",
    "value": ["Space", "Final Frontier"]
  },
  {
    "id": "description",
    "value": "A great description"
  }
]
```

__Response__

`204 (NO CONTENT)`: The metadata of the given namespace has been updated.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

### DELETE /api/events/{event_id}/metadata

Delete the metadata namespace catalog of the specified event. This will remove all fields and values of the catalog.

Query String Parameters |Type                         | Description
:-----------------------|:----------------------------|:----------------------------
`type`                  | [`flavor`](types.md#flavor) | The type of metadata to delete

Note that the metadata catalog of type `dublincore/episode` cannot be deleted.

__Response__

`204 (NO CONTENT)`: The metadata of the given namespace has been updated.<br/>
`403 (FORBIDDEN)`: The main metadata catalog dublincore/episode cannot be deleted as it has mandatory fields.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

# Publications

### GET /api/events/{event_id}/publications

Returns an event's list of publications.

Query String Parameter     |Type                         | Description
:--------------------------|:----------------------------|:-----------
`sign`                     | [`boolean`](types.md#basic) | Whether publication urls (version 1.7.0 or higher) and distribution urls should be pre-signed.

By setting the optional `sign` parameter to `true`, the method will pre-sign distribution urls if URL signing is turned
on in Opencast. Starting with version 1.7.0, publication urls will also be signed if configured. Remember to consider
the [maximum validity of signed URLs](security-api.md#Introduction) when caching this response.

__Response__

`200 (OK)`: The list of publications is returned.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

```
[
  {
    "id":"publication-1",
    "channel":"engage",
    "mediatype":"text/html",
    "url":"http://engage.opencast.org/engage/ui/player.html?id=123"
  },
  {
    "id":"publication-2",
    "channel":"oaipmh",
    "mediatype":"text/html",
    "url":"http://oaipmh.opencast.org/default/?verb=GetRecord&id=123"
  }
]
```

### GET /api/events/{event_id}/publications/{publication_id}

Returns a single publication.

Query String Parameter     |Type                         | Description
:--------------------------|:----------------------------|:-----------
`sign`                     | [`boolean`](types.md#basic) | Whether publication urls (version 1.7.0 or higher) and distribution urls should be pre-signed.

By setting the optional `sign` parameter to `true`, the method will pre-sign distribution urls if URL signing is turned
on in Opencast. Starting with version 1.7.0, publication urls will also be signed if configured. Remember to consider
the [maximum validity of signed URLs](security-api.md#Introduction) when caching this response.

__Response__

`200 (OK)`: The track details are returned.<br/>
`404 (NOT FOUND)`: The specified event does not exist.</br>
`404 (NOT FOUND)`: The specified publication does not exist.

```
{
  "id":"publication-1",
  "channel":"engage",
  "mediatype":"text/html",
  "url":"http://engage.opencast.org/engage/ui/player.html?id=123",
  "media":[
    {
      "id":"track-1",
      "mediatype":"video/mp4",
      "url":"http://download.opencast.org/123/presenter.mp4",
      "flavor":"presenter/delivery",
      "size":84938490,
      "checksum":"58308405383094",
      "tags":[

      ],
      "has_audio":true,
      "has_video":true,
      "is_master_playlist": false,
      "is_live": false,
      "duration":3648,
      "description":"Video: h264 (Constrained Baseline) (avc1 / 0x31637661), yuv420p, 640x360, 447 kb/s, 25 fps, 25"
    },
    {
      "id":"track-2",
      "mediatype":"audio/aac",
      "url":"http://download.opencast.org/123/presenter.m4a",
      "flavor":"presenter/audio",
      "size":9364,
      "checksum":"839478372",
      "tags":[

      ],
      "has_audio":true,
      "has_video":false,
      "is_master_playlist": false,
      "is_live": false,
      "duration":3648,
      "description":"aac (mp4a / 0x6134706D), 44100 Hz, stereo, fltp, 96 kb/s (default)"
    }
  ],
  "attachments":[
    {
      "id":"attachment-1",
      "mediatype":"image/png",
      "url":"http://download.opencast.org/123/preview.png",
      "flavor":"presenter/preview",
      "size":62728,
      "checksum":"389475737",
      "tags":[

      ]
    }
  ],
  "metadata":[
    {
      "id":"catalog-1",
      "mediatype":"text/xml",
      "url":"http://download.opencast.org/123/dublincore.xml",
      "flavor":"dublincore/episode",
      "size":364,
      "checksum":"18498498383",
      "tags":[

      ]
    }
  ]
}
```

# Scheduling Information

### GET /api/events/{event_id}/scheduling

Available since API version 1.1.0.

Returns an event's scheduling information.

__Response__

`200 (OK)`: The scheduling information is returned.<br/>
`204 (NO CONTENT)`: The event is not scheduled.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

```
{
  "agent_id": "ca24",
  "start": "2018-03-27T16:00:00Z",
  "end": "2018-03-27T19:00:00Z",
  "inputs": ["default"]
}
```

### PUT /api/events/{event_id}/scheduling

Available since API version 1.1.0.

Update the scheduling information of the event with id `{event_id}`.

Form Parameters             |Type            | Description                              | Default | Version
:---------------------------|:---------------|:-----------------------------------------|:--------|:-------
`scheduling`                | `string`       | The scheduling information.              | <span class="required">Required</span>| 1.1.0
`allowConflict`             | `boolean`      | Allow conflicts when updating scheduling.| false   | 1.2.0

__Sample__

scheduling:
```
{
  "agent_id": "ca24",
  "start": "2018-03-27T16:00:00Z",
  "end": "2018-03-27T19:00:00Z",
  "inputs": ["default"]
}
```

__Response__

`204 (NO CONTENT)`: The scheduling information of the event has been updated.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.<br/>
`404 (NOT FOUND)`: The specified event does not exist.<br/>
`409 (CONFLICT)`: The scheduling information could not be updated due to a conflict. A list of conflicting events is returned.

In case of a conflict:
```
[
  {
    "start":"2018-03-21T14:00:00Z",
    "end":"2018-03-21T16:00:00Z",
    "title":"My Event 06"
  }
]
```

`allowConflict` allows the schedule to be updated without checking for conflicts.
To allow conflicts (`true`) the call **MUST** be made with a user that has an _Administrative Role_.

If not handled properly this will likely cause two or more events to be
scheduled on a particular capture agent at the same time, which will then
cause a capture failure for all but one of the events.

The person making this call and allowing conflicts to exist, will bear the
responsibility of resolving the conflicts that might result.
