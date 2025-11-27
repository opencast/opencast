# Upgrading Opencast from 18.x to 19.x

This guide describes how to upgrade Opencast 18.x to 19.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

1. Read the [release notes](releasenotes.md)
2. Stop your current Opencast instance
3. Replace Opencast with the new version
4. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
5. Upgrade the database using [the upgrade script](#database-upgrade)
6. Start Opencast
7. [Rebuild the index (if needed)](#index-rebuild)

## Configuration Changes

Check for changes in the configuration and apply those relevant to your setup to your files. You can use the following
command to list all changes:
```
git diff origin/r/{{ opencast_major_version() | int - 1 }}.x origin/r/{{ opencast_major_version() }}.x -- etc/
```

The most important changes are:

- Default workflows are now in yaml [[#6798](https://github.com/opencast/opencast/pull/6798)]
- The configuration key `video-source-flavor` for the Subtitle Timeshift Workflow Operation Handler changed to `video-source-flavors` (plural). [[#6901](https://github.com/opencast/opencast/pull/6901)]
- Configuring JWT authentication is now substantially simpler [[#7189](https://github.com/opencast/opencast/pull/7189)]
- The editor now displays less metadata by default [[#7053](https://github.com/opencast/opencast/pull/7053)]
- The plugin `opencast-plugin-legacy-annotation` has been removed from `org.opencastproject.plugin.impl.PluginManagerImpl.cfg`. [[#6902](https://github.com/opencast/opencast/pull/6902)]
- Target tag handling has changed with [[#6648](https://github.com/opencast/opencast/pull/6648)]:

## Database Upgrade

This script will drop a table which is no longer used in Opencast 19. You will find database
upgrade scripts in `docs/upgrade/{{ opencast_major_version() | int - 1 }}_to_{{ opencast_major_version() }}/`. **Make
sure to backup your database before migrating, to be able to easily revert changes, if necessary.**


## Index Rebuild

An index rebuild for for 19.x should not be required if upgrading from an 18.x system.  Upgrading from a version prior
to 18.x requires an index rebuild, follow the 18.x upgrade guide for details.
