[TOC]

# General

### GET /api/series

Returns a list of series.

The following query string parameters are supported to filter, sort and pagingate the returned list:

Query String Parameter |Type                         | Description
:----------------------|:----------------------------|:-----------
`filter`               | [`string`](types.md#basic)  | A comma-separated list of filters to limit the results with (see [Filtering](usage.md#filtering)). See the below table for the list of available filters
`sort`                 | [`string`](types.md#basic)  | A comma-separated list of sort criteria (see [Sorting](usage.md#sorting)).  See the below table for the list of available sort criteria
`limit`                | [`integer`](types.md#basic) | The maximum number of results to return (see [Pagination](usage.md#pagination))
`offset`               | [`integer`](types.md#basic) | The index of the first result to return (see [Pagination](usage.md#pagination))

The following filters are available:

Filter Name    | Description
:--------------|:-----------
`contributors` | Series where the contributors specified in the metadata field match. Can occur multiple times
`Creator`      | Series where the creator specified in the metadata field match (please use `creator` for version 1.1.0 and higher instead)
`creationDate` | Series that were created between two dates. The two dates are in UTC format to the second i.e. yyyy-MM-ddTHH:mm:ssZ e.g. 2014-09-27T16:25Z. They are seperated by a forward slash (url encoded or not) so an example of the full filter would be CreationDate:2015-05-08T00:00:00.000Z/2015-05-10T00:00:00.000Z
`language`     | Series based upon the language specified
`license`      | Series based upon the license specified
`organizers`   | Series where the organizers specified in the metadata field match. Can occur multiple times
`managedAcl`   | Series who have the same managed acl name
`subject`      | By the subject they are a part of. Can occur multiple times
`textFilter`   | Filters series where any part of the series' metadata fields match this value
`title`        | By the title of the series
`identifier`   | By the technical identifiers of the series. Can occur multiple times (version 1.1.0 and higher)
`desription`   | By the description of the series (version 1.1.0 and higher)
`creator`      | Series where the creator specified in the metadata field match (version 1.1.0 and higher)
`publishers`   | Series where the publishers specified in the metadata field match. Can occur multiple times (version 1.1.0 and higher)
`rightsholder` | By the rights holder of the series (version 1.1.0 and higher)

The list can be sorted by the following criteria:

Sort Criteria  | Description
:--------------|:-----------
`contributors` | By the series contributors
`created`      | By when the series was created
`creator`      | By who created the series
`title`        | By the title of the series

__Sample request__
```xml
https://opencast.domain.com/api/series?filter=creator:Default Administrator&sort=title:ASC&limit=2&offset=1
```

__Response__

`200 (OK)`: A (potentially empty) list of series is returned as JSON array contained JSON objects describing the series:

Field            | Type                                 | Description
:----------------|:-------------------------------------|:-----------
`identifier`     | [`string`](types.md#basic)           | The unique identifier of the series
`created`\*      | [`datetime`](types.md#date-and-time) | The data when the series was created
`creator`        | [`string`](types.md#basic)           | The name of the user that has created the series
`title`\*        | [`string`](types.md#basic)           | The title of the series
`contributors`\* | [`array[string]`](types.md#array)    | The contributors of the series
`publishers`\*   | [`array[string]`](types.md#array)    | The publishers of the series
`subjects`\*     | [`array[string]`](types.md#array)    | The subjects of the series
`organizers`\*   | [`array[string]`](types.md#array)    | The organizers of the series
`description`\*  | [`string`](types.md#basic)           | The description of the series (version 1.1.0 and higher)
`language`\*     | [`string`](types.md#basic)           | The language of the series (version 1.1.0 and higher)
`license`\*      | [`string`](types.md#basic)           | The license of the series (version 1.1.0 and higher)
`rightsholder`\* | [`string`](types.md#basic)           | The rights holder of the series (version 1.1.0 and higher)

\* Metadata fields from the default metadata catalog `dublincore/series`


__Example__

```
[
  {
    "identifier": "dc11ab0a-fd5b-462d-a939-0a4703df38cf",
    "creator": "Opencast Project Administrator",
    "created": "2018-03-19T15:40:21Z",
    "subjects": [
      "Mathematics"
    ],
    "organizers": [
      "John Doe",
      "Prof. X"
    ],
    "publishers": [
      "University of Prof. X"
    ],
    "contributors": [
      "Hans Muster",
      "Maria Müller"
    ],
    "title": "Advanced Mathematics"
  },
  {
    "identifier": "6a4462ca-3783-432a-81c3-962ca756dc6f",
    "creator": "Opencast Project Administrator",
    "created": "2018-03-19T15:41:20Z",
    "subjects": [
      "Physics",
      "Mathematics"
    ],
    "organizers": [
      "Dr. Who"
    ],
    "publishers": [
      "University of Prof. X",
      "Doctor Who"
    ],
    "contributors": [
      "Dr. Who"
    ],
    "title": "Basics of Physics"
  }
]
```

### GET /api/series/{series_id}

Returns a single series.

__Response__

`200 (OK)`: The series is returned as a JSON object containing the following fields:

Field            | Type                                 | Description
:----------------|:-------------------------------------|:-----------
`identifier`     | [`string`](types.md#basic)           | The unique identifier of the series
`created`\*      | [`datetime`](types.md#date-and-time) | The data when the series was created
`creator`        | [`string`](types.md#basic)           | The name of the user that has created the series
`title`\*        | [`string`](types.md#basic)           | The title of the series
`contributors`\* | [`array[string]`](types.md#array)    | The contributors of the series
`publishers`\*   | [`array[string]`](types.md#array)    | The publishers of the series
`subjects`\*     | [`array[string]`](types.md#array)    | The subjects of the series
`organizers`\*   | [`array[string]`](types.md#array)    | The organizers of the series
`organization`\* | [`string`](types.md#basic)           | The identifier of the tenant this series belongs to
`opt_out`        | [`string`](types.md#basic)           | Field is not used
`language`\*     | [`string`](types.md#basic)           | The language of the series (version 1.1.0 and higher)
`license`\*      | [`string`](types.md#basic)           | The license of the series (version 1.1.0 and higher)
`rightsholder`\* | [`string`](types.md#basic)           | The rights holder of the series (version 1.1.0 and higher)

\* Fields from the default metadata catalog `dublincore/series`

`404 (NOT FOUND)`: The specified series does not exist.

__Example__

```
{
  "identifier": "dc11ab0a-fd5b-462d-a939-0a4703df38cf",
  "creator": "Opencast Project Administrator",
  "opt_out": false,
  "created": "2018-03-19T15:40:21Z",
  "subjects": [
    "Mathematics"
  ],
  "organization": "mh_default_org",
  "organizers": [
    "John Doe",
    "Prof. X"
  ],
  "description": "This is a advanced mathematics course",
  "publishers": [
    "University of Prof. X"
  ],
  "contributors": [
    "Hans Muster",
    "Maria Müller"
  ],
  "title": "Advanced Mathematics"
}

```

### POST /api/series

Creates a series.

Form Parameters | Required |Type                             | Description
:---------------|:---------|:--------------------------------|:-----------
`metadata`      | yes      | [`catalogs`](types.md#catalogs) | Series metadata
`acl`           | no       | [`acl`](types.md#acl)           | A collection of roles with their possible action
`theme`         | no       | [`string`](types.md#string)     | The theme ID to be applied to the series

__Sample__

metadata:
```
[
  {
    "label": "Opencast Series DublinCore",
    "flavor": "dublincore/series",
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
      }
    ]
  }
]
```

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

theme:
```
"1234"
```

__Response__

`201 (CREATED)`: A new series is created and its identifier is returned in the `Location` header.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.<br/>
`401 (UNAUTHORIZED)`: The user doesn't have the rights to create the series.

```xml
Location: http://api.opencast.org/api/series/4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f
```
```
{
  "identifier": "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f"
}
```

### DELETE /api/series/{series_id}

Deletes a series

__Response__

`204 (NO CONTENT)`: The series has been deleted.<br/>
`404 (NOT FOUND)`: The specified series does not exist.


## Metadata

This section describes how to use the External API to work with metadata catalogs associated to series.

Opencast manages the bibliographic metadata of series using metadata catalogs which are identified by flavors.
The default metadata catalog for Opencast series has the flavor `dublincore/series`. Opencast additionally supports
extended metadata catalogs for series that can be configured.

The External API supports both the default series metadata catalog and series extended metadata catalogs.
For the default series metadata catalog, the metadata is directly returned in the responses.

Since the metadata catalogs can be configured, the External API provides a facility to retrieve the catalog
configuration of series metadata catalogs. For more details about this mechanism, please refer to
["Metadata Catalogs"](types.md#metadata-catalogs).

### GET /api/series/{series_id}/metadata

Returns a series' metadata of all types.

__Response__

`200 (OK)`: The series' metadata are returned as [`catalogs`](types.md#catalogs)
`404 (NOT FOUND)`: The specified series does not exist.

__Example__

```
[
  {
    "label": "EVENTS.EVENTS.DETAILS.CATALOG.EPISODE",
    "flavor": "dublincore/series",
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
    "flavor": "license/series",
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

### GET /api/series/{series_id}/metadata

Returns a series' metadata collection of the given type when the query string parameter type is specified. For each
metadata catalog there is a unique property called the flavor such as dublincore/series so the type in this example
would be "dublincore/series".


Query String Parameters |Type                         | Description
:-----------------------|:----------------------------|:-----------
`type`                  | [`flavor`](types.md#flavor) | The type of metadata to return

__Response__

`200 (OK)`: The series' metadata are returned as [`fields`](types.md#fields) above.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

__Example__

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

### PUT /api/series/{series_id}/metadata

Update a series' metadata of the given type.

Query String Parameters | Required | Type                        | Description
:-----------------------|:---------|:----------------------------|:-----------
`type`                  | yes      | [`flavor`](types.md#flavor) | The type of metadata to update


Form Parameters | Required | Type                        | Description
:---------------|:---------|:----------------------------|------------
`metadata`      | yes      | [`values`](types.md#values) | Series metadata as Form param

Note that metadata fields not contained in the argument won't be updated.

__Example__

metadata:
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

__Response__

`200 (OK)`: The series' metadata have been updated.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

```
Returns: The full metadata catalog of the series
```

### DELETE /api/series/{series_id}/metadata

Deletes a series' metadata catalog of the given type. All fields and values of that catalog will be deleted.

Query String Parameters | Required | Type                        | Description
:-----------------------|:---------|:----------------------------|:-----------
`type`                  | yes      | [`flavor`](types.md#flavor) | The type of metadata to delete

Note that the default metadata catalog (type dublincore/series) cannot be deleted.

__Response__

`204 (NO CONTENT)`: The metadata have been deleted.<br/>
`403 (FORBIDDEN)`: The main metadata catalog dublincore/series cannot be deleted as it has mandatory fields.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

# Access Policy

### GET /api/series/{series_id}/acl

Returns a series' access policy.

__Response__

`200 (OK)`: The series' access policy of type [`acl`](types.md#acl) is returned.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

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

### PUT /api/series/{series_id}/acl

Updates a series' access policy.

Parameters | Required | Type                  | Description
:----------|:---------|:----------------------|:-----------
`acl`      | yes      | [`acl`](types.md#acl) | Access policy to be applied

Note that the existing access policy will be overwritten.

__Response__

`200 (OK)`: The updated access control list of type [`acl`](types.md#acl) is returned.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

__Example__

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

returns

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

# Properties

Properties can be assigned to series in means of key-value pairs. Both the property name (key) and property value are
of type `string`.

### GET /api/series/{series_id}/properties

Returns a series' properties.

__Response__

`200 (OK)`: The series' properties are returned as [`property`](types.md#property)  <br/>
`404 (NOT FOUND)`: The specified series does not exist.

__Example__

```
{
  "ondemand": "true",
  "live": "false"
}
```

### PUT /api/series/{series_id}/properties

Add or update properties of a series.

Form parameters | Required | Type                            | Description
:---------------|:---------|:--------------------------------|:-----------
`properties`    | yes      | [`property`](types.md#property) | List of properties to be assigned to the series

The request can be used to add new properties and/or update existing properties. Properties not included in the request
are not affected.

__Response__

`200 (OK)`: The added/updated series' properties are returned as JSON object.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

__Example__

Assume that the series already has the properties `theme`=`1000` and `live`=`true`.

To add the property `ondemand` and update the value of the existing property `live` the following form parameter is
used:

properties:
```
{
  "ondemand": "true",
  "live": "false"
}
```

The response of the request will contain the properties added/updated by this request:

```
{
  "ondemand": "true",
  "live": "false"
}
```

After this, the properties of the series are:

```
{
  "theme": "1000",
  "ondemand": "true",
  "live": "false"
}
```