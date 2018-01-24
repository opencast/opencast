[TOC]
# General

### GET /api/events

Returns a list of events.

By setting the optional `sign` parameter to `true`, the method will pre-sign distribution urls if signing is turned on in Opencast. Remember to consider the [maximum validity of signed URLs](security-api.md#Introduction) when caching this response.

Query String Parameter     |Type            | Description
:--------------------------|:---------------|:----------------------------
`sign`                     | `boolean`      | Whether public distribution urls should be signed.
`withacl`                  | `boolean`      | Whether the acl metadata should be included in the response.
`withmetadata`             | `boolean`      | Whether the metadata catalogs should be included in the response.
`withpublications`         | `boolean`      | Whether the publication ids and urls should be included in the response.
`filter`                   | `string`       | A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon ":" and then the value to filter with so it is the form `Filter Name`:`Value to Filter With`. See the below table for the list of available filters.
`sort`                     | `string`       | Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: `Sort Name`:`ASC` or `Sort Name`:`DESC`. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory. See the below table about the available sort names in the table below.
`limit`                    | `integer`      | The maximum number of results to return for a single request.
`offset`                   | `integer`      | Number of results to skip based on the limit. 0 is the first set of results up to the limit, 1 is the second set of results after the first limit, 2 is third set of results after skipping the first two sets of results etc.

Filter Name     | Description
:---------------|:------------------
`contributors`  | Events where the contributors match.
`location`      | Events based upon the location it is scheduled in.
`series`        | Events based upon which series they are a part of.
`subject`       | Filters events based upon which subject they are a part of.
`textFilter`    | Filters events where any part of the event's metadata fields match this value.

Sort Name           | Description
:-------------------|:---------------
`title`             | By the title of the event.
`presenter`         | By the presenter of the event.
`start_date`        | By the start date of the event.
`end_date`          | By the end date of the event.
`review_status`     | By whether the event has been reviewed and approved or not.
`workflow_state`    | By the current processing state of the event. Is it scheduled to be recorded (INSTANTIATED), currently processing (RUNNING), paused waiting for a resource or user paused (PAUSED), cancelled (STOPPED), currently failing (FAILING), already failed (FAILED), or finally SUCCEEDED.
`scheduling_status` | By the current scheduling status of the event.
`series_name`       | By the series name of the event.
`location`          | By the location (capture agent) that the event will be or has been recorded on.


__Sample request__

```xml
https://opencast.example.org/api/events?sort=title:DESC&limit=5&offset=1&filter=location:ca-01
```

__Response__

`200 (OK)`: A (potentially empty) list of events is returned.

```
[
  {
    "archive_version": 2,
    "created": "2015-03-12T10:38:54Z",
    "creator": "Opencast Administrator",
    "contributors": ["Physics Department"],
    "description": "Cooling without moving parts and using only heat as an input",
    "duration": 7200000,
    "has_previews": true,
    "identifier": "e6aeb8df-a852-46cd-8128-b89de696f20e",
    "location": "physics-e-01",
    "presenters": ["Prof. A. Einstein"],
    "publication_status": [ "youtube", "internal" ],
    "processing_state": "SUCCEEDED",
    "start_time": "2015-03-20T04:00:00Z",
    "subjects": ["Space", "Final Frontier"],
    "title": "Einstein refrigerator"
  },
  {
    "archive_version": 5,
    "created": "2015-03-12T10:38:54Z",
    "creator": "Opencast Administrator",
    "contributors": ["Physics Department"],
    "description": "The history of the universe from the big bang to black holes",
    "duration": 7200000,
    "has_previews": true,
    "identifier": "f5aeb8df-a852-46cd-8128-b89de696f20e",
    "location": "physics-e-02",
    "presenters": ["Prof. Stephen Hawking"],
    "publication_status": [ "youtube", "internal" ],
    "processing_state": "SUCCEEDED",
    "start_time": "2015-03-20T04:00:00Z",
    "subjects": ["Space", "Final Frontier"],
    "title": "The Theory of Everything"
  }
]
```

<!--- ##################################################################### -->
### POST /api/events

Creates an event by sending metadata, access control list, processing instructions and files in a [multipart request](http://www.w3.org/Protocols/rfc1341/7_2_Multipart.html).

Multipart Form Parameters  |Type            | Description
:--------------------------|:---------------|:----------------------------
`acl`                      | `string`       | A collection of roles with their possible action
`metadata`                 | `string`       | Event metadata as Form param
`presenter`                | `file`         | Presenter movie track
`presentation`             | `file`         | Presentation movie track
`audio`                    | `file`         | Audio track
`processing`               | `string`       | Processing instructions task configuration

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

__Response__

`201 (CREATED)`: A new event is created and its identifier is returned in the `Location` header.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.

```xml
Location: http://api.opencast.org/api/events/e6aeb8df-a852-46cd-8128-b89de696f20e
```
```
{
  "identifier": "e6aeb8df-a852-46cd-8128-b89de696f20e"
}
```

<!--- ##################################################################### -->
### GET /api/events/{event_id}

Returns a single event.

By setting the optional `sign` parameter to `true`, the method will pre-sign distribution urls if signing is turned on in Opencast. Remember to consider the [maximum validity of signed URLs](security-api.md#Introduction) when caching this response.

Query String Parameter     |Type            | Description
:--------------------------|:---------------|:----------------------------
`sign`                     | boolean        | Whether public distribution urls should be signed.
`withacl`                  | boolean        | Whether the acl metadata should be included in the response.
`withmetadata`             | boolean        | Whether the metadata catalogs should be included in the response.
`withpublications`         | boolean        | Whether the publication ids and urls should be included in the response.

__Response__

`200 (OK)`: The event is returned.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

```
{
  "archive_version": 2,
  "created": "2015-03-12T10:38:54Z",
  "creator": "Opencast Administrator",
  "contributors": ["Physics Department"],
  "description": "Cooling without moving parts and using only heat as an input",
  "duration": 7200000,
  "has_previews": true,
  "identifier": "e6aeb8df-a852-46cd-8128-b89de696f20e",
  "location": "physics-e-01",
  "presenters": ["Prof. A. Einstein"],
  "publication_status": [ "youtube", "internal" ],
  "processing_state": "SUCCEEDED",
  "start_time": "2015-03-20T04:00:00Z",
  "subjects": ["Space", "Final Frontier"],
  "title": "Einstein refrigerator"
}
```
<!--- ##################################################################### -->
### POST /api/events/{event_id}

Updates an event.

Multipart Form Parameters  |Type            | Description
:--------------------------|:---------------|:----------------------------
`acl`                      | `string`       | A collection of roles with their possible action
`metadata`                 | `string`       | Event metadata as Form param
`presenter`                | `file`         | Presenter movie track
`presentation`             | `file`         | Presentation movie track
`audio`                    | `file`         | Audio track
`processing`               | `string`       | Processing instructions task configuration

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

<!--- ##################################################################### -->
### DELETE /api/events/{event_id}

Deletes an event.

__Response__

`204 (NO CONTENT)`: The event has been deleted.<br/>
`404 (NOT FOUND)`: The specified event does not exist.


# Access Policy

Most events in Opencast come with an access control list (ACL), containing entries that map actions to roles, either allowing or denying that action.

The section on roles in the chapter on [Authorization](authorization.md#AccessControl) will help shed some light on what kind of roles are available and how they are assigned to the current user.

<!--- ##################################################################### -->
### GET /api/events/{event_id}/acl

Returns an event's access policy.

__Response__

`200 (OK)`: The access control list for the specified event is returned.<br/>
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

<!--- ##################################################################### -->
### PUT /api/events/{event_id}/acl

Update an event's access policy.

Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`acl`                      | `string`       | Access policy

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

<!--- ##################################################################### -->
### POST /api/events/{event_id}/acl/{action}

Grants permission to execute `action` on the specified event to any user with role `role`. Note that this is a convenience method to avoid having to build and post a complete access control list.

Path Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`event_id`                 | `string`       | Event identifier
`action`                   | `string`       | The action that is allowed to be executed

Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`role`                     | `string`       | The role that is granted permission



__Sample__

```
role: "ROLE_STUDENT"
```

__Response__

`204 (NO CONTENT)`: The permission has been created in the access control list of the specified event.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

<!--- ##################################################################### -->
### DELETE /api/events/{event_id}/acl/{action}/{role}

Revokes permission to execute `action` on the specified event from any user with role `role`.

Path Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`event_id`                 | `string`       | Event identifier
`action`                   | `string`       | The action that is no longer allowed to be executed
`role`                     | `string`       | The role that is no longer granted permission

__Response__

`204 (NO CONTENT)`: The permission has been revoked from the access control list of the specified event.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

# Metadata

<!--- ##################################################################### -->
### GET /api/events/{event_id}/metadata

Returns the complete set of metadata.

__Response__

`200 (OK)`: The metadata collection is returned.<br/>
`404 (OK)`: The specified event does not exist.

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

<!--- ##################################################################### -->
### GET /api/events/{event_id}/metadata

Returns the event's metadata of the specified type. For a metadata catalog there is the flavor such as "dublincore/episode" and this is the unique type.

Query String Parameters    |Type            | Description
:--------------------------|:---------------|:----------------------------
`type`                     | String         | The type of metadata to get

__Response__

`200 (OK)`: The metadata collection is returned.<br/>
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

<!--- ##################################################################### -->
### PUT /api/events/{event_id}/metadata

Update the metadata with the matching type of the specified event. For a metadata catalog there is the flavor such as "dublincore/episode" and this is the unique type.

Query String Parameters    |Type            | Description
:--------------------------|:---------------|:----------------------------
`type`                     | String         | The type of metadata to update

Form Parameters             |Type            | Description
:---------------------------|:---------------|:----------------------------
`metadata`                  | `string`       | Metadata catalog in JSON format

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

<!--- ##################################################################### -->
### DELETE /api/events/{event_id}/metadata

Delete the metadata namespace catalog of the specified event. This will remove all fields and values of the catalog.

Query String Parameters    |Type            | Description
:--------------------------|:---------------|:----------------------------
`type`                     | String         | The type of metadata to delete

__Response__

`204 (NO CONTENT)`: The metadata of the given namespace has been updated.<br/>
`403 (FORBIDDEN)`: The main metadata catalog dublincore/episode cannot be deleted as it has mandatory fields.<br/>
`404 (NOT FOUND)`: The specified event does not exist.

# Publications

<!--- ##################################################################### -->
### GET /api/events/{event_id}/publications

Returns an event's list of publications.

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

<!--- ##################################################################### -->
### GET /api/events/{event_id}/publications/{publication_id}

Returns a single publication.

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
