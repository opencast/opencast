Upgrading Opencast from 5.x to 6
================================

This guide describes how to upgrade Opencast 5.x to 6.0. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).

How to Upgrade
--------------

1. Stop your current Opencast instance
2. Replace Opencast 5.x with 6.x
3. Back-up Opencast files and database (optional)
4. [Upgrade the database](#database-migration)
5. [Rebuild Elasticsearch index](#rebuild-elasticsearch-index)
6. Review the [configuration](#configuration-changes) and [security configuration
   changes](#security-configuration-changes) and adjust your configuration accordingly


Database Migration
------------------

The new asset manager extensions require a database migration. The migration should be very fast and uncritical but as
with all database migrations, we recommend to make a database backup before attempting the upgrade regardless.

You can find the database upgrade script in `docs/upgrade/5_to_6/`.


Rebuild Elasticsearch Index
---------------------------

### Admin Interface

The update requires an Elasticsearch index rebuild. For that, stop Opencast, delete the index directory at `data/index`,
restart Opencast and make an HTTP POST request to `/admin-ng/index/recreateIndex`.

Example (using cURL):

    curl -i --digest -u <digest_user>:<digest_password> -H "X-Requested-Auth: Digest" -s -X POST \
      https://example.opencast.org/admin-ng/index/recreateIndex

You can also just open the REST documentation, which can be found under the “Help” section in the admin interface (the
“?” symbol at the top right corner). Then go to the “Admin UI - Index Endpoint” section and use the testing form on
`/recreateIndex` to issue a POST request.

In both cases you should get a 200 HTTP status.


### External API

If you are using the External API, then also trigger a rebuilt of its index by sending an HTTP POST request to
`/api/recreateIndex`.

Example (using cURL):

    curl -i --digest -u <digest_user>:<digest_password> -H "X-Requested-Auth: Digest" -s -X POST \
      https://example.opencast.org/api/recreateIndex

You can also just open the REST documentation, which can be found under the “Help” section in the admin interface (the
“?” symbol at the top right corner). Then go to the “External API - Base Endpoint” section and use the testing form on
`/recreateIndex`.

In both cases you should again get a 200 HTTP status.

Configuration Changes
---------------------

The Paella Player configuration has been updated for version 6.0.x.


Security Configuration Changes
------------------------------

The tenant security configuration (e.g. `etc/security/mh_default_org.xml`) has been changed and
([[MH-13082](https://opencast.jira.com/browse/MH-13082)][[#449](https://github.com/opencast/opencast/pull/449)]).  The
LTI OAuth configuration part was moved to separate configuration files, but all Opencast nodes must adapt the
configuration changes, regardless of whether the LTI is used or not.

The configuration file `etc/org.opencastproject.kernel.security.LtiLaunchAuthenticationHandler.cfg` introduces the LTI
authentication configurations.  It is now possible to define multiple trusted OAuth consumer keys.  Some other security
related configurations are also added. Please consult the [LTI configuration guide
](../modules/ltimodule/#configure-lti-optional) for the complete documentation.

In the configuration file `etc/org.opencastproject.kernel.security.OAuthConsumerDetailsService.cfg` one or more OAuth
consumer keys and their secrets can be defined.  Please consult the [LTI configuration guide
](../modules/ltimodule/#configure-oauth-authentication) for the complete documentation.
