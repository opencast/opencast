# Opencast 11: Release Notes


Features
--------

- New Elasticsearch architecture, now there is one elastic search for the admin ui and the external api. This
  simplifies the maintenance and double the speed of index rebuild.
- New *select-version* workflow operation handler, used to replace the media package in the currently
  running workflow with an older version from the asset manager.
- Additional S3 distribution workflow operation handlers: *retract-partial-aws*, *publish-configure-aws*,
  *retract-configure-aws*
- Wowza stream security configuration now allows the configuration "prefix:secret"
- Allows upload of tracks (e.g. subtitles) as assets for new and existing
  events.

Improvements
------------

- Save buttons for the metadata of existing series and events in the Admin UI.
- Moved the configuration of the user interface configuration service (providing the /ui-config endpoint)
  from the global configuration to a service configuration file.
- Extracting preview images are now from the source material instead, instead of processed files.
- The groups are always updated regarded their user reference provider.
- `execute-many` and `execute-once` now print all the output in the logs
- Add support for `WebP` and `Advanced SubStation Alpha` mime types

Behavior changes
-----------------

- There is now only one Elasticsearch index for both the Admin UI and the External API. Its structure is identical
  to the old Admin UI index, thus migration is possible, alternatively the index has to be rebuilt or the old Admin UI
  index can be configured (see upgrade guide).
  With this change the index rebuild should now be twice as fast as before. The index endpoints to clear and rebuild
  the index were moved to `/index`.
- There is a completely new set of workflows. Please make sure to check your local configuration and adapt
  it accordingly if you made changes to your workflows before. Opencast will also continue to work with the old set of
  workflows. The new ones just remove a lot of redundancies, making the whole process more efficient.
  Some of the new workflows (e.g. `fast`) now use slightly different workflow configurations. This could potentially
  cause problems if you scheduled recordings using the old workflows but have the events processed using the new
  workflows. Please make sure the workflow you use work fine, or do not have anything scheduled via the upgrade.
- Changes to the service registry config at `ServiceRegistryJpaImpl.cfg`:
    - The usage of `max.attempts` is modified in the sense that if you set -1, you can disable services going into error
      state completely. Before, this was equivalent to 0, which would have the service go into error state after one
      attempt, though this was undocumented. Check your configuration to be sure you didn't rely on this behavior.
    - `no.error.state.service.types` was added. With this, you can define service types that should never go into error
      state.
