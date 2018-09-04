[TOC]

# General

### GET /api/groups

Returns a list of groups.

The following query string parameters can be used to filter, sort and paginate the returned list:

Query String Parameter |Type                         | Description
:----------------------|:----------------------------|:-----------
`filter`               | [`string`](types.md#basic)  | A comma-separated list of filters to limit the results with (see [Filtering](usage.md#filtering)). See the below table for the list of available filters
`sort`                 | [`string`](types.md#basic)  | A comma-separated list of sort criteria (see [Sorting](usage.md#sorting)).  See the below table for the list of available sort criteria
`limit`                | [`integer`](types.md#basic) | The maximum number of results to return (see [Pagination](usage.md#pagination))
`offset`               | [`integer`](types.md#basic) | The index of the first result to return (see [Pagination](usage.md#pagination))


The following filters are available:

Filter Name | Description
:-----------|:-----------
`name`      | Groups where the name specified in the metadata field match

The list can be sorted by the following criteria:

Sort Criteria | Description
:-------------|:-----------
`name`        | By the group name
`description` | By the group description
`role`        | By the group role
`members`     | By the group members
`roles`       | By the group roles

__Sample request__
```xml
https://opencast.domain.com/api/groups?sort=name:ASC&limit=2&offset=1
```

__Response__

`200 (OK)`: A (potentially empty) list of groups as JSON array of groups wheres the JSON objects representing groups
have the following fields:

Field         | Type                       | Description
:-------------|:---------------------------|:-----------
`identifier`  | [`string`](types.md#basic) | The identifier of the group
`role`        | [`string`](types.md#basic) | The role of the group
`organization`| [`string`](types.md#basic) | The tenant identifier
`roles`       | [`string`](types.md#basic) | The roles assigned to the group (comma-separated list)
`members`     | [`string`](types.md#basic) | The list of users that belong to this group (comma-separated list of usernames)
`name`        | [`string`](types.md#basic) | The name of the group
`description` | [`string`](types.md#basic) | The description of the group

__Example__

```
[
  {
    "identifier": "MH_DEFAULT_ORG_SYSTEM_ADMINS",
    "role": "ROLE_GROUP_MH_DEFAULT_ORG_SYSTEM_ADMINS",
    "organization": "mh_default_org",
    "roles": "ROLE_OAUTH_USER,ROLE_SUDO,ROLE_ADMIN,ROLE_ANONYMOUS",
    "members": "admin,admin2",
    "name": "Opencast Project System Administrators",
    "description": "System administrators of 'Opencast Project'"
  },
  {
    "identifier": "MH_DEFAULT_ORG_EXTERNAL_APPLICATIONS",
    "role": "ROLE_GROUP_MH_DEFAULT_ORG_EXTERNAL_APPLICATIONS",
    "organization": "mh_default_org",
    "roles": "ROLE_EXAMPLE1,ROLE_EXAMPLE2,ROLE_EXAMPLE3",
    "members": "apiuser",
    "name": "Opencast Project External Applications",
    "description": "External application users of 'Opencast Project'"
  }
]
```

### GET /api/groups/{group_id}

Returns a single group.


__Response__

`200 (OK)`: The group is returned as JSON object with the following fields:

Field         | Type                       | Description
:-------------|:---------------------------|:-----------
`identifier`  | [`string`](types.md#basic) | The identifier of the group
`role`        | [`string`](types.md#basic) | The role of the group
`organization`| [`string`](types.md#basic) | The tenant identifier
`roles`       | [`string`](types.md#basic) | The roles assigned to the group (comma-separated list)
`members`     | [`string`](types.md#basic) | The list of users that belong to this group (comma-separated list of usernames)
`name`        | [`string`](types.md#basic) | The name of the group
`description` | [`string`](types.md#basic) | The description of the group


`404 (NOT FOUND)`: The specified group does not exist.

__Example__

```
{
  "identifier": "MH_DEFAULT_ORG_SYSTEM_ADMINS",
  "role": "ROLE_GROUP_MH_DEFAULT_ORG_SYSTEM_ADMINS",
  "organization": "mh_default_org",
  "roles": "ROLE_OAUTH_USER,ROLE_SUDO,ROLE_ADMIN,ROLE_ANONYMOUS",
  "members": "admin,admin2",
  "name": "Opencast Project System Administrators",
  "description": "System administrators of 'Opencast Project'"
}
```

### POST /api/groups

Creates a group.

Form Parameters | Required | Type                       | Description
:---------------|:---------|:---------------------------|:-----------
`name`          | yes      | [`string`](types.md#basic) | Group Name
`description`   | no       | [`string`](types.md#basic) | Group Description
`roles`         | no       | [`string`](types.md#basic) | Comma-separated list of roles
`members`       | no       | [`string`](types.md#basic) | Comma-separated list of members

__Response__

This request does not return data.

`201 (CREATED)`: A new group is created.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.<br/>
`409 (CONFLICT)`: The group could not be created because a group with the same name already exists.

### PUT /api/groups/{group_id}

Updates a group.

Form Parameters | Required | Type                       | Description
:---------------|:---------|:---------------------------|:-----------
`name`          | yes      | [`string`](types.md#basic) | Group Name
`description`   | no       | [`string`](types.md#basic) | Group Description
`roles`         | no       | [`string`](types.md#basic) | Comma-separated list of roles
`members`       | no       | [`string`](types.md#basic) | Comma-separated list of members

If any of form parameters are ommited, the respective fields of the group will not be changed.

__Response__

This request does not return data.

`200 (OK)`: The group has been updated.<br/>
`404 (NOT FOUND)`: The specified group does not exist.<br/>

### DELETE /api/groups/{group_id}

Deletes a group.

__Response__

This request does not return data.

`204 (NO CONTENT)`: The group has been deleted.<br/>
`404 (NOT FOUND)`: The specified group does not exist.

# Members

### POST /api/groups/{group_id}/members

Adds a member to a group.

Form Parameters | Required | Type                       | Description
:---------------|:---------|:---------------------------|:-----------
`member`        | yes      | [`string`](types.md#basic) | The username of the member to be added

__Response__

`200 (OK)`: The member has been added or was already member of the group.<br/>

If the member has been added, the request does not return data.
In case that the member already was in the group, the following message is returned as string:

```
Member is already member of group
```

`404 (NOT FOUND)`: The specified group does not exist.

### DELETE /api/groups/{group_id}/members/{member_id}

Removes a member from a group

__Response__

`200 (NO CONTENT)`: The member has been removed.<br/>
`404 (NOT FOUND)`: The specified group or member does not exist.
