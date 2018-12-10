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


Rebuild ElasticSearch Index
---------------------------

The update requires an ElasticSearch index rebuild. For that, stop Opencast, delete the index directory at `data/index`,
start Opencast and use one of the following methods to recreate the index:

- Make an HTTP POST request to `/admin-ng/index/recreateIndex` using your browser or an alternative HTTP client.
- Open the REST documentation, which can be found under the “Help” section in the Admin UI (by clicking on the “?”
  symbol at the top right corner). Then go to the “Admin UI - Index Endpoint” section and use the testing form on
  `/recreateIndex`.

In both cases, the resulting page is empty but should return a HTTP status 200.

If you are going to use the External API, then the corresponding ElasticSearch index must also be recreated:

- Make an HTTP POST request to `/api/recreateIndex` using your browser or an alternative HTTP client.
- Open the REST documentation, which can be found under the “Help” section in the Admin UI (by clicking on the “?”
  symbol at the top right corner). Then go to the “External API - Base Endpoint” section and use the testing form on
  `/recreateIndex`.

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
