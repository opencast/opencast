[TOC]

# General

### GET /api/groups

Returns a list of groups.

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
        "ROLE_SUDO",
        "MATTERHORN_ADMINISTRATOR"
      ]
    },
    "name": "Default System Administrators",
    "identifier": "MH_DEFAULT_ORG_SYSTEM_ADMINS",
    "members": {
      "member": "john"
    }
  },
  {
    "organization": "mh_default_org",
    "description": "External application users of 'Default'",
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

<!--- ##################################################################### -->
### GET /api/groups/{group_id}

Returns a single group.

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
      "ROLE_SUDO",
      "MATTERHORN_ADMINISTRATOR"
    ]
  },
  "name": "Default System Administrators",
  "identifier": "MH_DEFAULT_ORG_SYSTEM_ADMINS",
  "members": {
    "member": "john"
  }
}
```

<!--- ##################################################################### -->
### POST /api/groups

Creates a group.

Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`name`                     | `String`       | Group Name
`description`              | `String`       | Group Description
`roles`                    | `String`       | Comma-separated list of roles
`users`                    | `String`       | Comma-separated list of users

__Response__

`201 (CREATED)`: A new group is created.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.

<!--- ##################################################################### -->
### PUT /api/groups/{group_id}

Updates a group.

Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`name`                     | `String`       | Group Name
`description`              | `String`       | Group Description
`roles`                    | `String`       | Comma-separated list of roles
`users`                    | `String`       | Comma-separated list of users

__Sample__

```
TODO
```

__Response__

`200 (OK)`: The group has been updated.<br/>
`404 (OK)`: The specified group does not exist.<br/>

<!--- ##################################################################### -->
### DELETE /api/groups/{group_id}

Deletes a group.

__Response__

`204 (NO CONTENT)`: The group has been deleted.<br/>
`404 (OK)`: The specified group does not exist.

# Members

<!--- ##################################################################### -->
### POST /api/groups/{group_id}/members

Adds a member to a group.

__Sample__

```
TODO
```

__Response__

```
TODO
```

<!--- ##################################################################### -->
### DELETE /api/groups/{group_id}/members/{user_id}

Removes a member from a group

__Sample__

```
TODO
```

__Response__

```
TODO
```
