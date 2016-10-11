[TOC]

# General

### GET /api/series

Returns a list of series.

Query String Parameter     |Type            | Description
:--------------------------|:---------------|:--------------------------------------------------------------------------
`filter`                   | `string`       | A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon ":" and then the value to filter with so it is the form `Filter Name`:`Value to Filter With`. See the below table for the list of available filters.
`sort`                     | `string`       | Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: `Sort Name`:`ASC` or `Sort Name`:`DESC`. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory. See the below table about the available sort names in the table below.
`limit`                    | `integer`      | The maximum number of results to return for a single request.
`offset`                   | `integer`      | Number of results to skip based on the limit. 0 is the first set of results up to the limit, 1 is the second set of results after the first limit, 2 is third set of results after skipping the first two sets of results etc.

Filter Name     | Description
:---------------|:------------------
`contributors`  | Series where the contributors specified in the metadata field match.
`creator`       | Series where the creator specified in the metadata field match.
`creationDate`  | Series that were created between two dates. The two dates are in UTC format to the second i.e. yyyy-MM-ddTHH:mm:ssZ e.g. 2014-09-27T16:25Z. They are seperated by a forward slash (url encoded or not) so an example of the full filter would be CreationDate:2015-05-08T00:00:00.000Z/2015-05-10T00:00:00.000Z
`language`      | Series based upon the language specified.
`license`       | Series based upon the license specified.
`organizers`    | Series where the organizers specified in the metadata field match.
`managedAcl`    | Series who have the same managed acl name.
`subject`       | By the subject they are a part of.
`textFilter`    | Filters series where any part of the series' metadata fields match this value.
`title`         | By the title of the series.

Sort Name           | Description
:-------------------|:---------------
`contributors`      | By the series contributors.
`created`           | By when the series was created.
`creator`           | By who created the series.
`title`             | By the title of the series.

__Sample request__
```xml
https://opencast.domain.com/api/series?filter=creator:Default Administrator&sort=title:ASC&limit=2&offset=1
```

__Response__

`200 (OK)`: A (potentially empty) list of series is returned.

```
[
  {
    "contributors": ["John Doe"],
    "title": "The Opencast API",
    "publishers": ["John Doe"],
    "subjects": ["Topic", "Screencast"],
    "created": "2015-03-12T09:51:32Z",
    "organizers": ["Opencast Community"],
    "identifier": "763545de-7e1c-4c8a-bcd9-902511f0e15b",
    "creator": "Opencast Administrator"
  },
  {
    "contributors": ["Jane Doe"],
    "title": "The Opencast Admin UI",
    "publishers": ["John Doe"],
    "subjects": ["Topic", "Screencast"],
    "created": "2015-03-12T09:51:32Z",
    "organizers": ["Opencast Community"],
    "identifier": "353545de-7e1c-4c8a-bcd9-902511f0e15b",
    "creator": "Opencast Administrator"
  }
]
```

<!--- ##################################################################### -->
### GET /api/series/{series_id}

Returns a single series.

__Response__

`200 (OK)`: The series is returned.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

```
{
  "identifier": "4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f",
  "title": "The Opencast API",
  "description": "A cool demo of the Opencast API",
  "subjects": ["Topic", "Screencast"],
  "organization": "mh_default_org",
  "creator": "Default Administrator",
  "created": "2015-03-12T09:58:06Z",
  "organizers": ["Opencast Community"],
  "contributors": ["John Doe"],
  "publishers": ["John Doe"],
  "opt_out": false
}
```

<!--- ##################################################################### -->
### POST /api/series

Creates a series.

Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`metadata`                 | String         | Series metadata
`acl`                      | String         | A collection of roles with their possible action
`theme`                    | String         | The theme ID to be applied to the series

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

<!--- ##################################################################### -->
### DELETE /api/series/{series_id}

Deletes a series

__Response__

`204 (NO CONTENT)`: The series has been deleted.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

# Metadata

<!--- ##################################################################### -->
### GET /api/series/{series_id}/metadata

Returns a series' metadata of all types.

__Response__

`200 (OK)`: The series' metadata are returned.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

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

<!--- ##################################################################### -->
### GET /api/series/{series_id}/metadata

Returns a series' metadata collection of the given type when the query string parameter type is specified. For each metadata catalog there is a unique property called the flavor such as dublincore/series so the type in this example would be "dublincore/series".

Query String Parameters    |Type            | Description
:--------------------------|:---------------|:----------------------------
`type`                     | String         | The type of metadata to return

__Response__

`200 (OK)`: The series' metadata are returned.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

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
### PUT /api/series/{series_id}/metadata

Update a series' metadata of the given type. For a metadata catalog there is the flavor such as "dublincore/series" and this is the unique type.

Query String Parameters    |Type            | Description
:--------------------------|:---------------|:----------------------------
`type`                     | String         | The type of metadata to update


Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`metadata`                 | String         | Series metadata as Form param

__Sample__

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

<!--- ##################################################################### -->
### DELETE /api/series/{series_id}/metadata

Deletes a series' metadata catalog of the given type. All fields and values of that catalog will be deleted.

Query String Parameters    |Type            | Description
:--------------------------|:---------------|:----------------------------
`type`                     | String         | The type of metadata to delete

__Response__

`204 (NO CONTENT)`: The metadata have been deleted.<br/>
`403 (FORBIDDEN)`: The main metadata catalog dublincore/series cannot be deleted as it has mandatory fields.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

# Access Policy

<!--- ##################################################################### -->
### GET /api/series/{series_id}/acl

Returns a series' access policy.

__Response__

`200 (OK)`: The series' access policy is returned.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

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
### PUT /api/series/{series_id}/acl

Updates a series' access policy.

Parameters                 |Type            | Description
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

`200 (OK)`: The access control list for the specified series is updated.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

# Properties

<!--- ##################################################################### -->
### GET /api/series/{series_id}/properties

Returns a series' properties.

__Response__

`200 (OK)`: The series' properties are returned.<br/>
`404 (NOT FOUND)`: The specified series does not exist.

```
{
  "ondemand": "true",
  "live": "false"
}
```

<!--- ##################################################################### -->
### PUT /api/series/{series_id}/properties

Updates a series' properties

Form parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`properties`               | `string`       | Series properties

__Sample__

properties:
```
{
  "ondemand": "true",
  "live": "false"
}
```

__Response__

`200 (OK)`: Successfully updated the series' properties.<br/>
`404 (NOT FOUND)`: The specified series does not exist.
