Basic Statistics
================

Opencast records basic information about how videos are used, like when they are played and which parts of them are
watched. This is used to calculate view counts. In a standard installation, nothing needs to be configured. This page
describes the few things you should know about if you run a cluster or a more customized setup.

The recorded data does not contain IP addresses or user agents. These are only used to derive an anonymous session
identifier, together with a secret that is replaced every day. Sessions can therefore not be linked across days.


Configuration
-------------

The configuration file is `etc/org.opencastproject.basicstatistics.BasicStatisticsService.cfg`.

Statistical data may be incomplete or missing from before Opencast started collecting it. Users are told about this
using a threshold date. By default, this is the date statistics were first started on the server. To use a specific date
instead, set it in ISO format:

    completeness.threshold.date=2026-08-28

Everybody can send statistics to Opencast, as they come from the browsers of the viewers. To keep anyone from filling up the database, there are limits for what one client can send, in bytes: for a single request, and for all the requests of one IP address within a minute and within an hour. Anything above a limit is ignored. A limit of 0 means there is no limit. These are the defaults:

    ratelimit.request.bytes=8192
    ratelimit.minute.bytes=524288
    ratelimit.hour.bytes=8388608

Raw events are only kept for a limited time, after which they are deleted, so the underlying database table does
not grow forever. Once an event is deleted, the day it belongs to can no longer be covered by manual
re-aggregation. A limit of 0 means events are kept forever. The default is 90 days:

    raw.events.retention.days=90

The same setting also bounds how old an event reported to `trustedPush` by a trusted server (see below) may be,
since nothing older than retention could ever be aggregated before it gets deleted.

Increasing this value after already running with a smaller one does not recover days that were already deleted
under the old, smaller setting: a trusted server could then push an event old enough to land in such an
already-pruned day. If that day is later manually re-aggregated, it would be recomputed from only that one
resurfaced event, which can show a lower view count than before — treat that as a sign the day's data is
incomplete, not as a correction.


Requirements
------------

### One admin node

The secret mentioned above is created by the node running the admin interface (admin, admin and presentation, or
all-in-one) and is fetched from there by presentation nodes. Running more than one such node in a cluster is not
supported: each of them would create its own secret, and the same viewer would not get a consistent session identifier.

### Reverse proxy

If Opencast runs behind a reverse proxy:

- Pass the address of the client on in the `X-Forwarded-For` header. Without it, all viewers look like they come from
  the proxy, and are told apart only by their browser. Viewers with the same browser would be counted as one. The
  example in [Using Nginx](https/nginx.md) does this.
- Browsers send their statistics directly to `/basic-statistics/clientPush` on Opencast. If the player is not served
  from the same host as Opencast, the proxy has to allow these cross-origin (CORS) requests, including the preflight
  request that browsers send for them. This is not needed if the player is served by Opencast itself.


File Delivery by Another Server
-------------------------------

Opencast's static file server records which video files are delivered. If the files are delivered by something else,
Opencast does not see these requests. This is the case for example when a separate file server
like octoka is used. That server then has to report the deliveries to Opencast itself:

1. Create a user with the role `ROLE_BASIC_STATISTICS_TRUSTED_PUSH` (see
   [Authentication, Authorization and User Management](security.md)).
2. Configure the file server with the credentials of this user. It has to send its reports to
   `POST /basic-statistics/trustedPush`, and has to include the IP address and the user agent of the client that
   requested the file. The details are described in the
   [developer documentation](https://docs.opencast.org/develop/developer/architecture/basicstatistics/).

Anyone with these credentials can add arbitrary data to the statistics, so use a dedicated user, keep the credentials
secret and use HTTPS.

To stop Opencast's own static file server from recording file fetches, without affecting anything served through it,
set this in `etc/org.opencastproject.fsresources.StaticResourceServlet.cfg`:

    record.file.fetches=false
