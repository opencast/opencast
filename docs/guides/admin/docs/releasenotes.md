# Opencast 13: Release Notes


Opencast 13.8
-------------

The eighth maintenance release of Opencast 13.

- Increase index rebuild logging frequency for batches ([#5105](https://github.com/opencast/opencast/pull/5105))
- Add endpoint to get languages & licenses ([#5102](https://github.com/opencast/opencast/pull/5102))
- Log dispatch interval in seconds, not milliseconds ([#5064](https://github.com/opencast/opencast/pull/5064))
- Allow engage ui and ltitools to handle non-16/9 thumbnails ([#5063](https://github.com/opencast/opencast/pull/5063))
- Allow deletion of reference users from the Admin UI ([#5058](https://github.com/opencast/opencast/pull/5058))
- Dont copy media files to new scheduled event ([#5055](https://github.com/opencast/opencast/pull/5055))
- Fix tainted canvas in editor thumbnail extractor ([#5052](https://github.com/opencast/opencast/pull/5052))
- Enable Tobira adopter stats tracking ([#5040](https://github.com/opencast/opencast/pull/5040))
- Count user references ([#5032](https://github.com/opencast/opencast/pull/5032))
- Show user information in event workflow details ([#4970](https://github.com/opencast/opencast/pull/4970))
- Automate PR comments with built tarball links ([#4969](https://github.com/opencast/opencast/pull/4969))

See [changelog](changelog.md) for a comprehensive list of changes.


Opencast 13.7
-------------

The seventh maintenance release of Opencast 13.

- Various fixes including fixes for live events, Paella player 7, user references and workspace cleanup.

See [changelog](changelog.md) for a comprehensive list of changes.


Opencast 13.6
-------------

The sixth maintenance release of Opencast 13.

- Extend configuration options of Amberscript integration ([#4868](https://github.com/opencast/opencast/pull/4868))
- Mitigate not loading ACLs on first start ([#4923](https://github.com/opencast/opencast/pull/4923))
- Remove empty options in ACL template select ([#4961](https://github.com/opencast/opencast/pull/4961))
- Editor Release 2023-04-20 ([#4879](https://github.com/opencast/opencast/pull/4879))
- Fix missing JARs in released distributions ([#4925](https://github.com/opencast/opencast/pull/4925))

See [changelog](changelog.md) for a comprehensive list of changes.


Opencast 13.5
-------------

The fifth maintenance release of Opencast 13.

- Whisper language detection and tagging ([#4770](https://github.com/opencast/opencast/pull/4770))
  Important Note:
    - If you use whisper, update to the latest version !
- Paella 7: Allow to disable the cookie consent banner ([#4782](https://github.com/opencast/opencast/pull/4782))
- Add paging to asset manager index rebuild ([#4783](https://github.com/opencast/opencast/pull/4783))
- Fixed pagination when reindexing asset manager ([#4843](https://github.com/opencast/opencast/pull/4843))

See [changelog](changelog.md) for a comprehensive list of changes.


Opencast 13.4
-------------

The fourth maintenance release of Opencast 13.

- Fix default values for the Admin UI ACL editor. Previously the roles set for new events might not correspond to the
  rest of Opencast's defaults. ([#4799](https://github.com/opencast/opencast/pull/4799))

See [changelog](changelog.md) for a comprehensive list of changes.


Opencast 13.3
-------------

The third maintenance release of Opencast 13.

- Add paging to asset manager index rebuild ([#4783](https://github.com/opencast/opencast/pull/4783))
- Fix reindex of multi-tanant systems ([#4740](https://github.com/opencast/opencast/pull/4740))
- Fix exception when retrieving comments where the author is missing ([#4739](https://github.com/opencast/opencast/pull/4739))
- Paella 7 matomo plugin ([#4722](https://github.com/opencast/opencast/pull/4722))
- Dependabot-batcher update ([#4707](https://github.com/opencast/opencast/pull/4707))
- Fix typo and adds recomendations to whisper doc ([#4683](https://github.com/opencast/opencast/pull/4683))

See [changelog](changelog.md) for a comprehensive list of changes.


Opencast 13.2
-------------

The second maintenance release of Opencast 13.

- Fix calendar.json endpoint ([#4619](https://github.com/opencast/opencast/pull/4619))
- Add missing expected response code ([#4628](https://github.com/opencast/opencast/pull/4628))
- Add webvtt-to-cutmarks to list of workflow operations ([#4654](https://github.com/opencast/opencast/pull/4654))
- Fix multiple bugs in the adopters registration resulting in incorrect counts ([#4616](https://github.com/opencast/opencast/pull/4616))

See [changelog](changelog.md) for a comprehensive list of changes.


Opencast 13.1
-------------

The first maintenance release of Opencast 13.

- Bug fix: publish engage woh with merge SKIP the operation if media package not in search index
([#4478](https://github.com/opencast/opencast/pull/4478))
- Add silent detection based on subtitles (webvtt-to-cutmarks woh) ([#4482](https://github.com/opencast/opencast/pull/4482))
- Fix: series deleted from search index cannot be re-added ([#4502](https://github.com/opencast/opencast/pull/4502))
- Adds Whisper STT to SpeechToText WoH ([#4513](https://github.com/opencast/opencast/pull/4513))
- Change default hotkeys for create dialogs in admin UI ([#4516](https://github.com/opencast/opencast/pull/4516))
- Reduce number of snapshots taken in the new editor backend ([#4519](https://github.com/opencast/opencast/pull/4519))
- Avoid using jobs in SeriesUpdatedEventHandler ([#4536](https://github.com/opencast/opencast/pull/4536))

See [changelog](changelog.md) for a comprehensive list of changes.


Opencast 13.0
-------------

### Features

- New Plugin Management. Some parts of Opencast have become plugins and are now disabled by default but can be easily
  enabled again by flipping a single configuration option. ([#3218](https://github.com/opencast/opencast/pull/3218))
  For a list of plugins, take a look at
    - `etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg` or the
    - documentation: [The Plugin Management Documentation](modules/plugin-management.md)
- Comments in the event index. Adds comment reason, text and resolvedStatus to the events index. This allows for
  filtering events by certain comments in the Admin UI by using the searchbar. For example, if you are using the notes
  column in the events table, you could search for all events that have a note that contains "silent". This requires
  an index rebuild. ([#4029](https://github.com/opencast/opencast/pull/4029))
- Support for f4v file type ([#4280](https://github.com/opencast/opencast/pull/4280))
- Whisper as new Speech-To-Text engine. The new engine comes with additional configuration options and with a config
  file to choose what model to use and the CLI command. ([#4513](https://github.com/opencast/opencast/pull/4513))
- Specialist worker notes added. Adds the ability to define a list of worker nodes that gets preferred when dispatching
  compilation jobs (Job Type: org.opencastproject.composer). This could for example be useful in a setup with one or
  more GPU accelerated nodes. ([#3741](https://github.com/opencast/opencast/pull/3741))
  This feature is disabled by default and only activated when a list of specialized worker nodes is set.
  The comma-separated list of worker nodes is defined in the configuration file:
    - `etc/org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.cfg`
  To ensure backwards compatibility not defining a list of specialized worker nodes is safe and leaves the behavior of
  the system unchanged.
- The new `assert` workflow operation allows asserting that certain expressions are `true` or `false`. This replaces
  the similar `failing` operation. ([#4358](https://github.com/opencast/opencast/pull/4358))

### Improvements

- Much faster fading effect for cuts from the video editor. ([#4013](https://github.com/opencast/opencast/pull/4013))
- Default resolution of live schedule impl changed to standard 16:9 ([#4098](https://github.com/opencast/opencast/pull/4098))
- JobDispatcher extracted from inside the ServiceRegistry into its own class. This makes the JobDispatcher a
  configurable OSGi service, such that dispatching can be enabled or disabled during runtime. Also changes the
  default job dispatch behaviour to not dispatch jobs, and alters the node profiles to enable it by default on
  the expected nodes. This should not change current job dispatcher behaviour, aside from being able to enable
  or disable job dispatch on the fly. ([#3607](https://github.com/opencast/opencast/pull/3607))
- Multi-tenant clusters can now define a common metadata catalog per tenant. ([#4181](https://github.com/opencast/opencast/pull/4181))
- PostgreSQL is no longer considered experimental and fully supported. ([#4359](https://github.com/opencast/opencast/pull/4359))
- The `send-email` workflow operation now has the following improvements:
    - Templates can now include values from all extended metadata catalogs. Previously, the flavor of the extended
      catalog had to start with `dublincore/`. ([#4376](https://github.com/opencast/opencast/pull/4376))
    - Templates can now include values from organization properties.
      This can be useful for multi-tenant systems. ([#4380](https://github.com/opencast/opencast/pull/4380))
    - `send-email` can now send multipart emails using text and HTML. ([#4408](https://github.com/opencast/opencast/pull/4408))
- User details can now be mapped from LDAP attributes. ([#4440](https://github.com/opencast/opencast/pull/4440))


### Behavior changes

- Navigation shortcuts to switch tabs in modals for new events and new series in admin-ui. The combinations are:
  `alt + enter` for next or - if applicable - the create button, and `alt + backspace` for the previous button
  (alt == option on macOS). **Note**: Shortcuts will not be detected while `<input>` or `<select>` elements are focused.
  In most cases, you will need to press the enter-key first. US translations for the hotkey cheatSheet are included and
  can be seen by pressing the `?` key while the modals are open. ([#3998](https://github.com/opencast/opencast/pull/3998))
- Extended metadata of events and series are put into the Elasticsearch index. This can be used to filter by extended
  metadata fields via the full text search. The series extended metadata is indexed by the Series Service and the event
  extended metadata by the Asset Manager. ([#3274](https://github.com/opencast/opencast/pull/3274))
- Theodul Player is disabled by default and will be removed in OC 14 ([#4315](https://github.com/opencast/opencast/pull/4315))
- The LDAP implementation has been simplified, which may require adapting the `etc/security/mh_default_org.xml`
  configuration file. ([#4383](https://github.com/opencast/opencast/pull/4383))
- Captions are now published in the default workflows. ([#4415](https://github.com/opencast/opencast/pull/4415))

### API changes

- New endpoint to the External API Events endpoints. It allows uploading a track to an event by sending the updated
  media package to the archive. It also allows removing all other tracks of the specified flavor. It does not start
  a workflow. ([#3670](https://github.com/opencast/opencast/pull/3670))
- The ingest API now allows setting tags when ingesting attachments or catalogs via URL.
  ([#4156](https://github.com/opencast/opencast/pull/4156))
- The ingest API now allows downloading from HTTP sources protected by HTTP basic auth. Previously only digest auth was
  supported. ([#4180](https://github.com/opencast/opencast/pull/4180))


Release Schedule
----------------

| Date                        | Phase                       |
|-----------------------------|-----------------------------|
| November 16, 2022           | Cutting the release branch  |
| November 21, 2022           | Translation week            |
| November 28, 2022           | Public QA phase             |
| December 14, 2022           | Release of Opencast 13.0    |

Release Managers
----------------

- Nadine Weidner (ELAN e.V.)
- Matthias Neugebauer (University of Münster / educast.nrw)
