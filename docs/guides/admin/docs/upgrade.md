# Upgrading Opencast from 20.x to 21.x

This guide describes how to upgrade Opencast 20.x to 21.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

1. Read the [release notes](releasenotes.md)
2. Stop your current Opencast instance
3. Set up [OpenSearch 3](#opensearch-3)
4. Replace Opencast with the new version
5. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
6. Review [data changes](#data-changes)
7. Check your workflows for [case-sensitive condition keywords](#workflow-conditions)
8. Upgrade the database using [the upgrade script](#database-upgrade)
9. Start Opencast
10. [Rebuild the index](#index-rebuild)

## OpenSearch 3

Opencast 21 uses the OpenSearch 3.x client and therefore requires OpenSearch 3.x. Elasticsearch and OpenSearch 1.x
are no longer supported.

OpenSearch 3.x refuses to open indices whose segments were created before OpenSearch 2.0.0. Every existing Opencast
index was created with Elasticsearch 7.10.2 or OpenSearch 1.x, so upgrading your OpenSearch node in place is not
enough. Instead, set up a fresh OpenSearch 3.x node (or remove all existing Opencast indices) before starting
Opencast 21. Don't forget to install the `analysis-icu` plugin. See the [OpenSearch
configuration](configuration/searchindex/opensearch.md) for details.

Opencast creates the indices automatically on startup. Afterwards, the data needs to be added again with a [full index
rebuild](#index-rebuild).

Note that the OpenSearch configuration keys in `etc/custom.properties` and the name of the index configuration file
have changed as well. See the [configuration changes](#configuration-changes) below.

## External API

The External API versions 1.0.0 to 1.10.0 have been removed. Requests explicitly asking for one of these versions in
their `Accept` header are now rejected with `406 Not Acceptable`. If you are running integrations that pin an old
External API version, update them to use version 1.11.0 or later before upgrading.

## Configuration Changes

Check for changes in the configuration and apply those relevant to your setup to your files. You can use the following
command to list all changes:
```
git diff origin/r/{{ opencast_major_version() | int - 1 }}.x origin/r/{{ opencast_major_version() }}.x -- etc/
```

### Changes Requiring Action

The following changes affect most installations. If you keep your old configuration files, Opencast may ignore your
settings or fail to work properly.

- **OpenSearch**
    - `etc/custom.properties`: All `org.opencastproject.elasticsearch.*` options have been renamed to
      `org.opencastproject.opensearch.*`. Old keys are ignored, so make sure to rename the hostname, scheme, port,
      username and password options if you have set them.
    - `etc/org.opencastproject.elasticsearch.index.ElasticsearchIndex.cfg` has been renamed to
      `etc/org.opencastproject.opensearch.index.OpenSearchIndex.cfg`. The documentation and the default value of
      `index.name` have been updated accordingly. If you have customized this file, move your changes to the new file.
- **Retract workflow**
    - `etc/workflows/delete.yaml`: The workflow `delete` has been renamed to `retract-for-deletion` to avoid confusion.
      The file name remains `delete.yaml`.
    - `etc/org.opencastproject.adminui.impl.AdminUIConfiguration.cfg` and
      `etc/org.opencastproject.external.endpoint.EventsEndpoint.cfg`: The default value of `retract.workflow.id` has
      been updated to `retract-for-deletion`. Update these files if you have set the option explicitly, and check
      your own workflows and integrations for references to the old workflow ID `delete`.
- **Security configuration**
    - `etc/security/mh_default_org.xml` has received significant updates, especially for Spring 5. If you have
      customized this file, apply your customizations to the new version instead of keeping your old file. The same
      applies to the security configuration files of any additional organizations.
- **LTI**
    - `etc/org.opencastproject.security.lti.LtiLaunchAuthenticationHandler.cfg` has been renamed to
      `etc/org.opencastproject.security.lti.LtiLaunchAuthenticationHandlerImpl.cfg`. The options are unchanged. If you
      have customized this file, rename it, otherwise your settings will not be applied.
- **Paella Player 7** has been removed [[#7589](https://github.com/opencast/opencast/pull/7589)]
    - `etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg`: The `opencast-plugin-paella7` plugin has been
      removed.
    - `etc/ui-config/mh_default_org/paella7/` has been removed.
    - `etc/org.opencastproject.organization-mh_default_org.cfg`: The documentation has been updated to reflect the
      removal. If you still use Paella Player 7, switch `prop.player` to Paella Player 8
      (`/paella8/ui/watch.html?id=#{id}`, the default) and migrate your Paella configuration to
      `etc/ui-config/<organization>/paella8/`.
- **HTTP Client**
    - Opencast now uses Apache HttpClient 5 instead of the end-of-life HttpClient 4 for all internal HTTP communication.
      Requests between nodes share a connection pool whose size can be configured with the new options
      `org.opencastproject.security.http.max.connections.per.route` and
      `org.opencastproject.security.http.max.connections` in `etc/custom.properties`. The digest nonce retry options
      `org.opencastproject.security.digest.nonce.*` keep their meaning but now trigger on a stale nonce reported by the
      server. External plugins using the `TrustedHttpClient` service must be migrated to the HttpClient 5 API
      (`ClassicHttpRequest`, `ClassicHttpResponse`, `getCode()`).

### New and Changed Options

- **Lifecycle management (beta)** [[#6139](https://github.com/opencast/opencast/pull/6139)]
    - `etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg`: The new `opencast-plugin-lifecycle-management`
      plugin has been added. It is disabled by default.
    - `etc/lifecyclepolicies/`: New directory for storing lifecycle policies. An example can be found in
      `sample.yaml.example`.
    - `etc/org.apache.felix.fileinstall-lifecyclepolicies.cfg`: New file that governs how Opencast observes the
      lifecycle policy directory.
    - `etc/org.opencastproject.lifecyclemanagement.impl.LifeCycleTaskRunner.cfg`: New file with options for lifecycle
      management.
- **Asynchronous Whisper** [[#7040](https://github.com/opencast/opencast/pull/7040)]
    - `etc/org.opencastproject.speechtotext.async.impl.SpeechToTextWorkflowSchedulerQuartz.cfg`: New file to configure
      the asynchronous generation of subtitles with Whisper. The feature is disabled by default. See the
      [Whisper documentation](configuration/transcription.configuration/whisper.md) for details.
- **Job dispatching**
    - `etc/org.opencastproject.serviceregistry.impl.JobDispatcher.cfg`: `dispatch.interval` no longer needs to be set.
      Dispatching is enabled on every node by default, and the active dispatcher is elected automatically from the
      available nodes. Set `dispatch.interval` to `0` to exclude a node from the election. The new option
      `dispatch.lock.timeout` sets how long a dispatch lease stays valid before another node may take over.
- **Search updates**
    - `etc/org.opencastproject.event.handler.SearchUpdatedEventHandler.cfg`: New file with the option
      `distribution.check.availability`. It controls whether Opencast checks the availability of distributed files
      after redistributing them, e.g. when the metadata or ACL of an event's series changes. The default behavior has
      not changed.
- **LDAP**
    - `etc/org.opencastproject.kernel.security.SpringSecurityConfigurationArtifactInstaller.cfg`: New file to configure
      the expected LDAP instance IDs for each organization.
- **YouTube publication**
    - `etc/org.opencastproject.publication.youtube.YouTubeV3PublicationServiceImpl.cfg`: New options to set the audio
      and metadata language in YouTube (`languageTarget`, `languagePatterns.*`), to set the Creative Commons license in
      YouTube (`ccLicenses`), and to set the visibility of newly created playlists (`playlistPrivacy`, default:
      `public`).

### Other Changes

- `etc/custom.properties`: The documentation of the database connection pool options has been updated. The listed
  values are recommendations for production, not Opencast's defaults.
- `etc/org.ops4j.pax.logging.cfg`: New logger configuration to stop client errors in web requests from flooding the
  logs with stack traces.
- `etc/workflows`: Thumbnail generation has been updated, and several bugs have been fixed. If you use your own
  workflows based on the community workflows, review these changes.

## Data Changes

### Media package identifier validation

Media package and media package element identifiers are now validated for path traversal (see the release note
*Media package identifier validation*). Identifiers that contain a path separator (e.g. DOI-style media package ids
like `10.0000/2`) or that are empty, `.` or `..` are rejected when a media package is parsed. This also applies to the
identifiers of the elements of a media package: a single element with such an identifier makes the whole media package
unloadable, even if the media package id itself is fine.

If your deployment has events whose media package or element identifiers are affected, those media packages can no
longer be loaded after the upgrade: the affected events cannot be listed or retracted through the API. The services
skip them with an error in the log, so they no longer keep the search service from starting or abort an index rebuild,
but they are missing from the results.

Check the database before upgrading, with the queries below. If they list nothing, no action is needed.

Deal with the affected events before the upgrade, while they can still be loaded, e.g. by ingesting their media again
under a new identifier and deleting the old event. Opencast does not ship a tool for renaming identifiers in place,
and doing so by hand is not a matter of updating the four tables queried below. The identifiers are also stored in
other tables (`oc_assets_asset`, `oc_assets_properties`, `oc_assets_version_claim`, `oc_scheduled_extended_event`,
`oc_oaipmh_elements`, `oc_event_comment`), in the OpenSearch index and in the directory names of the file system
stores, and all of them have to stay consistent.

#### Database

Media package XML is stored in four tables: `oc_search` (search service), `oc_assets_snapshot` (asset manager),
`oc_oaipmh` (OAI-PMH publications) and `oc_workflow` (workflow instances). The following queries list the records
whose media package id, or any `id` attribute in the stored media package XML, is empty, `.` or `..` or contains `/`
or `\`.

MariaDB:

```sql
SELECT id, series_id
FROM oc_search
WHERE id REGEXP '^[.]{0,2}$|[/\\\\]'
   OR mediapackage_xml REGEXP '[[:space:]]id="([.]{0,2}|[^"]*[/\\\\][^"]*)"';

SELECT DISTINCT mediapackage_id, organization_id
FROM oc_assets_snapshot
WHERE mediapackage_id REGEXP '^[.]{0,2}$|[/\\\\]'
   OR mediapackage_xml REGEXP '[[:space:]]id="([.]{0,2}|[^"]*[/\\\\][^"]*)"';

SELECT mp_id, organization, repo_id
FROM oc_oaipmh
WHERE mp_id REGEXP '^[.]{0,2}$|[/\\\\]'
   OR mediapackage_xml REGEXP '[[:space:]]id="([.]{0,2}|[^"]*[/\\\\][^"]*)"';

SELECT id, mediapackage_id, organization_id
FROM oc_workflow
WHERE mediapackage_id REGEXP '^[.]{0,2}$|[/\\\\]'
   OR mediapackage REGEXP '[[:space:]]id="([.]{0,2}|[^"]*[/\\\\][^"]*)"';
```

PostgreSQL:

```sql
SELECT id, series_id
FROM oc_search
WHERE id ~ '^[.]{0,2}$|[/\\]'
   OR mediapackage_xml ~ '[[:space:]]id="([.]{0,2}|[^"]*[/\\][^"]*)"';

SELECT DISTINCT mediapackage_id, organization_id
FROM oc_assets_snapshot
WHERE mediapackage_id ~ '^[.]{0,2}$|[/\\]'
   OR mediapackage_xml ~ '[[:space:]]id="([.]{0,2}|[^"]*[/\\][^"]*)"';

SELECT mp_id, organization, repo_id
FROM oc_oaipmh
WHERE mp_id ~ '^[.]{0,2}$|[/\\]'
   OR mediapackage_xml ~ '[[:space:]]id="([.]{0,2}|[^"]*[/\\][^"]*)"';

SELECT id, mediapackage_id, organization_id
FROM oc_workflow
WHERE mediapackage_id ~ '^[.]{0,2}$|[/\\]'
   OR mediapackage ~ '[[:space:]]id="([.]{0,2}|[^"]*[/\\][^"]*)"';
```

#### File system stores

If you cannot check the database, the file system layouts also betray affected identifiers. These checks are
heuristics: anything they list is worth a closer look, not necessarily broken.

Working file repository (`<wfr>/mediapackage/<mediaPackageId>/<elementId>/`):

```bash
# element directories without a checksum file (incomplete, or renamed out-of-band)
find "<wfr>/mediapackage" -mindepth 2 -maxdepth 2 -type d \
    -exec sh -c 'ls "$1"/*.md5 2>/dev/null | grep -q . || echo "$1"' _ {} \;
# a checksum file directly below a media package directory (element id was '.' or '')
find "<wfr>/mediapackage" -mindepth 2 -maxdepth 2 -name '*.md5'
# directory names containing a backslash
find "<wfr>" -name '*\\*'
```

Asset stores (`<store-root>/<organizationId>/<mediaPackageId>/<version>/<elementId>`):

```bash
# a version directory (a plain number) directly below the store root or an organization
# directory (media package id was '..', '.' or '')
find "<store-root>" -maxdepth 2 -type d -regextype posix-extended -regex '.*/[0-9]+'
# a directory at the level of the element files (a media package or element id contained a
# separator)
find "<store-root>" -mindepth 4 -maxdepth 4 -type d
# directory names containing a backslash
find "<store-root>" -name '*\\*'
```

Download distribution (`<download-root>/<organizationId>/<channelId>/<mediaPackageId>/<elementId>/`):

```bash
# a plain file directly below a media package directory (element id was '.' or '')
find "<download-root>" -mindepth 4 -maxdepth 4 -type f
# directory names containing a backslash
find "<download-root>" -name '*\\*'
```

## Workflow Conditions

The condition keywords `AND`, `OR`, `NOT`, `true` and `false` in workflow operation conditions (e.g. in `if`) must now
be written exactly like this. This has always been the rule, but until now a different case happened to work. With
Opencast 21, a wrong case makes the workflow fail. Check your own workflows before upgrading.

## Database Upgrade

The upgrade script drops the table `oc_bundleinfo`, whose bundle name column was too short for some bundle names.
Opencast recreates the table automatically on startup. You will find database upgrade scripts in
`docs/upgrade/{{ opencast_major_version() | int - 1 }}_to_{{ opencast_major_version() }}/`. **Make sure to backup your
database before migrating, to be able to easily revert changes, if necessary.**

## Index Rebuild

A full index rebuild is required. Besides the switch to [OpenSearch 3](#opensearch-3), which starts with empty indices,
the index mappings have changed for the improved search in the Admin UI and Engage
[[#7497](https://github.com/opencast/opencast/pull/7497)] and for custom ACL actions in the search index
[[#8028](https://github.com/opencast/opencast/pull/8028)]. The rebuild also makes sure that extended metadata is
indexed, which is now needed by the External API.

You can start the rebuild by making a POST request to `/index/rebuild`, either like this

```
curl -i -u <admin_user>:<password> -s -X POST https://example.opencast.org/index/rebuild
```

or by opening the REST docs under the “?” symbol at the top right corner of the Admin Interface, going to the
“Index Endpoint” section and using the testing form on `/rebuild` to issue a POST request.

In both cases you should get a 200 HTTP status. The log or the Admin Interface will tell you when the rebuild is
completed. Depending on the size of your installation, this may take a while.
