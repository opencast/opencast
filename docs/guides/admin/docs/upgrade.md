Upgrading Opencast from 12.x to 13.x
====================================

This guide describes how to upgrade Opencast 12.x to 13.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Read the [release notes](releasenotes.md) (especially the section of behaviour changes)
4. [Review the configuration changes and adjust your configuration accordingly](#configuration-changes)
5. [Migrate the database](#database-migration)
6. Start Opencast
7. [Rebuild the Elasticsearch indexes](#rebuild-the-elasticsearch-indexes)

Configuration Changes
---------------------

`etc/org.apache.felix.fileinstall-workflows.cfg`:

- The inclusion filter was adapted for YAML.

`etc/org.opencastproject.db.DBSessionFactoryImpl.cfg`:

- New configuration file allowing to adopt retry behavior for DB transactions.

`etc/org.opencastproject.ingest.impl.IngestServiceImpl.cfg`:

- New configuration options for allowing ingests from HTTP sources protected by basic auth.

`etc/org.opencastproject.liveschedule.impl.LiveScheduleServiceImpl.cfg`:

- Default streaming resolution was changed to 16:9.

`etc/org.opencastproject.organization-mh_default_org.cfg`:

- New configuration option to redirect Theodul requests to the configured default player.
- New configuration options for Admin UI keyboard shortcut.

`etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg`:

- New configuration file for Opencast plugins. LMS role provider (Brightspace, Canvas, Moodle and Sakai), transcription
  services, the legacy annotation service and the Theodul player are now plugins and off by default. Note that the
  Theodul player will be fully removed in Opencast 14. You may also refer to the [Plugin
  Management](modules/plugin-management.md) documentation.

`etc/org.opencastproject.serviceregistry.impl.JobDispatcher.cfg`:

- New configuration file resulting out of an internal service registry refactoring. The `dispatch.interval` option,
  previously configured in `etc/org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.cfg`, was moved to this
  file.

`etc/org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.cfg`:

- The `dispatch.interval` option was moved to `etc/org.opencastproject.serviceregistry.impl.JobDispatcher.cfg`.
- New configuration options for encoding specialized workers.

`etc/org.opencastproject.speechtotext.impl.SpeechToTextServiceImpl.cfg`:

- New configuration option for switching between Vosk and Whisper for creating automated subtitles. The default remains
  Vosk.

`etc/org.opencastproject.speechtotext.impl.engine.WhisperEngine.cfg`:

- New configuration file for configuring Whisper.

`etc/org.opencastproject.transcription.amberscript.AmberscriptTranscriptionService.cfg`:

- The default documented workflow incorrectly included `.xml` in the name.

`etc/org.opencastproject.ui.metadata.CatalogUIAdapterFactory-episode-common.cfg` and
`etc/org.opencastproject.ui.metadata.CatalogUIAdapterFactory-series-common.cfg`:

- The organization was changed to the wildcard `*` as each tenant can now have a custom common metadata catalog.

`etc/org.opencastproject.userdirectory.ldap.cfg.template`:

- New configuration options for mapping LDAP attributes to user details.

`etc/org.opencastproject.videoeditor.impl.VideoEditorServiceImpl.cfg`:

- Default values for the fade between cuts as well as the used FFmpeg command were changed.

`etc/email/errorDetails`:

- The included metadata was changed to the new syntax.

`etc/listproviders/event.upload.asset.options.properties`:

- `.f4v` was added as allowed file type.

`etc/security/mh_default_org.xml`:

- New role mappings for paths have been added.
- Basic auth entrypoint has been added to allow HTTP clients to force Opencast to use basic auth. Analogously to digest
  auth, the `X-Requested-Auth: Basic` must be included in the request.
- LDAP configuration has been adapted.

Workflow changes:

- The `failing` workflow operation is replaced by `assert`. Refer to the [Assert Workflow
  Operation](workflowoperationhandlers/assert-woh.md) documentation for more details.
- The `send-email` workflow operation no longer has the configuration option `use-html`. Instead you may now
  additionally use the `body-html` or `body-html-template-file` options for passing an HTML template. If you configure
  a text and HTML template, a multipart email including both will be created.
- The `send-email` workflow operation deprecates the `${catalogs['SUBTYPE']['FIELD']}` syntax in favor of
  `${catalogs['FLAVOR']['FIELD']}` for including catalog values into templates. The old syntax may be removed from
  future Opencast versions. Refer to the [Send Email Workflow Operation](workflowoperationhandlers/send-email-woh.md)
  documentation for more details.
- `etc/workflows/partial-error.xml`, `etc/workflows/partial-publish.xml`, `etc/workflows/publish.xml` and
  `etc/workflows/schedule-and-upload.xml` have been adapted to publish captions.

Database Migration
------------------

You can find database upgrade scripts in `docs/upgrade/12_to_13/`. These scripts are suitable for both, MariaDB and
PostgreSQL. Changes include DB schema optimizations as well as fixes for the new workflow tables.

Rebuild the Elasticsearch Indexes
----------------------------------

The 13.0 release contains multiple changes to the Elasticsearch indexes an requires a rebuild.

Start your new Opencast and make an HTTP POST request to `/index/rebuild`.

Example (using cURL):

    curl -i -u <admin_user>:<password> -s -X POST https://example.opencast.org/index/rebuild

You can also just open the REST documentation, which can be found under the “Help” section in the admin interface (the
“?” symbol at the top right corner). Then go to the “Index Endpoint” section and use the testing form on
`/rebuild` to issue a POST request.

In both cases you should get a 200 HTTP status.
