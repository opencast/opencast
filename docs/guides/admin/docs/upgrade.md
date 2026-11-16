# Upgrading Opencast from 20.x to 21.x

This guide describes how to upgrade Opencast 20.x to 21.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

TODO

## External API

The External API versions 1.0.0 to 1.10.0 have been removed. Requests explicitly asking for one of these versions in
their `Accept` header are now rejected with `406 Not Acceptable`. If you are running integrations that pin an old
External API version, update them to use version 1.11.0 or later before upgrading.

## Configuration Changes

- Opencast now uses Apache HttpClient 5 instead of the end-of-life HttpClient 4 for all internal HTTP communication.
  Requests between nodes share a connection pool whose size can be configured with the new options
  `org.opencastproject.security.http.max.connections.per.route` and `org.opencastproject.security.http.max.connections`
  in `etc/custom.properties`. The digest nonce retry options `org.opencastproject.security.digest.nonce.*` keep their
  meaning but now trigger on a stale nonce reported by the server. External plugins using the `TrustedHttpClient`
  service must be migrated to the HttpClient 5 API (`ClassicHttpRequest`, `ClassicHttpResponse`, `getCode()`).
