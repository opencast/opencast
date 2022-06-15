# Opencast 12: Release Notes

Features
--------

- Opencast 12 ships Paella Player 6.5.6 as its new default player.
- Paella Player 7 supporting new features like CSS customization is now included and
  [can be configured as an alternative player](modules/paella.player7/configuration.md).
- Upgrade the [Standalone Video Editor](modules/editor.md) to
  [version 2022-06-15](https://github.com/opencast/opencast-editor/releases/tag/2022-06-15).
- Upgrade [Opencast Studio](modules/studio.md) to
  [version 2022-06-15](https://github.com/elan-ev/opencast-studio/releases/tag/2022-06-15)
  coming with plenty new accessibility features.
- You can now define workflows in YAML. An
  [example workflow written in YAML](https://github.com/opencast/opencast/blob/r/12.x/etc/workflows/fast.yaml) is provided.
  [Documentation is available](configuration/workflow.md#using-yaml-files-with-workflows).

Improvements
------------

- ActiveMQ is no longer required by Opencast. It is safe to uninstall.
- In case a request to Elasticsearch fails because of an
  ElasticsearchStatusException, you can now configure Opencast to try again.
  For this, set `max.retry.attempts.[get|update]` in
  `etc/org.opencastproject.elasticsearch.index.ElasticsearchIndex.cfg`
  to something higher than 0. Set `retry.waiting.period.[get|update]` to a time
  period in ms to wait between retries (default: 1 second) so you don't
  overwhelm Elasticsearch.
  Both parameters can be configured separately for read-only actions and those
  that also update or delete, since arguably the success of the latter is more
  important. Changing this config does not require a restart of Opencast. See
  our [Elasticsearch docs](configuration/elasticsearch.md) for more details.
- The Series Service does not require a Solr Index anymore, simplifying the
  installation of Opencast.
- The Workflow Service does not require a Solr Index anymore, simplifying the
  installation of Opencast.
- Workflows' data is now atomically stored in the database instead of XML to
  improve access speed. [[#3376](https://github.com/opencast/opencast/pull/3376)]
- You can now specify a fallback language and use a placeholder in the Vosk-based
  [Speech to Text Workflow Operation Handler](workflowoperationhandlers/speech-to-text-woh.md).
- Improved performance when rebuilding the Elasticsearch event index. [[#3775](https://github.com/opencast/opencast/pull/3775)]
- Documentation for developers and testers has been added explaining how to
  [explore Opencast's H2 database](https://docs.opencast.org/r/12.x/developer/explore-h2-database/).


Behavior changes
-----------------

- Due to the lack of usage and thus testing, official support of Opencast for
  MySQL databases is dropped. Please use MariaDB or PostegreSQL instead.
  This does not mean that Opencast will stop working with MySQL immediately,
  but we like to highlight that developers are not spending any time on testing
  this, nor do we provide any configuration examples or support if additional
  steps may be necessary.
- The syntax of the JDBC connection configuration for MariaDB has slightly
  changed to an update of the MariaDB Connector/J. When upgrading make sure
  to follow the [upgrade guide](upgrade.md).
- Events for the same location can now be scheduled without a buffer time between them. [[#1370](https://github.com/opencast/opencast/pull/1370/files)]
- Changed inbox behaviour for additional files for scheduled events. [[#3687](https://github.com/opencast/opencast/pull/3687)]
- Identifiers for auto-generated capture series are now generated slightly
  different. This may cause new series to be generated for capture agents in
  some cases. [[#3810](https://github.com/opencast/opencast/pull/3810)]

API changes
-----------
- Important: The endpoint for querying workflows has been removed from the
  External API.
- [[#3204](https://github.com/opencast/opencast/pull/3204)] removes the fulltext
  search query from the series endpoint and adds it to the
  [External API](https://docs.opencast.org/r/12.x/developer/#api/series-api/).
- [[#3376](https://github.com/opencast/opencast/pull/3376)] removes the
  `tasks.json` endpoint from the admin interface job endpoint.
- [[#3376](https://github.com/opencast/opencast/pull/3376)] adds an endpoint to
  check for active workflows on a mediapackage to the workflow service.
- The endpoint to search through workflows has been removed from the workflow
  service.

Release Schedule
----------------

| Date                        | Phase                       |
|-----------------------------|-----------------------------|
| May 18, 2022                | Cutting the release branch  |
| May 23, 2022                | Translation week            |
| May 30, 2022                | Public QA phase             |
| June 15, 2022               | Release of Opencast 12.0    |


Release Managers
----------------

- Felix Pahlow (Martin-Luther-University of Halle-Wittenberg)
- Lars Kiesow (ELAN e.V.)
