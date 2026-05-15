# Opencast 19: Release Notes

## Opencast 19.5

This version contains updates for the admin,editor, and studio submodules, along with the usual collection of bug fixes.

## Opencast 19.4

This release contains a fix for
(Episode ID Roles)[https://docs.opencast.org/r/19.x/admin/#configuration/episode-id-roles/#episode-id-roles], which
were broken in earlier 19.x releases.  Adopters affected by this issue should either reindex their search index or
republish their recordings.  If you do not use episode id roles then this can safely be ignored.

## Opencast 19.3

This version contains an important fix for the admin UI, which had broken scheduling in 19.1 and 19.2.

## Opencast 19.2

This version contains updates for the editor, along with the usual collection of bug fixes.

## Opencast 19.1

This version contains updates for both the admin ui, and the editor, along with the usual collection of bug fixes.

## Opencast 19.0

### Features
- Episodes in the search index can be filtered according to whether or not they are live. [[#7032](https://github.com/opencast/opencast/pull/7032)]
- Paella 8 is now included in Opencast as an alternative player [[#7166](https://github.com/opencast/opencast/pull/7166)]
- A Matomo statistics provider has been added [[#7134](https://github.com/opencast/opencast/pull/7134)]
- Default series ACLs can now be configured [[#7041](https://github.com/opencast/opencast/pull/7041)]


### Breaking Changes
- Target tag behaviour has been unified across all workflow operation handlers. [[#6648](https://github.com/opencast/opencast/pull/6648)]
- The Opencast presets module has been removed.  This previously allowed for org, and system wide presets to be
  defined, but was undocumented and untested.  This change was discussed in [5615](https://github.com/opencast/opencast/issues/5615).
  [[#6903](https://github.com/opencast/opencast/pull/6903)]
- The Opencast annotation service has been removed.  If you had `opencast-plugin-legacy-annotation` enabled in
  `org.opencastproject.plugin.impl.PluginManagerImpl.cfg` then this affects you, otherwise it should not.  This change
  was discussed in [5615](https://github.com/opencast/opencast/issues/5615).  The relevant database tables will be
  automatically removed from your database systems by the upgrade script.  [[#6902](https://github.com/opencast/opencast/pull/6902)]


### Configuration Changes
- Default workflows are now in yaml [[#6798](https://github.com/opencast/opencast/pull/6798)]
- The configuration key `video-source-flavor` for the Subtitle Timeshift Workflow Operation Handler changed to
  `video-source-flavors` (plural). [[#6901](https://github.com/opencast/opencast/pull/6901)]
- Configuring JWT authentication is now substantially simpler [[#7189](https://github.com/opencast/opencast/pull/7189)]
- The editor now displays less metadata by default [[#7053](https://github.com/opencast/opencast/pull/7053)]
- The plugin `opencast-plugin-legacy-annotation` has been removed from `org.opencastproject.plugin.impl.PluginManagerImpl.cfg`.
  [[#6902](https://github.com/opencast/opencast/pull/6902)]
- Target tag handling has changed with [[#6648](https://github.com/opencast/opencast/pull/6648)]:

    All workflow operation handlers adapt the same method of
    handling target tags (if they support target tags).

    The chosen behaviour is that of the tag WOH:
    "If a target-tag starts with a '-', it will be removed from
    preexisting tags, if a target-tag starts with a '+', it will
    be added to preexisting tags. If there is no prefix, all
    preexisting tags are removed and replaced by the target-tags."

    This means that all WOH that did not support "+" and "-" now do.
    THIS CONSTITUTES A BEHAVIOUR CHANGE FOR SOME WORKFLOW OPERATION HANDLERS!
    In particular, some WOH were adding tags instead of replacing them,
    even though they were not prefaced with a "+". Adopters are advised
    to check their workflows.

For more details, please take a look at the [full changelog](changelog/opencast-19.md). If you want to update Opencast
from a previous version, you should also read the [upgrade guide](upgrade.md).

## Release Schedule

| Date              | Phase                    |
|-------------------|--------------------------|
| November 19, 2025 | Release Branch Cut       |
| December 17, 2025 | Release of Opencast 18.0 |


## Release Managers

- Greg Logan (Logan IT Enterprises)
- Lukas Gehrlein (ssystems Gmbh.)
