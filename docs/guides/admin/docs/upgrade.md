# Upgrading Opencast from 19.x to 20.x

This guide describes how to upgrade Opencast 19.x to 20.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

1. Read the [release notes](releasenotes.md)
2. Stop your current Opencast instance
3. Replace Opencast with the new version
4. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
5. Upgrade the database using [the upgrade script (if needed)](#database-upgrade)
6. Start Opencast
7. [Rebuild the index (if needed)](#index-rebuild)

If you decide partway through that you need to go back, see [Rolling Back](#rolling-back) below — how far back you can
go depends on how far you got.

## Configuration Changes

Check for changes in the configuration and apply those relevant to your setup to your files. You can use the following
command to list all changes:
```
git diff origin/r/{{ opencast_major_version() | int - 1 }}.x origin/r/{{ opencast_major_version() }}.x -- etc/
```

The most important changes are:

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


## Rolling Back

There is no tool or script to reverse an upgrade: the scripts referenced under [Database Upgrade](#database-upgrade)
only ever migrate forward. How far back you can go depends on how far the upgrade got before you decided to stop.

### Before Starting Opencast on the New Version

If you have not yet started Opencast on the new version (i.e. you stopped at step 5 or earlier), rolling back is
simple: reinstall the previous version's binaries and start it again. Nothing persistent has changed yet, since
Opencast only creates or modifies database tables once it actually runs.

### After Starting Opencast on the New Version

Once Opencast has been started on the new version, rolling back is only reliable by restoring a backup taken *before*
the upgrade. This is because starting Opencast, not just running the database upgrade script, can already change the
database: Opencast creates tables that do not exist yet on startup, whether or not you ran the upgrade script for
that version. There is no supported way to reverse either of these changes, so plan for this **before** you upgrade
rather than after.

To be able to roll back, back up beforehand:

- **The database.** This is the backup that actually matters for rolling back. Restore it, and only it, if the
  upgrade already changed the database; do not try to reuse the database as it is after the failed upgrade.
- **The `etc/` configuration directory**, or at least a note of your own customizations. Configuration keys are
  sometimes added, renamed or removed between versions (see [Configuration Changes](#configuration-changes)), so the
  old version's configuration is not guaranteed to be a subset of the new one.

The search index does not need a backup of its own for this purpose: it is rebuilt from the database and the asset
store, not a source of truth in itself. The working file repository and asset store are not usually touched by an
upgrade, but as always, check that specific version's upgrade notes to be sure. Back them up as part of your regular
backup routine regardless, independent of upgrading.

### Steps to Roll Back

1. Stop Opencast.
2. Reinstall the previous version's binaries.
3. Restore the `etc/` configuration you backed up before the upgrade, or manually undo the changes from
   [Configuration Changes](#configuration-changes).
4. If Opencast was started on the new version, or the database upgrade script was run, restore the database backup
   taken before the upgrade. If neither happened, the database does not need to be touched.
5. Clear Opencast's own runtime state, in particular `data/cache` and `data/generated-bundles`, so that the
   previous version does not start with a bundle cache left behind by the newer one. Keep `data/log` if you want to
   preserve the logs.
6. Start Opencast.
7. If you restored a database backup, [rebuild the index](#index-rebuild).

### OpenSearch

Rolling back Opencast does not roll back OpenSearch. If upgrading Opencast also involved changing the OpenSearch
version, decide separately whether that needs to be rolled back too.
