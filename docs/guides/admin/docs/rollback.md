# Rolling Back an Upgrade

There is no tool or script to reverse an upgrade: the database upgrade scripts described in the
[upgrade guide](upgrade.md) only ever migrate forward. How far back you can go depends on how far the upgrade got
before you decided to stop.

## Before Starting Opencast on the New Version

If you have neither run the database upgrade script nor started Opencast on the new version, rolling back is simple:
reinstall the previous version's binaries and start it again. Nothing persistent has changed yet, since Opencast only
creates or modifies database tables once it actually runs.

## After Starting Opencast on the New Version

Once the database upgrade script has been run or Opencast has been started on the new version, rolling back is only
reliable by restoring a backup taken *before* the upgrade. This is because starting Opencast, not just running the
database upgrade script, can already change the database: Opencast creates tables that do not exist yet on startup,
whether or not you ran the upgrade script for that version. There is no supported way to reverse either of these
changes, so plan for this **before** you upgrade rather than after.

To be able to roll back, back up beforehand:

- **The database.** This is the backup that actually matters for rolling back. Restore it, and only it, if the
  upgrade already changed the database; do not try to reuse the database as it is after the failed upgrade.
- **The `etc/` configuration directory**, or at least a note of your own customizations. Configuration keys are
  sometimes added, renamed or removed between versions (see the configuration changes in the
  [upgrade guide](upgrade.md)), so the old version's configuration is not guaranteed to be a subset of the new one.

The search index does not need a backup of its own for this purpose: it is rebuilt from the database and the asset
store, not a source of truth in itself. The working file repository and asset store are not usually touched by an
upgrade, but as always, check that specific version's upgrade notes to be sure. Back them up as part of your regular
backup routine regardless, independent of upgrading.

## Steps to Roll Back

1. Stop Opencast.
2. Reinstall the previous version's binaries.
3. Restore the `etc/` configuration you backed up before the upgrade, or manually undo the changes listed in the
   [upgrade guide](upgrade.md).
4. If Opencast was started on the new version, or the database upgrade script was run, restore the database backup
   taken before the upgrade. If neither happened, the database does not need to be touched.
5. Clear Opencast's own runtime state, in particular `data/cache` and `data/generated-bundles`, so that the
   previous version does not start with a bundle cache left behind by the newer one. Keep `data/log` if you want to
   preserve the logs.
6. Start Opencast.
7. If you restored a database backup, rebuild the index as described in the [upgrade guide](upgrade.md).

## OpenSearch

Rolling back Opencast does not roll back OpenSearch. If upgrading Opencast also involved changing the OpenSearch
version, decide separately whether that needs to be rolled back too.
