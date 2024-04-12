# Opencast 15: Release Notes

## Opencast 15.4

### Bug Fixes

- **Quickfix dangling hard links on cephfs volumes:**
Fixes Bug for CephFS regarding strayed inodes when hard linking
[[#5682](https://github.com/opencast/opencast/pull/5682)]
- **Fixed NPE on filtering data:**
Fixes NullPointerException on roles without a description when calling REST endpoint
[[#5667](https://github.com/opencast/opencast/pull/5667)]
- **Paella7: Avoid opening downloaded video:**
Fixes bug where a video is opened in another window instead of beeing downloaded in the Paella7 download plugin
[[#5673](https://github.com/opencast/opencast/pull/5673)]
- **Mark RPMs as available:**
Fixes an issue in the Opencast documentation, accidentally
marking the RPMs for Opencast 15 as not yet available
[[#5696](https://github.com/opencast/opencast/pull/5696)]

### Improvements

- **Remove Warnings During Build**
[[#5678](https://github.com/opencast/opencast/pull/5678)]
- **Fix more JavaDoc**
[[#5677](https://github.com/opencast/opencast/pull/5677)]
- **Make a dynamic OSGi dependency static**
[[#5670](https://github.com/opencast/opencast/pull/5670)]

## Opencast 15.3

This release comes with another small change to the community workflows, please check if it works for you.

### Configuration changes

- **Changes the partial-publish.xml workflow to use thumbnails created in the editor for publication**
  [[#5543](https://github.com/opencast/opencast/pull/5543)]

### Feature

- **Adds a C/C++ implementation of OpenAI's Whisper automatic speech recognition**
  [[#4973](https://github.com/opencast/opencast/pull/4973)]
- **Run Maven without installing**
  [[#5487]](https://github.com/opencast/opencast/pull/5487)

### Bug fixes

- **Fixes a paella-core issue regarding the frameList.getImage function and
    extends the options for types of flavors for navigation slides**
  [[#5339](https://github.com/opencast/opencast/pull/5339)]
- **Updates the chrome drivers to circumvent temporary 500 errors**
  [[#5618](https://github.com/opencast/opencast/pull/5618)]

See [changelog](./changelog.md#opencast-151) for a comprehensive list of changes.

## Opencast 15.2

Opencast valentines edition! A small release this time, but Opencast 15.2 includes a small change to the standard
workflows. Please check if it affects you.

### Configuration changes

- **Don't create composites for new editor**
  Creating composites for the editor was removed from the `schedule-and-upload` workflow since the new editor can
  display multiple tracks. If you still use the old one, you can comment the necessary operations back in.
  [[#5556](https://github.com/opencast/opencast/pull/5556)]

### Feature

- **Paella 7: Download audio transcripts (without timestamps)**
   Adds the ability to download the captions files without the timestamps.
  [[#5532](https://github.com/opencast/opencast/pull/5532)]

### Bug fixes

- **Don't log internal AmberScript service state**
  [[#5530](https://github.com/opencast/opencast/pull/5530)] -
- **Avoid unnecessary FFmpeg logs**
  [[#5529](https://github.com/opencast/opencast/pull/5529)] -
- **Editor shouldn't just overwrite existing files**
  [[#5528](https://github.com/opencast/opencast/pull/5528)] -

## Opencast 15.1

Opencast 15.1 includes important bug fixes and improvements.

### Bug fixes

- **Fix Paella Player 7 for single stream videos:**
This patch fixes the issue that Paella Player 7 will run into an error if a video has only one stream
[[#5539](https://github.com/opencast/opencast/pull/5539)]

- **Fix Paella 7 with no segments:**
This patch fixes the issue that Paella Player 7 will run into an error if a video has no segments
[[#5488](https://github.com/opencast/opencast/pull/5488)]

- **Fix Paella Player 7 login redirect:**
Paella Player 7 now redirects to the login page if a user doesn't have the role ROLE_USER
[[#5481](https://github.com/opencast/opencast/pull/5481)]

- **Update xmlsec version (CAS fix):**
This patch fixes a dependency mismatch for CAS users
[[#5540](https://github.com/opencast/opencast/pull/5540)]

- **Whisper transcription engine bug fixing:**
This patch fixes a number of minor bugs and inconveniences in the whisper transcription engine
[[#5436](https://github.com/opencast/opencast/pull/5436)]

- **Fix ACL template selection breaking after first selection:**
This patch fixes an error in the ACL template selection
[[#5537](https://github.com/opencast/opencast/pull/5537)]

### (New) Admin UI Bug fixes

- **Fix publication link not clickable:**
Fixes the little pop-up for the publications of an event in the events table.
[[#217](https://github.com/opencast/opencast-admin-interface/pull/217)]
- **Fix mixed_text metadata fields without collection:**
Fixes a bug that would crash the ui when opening event or series metadata details
[[#232](https://github.com/opencast/opencast-admin-interface/pull/232)]
- **Fix exception when opening timepicker:**
This also prevented scheduling events
[[#205](https://github.com/opencast/opencast-admin-interface/pull/205)]
- **Don't call Object.entries on undefined:**
Fixes a problem where opening workflow details would break entire ui
[[#239](https://github.com/opencast/opencast-admin-interface/pull/239)]
- **Don't render "invalid date" in table cells:**
Instead render nothing, like in the old admin ui
[[#241](https://github.com/opencast/opencast-admin-interface/pull/241)]
- **Refresh workflow operations in event details**
[[#219](https://github.com/opencast/opencast-admin-interface/pull/219)]
- **Remove useAppDispatch from tableThunks.ts:**
Fixes the ui crashing when switching between pages in a table
[[#246](https://github.com/opencast/opencast-admin-interface/pull/246)]

### Improvements

- **Don't warn about using the default tool:**
Changes the log level when Opencast uses the default tool for a LTI Consumer from warn to debug
[[#5538](https://github.com/opencast/opencast/pull/5538)]

- **Add support for custom actions in ACL to Tobira harvest API:**
ROLE_ADMIN is not explicitly included in the event ACL for the Tobira specific API anymore
[[#5492](https://github.com/opencast/opencast/pull/5492)]

- **Paella7: Add support for text/vtt captions in DownloadsPlugin:**
This PR adds the captions files to the Paella Player 7 downloads plugin
[[#5491](https://github.com/opencast/opencast/pull/5491)]

See [changelog](./changelog.md#opencast-151) for a comprehensive list of changes.

## Opencast 15.0

### Features

#### Subtitles as first class citizens
With Opencast 15 we want to put more emphasis on subtitles. You can find more details on how subtitles should be
handled going forward in [Subtitles](./configuration/subtitles.md).

This comes with a bit of migration. Namely, subtitles should not be stored as "attachments" or "catalogs" anymore, but
as "media" (as they are called in the Admin UI) or "tracks" (as they are called internally). Therefore, all subtitle
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

### Improvements
- The analyze-mediapackage workflow operation can now generate variables regarding the existence of publications
  (example: `publication_engage_player_exists`) [[#5419](https://github.com/opencast/opencast/pull/5419)]
- Opencast Studio now allows users to select a series (from all series they have write access to) when uploading.
  If `upload.seriesId` is set, then that series is pre-selected in the series-selector, but the user can still change it.
  If you require the old behavior, set `upload.seriesField` to `hidden`.
  [[#5418](https://github.com/opencast/opencast/pull/5418)]
- Opencast Studio and Opencast Editor received a graphical redesign. This just changes their appearance, not their
  functionality.
- Java 17 is now supported.
  [[#4966](https://github.com/opencast/opencast/pull/4966)]
- The asset manager now supports multiple archive storages. Please be aware that spreading your archive across multiple
  file system will likely result in performance decrease, so it is still recommended to avoid it if you can.
  [[#4955](https://github.com/opencast/opencast/pull/4955)]
- The oc-remember-me cookie can now be shared between nodes, so users don't have to log into every node individually.
  [[#4951](https://github.com/opencast/opencast/pull/4951)]

### Behavior changes
- The default admin ui is now the "new" admin ui. If it does not suit your needs for whatever reason, you can always
  switch back the "old" admin ui in `etc/ui-config/mh_default_org/runtime-info-ui/settings.json`
  [[#5414](https://github.com/opencast/opencast/pull/5414)]
- The editor workflow operation now copies over tags from source tracks to trimmed tracks.
  This was already the case when trimming was skipped. If you relied on the old behavior,
  you might need to amend your workflows.
  [[#5409](https://github.com/opencast/opencast/pull/5409)]
- With this release, there will no longer be a snapshot taken to archive new assets uploaded to an existing event _before_
  the workflow is started. Instead, the workflow is expected to take care of this. The community workflow already did
  this, but if you have custom workflows, you might need to make changes to ensure this.
  [[#5247](https://github.com/opencast/opencast/pull/5247)]
- Restrictions in regard to what kind of streams can be selected in the old integrated video editor were eased to allow
  for more diverse use cases. Previous errors were demoted to warnings. The user still has to select at least one stream,
  but publishing of e.g. an audio stream without a video stream is now allowed. For more details see PR 5023.
  [[#5023](https://github.com/opencast/opencast/pull/5023)]
- The default workflow for publishing videos will no longer render the video metadata on the preview Image.
  Instead, a simple cover image without any text will be created and displayed.
  You can revert this by simply adding the "coverimage" WOH back to the "partial-publish" workflow (look out for the
  target-flavor and tags).
  [[#5353](https://github.com/opencast/opencast/pull/5353)]
- The speech-to-text workflow operation will automatically set the tags `generator` and `generator-type` and no longer
  have to be configured in the workflow.
  [[#5352](https://github.com/opencast/opencast/pull/5352)]
- Paella Player 6 has been turned into a plugin. If you are still using it, you will need to enable it.
  [[#4965](https://github.com/opencast/opencast/pull/4965)]
- The publish-engage workflow operation has a new configuration option called `add-force-flavors`. It allows adding
  elements to an existing publication via the `merge` strategy without overwriting elements with the same flavor.
  [[#4617](https://github.com/opencast/opencast/pull/4617)]

### API changes
- There are no API changes

See [changelog](./changelog.md#opencast-150) for a comprehensive list of changes.

Release Schedule
----------------

| Date              | Phase                    |
|-------------------|--------------------------|
| November 22, 2023 | Feature Freeze           |
| November 29, 2023 | Translation week         |
| December 06, 2023 | Public QA phase          |
| December 12, 2023 | Release of Opencast 15.0 |

Release Managers
----------------

- Arne Wilken (ELAN e.V.)
- Berthold Bußkamp (ssystems GmbH)
