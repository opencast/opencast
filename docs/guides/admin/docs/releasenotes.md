# Opencast 13: Release Notes


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

### Improvements

- Much faster fading effect for cuts from the video editor. ([#4013](https://github.com/opencast/opencast/pull/4013))
- Default resolution of live schedule impl changed to standard 16:9 ([#4098](https://github.com/opencast/opencast/pull/4098))
- JobDispatcher extracted from inside the ServiceRegistry into its own class. This makes the JobDispatcher a
  configurable OSGi service, such that dispatching can be enabled or disabled during runtime. Also changes the
  default job dispatch behaviour to not dispatch jobs, and alters the node profiles to enable it by default on
  the expected nodes. This should not change current job dispatcher behaviour, aside from being able to enable
  or disable job dispatch on the fly. ([#3607](https://github.com/opencast/opencast/pull/3607))
- New workflow implementation and migration fixes ([#4456](https://github.com/opencast/opencast/pull/4456))

### Behavior changes

- Navigation shortcuts to switch tabs in modals for new events and new series in admin-ui. The combinations are:
  `alt + enter` for next or - if applicable - the create button, and `alt + backspace` for the previous button
  (alt == option on macOS). **Note**: Shortcuts will not be detected while `<input>` or `<select>` elements are focused.
  In most cases, you will need to press the enter-key first. US translations for the hotkey cheatSheet are included and
  can be seen by pressing the ?key while the modals are open. ([#3998](https://github.com/opencast/opencast/pull/3998))
- Extended metadata of events and series are put into the Elasticsearch index. This can be used to filter by extended
  metadata fields via the full text search. The series extended metadata is indexed by the Series Service and the event
  extended metadata by the Asset Manager. ([#3274](https://github.com/opencast/opencast/pull/3274))
- Style requirement on all workflow operation handler documentation pages. The pages must have a title ending in
  "Operation Handler".  ([#4330](https://github.com/opencast/opencast/pull/4330))
    - The page must list the operations identifier in the form: `ID: identifier`
- Theodul Player is disabled by default and will be removed in OC 14 ([#4315](https://github.com/opencast/opencast/pull/4315))

### API changes

- Entwine functional library from the userdirectory removed.
  Replacing the code with Java streams. ([#4109](https://github.com/opencast/opencast/pull/4109))
- Specialist worker notes added. Adds the ability to define a list of worker nodes that gets preferred when dispatching
  compilation jobs (Job Type: org.opencastproject.composer). This could for example be useful in a setup with one or
  more GPU accelerated nodes. ([#3741](https://github.com/opencast/opencast/pull/3741))
  This feature is disabled by default and only activated when a list of specialized worker nodes is set.
  The comma-separated list of worker nodes is defined in the configuration file:
    - `(etc/org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.cfg)`
  To ensure backwards compatibility not defining a list of specialized worker nodes is safe and leaves the behavior of
  the system unchanged.
- New endpoint to the External API Events endpoints. It allows uploading a track to an event by sending the updated
  media package to the archive. It also allows removing all other tracks of the specified flavor. It does not start
  a workflow. ([#3670](https://github.com/opencast/opencast/pull/3670))


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
