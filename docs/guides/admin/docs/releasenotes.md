# Opencast 20: Release Notes

## Opencast 20.2

This version contains addition updates for the admin, editor, and studio submodules, along with the usual collection of
bug fixes.  There are three things worthy of special attention:

- [#7752](https://github.com/opencast/opencast/pull/7752) makes a change to a workflow, which will need to be manually
  applied by adopters if they are using the `output-background` configuration key.
- [GHSA-m6c8-jcw2-5r25](https://github.com/opencast/opencast/security/advisories/GHSA-m6c8-jcw2-5r25) resolves a XSS
  issue in Paella.
- [GHSA-6f53-jp7x-gg7p](https://github.com/opencast/opencast/security/advisories/GHSA-6f53-jp7x-gg7p) resolves a
  session fixation issue which allowed for account takeovers via malicious links.

Neither security issue requires any changes by adopters, and they are both dropin compatible with existing 19.x installs.

## Opencast 20.1

This release contains minor admin ui fixes and code cleanups. Also it adds `/play` route enhancements described below.

### Preserve query parameters on `/play` redirect

  The `/play` route now passes through query parameters to the target player URL.
  This allows parameters like `jwt=...` or `t=10` to be preserved
  when redirecting to the configured player.

  This is particularly useful for third-party integrations
  where authentication tokens (like JWTs) or specific player settings
  need to be passed through the redirect to the actual player interface,
  while preserving the ability to configure the exact player on the Opencast side.

  If a parameter with the same name already exists in the configured player path,
  the value from the request will be appended, creating a multi-valued parameter.
  This behavior ensures that parameters are never lost,
  though player implementations may need to handle multiple values if they occur.


## Opencast 20.0

### Features
- Opencast now supports chapters in videos. You can use the Editor to add, modify and publish chapters. [[Editor #1647](https://github.com/opencast/editor/pull/1647)]
  [[#7142](https://github.com/opencast/opencast/pull/7142)]

### Breaking Changes
- no breaking changes in this release

### Configuration Changes
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

For more details, please take a look at the [full changelog](changelog/opencast-20.md). If you want to update Opencast
from a previous version, you should also read the [upgrade guide](upgrade.md).

## Release Schedule

| Date           | Phase                    |
|----------------|--------------------------|
| April 15, 2026 | Release Branch Cut       |
| Mai 15, 2026   | Release of Opencast 20.0 |


## Release Managers

- Martin Wygas (elan e.V.)
- Sascha Nösberger (University of Bern)
