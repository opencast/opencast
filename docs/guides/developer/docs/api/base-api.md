[TOC]

# Information

In order to assess key characteristics of the API and to test general connectivity, the API’s root url is not protected through authentication:

### GET /api

Returns key characteristics of the API such as the server name and the default version.

__Response__

`200 (OK)`: The api information is returned.
```
{
  "url": "https:\/\/api.opencast.org",
  "version": "v1.0.1"
}
```

# User and Organization

### GET /api/info/me

Returns information on the logged in user.

__Response__

`200 (OK)`: The user information is returned.

```
{
  "email": "nowhere@opencast.org",
  "name": "Opencast Student",
  "provider": "opencast",
  "userrole": "ROLE_USER_92623987_OPENCAST_ORG",
  "username": "92623987@opencast.org"
}
```

### GET /api/info/me/roles

Returns current user's roles.

__Response__

`200 (OK)`: The set of roles is returned.

```
[
  "ROLE_USER_92623987@opencast.org",
  "ROLE_STUDENT"
]
```

### GET /api/info/organization

Returns the current organization.

__Response__

`200 (OK)`: The organization details are returned.

```
{
  "adminRole": "ROLE_ADMIN",
  "anonymousRole": "ROLE_ANONYMOUS",
  "id": "opencast",
  "name": "Opencast"
}
```

### GET /api/info/organization/properties

Returns the current organization's properties.

__Response__

`200 (OK)`: The organization properties are returned.

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

`200 (OK)`: The default version is returned.

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

`200 (OK)`: The default version is returned.

```
{
  "default": "v1.1.0"
}
```
