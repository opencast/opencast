# Opencast 21: Release Notes

## Opencast 21.0

For more details, please also take a look at the [full changelog](changelog/opencast-21.md). If you want to update Opencast
from a previous version, you should also read the [upgrade guide](upgrade.md).

### Features

- Lifecycle Management (beta). New modules let you define policies that regularly check which entities match a filter and then run predefined actions on them. The goal is to automate administrative tasks specific to your installation. This is a plugin and is disabled by default. To enable it, set `opencast-plugin-lifecycle-management = on` in `etc/org.opencastproject.plugin.impl.PluginManagerImpl.cfg`.
- Asynchronous Whisper. Whisper can now optionally run fully asynchronously: a workflow starts the subtitle generation and finishes, and another workflow attaches the generated subtitles later. #7040
- Better search in Admin UI and Engage. Search in both was improved. This changed the index mappings, so an index rebuild is needed (see the OpenSearch 3 upgrade below, which requires one anyway). #7497
- External API 1.12.0. Returned metadata fields now have an optional listprovider attribute, so the API version was raised to 1.12.0.
- Ingest multipart fix. addDCCatalog and discardMediaPackage now also accept `multipart/form-data` correctly. Before, a literal + in the flavor (or, for addDCCatalog, anywhere in the Dublin Core catalog) was silently turned into a space. The existing `application/x-www-form-urlencoded` handling has not changed.
- Series subjects fix. The External API's `PUT /api/series/{id}/metadata` no longer rejects the subjects field with 404 Not Found. #3491
- Search index ACL mapping fix. Custom ACL actions (e.g. annotate) in the opencast_search index are now mapped as keyword instead of text, which fixes fuzzy instead of exact role matching. This is an index mapping change: delete the opencast_search index and rebuild the search index for the fix to apply to existing data. #5925

### Breaking Changes

OpenSearch 3.x is now required. Opencast now uses the OpenSearch 3.x client. Elasticsearch and OpenSearch 1.x/2.x are no longer supported. OpenSearch 3.x refuses indices whose segments were created before 2.0.0. Every existing Opencast index was created under Elasticsearch 7.10.2 or OpenSearch 1.x, so upgrading the OpenSearch node in place is not enough. Recommended upgrade path:
- Install a fresh OpenSearch 3.x node.
- Point the `org.opencastproject.opensearch.*` configuration at the new node.
- Restart Opencast. It creates an empty index.
- Send an authenticated `POST /index/rebuild` to the admin node. Track progress with `GET /index/rebuild/states.json`.
- Shut down the old node.

Alternatively, upgrade in two steps (1.x → 2.x → 3.x) and explicitly reindex or force-merge on 2.x. Reinstall analysis-icu at each step. The `docs/upgrade/20_to_21/update_index.sh` script does not apply and has been removed.

- Paella 7 removed. It was deprecated in Opencast 20. #7589
- Extended metadata in the External API must be indexed. If extended metadata is missing, run the Asset Manager index rebuild again.
- Case-sensitive condition keywords are now enforced. AND, OR, NOT, true and false in workflow conditions must be written exactly like this. This was always the rule, but a wrong case now makes the workflow fail instead of happening to work.

### Configuration Changes

- The community delete workflow, including its default configuration value, was renamed to retract-for-deletion to avoid confusion.
    `org.opencastproject.event.handler.SearchUpdatedEventHandler.cfg` has a new option to switch off checkAvailability after redistributing files when an event's series changes (e.g. metadata or ACL). This can improve performance. The default behavior has not changed.

## Release Schedule

| Date              | Phase                    |
| ----------------- | ------------------------ |
| October 14, 2026  | Release Branch Cut       |
| November 18, 2026 | Release of Opencast 21.0 |


## Release Managers

- Jonathan Lee Komar
- Matthias Neugebauer (shio solutions GmbH | University of Münster)
