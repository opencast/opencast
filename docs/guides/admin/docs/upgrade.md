# Upgrading Opencast from 21.x to 22.x

This guide describes how to upgrade Opencast 21.x to 22.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

TODO

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
    - `etc/custom.properties`: The options `org.opencastproject.opensearch.server.hostname`,
      `org.opencastproject.opensearch.server.scheme` and `org.opencastproject.opensearch.server.port` have been
      replaced by `org.opencastproject.opensearch.server.url`. If you have set any of the old options, combine them into
      a URL, e.g. `https://opensearch.example.com:9200`, and remove the old options. Opencast's OpenSearch index does
      not start while they are still set. You can now also list multiple nodes of an OpenSearch cluster, separated by
      commas.
    - `etc/org.opencastproject.opensearch.index.OpenSearchIndex.cfg`: The options `max.retry.attempts.get`,
      `max.retry.attempts.update`, `retry.waiting.period.get` and `retry.waiting.period.update` have been replaced by
      `max.retry.attempts` and `retry.waiting.period`, which apply to all requests. If you have set any of the old
      options, set the new ones instead and remove the old ones. The configuration is rejected while they are still
      set.
- The External API (API version 1.13.0 and newer) now returns at most 100 events from `/api/events` if the request does
  not specify a limit, and rejects limits larger than 10000 for `/api/events` and `/api/series` with
  `400 Bad Request`. Clients which do not request a specific API version get this behavior. Check integrations (for
  example LMS plugins) which expect `/api/events` to return all events in a single response: they have to page through
  the results using `limit` and `offset`. The response contains no total count, so a client can only tell that more
  results exist if it receives exactly as many results as it asked for. Clients which request API version 1.12.0 or
  older keep the previous behavior.

### New and Changed Options

- **HTTP idle timeout**
    - `etc/jetty-opencast.xml` has been removed. If you enabled it by setting `org.ops4j.pax.web.config.file` in
      `etc/org.ops4j.pax.web.cfg` to raise the HTTP idle timeout, remove that setting and configure the timeout with
      `org.ops4j.pax.web.server.connector.idleTimeout` (in milliseconds) in the same file instead.
