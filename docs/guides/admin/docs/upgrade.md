
Upgrading Opencast from 6.x to 7.x
==================================

This guide describes how to upgrade Opencast 6.x to 7.x. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).

How to Upgrade
--------------

1. Stop your current Opencast instance
2. Replace Opencast 6.x with 7.x
3. Optionally back-up Opencast files and database
4. [Upgrade the database](#database-migration)
5. Optionally [check for old ACL rule sets](#removal-of-deprecated-access-control-rule-set)
6. [Migrate the scheduled events](#scheduler-migration)
7. Optionally [update the player links](#update-player-links)
8. [Rebuild the Elasticsearch indexes](#rebuild-the-elasticsearch-indexes)
9. Review the configuration changes and adjust your configuration accordingly

Database Migration
------------------

As part of performance optimizations, some tables were modified. This requires a database schema update. Also, one table
is no longer needed and can be dropped. As with all database migrations, we strongly recommend to create a database
backup before attempting the upgrade.

You can find the database upgrade script in `docs/upgrade/6_to_7/`. This script is suitable for both, MariaDB and
MySQL.


Removal of Deprecated Access Control Rules Set
--------------------------------------------

Opencast 7 finally removes the long-since deprecated `security/xacml` flavor for access control lists. This had not been
used since before Opencast 1.2 (we could not track down its exact deprecation date due to its age). Additionally, all
rule-sets which had been modified since had also been automatically been updated to `security/xacml+series` which
serves as replacement for the old flavor.

In case Opencast still encounters such a rule set, it will now be ignored and access will be denied by default. A simple
update of the permissions would fix this if that is required.

Due to the extreme unlikeliness of anyone encountering this problem, there is no automatic migration. In case you run a
system migrated from a pre-1.2 Matterhorn, you can make sure that there are no old rule-sets left using the following
SQL queries:

```sql
-- Check OAI-PMH publications:
select * from oc_oaipmh_elements where flavor = 'security/xacml';
-- Check engage publications:
select * from oc_search where mediapackage_xml like '%"security/xacml"%';
-- Check asset manager:
select * from oc_assets_snapshot where mediapackage_xml like '%"security/xacml"%';
```


Scheduler Migration
-------------------

The way the scheduler stores its data was changed in Opencast 7 to improve performance when checking for conflicts.
The necessary database schema changes are part of the upgrade script in `docs/upgrade/6_to_7/`.

Additionally, old events need to be migrated to the new structure to preserve metadata related to scheduling. For this,
set the `maintenance` configuration option of `etc/org.opencastproject.scheduler.impl.SchedulerServiceImpl.cfg` to
`true` and start opencast.  The migration will start automatically. Wait until the migration is complete. Once complete,
the opencast log will contain a line saying `Finished migrating scheduled events`. Check if there were any errors during
the migration. If not, stop opencast and change `maintenance` back to `false` to put the scheduler back into its normal
mode of operation.

You should avoid running Opencast 7 without migrating the scheduled events first. Otherwise, your capture agents may
fetch an empty calendar.


Update Player Links
-------------------

> This step is optional

Opencast 7 comes with the capability of dynamically switching the configured player without requiring the republication
of all published material by providing the dynamic target `https://example.opencast.org/play/<id>`. This new target
will be used for all new engage publications.

But old publications still reference the players directly in the admin interface and the external API. This does not
pose any immediate problem unless you actually want to switch players in which case you would need to republish the
material once to update the publication links.

Alternatively, you can rewrite the old links all at once without re-publication using the following method:

1. Find the archive directory and run the following command (replacing `<playerlink>`):

        sed -i 's_<playerlink>_/play/_g' .../archive/*/*/*/manifest.xml

    For Theodul, Paella and the Engage player the specific commands would be:

        sed -i 's_/engage/theodul/ui/core.html?id=_/play/_g' .../archive/*/*/*/manifest.xml
        sed -i 's_/paella/ui/watch.html?id=_/play/_g' .../archive/*/*/*/manifest.xml
        sed -i 's_/engage/ui/watch.html?id=_/play/_g' .../archive/*/*/*/manifest.xml

2. Run the following SQL commands on your opencast database (replacing `<playerlink>`):

        UPDATE oc_assets_snapshot
          SET mediapackage_xml =
          REPLACE(mediapackage_xml, '<playerlink>', '/play/')
          WHERE INSTR(mediapackage_xml, '<playerlink>') > 0;

    For Theodul, Paella and the Engage player the specific commands would be:

        UPDATE oc_assets_snapshot
          SET mediapackage_xml =
          REPLACE(mediapackage_xml, '/engage/theodul/ui/core.html?id=', '/play/')
          WHERE INSTR(mediapackage_xml, '/engage/theodul/ui/core.html?id=') > 0;
        UPDATE oc_assets_snapshot
          SET mediapackage_xml =
          REPLACE(mediapackage_xml, '/paella/ui/watch.html?id=', '/play/')
          WHERE INSTR(mediapackage_xml, '/paella/ui/watch.html?id=') > 0;
        UPDATE oc_assets_snapshot
          SET mediapackage_xml =
          REPLACE(mediapackage_xml, '/engage/ui/watch.html?id=', '/play/')
          WHERE INSTR(mediapackage_xml, '/engage/ui/watch.html?id=') > 0;

Please ensure to execute these steps before rebuilding the index.


Rebuild the Elasticsearch Indexes
----------------------------------

Due to [MH-13396](https://opencast.jira.com/browse/MH-13396), the configuration of the Elasticsearch indexes of both
the admin ui and the external API have changed. Therefore, those indexes both need to be rebuilt.

### Admin Interface

Stop Opencast, delete the index directory at `data/index`, restart Opencast and make an HTTP POST request to
`/admin-ng/index/recreateIndex`.

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