- The default location of the user interface configuration service configuration is now
  `etc/org.opencastproject.uiconfig.UIConfigRest.cfg`. For more details, take a look at
  [pull request #2860](https://github.com/opencast/opencast/pull/2860).
- There are changes to how hosts are mapped to tenants. If you use a multi-tenant system you therefore need to update
  your `org.opencastproject.organization-*.cfg` configuration files:
  Before Opencast 11 the domain names were mapped to tenants and a common port number was assumed for all domains. Now
  you need to configure a URL per instance you want to map to a tenant.
```
# Before:
port=8080
prop.org.opencastproject.host.admin.example.org=tenant1-admin.example.org 
prop.org.opencastproject.host.presentation.example.org=tenant1-presentation.example.org
# Now:
prop.org.opencastproject.host.admin.example.org=https://tenant1-admin.example.org
prop.org.opencastproject.host.presentation.example.org=https://tenant1-presentation.example.org:8443
```
- Support for automatically setting up an HLS encoding ladder via the `{video,audio}.bitrates.mink`
  and `{video,audio}.bitrates.maxk` encoding profile options was removed. Instead, users should now explicitly specify
  the bit rate and bit rate control mechanism in the `ffmpeg.command`.
- Some S3 distribution workflow operation handlers have been renamed: *publish-aws* to *publish-engage-aws* and
  *retract-aws* to *retract-engage-aws*.
- The amount of job statistics for servers displayed in the admin interface was reduced to running and queued jobs to
  avoid performance problems and remove incorrect and/or misleading data.


API changes
-----------
- [[#2814](https://github.com/opencast/opencast/pull/2814)] - Add track fields `is_master_playlist` and `is_live` to
  external API
- [[#2878](https://github.com/opencast/opencast/pull/2878)] - Add endpoint to resume Index Rebuild for specified service
- [[#3002](https://github.com/opencast/opencast/pull/3002)] - Sign publication URL of events in External API
- [[#3148](https://github.com/opencast/opencast/pull/3148)] - Allow empty track duration


Additional Notes about 11.11
----------------------------

- Bug Fixes:
    - Filtering on fields in Admin UI may only show 10 terms
      (cf. [[#4200](https://github.com/opencast/opencast/pull/4200]).
    - S3 asset storage not releasing HTTP connections
      (cf. [[#4185](https://github.com/opencast/opencast/pull/4185]).
    - Race condition when loading workflow definitions
      (cf. [[#4182](https://github.com/opencast/opencast/pull/4182]).
    - Uploading multiple tracks with same flavor not working
      (cf. [[#4172](https://github.com/opencast/opencast/pull/4172]).
    - Asset manager warning after every upload to S3
      (cf. [[#4164](https://github.com/opencast/opencast/pull/4164]).
    - Ingest of series catalog via external URL not working
      (cf. [[#4155](https://github.com/opencast/opencast/pull/4155]).
- New features and updates:
    - Make deletion of live publication in case of capture errors configurable
      (cf. [[#3681](https://github.com/opencast/opencast/pull/3681]).

Additional Notes about 11.10
----------------------------

- Bug Fixes:
    - Fix CreatorDate Filter in GET api/series/
      (cf. [[#4068](https://github.com/opencast/opencast/pull/4068)]).
    - Race condition when loading 
- New features and updates:
    - Handle tracks with multiple videos in engage player
      (cf. [[#3923](https://github.com/opencast/opencast/pull/3923)]).

Additional Notes about 11.9
---------------------------

- Bug fixes:
    - Series ID's with length less than seven chars breaking the Admin UI
      (cf. [[#3902](https://github.com/opencast/opencast/pull/3902)]).
    - Ingest service download feature not working
      (cf. [[#3915](https://github.com/opencast/opencast/pull/3915)]).
- New features and updates:
    - New parameters to set the event's title and start date and time for the duplicate event workflow operation
      (cf. [[#3635](https://github.com/opencast/opencast/pull/3635)]). See the
      [dedicated doc section](workflowoperationhandlers/duplicate-event-woh.md) for more details.

Additional Notes about 11.8
---------------------------

- Bug fixes:
    - Users being removed from groups when not having the primary group role
      (cf. [[#3672](https://github.com/opencast/opencast/pull/3672)]).

Additional Notes about 11.7
---------------------------

- Security fixes:
    - This release fixes the issue that users can pass URLs from other tenants to the ingest service which will check
      only against the other organization but not against the one currently active. This allows users to easily ingest
      media from other tenants (cf.
      [[GHSA-qm6v-cg9v-53j3](https://github.com/opencast/opencast/security/advisories/GHSA-qm6v-cg9v-53j3)]).
- Bug fixes:
    - Series only being marked as deleted without actually being removed
      (cf. [[#3635](https://github.com/opencast/opencast/pull/3635)]).
    - Invalid ACLs submitted to api-endpoint being stored
      (cf. [[#3679](https://github.com/opencast/opencast/pull/3679)]).
    - Possible race condition when updating ACLs of newly created series
      (cf. [[#3680](https://github.com/opencast/opencast/pull/3680)]).
    - Failing video image extraction when the video track is shorter than the audio track
      (cf. [[#3707](https://github.com/opencast/opencast/pull/3707)]).
    - Fallback tracks not being used in the `partial-publish` workflow
      (cf. [[#3708](https://github.com/opencast/opencast/pull/3708)]).
    - Missing tags when encoding multiple qualities with the encode WOH
      (cf. [[#3639](https://github.com/opencast/opencast/pull/3639)]).
    - The metadata in the video editor can now be changed and saved again
      (cf. [[#3715](https://github.com/opencast/opencast/pull/3715)]).
- New features and updates:
    - Check if user can be loaded before starting a workflow
      (cf. [[#3661](https://github.com/opencast/opencast/pull/3661)]).
    - Make creation of default external API group configurable
      (cf. [[#3682](https://github.com/opencast/opencast/pull/3682)]).

Additional Notes about 11.6
---------------------------

- Bug fixes:
    - Upgrade Paella from 6.4.4 to 6.5.6 to repair HLS functionality (forward merged from 10.12 to this release).
    - LTI edit form not working on the presentation node (cf.
      [[#3556](https://github.com/opencast/opencast/pull/3556)]).
    - Concurrency problems with the Vosk module due to parallel removal of directories in the workspace
      (cf. [[#3630](https://github.com/opencast/opencast/pull/3630)]).
    - Termination State Services leaving nodes in maintenance mode
      (cf. [[#3605](https://github.com/opencast/opencast/pull/3605)]).
    - Stalling boot of services due to workflow definitions which failed to load
      (cf. [[#3567](https://github.com/opencast/opencast/pull/3567)]).
    - The `userdirectory-brightspace` module now works again with the latest version of the Brightspace API
      (cf. [[#3555](https://github.com/opencast/opencast/pull/3555)]).
- New features and updates:
    - Update the editor and studio to their latest releases (forward merged from 10.12 to this release).
    - The Google Speech Transcription Service now offers options to enable punctuations for transcription and to choose
      the transcription model to use. See the [dedicated doc section](modules/googlespeechtranscripts.md) for more
      details.
    - Improved error handling for the Vosk module (cf. [[#3631](https://github.com/opencast/opencast/pull/3631)]).
    - Added configuration option to control if events without series are added to an auto generated CA series during
      ingest (cf. [[#3586](https://github.com/opencast/opencast/pull/3586)]).

Additional Notes about 11.5
---------------------------

- Bug Fixes:
    - This release downgrades Paella from 6.5.5 to 6.4.4 to fix HLS videos not loading on slow connections (forward
      merged from 10.11 to this release).
    - Issues with the admin UI configuration (cf. [[#3532](https://github.com/opencast/opencast/pull/3532)]).
    - Exceptions when signing publication URLs (cf. [[#3540](https://github.com/opencast/opencast/pull/3540)]).
    - Problems in the admin UI when creating a series with  an empty title (cf.
      [[#3460](https://github.com/opencast/opencast/pull/3460)]).
    - Issues with Safari when using the editor (cf. [[#3544](https://github.com/opencast/opencast/pull/3544)]).
- New Features and updates
    - A notable new feature is the password strength indicator in the user modal. Also, the stand-alone editor was
      updated  to version 2022-03-22 (for details on the changes, see the corresponding
      [release notes](https://github.com/opencast/opencast-editor/releases/tag/2022-03-22) for the editor).
    - Configuration options for Elasticsearch have been added. In case a request to  Elasticsearch fails because of an
      `ElasticsearchStatusException`, you can now configure Opencast to try again. For this, set `max.retry.attempts`
      in `org.opencastproject.elasticsearch.index.ElasticsearchIndex.cfg` to  something higher than 0. Set
      `retry.waiting.period` to a time period in ms to wait between retries (default: 1 second) so  you don't overwhelm
      Elasticsearch. Both parameters can be configured separately for read-only actions and those that also update or
      delete, since arguably the success of the latter is more important. Changing this config does not require  a
      restart of Opencast. See the [Elasticsearch docs](configuration/elasticsearch.md) for more details.
    - Traditional chinese translations are back (cf. [[#3545](https://github.com/opencast/opencast/pull/3545)]).

Additional Notes about 11.4
---------------------------

- Improvements to the inbox behavior:
    - Extract basic metadata from compressed files using regular expressions.
      [[#3327](https://github.com/opencast/opencast/pull/3327)]
    - Match events sent to the inbox against the schedule [[#3340](https://github.com/opencast/opencast/pull/3340)]
- The capture agent calendar now can be provided as a JSON calendar
  [[#3368](https://github.com/opencast/opencast/pull/3368)]
- LDAP user directory behavior from 9.x is back [[#3344](https://github.com/opencast/opencast/pull/3344)]

Additional Notes about 11.3
---------------------------

This release fixes several bugs and a security issue related to logging which was fixed in 10.9 and forward merged to
this release (cf. [[#3305](https://github.com/opencast/opencast/pull/3305)]). A notable new feature is the
`speechtotext` workflow operation introducing support for the STT Engine Vosk (cf. the
[corresponding docs section](workflowoperationhandlers/speech-to-text-woh.md) and
[[#2855](https://github.com/opencast/opencast/pull/2855)]). Additionally, the design of the embed code selection
within the Admin UI was updated (cf. [[#3273](https://github.com/opencast/opencast/pull/3273)]). Furthermore,
[[#3152](https://github.com/opencast/opencast/pull/3152)] and [[#3154](https://github.com/opencast/opencast/pull/3154)]
introduced enhancements to the `execute-once` and `execute-many` workflow operations.

Additional Notes about 11.2
---------------------------

This release contains a security fix:

- Further mitigation for Log4Shell (CVE-2021-45105)

Like the previous release this is an out-of-order patch to address and resolve a further vulnerability discovered
by security researchers. Unlike the previous release it not only provides an updated version of Pax Logging, but
also entirely removes the replaced bundles from Opencast's assemblies to avoid confusion if people do find the old,
vulnerable version of Log4J somewhere on the filesystem, even though it is not used.

Additional Notes about 11.1
---------------------------

This release contains an updated version of Pax Logging, which provides Opencast's Log4j functionality.  Earlier
versions are affected by the Log4Shell vulnerability, which was partially mitigated in 11.0 by
[GHSA-mf4f-j588-5xm8](https://github.com/opencast/opencast/security/advisories/GHSA-mf4f-j588-5xm8).  Further
vulnerability discoveries by security researchers have rendered the previous mitigations ineffective.  Normally
we would wait for our underlying runtime (Apache Karaf) to update, however in light of the severity of these issues
we have issued an out-of-order patch to address, and resolve, these concerns immediately.


Release Schedule
----------------

| Date                        | Phase                    |
|-----------------------------|--------------------------|
| November 17, 2021           | Feature freeze           |
| November 22, 2021           | Translation week         |
| November 29, 2021           | Public QA phase          |
| December 15, 2021           | Release of Opencast 11.0 |


Release managers
----------------

- Maximiliano Lira Del Canto (University of Cologne)
- Jonathan Neugebauer (University of Münster)
