Upgrading Opencast from 6.x to 7.x
==================================

This guide describes how to upgrade Opencast 6.x to 7.x. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).

How to Upgrade
--------------

1. Stop your current Opencast instance
2. Replace Opencast 6.x with 7.x
3. Back-up Opencast files and database (optional)
4. [Upgrade the database](#database-migration)
5. [Upgrade the ActiveMQ configuration](#activemq-migration)
6. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly
7. Migrate the scheduled events
8. [Optionally update the player links](#update-player-links)
9. Rebuild the Elastic Search indexes

Database Migration
------------------

As part of performance optimizations, some tables were modified. This requires a database schema update. Also, one table
is no longer needed and can be dropped. As with all database migrations, we strongly recommend to create a database
backup before attempting the upgrade.

You can find the database upgrade script in `docs/upgrade/6_to_7/`. This script is suitable for both, MariaDB and
MySQL.


ActiveMQ Migration
------------------

*So far, this is not required*


Removal of Deprecated Access Control Ruleset
--------------------------------------------

Opencast 7 finally removes the long-since deprecated `security/xacml` flavor for access control lists. This had not been
used since before Opencast 1.2 (we could not track down its exact deprecation date due to its age). Additionally, all
rule-sets which had been modified since had also been automnatically been updated to `security/xacml+series` which
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


Configuration Changes
---------------------

- `etc/org.opencastproject.scheduler.impl.SchedulerServiceImpl.cfg` no longer needs the `transaction_cleanup_offset`
  option.
- `etc/org.opencastproject.scheduler.impl.SchedulerServiceImpl.cfg` has a new option `maintenance` which temporarily
  disables the scheduler if set to `true`.


Scheduler Migration
-------------------

The way the Scheduler stores its data was changed in Opencast 7 to improve performance when checking for conflicts.

The necessary database schema changes are part of the upgrade script in `docs/upgrade/6_to_7/`.

To actually migrate the data, set the `maintenance` configuration option of
`etc/org.opencastproject.scheduler.impl.SchedulerServiceImpl.cfg` to `true` and start opencast. The migration will start
automatically. Wait until the migration is complete. Once complete, the opencast log will contain a line saying
`Finished migrating scheduled events`. Check if there were any errors during the migration. If not, stop opencast and
change `maintenance` back to `false` to put the scheduler back into its normal mode of operation.

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


Rebuild the Elastic Search Indexes
----------------------------------

Due to [MH-13396](https://opencast.jira.com/browse/MH-13396), the configuration of the Elastic Search indexes of both
the Admin UI and the External API have changed.

Therefore, those indexes both need to be rebuilt.
