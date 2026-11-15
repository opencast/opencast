# Upgrading Opencast from 18.x to 19.x

This guide describes how to upgrade Opencast 18.x to 19.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

1. Read the [release notes](releasenotes.md)
2. Check your data for [media package identifiers which are no longer accepted](#media-package-identifier-validation)
3. Stop your current Opencast instance
4. Replace Opencast with the new version
5. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
6. Upgrade the database using [the upgrade script](#database-upgrade)
7. Start Opencast
8. [Rebuild the index (if needed)](#index-rebuild)

## Configuration Changes

Check for changes in the configuration and apply those relevant to your setup to your files. You can use the following
command to list all changes:
```
git diff origin/r/{{ opencast_major_version() | int - 1 }}.x origin/r/{{ opencast_major_version() }}.x -- etc/
```

The most important changes are:

- Default workflows are now in yaml [[#6798](https://github.com/opencast/opencast/pull/6798)]
- The configuration key `video-source-flavor` for the Subtitle Timeshift Workflow Operation Handler changed to
  `video-source-flavors` (plural). [[#6901](https://github.com/opencast/opencast/pull/6901)]
- Configuring JWT authentication is now substantially simpler [[#7189](https://github.com/opencast/opencast/pull/7189)]
- The editor now displays less metadata by default [[#7053](https://github.com/opencast/opencast/pull/7053)]
- The plugin `opencast-plugin-legacy-annotation` has been removed from `org.opencastproject.plugin.impl.PluginManagerImpl.cfg`.
  [[#6902](https://github.com/opencast/opencast/pull/6902)]
- Target tag handling has changed with [[#6648](https://github.com/opencast/opencast/pull/6648)]:
- The `X-Forwarded-For`, `X-Forwarded-Proto` and `X-Forwarded-SSL` headers are now only honored if the request came
  from a reverse proxy listed in the new `etc/org.opencastproject.kernel.filter.proxy.TrustedProxies.cfg`. Its default
  covers loopback and the private address ranges, so the usual set-up with a proxy next to Opencast keeps working. If
  your proxy reaches Opencast from a public address, add it there, or Opencast will log the proxy as the client and
  serve plain HTTP URLs. See [Trusted Proxies](configuration/trusted-proxies.md).

## Database Upgrade

This script will drop a table which is no longer used in Opencast 19. You will find database
upgrade scripts in `docs/upgrade/{{ opencast_major_version() | int - 1 }}_to_{{ opencast_major_version() }}/`. **Make
sure to backup your database before migrating, to be able to easily revert changes, if necessary.**


## Index Rebuild

[[#7477](https://github.com/opencast/opencast/pull/7477)] a fix applied in 19.4 re-enabled
(Episode ID Roles)[https://docs.opencast.org/r/19.x/admin/#configuration/episode-id-roles/#episode-id-roles].  If your
site uses these roles, a reindex of the search index or republishing the affected events will resolve the issue.  This
can be safely ignored if your site does not use episode id roles.

An index rebuild for for 19.x should not be required if upgrading from an 18.x system except for the above.  Upgrading
from a version prior to 18.x requires an index rebuild, follow the 18.x upgrade guide for details.

## Data Changes (19.11 only)

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
