# Upgrading Opencast from 21.x to 22.x

This guide describes how to upgrade Opencast 21.x to 22.x.
In case you need to upgrade older versions of Opencast, please refer to the documentation of
[those versions](https://docs.opencast.org) first.

TODO

## Configuration Changes

- The External API (API version 1.13.0 and newer) now returns at most 100 events from `/api/events` if the request does
  not specify a limit, and rejects limits larger than 10000 for `/api/events` and `/api/series` with
  `400 Bad Request`. Clients which do not request a specific API version get this behavior. Check integrations (for
  example LMS plugins) which expect `/api/events` to return all events in a single response: they have to page through
  the results using `limit` and `offset`. The response contains no total count, so a client can only tell that more
  results exist if it receives exactly as many results as it asked for. Clients which request API version 1.12.0 or
  older keep the previous behavior.

## External API

The External API versions 1.0.0 to 1.10.0 have been removed. Requests explicitly asking for one of these versions in
their `Accept` header are now rejected with `406 Not Acceptable`. If you are running integrations that pin an old
External API version, update them to use version 1.11.0 or later before upgrading.
