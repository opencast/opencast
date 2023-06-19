Upgrading Opencast from 13.x to 14.x
====================================

This guide describes how to upgrade Opencast 13.x to 14.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Read the [release notes](releasenotes.md) (especially the section of behaviour changes)
4. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
5. [Migrate the database](#database-migration)
6. Start Opencast

Configuration changes
---------------------

### Analyze-mediapackage workflow operation changes

The behaviour of the `analyze-mediapackage` workflow operation has been changed. Instead of replacing every character that doesn't match `a-z` or `0-9` with an underscore character, the operation now only replaces the `/` separating flavor and subflavor. This makes it behave identical to the `analyze-tracks` operation. If you make use of `analyze-mediapackage` workflow operation in your custom workflows, please adopt this changes.

### Composite workflow operation changes

Instead of the `#{compositeCommand}` variable which was build by the composite workflow operation handler and could not
be configured, the composite operation now supports multiple different variables for constructing the ffmpeg command to
create the composite, most of which can be configured in the encoding profile. This is relevant e.g. for GPU encoding.

If you use custom encoding profiles for composite or use the existing profiles in a different way than the standard
workflows do, you might need to make some changes. Specifically, the `#{compositeCommand}` variable is no longer
supported in the ffmpeg command, and the `mp4-preview` profile now only supports dual-streams and no watermark,
while the `composite` profile retains its full functionality, but offers more configuration options than before.

For more details see the updated documentation for the
[composite operation](workflowoperationhandlers/composite-woh.md).

### New default editor

The default editor of Opencast has changed.
If you want to continue using the internal editor of the old admin interface,
you need to specifically configure this in `etc/org.opencastproject.organization-mh_default_org.cfg`
by configuring `prop.admin.editor.url`.

Note that the old admin interface is deprecated and will be removed in one of the next major releases.
Even if you use the old editor for now, please make sure to test the new one
and report potential problems.

### New default player

Paella 7 is the new default player in Opencast.
If you want to continue using the Paella 6 you need to specifically configure this in `etc/org.opencastproject.organization-mh_default_org.cfg` by configuring `prop.player`.

Note that the old player Paella 6 is deprecated and will be removed in one of the next major releases.
Even if you use the old player for now, please make sure to test the new one
and report potential problems.

### Theodul player removed

The Theodul player was removed and can not be used any more. Please move to the new [Paella7 player](#new-default-player).

### Global oc-remember-me cookie

It's now possible, to use the same `oc-remember-me` cookie for all nodes.
So, if you log into the admin node for example, you don't have to log in again, when switching to the presentation node. You can enable it in the `etc/security/mh_default_org.xml` configuration (search for `rememberMeServices` bean and set the property named `key`). 

### Login page moved

The login web page is moved from `/admin-ng/login.html` to `/login.html`. You may want to adopt this change in yor reverse proxy configuration in some cases.

Database Migration
------------------

You will find database upgrade scripts in `docs/upgrade/13_to_14/`. These scripts are suitable for both, MariaDB and
PostgreSQL. Changes include DB schema optimizations as well as fixes for the new workflow tables.
