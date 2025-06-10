# Opencast 17: Release Notes

## Opencast 17.4

This release contains a few bug fixes and enhancements of existing features. Among others, it enables the use of
Elasticsearch even if the setup is in yellow state ([#6681](https://github.com/opencast/opencast/pull/6681)).
It also adds an endpoint to trigger the update of a single event in search
([#6653](https://github.com/opencast/opencast/pull/6653)).

Most importantly, this contains a new version of both the Editor
([17.x-2025-05-23](https://github.com/opencast/opencast-editor/releases/tag/17.x-2025-05-23)) and the Admin UI
([17.x-2025-05-24](https://github.com/opencast/opencast-admin-interface/releases/tag/17.x-2025-05-24)).

Additionally it contains a new version of Opencast Studio
([2025-04-30](https://github.com/elan-ev/opencast-studio/releases/tag/2025-04-30))
with a bugfix regarding incorrect error messages when uploading.

## Opencast 17.3

This release contains a few bug fixes, among them performance improvements for the metrics endpoint
([#6627](https://github.com/opencast/opencast/pull/6627)). It's now also possible to trigger an index update for
the archived information of an event ([#6575](https://github.com/opencast/opencast/pull/6575)).

Additionally, this contains new versions of both the Admin Interface
([2025-04-17](https://github.com/opencast/opencast-admin-interface/releases/tag/2025-04-17)) and Studio
([2025-04-02](https://github.com/elan-ev/opencast-studio/releases/tag/2025-04-02)).

## Opencast 17.2

This is a maintenance release of Opencast 17.
It fixes a few minor bugs.

## Opencast 17.1

This release contains a couple of bug fixes. It also includes new versions for the editor
([2025-01-08](https://github.com/opencast/opencast-editor/releases/tag/2025-01-08)) and the admin interface
([2025-01-21](https://github.com/opencast/opencast-admin-interface/releases/tag/2025-01-21)). See the respective release
notes for more details.

One can now also remove subtitles from within the editor [[#6361](https://github.com/opencast/opencast/pull/6361)] and
configure asset upload options in more detail [[#6362](https://github.com/opencast/opencast/pull/6362)].

## Opencast 17.0

### Features
- New roles were added with which a user can get access to an event without having to be added to the ACLs. For more
  details see the relevant [documentation](configuration/episode-id-roles.md).
  [[#5056](https://github.com/opencast/opencast/pull/5056)]
- You can now generate captions with Whisper asynchronously. [[#6247](https://github.com/opencast/opencast/pull/6247)]
- The Whisper.cpp integration now automatically re-encodes audio to a suitable format. It's
  no longer necessary to include a separate encode operation for this in the workflow.
  [[#6248](https://github.com/opencast/opencast/pull/6248)]
- You can now provide terms of use to be presented to new users in the Admin UI. For configuration see
  [the respective docs](configuration/admin-ui/terms.md). [[#6010](https://github.com/opencast/opencast/pull/6010)]
- There is a new UI for the documentation of our REST endpoints but since it currently has some issues, it's not turned
  on by default. You can however already try it out. [[#5668](https://github.com/opencast/opencast/pull/5668)]
- There is a new GraphQL API as an alternative to the existing REST API. Please be aware that this is not yet production
  ready and may change without further notice. [[#5766](https://github.com/opencast/opencast/pull/5766)]
- There is a new filter available in the Admin interface to show only events that are published or not published.
  [[#6023](https://github.com/opencast/opencast/pull/6023)]
- The integration of Tobira in the Admin interface was improved.
  [[#6091](https://github.com/opencast/opencast/pull/6091)]
- Paella Player 7 now also shows a preview image in portrait mode.
  [[#6052](https://github.com/opencast/opencast/pull/6052)]

### Breaking Changes
- **The old Admin UI was removed completely and is no longer available.**
[[#5965](https://github.com/opencast/opencast/pull/5965)]
- We changed the sorting of events and series in the new Admin interface to something more intuitive. This **requires the
  installation of the OpenSearch/Elasticsearch plugin** `analysis-icu`. Check the [upgrade guide](upgrade.md) for more
  details. [[#5413](https://github.com/opencast/opencast/pull/5413)]
- The required Java version is now 17. [[#5763](https://github.com/opencast/opencast/pull/5763)]
- The Microsoft Azure transcription integration was completely rewritten. Gstreamer is no longer needed as a dependency.
  Please consult the updated [documentation](configuration/transcription.configuration/microsoftazure.md) to find out
  how to change your configuration and workflows so they are compatible again.
  [[#5876](https://github.com/opencast/opencast/pull/5876)]
- ACL templates in `etc/acl/` must be provided in JSON format from now on. XACML files are no longer accepted.
  [[#6018](https://github.com/opencast/opencast/pull/6018)]

### Configuration Changes
- We no longer use the prepared flavor in our workflows and the Editor configuration as it was no longer serving any
  purpose and made updating subtitles from the Editor difficult. Instead, we now always use the source flavor.
  [[#5862](https://github.com/opencast/opencast/pull/5862)]
- Since we removed feeds in OC 16, we no longer generate feed previews in our workflows.
  [[#6087](https://github.com/opencast/opencast/pull/6087)]
- Waveform peaks are now more visible, but that behavior is configurable.
  [[#6028](https://github.com/opencast/opencast/pull/6028)]

For more details, please take a look at the [full changelog](changelog.md). If you want to update Opencast from a
previous version, you should also read the [upgrade guide](upgrade.md).


## Release Schedule

| Date             | Phase                    |
|------------------|--------------------------|
| November 6, 2024 | Release Branch Cut       |
| December 4, 2024 | Release of Opencast 17.0 |


## Release Managers

- Katrin Ihler (elan e.V.)
- Veronika Schröer (University of Konstanz)
