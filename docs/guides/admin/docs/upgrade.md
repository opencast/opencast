Upgrading Opencast from 14.x to 15.x
====================================

This guide describes how to upgrade Opencast 14.x to 15.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Read the [release notes](releasenotes.md) (especially the section of behaviour changes)
1. Stop your current Opencast instance
1. Replace Opencast with the new version
1. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
1. [Migrate the database](#database-migration)
1. Start Opencast

Subtitle changes
----------------
With Opencast 15 we want to put more emphasis on subtitles. You can find more details on how subtitles should be
handled going forward in [Subtitles](./configuration/subtitles.md).

This comes with a bit of migration. Namely, subtitles should not be stored as "attachments" or "catalogs" anymore, but
as "media"(as they are called in the Admin UI) or "tracks" (as they are called internally). Therefore, all subtitle
files currently stored as attachments or catalogs in your events should be moved to tracks. This can easily be
accomplished with the "changetype" workflow operation handler new to Opencast 15. See example below. (Subtitles should
then be republished)

Additionally, we recommend adding a language tag `lang:<language-code>` to your subtitle files. While tags for subtitles
are optional, the flavor will not encode the given language for a subtitle anymore, so a language tag is useful for
identification and display purposes.
You can read more about subtitles tags in [Subtitles](./configuration/subtitles.md).

If your subtitles are not in WebVTT format, they should be converted to WebVTT as well. While other formats are possible,
WebVTT is the only one that will be guaranteed to work.

<details>

<summary>Example workflow for changing subtitle type to track</summary>
```xml
<?xml version="1.0" encoding="UTF-8"?>
<definition xmlns="http://workflow.opencastproject.org">

<id>change-subtitles</id>
  <title>Change Subtitles Type</title>
  <tags>
    <tag>archive</tag>
  </tags>
  <description></description>
  <operations>

    <!-- Add a language tag for the english subtitles -->

    <operation
        id="tag"
        description="Tagging media package elements">
      <configurations>
        <configuration key="source-flavors">captions/vtt+en</configuration>
        <configuration key="target-tags">+lang:en</configuration>
      </configurations>
    </operation>

    <!-- Change the type of all files with the "captions/*" flavor to track -->
    <!-- And their flavor to "captions/source" -->

    <operation
        id="changetype"
        description="Retracting elements flavored with presentation and tagged with preview from Engage">
      <configurations>
        <configuration key="source-flavors">captions/*</configuration>
        <configuration key="target-flavor">captions/source</configuration>
        <configuration key="target-type">track</configuration>
      </configurations>
    </operation>

    <!-- Save changes -->

    <operation
        id="snapshot"
        if="NOT (${presenter_delivery_exists} OR ${presentation_delivery_exists})"
        description="Archive publishing information">
      <configurations>
        <configuration key="source-tags">archive</configuration>
      </configurations>
    </operation>

    <!-- Clean up work artifacts -->

    <operation
        id="cleanup"
        fail-on-error="false"
        description="Remove temporary processing artifacts">
      <configurations>
        <!-- On systems with shared workspace or working file repository -->
        <!-- you want to set this option to false. -->
        <configuration key="delete-external">true</configuration>
        <!-- ACLs are required again when working through ActiveMQ messages -->
        <configuration key="preserve-flavors">security/*</configuration>
      </configurations>
    </operation>

  </operations>
</definition>
```
</details>

Configuration changes
---------------------
- The default admin ui is now the "new" admin ui. If it does not suit your needs for whatever reason, you can always
  switch back the "old" admin ui in `etc/ui-config/mh_default_org/runtime-info-ui/settings.json`
  [[#5414](https://github.com/opencast/opencast/pull/5414)]
- Paella Player 6 has been turned into a plugin. If you are still using it, you will need to enable it.
  [[#4965](https://github.com/opencast/opencast/pull/4965)]

Database Migration
------------------

There is no database migration required for upgrading form 14.x to 15.x.

Elasticsearch Index Rebuild
---------------------------
There is no index rebuild required for upgrading form 14.x to 15.x.
