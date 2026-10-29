# Upgrading Opencast from 19.x to 20.x

This guide describes how to upgrade Opencast 19.x to 20.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

1. Read the [release notes](releasenotes.md)
2. Stop your current Opencast instance
3. Replace Opencast with the new version
4. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
5. Upgrade the database using [the upgrade script (if needed)](#database-upgrade)
6. Start Opencast and [check that it is working](configuration/basic.md#checking-that-opencast-is-working)
7. [Rebuild the index (if needed)](#index-rebuild)

## Configuration Changes

Check for changes in the configuration and apply those relevant to your setup to your files. You can use the following
command to list all changes:
```
git diff origin/r/{{ opencast_major_version() | int - 1 }}.x origin/r/{{ opencast_major_version() }}.x -- etc/
```

The most important changes are:

- Opencast now uses Apache HttpClient 5 instead of the end-of-life HttpClient 4 for all internal HTTP communication.
  Requests between nodes share a connection pool whose size can be configured with the new options
  `org.opencastproject.security.http.max.connections.per.route` and `org.opencastproject.security.http.max.connections`
  in `etc/custom.properties`. The digest nonce retry options `org.opencastproject.security.digest.nonce.*` keep their
  meaning but now trigger on a stale nonce reported by the server. External plugins using the `TrustedHttpClient`
  service must be migrated to the HttpClient 5 API (`ClassicHttpRequest`, `ClassicHttpResponse`, `getCode()`).
- Paella 8 is now the default Paella version.  Paella 7 is scheduled for removal in Opencast 21. [[#7214](https://github.com/opencast/opencast/pull/7214)]
- The circular dependency between index and list providers has been eliminated. The configuration for the ACL additional
  actions list provider has been moved to the organization configuration. [#7128](https://github.com/opencast/opencast/pull/7128)
- JMX beans appear to be unused, and there are performance concerns surrounding JMX statistics. Therefore, they have
  been removed to simplify our code. [[#7317](https://github.com/opencast/opencast/pull/7317)]
- Disable service states by default. These tended to cause more issues in modern systems.  Functionality is still
  present, just disabled by default. [[#7450](https://github.com/opencast/opencast/pull/7450)]
- The configuration option "heartbeat.interval" was removed from `etc/org.opencastproject.serviceregistry.impl.JobDispatcher.cfg`
  Furthermore, only dispatching nodes will have a heartbeat from now on. [[#7311](https://github.com/opencast/opencast/issues/7311)]
- All capture agent inputs are now preselected when scheduling a new event in the Admin UI [[Admin Interface #1566](https://github.com/opencast/opencast/issues/1566)]
- Whether you were uploading or scheduling a new event in the Admin UI, the "Create Event" modal would always offer you
  workflows tagged with either "upload" or "schedule". Now if you are uploading, you only get workflows tagged with
  "upload". And if you are scheduling, you only get workflows tagged with "schedule". [[Admin Interface #1567](https://github.com/opencast/admin-interface/issues/1567)]
- A new config option to filter available roles in the Admin UI access policy dropdowns was added. Allows you to
  effectively remove catgories of roles (like ROLE_GROUP) from the dropdowns to make them more usable.
  [[Admin Interface #1561](https://github.com/opencast/admin-interface/issues/1561)]
  [[#7541](https://github.com/opencast/opencast/pull/7541)]
- The editor thumbnail view has been improved. [[Editor #1663](https://github.com/opencast/editor/pull/1663)]

## Database Upgrade

A database upgrade for 20.x is not required if upgrading from an 19.x system. Upgrading from a version prior to 19.x
may require a database upgrade, follow the corresponding upgrade guides for details.


## Index Rebuild

An index rebuild for 20.x is not required if upgrading from an 19.x system. Upgrading from a version prior to 19.x
may require an index rebuild, follow the corresponding upgrade guides for details.
