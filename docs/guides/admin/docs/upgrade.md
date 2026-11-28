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

### New and Changed Options

- **HTTP idle timeout**
    - `etc/jetty-opencast.xml` has been removed. If you enabled it by setting `org.ops4j.pax.web.config.file` in
      `etc/org.ops4j.pax.web.cfg` to raise the HTTP idle timeout, remove that setting and configure the timeout with
      `org.ops4j.pax.web.server.connector.idleTimeout` (in milliseconds) in the same file instead.
- **Live schedule**
    - The option to keep the live publication of an event when the capture agent reports a capture error has moved
      from `etc/org.opencastproject.liveschedule.message.SchedulerUpdateHandler.cfg` to
      `etc/org.opencastproject.liveschedule.impl.LiveScheduleServiceImpl.cfg` and has been renamed from
      `live.deleteOnCapureError` to `live.deleteOnCaptureError`. If you had set it to `false`, set the new option
      instead. The old file is no longer used and can be removed.
