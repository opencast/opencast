# Upgrading Opencast from 15.x to 16.x

This guide describes how to upgrade Opencast 15.x to 16.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Read the [release notes](releasenotes.md)
1. Stop your current Opencast instance
1. Replace Opencast with the new version
1. [Migrate Solr to OpenSearch/Elasticsearch](#search-service-migration)
1. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
1. [API changes](#api-changes)
1. Start Opencast

## Search Service Migration

> Opencast works with both Opensearch 1.x and Elasticsearch 7.10. For this section we will just talk about OpenSearch,
> which is our recommendation, but this will work with both.

The search service now uses OpenSearch instead of Solr. This means that you need to ensure that the presentation node
has access to your OpenSearch. Previously, only the admin node needed access and many installation just has OpenSearch
as an internal service on that machine.

You also need to rebuild the search service index. Note that the search service has a separate index which is way faster
to rebuild and you don't need to rebuild all indexes. To start this process:

- Go to the REST docs
- Search for `/index`
- Go to `POST /rebuild/{service}`
- Set `Search` as service and submit
- The logs should show the status of the index rebuild

## Configuration changes

Check for changes in configuration files and apply those to your local copies. You can use the following command
to list all changes:
```
git diff origin/r/15.x origin/r/16.x -- etc/
```

Please make sure to apply at least the following changes since they are crucial for a stable setup:
- Grant acces to the assets directory in `etc/security/mh_default_org.xml`. @see [#5937](https://github.com/opencast/opencast/pull/5937/files)

## API Changes

Migrating from Solr to OpenSearch/Elasticsearch in the search service caused
subtle changes in the `/search/…` API endpoints. The most important changes are:

- The endpoints are now JSON only. No more XML
- The `search-results` sub-object has been removen and it's contents moved one layer up.
- Dublin Core metadata and ACLs are now available via `acl` and `dc` attributes in episodes and series.

### Series

```
❯ curl -s -u admin:opencast 'http://localhost:8080/search/series.json?limit=1&offset=0' | jq
```
```json
{
  "limit": "1",
  "result": [
    {
      "org": "mh_default_org",
      "acl": {
        "read": [
          "ROLE_USER"
        ],
        "dance": [
          "ROLE_USER"
        ]
      },
      "dc": {
        "identifier": [
          "e12d5290-0825-4868-a8e6-1d75d4af9d41"
        ],
        "creator": [
          "lk"
        ],
        "created": [
          "2022-06-30T16:39Z"
        ],
        "modified": [
          "2022-06-30T18:40:49.898224"
        ],
        "title": [
          "I 🖤 Opencast"
        ]
      }
    }
  ],
  "total": 0,
  "offset": "0"
}
```

### Episodes

```
❯ curl -s -u admin:opencast 'http://localhost:8080/search/episode.json?limit=1' | jq
```
```json
{
  "limit": 1,
  "result": [
    {
      "mediapackage": {
        "duration": 4160.0,
        "seriestitle": "I 🖤 Opencast",
        "metadata": {
          "catalog": {
            "mimetype": "text/xml",
            "id": "62a0124f-f321-4d0f-8891-6f67e7cfb4e6",
            "type": "dublincore/episode",
            "url": "http://localhost:8080/static/mh_default_org/engage-player/04/87b61d14-0457-4810-9ef3-04dba28dcd18/dublincore.xml",
            "tags": ""
          }
        },
        "attachments": {
          "attachment": [
            {
              "ref": "track:9ae01c64-136c-4297-ae8a-888ca7135be5",
              "size": 49701.0,
              "mimetype": "image/jpeg",
              "id": "9154577f-575d-454b-96d5-81f18651959f",
              "type": "presenter/player+preview",
              "url": "http://localhost:8080/static/mh_default_org/engage-player/04/c0e8bf65-fcde-463a-b8a5-ad6025ed00ef/goat_1_000s_player.jpg",
              "tags": {
                "tag": "engage-download"
              }
            },
            {
              "ref": "track:9ae01c64-136c-4297-ae8a-888ca7135be5",
              "size": 4137.0,
              "mimetype": "image/jpeg",
              "id": "b776aaaf-b9ed-43d5-9ae5-830a51988081",
              "type": "presenter/search+preview",
              "url": "http://localhost:8080/static/mh_default_org/engage-player/04/707bb709-61c0-4b39-becb-51553e297ce4/goat_1_000s_search.jpg",
              "tags": {
                "tag": "engage-download"
              }
            },
            {
              "mimetype": "text/xml",
              "id": "868ebbf2-2670-48cb-b404-b2d5113dfab8",
              "type": "security/xacml+series",
              "url": "http://localhost:8080/static/mh_default_org/engage-player/04/security-policy-series/xacml.xml",
              "tags": ""
            },
            {
              "mimetype": "text/xml",
              "id": "d82f6b92-4d06-44f9-a3a0-6a078ccfdf72",
              "type": "security/xacml+episode",
              "url": "http://localhost:8080/static/mh_default_org/engage-player/04/3a2e4cf3-6fb4-4cd2-a2b7-e03a0bba5e24/episode_security.xml",
              "tags": ""
            }
          ]
        },
        "series": "e12d5290-0825-4868-a8e6-1d75d4af9d41",
        "creators": {
          "creator": "hafnyl"
        },
        "start": "2022-06-30T16:39:00Z",
        "id": "04",
        "media": {
          "track": {
            "video": {
              "framecount": 208.0,
              "framerate": 50.0,
              "bitrate": 9390780.0,
              "id": "video-1",
              "device": "",
              "encoder": {
                "type": "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10"
              },
              "resolution": "1920x1080"
            },
            "type": "presenter/preview",
            "url": "http://localhost:8080/static/mh_default_org/engage-player/04/9b8e8a5f-831f-4114-baf1-5c333c0ba482/goat.mp4",
            "tags": {
              "tag": [
                "atom",
                "default",
                "engage-download",
                "engage-streaming",
                "rss"
              ]
            },
            "master": false,
            "duration": 4160.0,
            "ref": "track:9ae01c64-136c-4297-ae8a-888ca7135be5",
            "size": 4955801.0,
            "checksum": {
              "$": "02780f257e9397883517ebed78a2df98",
              "type": "md5"
            },
            "mimetype": "video/mp4",
            "id": "33e1c89c-586a-482b-a98d-14e6c4439fef",
            "audio": {
              "framecount": 196.0,
              "channels": 2.0,
              "samplingrate": 48000.0,
              "bitrate": 126382.0,
              "id": "audio-1",
              "device": "",
              "encoder": {
                "type": "AAC (Advanced Audio Coding)"
              }
            },
            "live": false
          }
        },
        "title": "I 🖤 Opencast (04)",
        "publications": ""
      },
      "org": "mh_default_org",
      "acl": {
        "read": [
          "ROLE_USER"
        ],
        "dance": [
          "ROLE_USER"
        ]
      },
      "dc": {
        "extent": [
          "PT4.163S"
        ],
        "identifier": [
          "04"
        ],
        "creator": [
          "hafnyl"
        ],
        "created": [
          "2022-06-30T16:39Z"
        ],
        "modified": [
          "2022-06-30T18:40:43.627963"
        ],
        "isPartOf": [
          "e12d5290-0825-4868-a8e6-1d75d4af9d41"
        ],
        "title": [
          "I 🖤 Opencast (04)"
        ]
      }
    }
  ],
  "total": 5,
  "offset": 0
}
```
