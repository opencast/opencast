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


Database Migration
------------------

As part of performance optimizations, a foreign key constraint was added to one table. This requires a database schema
update. As with all database migrations, we recommend to create a database backup before attempting the upgrade.

You can find the database upgrade script in `docs/upgrade/6_to_7/`. This script is suitable for both, MariaDB and
MySQL.


ActiveMQ Migration
------------------

*So gar, this is not required*


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

- *TODO*
