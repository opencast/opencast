# Opencast 12: Release Notes

Opencast 12.13
--------------

The off-schedule release of Opencast 12.
The release fixes corrupt zip headers in the distributed jar files.

For more details, please take a look at [the changelog](changelog.md).

Opencast 12.12
--------------

The final maintenance release of Opencast 12.
The release fixes a few minor bugs.

For more details, please take a look at [the changelog](changelog.md).

Opencast 12.11
--------------

The eleventh maintenance release of Opencast 12.
The release fixes a few minor bugs and contains some performance improvements.

For more details, please take a look at [the changelog](changelog.md).

Opencast 12.10
--------------

The tenth maintenance release of Opencast 12.
The release fixes a problem with the default configuration.

For more details, please take a look at [the changelog](changelog.md).

Opencast 12.9
-------------

The ninth maintenance release of Opencast 12.
The release fixes a few minor bugs.

For more details, please take a look at [the changelog](changelog.md).

Opencast 12.8
-------------

The eights maintenance release of Opencast 12.
The release fixes a number of minor bugs.

For more details, please take a look at [the changelog](changelog.md).

Opencast 12.7
-------------

The seventh maintenance release of Opencast 12.

Notable changes are:

- Added a tag-engage-WOH which can be used to update tags and flavors directly
  in the engage publication
  (cf. [[#4590](https://github.com/opencast/opencast/pull/4590)]).
- Fixed an issue with signed URLs pointing at Wowza
  (cf. [[#4563](https://github.com/opencast/opencast/pull/4563)]).
- Make event updating faster after changes to series metadata/ACL
  (cf. [[#4539](https://github.com/opencast/opencast/pull/4539)]).
- Fix for segment preview if there is only one in Paella Player 7
  (cf. [[#4550](https://github.com/opencast/opencast/pull/4550)]).

Opencast 12.6
-------------

The sixth maintenance release of Opencast 12.

Notable changes are:

- With a new authentication filter you can now accept JWTs not only from
  request headers, but also from query and form parameters of a `GET` and
  `POST`-request, respectively
  (cf. [[#4412](https://github.com/opencast/opencast/pull/4412)]).
- There is a new `POST /redirect/get` endpoint which will redirect a user to a
  given URL. Note that it only supports redirect to URLs that "live" on the
  same server (relative URLs). This is useful for external applications which
  can use it to implement patterns like Post/Redirect/Get, for example in the
  context of an authentication scheme in combination with the new
  `JWTRequestParameterAuthenticationFilter`
  (cf. [[#4412](https://github.com/opencast/opencast/pull/4412)]).
- Fix *Go to Admin* and *Go to Media Module* buttons
  (cf. [[#3959](https://github.com/opencast/opencast/issues/3959)]).
- Fix Login redirect from Paella-Player 6
  (cf. [[#4493](https://github.com/opencast/opencast/issues/4493)]).
- Fix Paella Player not finding any video if audio or unsupported
  media is delivered together with supported media in Paella Player 6
  (cf. [[#4486](https://github.com/opencast/opencast/pull/4486)]).


Opencast 12.5
-------------

The fifth maintenance release of Opencast 12.

Security related changes are:

- Paella Player 6: Validate redirect to login page

Notable changes are:

- Tobira: The harvesting API now identifies the master playlist of events with
  multiple HLS streams. This is needed for example when you are using the
  `multiencode` operation; without this information, the quality selection in
  Paella can't work in Tobira.
  (See also [elan-ev/tobira#573](https://github.com/elan-ev/tobira/issues/573).)
  If this affects you, you need to re-synchronize your Tobira instance
  (cf. [[#4370](https://github.com/opencast/opencast/pull/4370)]).
- Fix for wrong type handling in the encode WOH
  (cf. [[#4382](https://github.com/opencast/opencast/pull/4382)]).
- Fix for live publications not being retracted
  (cf. [[#4250](https://github.com/opencast/opencast/pull/4250)]).
- Azure transcription service improvements
  (cf. [[#4441](https://github.com/opencast/opencast/pull/4441)]).
- Editor: Let Editor know if files are available locally
  (cf. [[#4411](https://github.com/opencast/opencast/pull/4411)]).
- LDAP: Do not add the roleprefix to extra roles
  (cf. [[#4377](https://github.com/opencast/opencast/pull/4377)]).
- E-Mail: Use UTF-8 as charset for HTML mails
  (cf. [[#4375](https://github.com/opencast/opencast/pull/4375)]).

New Features and updates:
- Initialization of new event ACL with series ACL in the Admin UI is now configurable
  (cf. [[#4249](https://github.com/opencast/opencast/pull/4249)]).

There are more bug fixes.
See [changelog](changelog.md) for a comprehensive list of changes.

Opencast 12.4
-------------

The fourth maintenance release of Opencast 12.
Notable changes are:

- [Stand-Alone Video Editor](modules/editor.md): Edit subtitles and create thumbnails.
    - Both features are disabled by default and will require additional configuration.
- Stand-Alone Video Editor: Usability and accessibility improvements (tooltips, high contrast mode) and bug fixes
- Paella Player 7: Updates
- Admin UI: Save button for event/series "Access policy"-tab
- Bug fixes

See [changelog](changelog.md) for a comprehensive list of changes.

Opencast 12.3
-------------

The third maintenance release of Opencast 12.
Notable changes are:

- Added Tobira connector to allow Opencast to work with the new video portal
- Fixed deletion of scheduled events
- Fix MariaDB upgrade script

See [changelog](changelog.md) for a comprehensive list of changes.

Opencast 12.2
-------------

The second maintenance release of Opencast 12.
Notable changes:

- New Opencast Studio version including dark mode feature
- Fix OAI-PMH
- Update CAS documentation
- Bugfix update the MariaDB database driver (again)
- Use an event title fallback from Dublin Core catalog while publishing to
  Engage
- Update the migration script installation instructions, which will otherwise
  fail with newer versions of the MariaDB Python connector

See [changelog](changelog.md) for a comprehensive list of changes.

Opencast 12.1
-------------

The first maintenance release of Opencast 12.
Notable changes:

- New editor version including dark mode feature
- Updated Paella Player 7 beta version
- Fix email sending
- Fix MariaDB database driver
- Allow creating a new event with metadata from another event
- Azure transcription integration

See [changelog](changelog.md) for a comprehensive list of changes.


Opencast 12.0
-------------

### Features

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
- The new [publication to workspace workflow operation handler](workflowoperationhandlers/publication-to-workspace-woh.md)
  allows you to copy media package elements from existing publications
  [[#3554](https://github.com/opencast/opencast/pull/3554)].

### Improvements

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
  [Speech to Text Workflow Operation Handler](workflowoperationhandlers/speechtotext-woh.md).
- Improved performance when rebuilding the Elasticsearch event index. [[#3775](https://github.com/opencast/opencast/pull/3775)]
- Documentation for developers and testers has been added explaining how to
  [explore Opencast's H2 database](https://docs.opencast.org/r/12.x/developer/explore-h2-database/).


### Behavior changes

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

### API changes

<div class=warn>
The endpoint for querying workflows has been completely removed from the External API.
It was conflicting with our removal of Solr.
We tried making sure that no one was using this, but if we missed you and you desperately need it, please reach out.
We will then see what we can reasonaably do about this.
</div>

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
