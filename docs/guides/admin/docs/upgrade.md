# Upgrading Opencast from 16.x to 17.x

This guide describes how to upgrade Opencast 16.x to 17.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

1. Read the [release notes](releasenotes.md)
2. Stop your current Opencast instance
3. Install the required [Elasticsearch/OpenSearch plugin](#elasticsearchopensearch-plugin-installation)
4. Replace Opencast with the new version
5. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
6. Start Opencast
7. [Rebuild the index](#index-rebuild)
8. Optional: [Remove Gstreamer](#uninstall-gstreamer)

## Elasticsearch/OpenSearch Plugin Installation

The `analysis-icu` plugin for OpenSearch is required for Opencast 17. To install the plugin, run either

    bin/opensearch-plugin install analysis-icu

or

    bin/elasticsearch-plugin install analysis-icu

on all nodes of your Elasticsearch/Opensearch cluster.

After installation, restart the Elasticsearch/OpenSearch service before starting the Opencast 17 service
to apply the plugin changes.

## Configuration Changes

Check for changes in the configuration and apply those relevant to your setup to your files. You can use the following
command to list all changes:
```
git diff origin/r/{{ opencast_major_version() | int - 1 }}.x origin/r/{{ opencast_major_version() }}.x -- etc/
```

The most important changes are:
- The prepared flavor was removed from our workflows and the Editor configuration, the source flavor is now used instead.
  If you have custom workflows, you only need to check default settings of EditorServiceImpl.cfg.
  If you use the affected community workflows, ensure any custom workflows and integrations are still compatible.
  [[#5862](https://github.com/opencast/opencast/pull/5862)]
- We no longer generate feed previews in our workflows. [[#6087](https://github.com/opencast/opencast/pull/6087)]
- ACL templates in `etc/acl/` must now be provided in JSON format.
  [[#6018](https://github.com/opencast/opencast/pull/6018)]
- The Microsoft Azure transcription integration was completely rewritten, please check
  [documentation](configuration/transcription.configuration/microsoftazure.md) if you use it.
  [[#5876](https://github.com/opencast/opencast/pull/5876)]

## Index Rebuild

[[#5413](https://github.com/opencast/opencast/pull/5413)] only requires an update of the index mappings, for which there
are migration scripts in `docs/upgrade/16_to_17/`, but [[#6023](https://github.com/opencast/opencast/pull/6023)]
requires a full rebuild of the indexes that feed the Admin Interface and External API, so that is what's described here.

You can do so by making a POST request to `/index/rebuild`, either like this

    curl -i -u <admin_user>:<password> -s -X POST https://example.opencast.org/index/rebuild

or by opening the REST docs under the “?” symbol at the top right corner of the Admin Interface, going to the “Index
Endpoint” section and using the testing form on `/rebuild` to issue a POST request.

In both cases you should get a 200 HTTP status. The log or the Admin Interface will tell you when the rebuild is
completed.

## Uninstall Gstreamer

The Microsoft Azure transcription integration was completely rewritten by
[[#5876](https://github.com/opencast/opencast/pull/5876)] and Gstreamer is no longer needed as a dependency.
If you had it installed for this purpose, you can remove it now.
