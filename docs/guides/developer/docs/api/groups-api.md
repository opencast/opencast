[TOC]

# General

### GET /api/groups

Returns a list of groups.

Query String Parameter     |Type            | Description
:--------------------------|:---------------|:--------------------------------------------------------------------------
`filter`                   | `string`       | A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon ":" and then the value to filter with so it is the form `Filter Name`:`Value to Filter With`. See the below table for the list of available filters.
`sort`                     | `string`       | Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: `Sort Name`:`ASC` or `Sort Name`:`DESC`. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory. See the below table about the available sort names in the table below.
`limit`                    | `integer`      | The maximum number of results to return for a single request.
`offset`                   | `integer`      | Number of results to skip based on the limit. 0 is the first set of results up to the limit, 1 is the second set of results after the first limit, 2 is third set of results after skipping the first two sets of results etc.

Filter Name     | Description
:---------------|:------------------
`name`          | Groups where the name specified in the metadata field match.

Sort Name        | Description
:----------------|:---------------
`name`           | By the group name
`description`    | By the group description
`role`           | By the group role
`members`        | By the group members
`roles`          | By the group roles

__Sample request__
```xml
https://opencast.domain.com/api/groups?sort=name:ASC&limit=2&offset=1
```

__Response__

`200 (OK)`: A (potentially empty) list of groups.

```
[
  {
    "organization": "mh_default_org",
    "description": "System administrators",
    "roles": {
      "role": [
        "ROLE_ADMIN",
        "ROLE_SUDO"
      ]
    },
    "name": "Default System Administrators",
    "identifier": "MH_DEFAULT_ORG_SYSTEM_ADMINS",
    "members": {
      "member": "admin"
    }
  },
  {
    "organization": "mh_default_org",
    "description": "External application users",
    "roles": {
      "role": [
        "ROLE_API",
        "ROLE_SUDO",
        "ROLE_API_SERIES_EDIT",
        "ROLE_API_EVENTS_VIEW"
      ]
    },
    "name": "Default External Applications",
    "identifier": "MH_DEFAULT_ORG_EXTERNAL_APPLICATIONS",
    "members": ""
  }
]
```

### GET /api/groups/{group_id}

Returns a single group.

__Sample request__
```xml
https://opencast.domain.com/api/groups/MH_DEFAULT_ORG_SYSTEM_ADMINS
```

__Response__

`200 (OK)`: The group is returned.<br/>
`404 (NOT FOUND)`: The specified group does not exist.

```
{
  "organization": "mh_default_org",
  "description": "System administrators",
  "roles": {
    "role": [
      "ROLE_ADMIN",
      "ROLE_SUDO"
    ]
  },
  "name": "Default System Administrators",
  "identifier": "MH_DEFAULT_ORG_SYSTEM_ADMINS",
  "members": {
    "member": "admin"
  }
}
```

### POST /api/groups

Creates a group.

Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`name`                     | `String`       | Group Name
`description`              | `String`       | Group Description
`roles`                    | `String`       | Comma-separated list of roles
`members`                  | `String`       | Comma-separated list of members

__Response__

`201 (CREATED)`: A new group is created.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.

### PUT /api/groups/{group_id}

Updates a group.

Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`name`                     | `String`       | Group Name
`description`              | `String`       | Group Description
`roles`                    | `String`       | Comma-separated list of roles
`members`                  | `String`       | Comma-separated list of members

__Sample__

```xml
TODO
```

__Response__

`200 (OK)`: The group has been updated.<br/>
`404 (NOT FOUND)`: The specified group does not exist.<br/>

### DELETE /api/groups/{group_id}

Deletes a group.

__Response__

`204 (NO CONTENT)`: The group has been deleted.<br/>
`404 (NOT FOUND)`: The specified group does not exist.

# Members

### POST /api/groups/{group_id}/members

Adds a member to a group.

Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`member`                   | `String`       | Member Name

__Sample__
```xml
https://opencast.domain.com/api/groups/MH_DEFAULT_ORG_SYSTEM_ADMINS/members
```

__Response__

`200 (OK)`: The member was already member of the group.<br/>
`204 (NO CONTENT)`: The member has been added.<br/>
`404 (NOT FOUND)`: The specified group does not exist.

### DELETE /api/groups/{group_id}/members/{member_id}

Removes a member from a group

__Sample__
```xml
https://opencast.domain.com/api/groups/MH_DEFAULT_ORG_SYSTEM_ADMINS/members/admin
```

__Response__

`204 (NO CONTENT)`: The member has been removed.<br/>
`404 (NOT FOUND)`: The specified group or member does not exist.
