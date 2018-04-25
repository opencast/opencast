[TOC]

# Information

In order to assess key characteristics of the External API and to test general connectivity, the External API’s root
url is not protected through authentication:

### GET /api

Returns key characteristics of the External API such as the API base URL and the default version.

__Response__

`200 (OK)`: The api information is returned as a JSON object containing the following fields:

Field     | Type                       | Description
:---------|:---------------------------|:-----------
`version `| [`string`](types.md#basic) | Default version of the External API
`url`     | [`string`](types.md#basic) | Base URL clients shall use to communicate with the External API


__Example__

```
{
  "url": "https:\/\/api.opencast.org\/api",
  "version": "v1.0.1"
}
```

# User and Organization

### GET /api/info/me

Returns information on the logged in user.

__Response__

`200 (OK)`: The user information is returned as a JSON object containing the following fields:

Field      | Type                       | Description
:----------|:---------------------------|:-----------
`provider` | [`string`](types.md#basic) | The Opencast user provider that manages this user
`name`     | [`string`](types.md#basic) | Displayable name of the user
`username` | [`string`](types.md#basic) | The username
`userrole` | [`string`](types.md#basic) | The role uniquly identifying the user
`email`    | [`string`](types.md#basic) | The e-mail address of the user

__Example__

```
{
  "provider": "opencast",
  "name": "Opencast Student",
  "userrole": "ROLE_USER_92623987_OPENCAST_ORG",
  "email": "nowhere@opencast.org",
  "username": "92623987@opencast.org"
}
```

### GET /api/info/me/roles

Returns current user's roles.

__Response__

`200 (OK)`: The set of roles is returned as [`array[string]`](types.md#array).

__Example__

```
[
  "ROLE_USER_92623987@opencast.org",
  "ROLE_STUDENT"
]
```

### GET /api/info/organization

Returns the current organization.

__Response__

`200 (OK)`: The organization details are returned as JSON object containing the following fields:

Field          | Type     | Description
:--------------|:---------|:-----------
`adminRole`    | [`string`](types.md#basic) | The role administrator users have
`anonymousRole`| [`string`](types.md#basic) | The role unauthenticated users have
`id`           | [`string`](types.md#basic) | The tenant identifier
`name`         | [`string`](types.md#basic) | The tenant name

__Example__

```
{
  "adminRole": "ROLE_ADMIN",
  "anonymousRole": "ROLE_ANONYMOUS",
  "id": "mh_default_org",
  "name": "Opencast"
}
```

### GET /api/info/organization/properties

Returns the current organization's properties. The set of properties is a key-value set that depends on the
configuration of Opencast.

__Response__

`200 (OK)`: The organization properties are returned as [`property`](types.md#property).

__Example__

```
{
  "org.opencastproject.feed.url": "http://feeds.opencast.org",
  "org.opencastproject.admin.documentation.url": "http://documentation.opencast.org",
  "org.opencastproject.external.api.url": "http://api.opencast.org"
}
```

# Versions

### GET /api/version

Returns a list of available version as well as the default version.

__Response__

`200 (OK)`: The version information is returned as JSON object containing the following fields:

Field      | Type                              | Description
:----------|:----------------------------------|:-----------
`versions` | [`array[string]`](types.md#array) | All External API versions supported by this server
`default`  | [`string`](types.md#basic)        | The default External API version used by this server

__Example__

```
{
  "versions": [
    "v1.0.0",
    "v1.1.0"
  ],
  "default": "v1.1.0"
}
```

### GET /api/version/default

Returns the default version.

__Response__

`200 (OK)`: The default version is returned as JSON object containing the following fields:

Field     | Type                       | Description
:---------|:---------------------------|:-----------
`default` | [`string`](types.md#basic) | The default External API version used by this server

__Example__

```
{
  "default": "v1.1.0"
}
```
