Tobira User Provider
====================

This module can be used to get user information in Opencast that is otherwise only known in Tobira.

It is useful when ACL entries reference Tobira users and Opencast needs to resolve those users to display name, email, and roles.
The provided information is searchable in ACL editors, so these user can also be added as new entries.
Depending on whether role sanitization (see `etc/org.opencastproject.userdirectory.UserIdRoleProvider.cfg`) is enabled, the entries are either shown as user role only (sanitization enabled, default) or with their display name and email address, if provided.

## What it does

- Looks up users via Tobira's GraphQL API.
- Exposes resolved users as an external UserProvider and RoleProvider.
- Caches lookups in memory to reduce repeated remote calls.

## Required configuration

Adjust this config with your specific values:

`etc/org.opencastproject.userdirectory.tobira.cfg`

Required properties:

```properties
# Organization
org.opencastproject.userdirectory.tobira.org=mh_default_org

# Tobira base URL
org.opencastproject.userdirectory.tobira.url=https://tobira.example.com

# Trusted key (as configured in Tobira)
org.opencastproject.userdirectory.tobira.trustedKey=replace-me
```

Optional properties:

```properties
# Maximum number of cached user lookups (Default: 500)
#
# Limits how many username lookups are kept in memory at once.
# Increase if you expect many distinct users in a short period and want to reduce repeated Tobira requests.
# Decrease if you want lower memory usage.
org.opencastproject.userdirectory.tobira.cache.size=500

# Cache entry lifetime in minutes (Default: 60)
#
# Time-to-live for cached user entries after they were written.
# Lower values keep user data fresher but increase request volume to Tobira.
# Higher values reduce Tobira traffic but may keep stale display names/emails/roles longer.
org.opencastproject.userdirectory.tobira.cache.expiration=60
```

## How to enable

Make sure the above required properties are correctly configured and enable the plugin feature in `etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg`.
