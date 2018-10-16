Upgrading Opencast 3.x to 3.7
=============================

Opencast 3.7 introduces changes to the LTI authentication system to repair a vulnerability discovered in older 3.x versions.

How to Upgrade
--------------

1. Create a backup of your security configuration xml files
2. Reset the security config files back to their original values.
3. Reconfigure the security settings as if you were installing for the first time


Upgrading Opencast 2.3 To 3.0
=============================

This guide describes how to upgrade Opencast 2.3.x to 3.x. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).


How to Upgrade
--------------

1. Download Opencast 3.0
2. Stop your current Opencast instance
3. Back-up Opencast files and database (optional)
4. [Upgrade the database](#database-migration)
5. Update the third party tools
6. Replace Opencast 2.3 with 3.0
7. Recreate admin ui search index
8. Review the configuration changes and adjust your configuration accordingly


Database Migration
------------------

Opencast 3.0 includes the following database changes:

1. Support for OAI-PMH (MH-12013)
2. Fix for a mis-named role name for the External API (MH-12015)

It should be needless to say that this migration should not take a lot of time and should be safe. Nevertheless, as with
all database migrations, we recommend to make a database backup before attempting the upgrade.

You can find the database upgrade script [in the Opencast repository
](https://github.com/opencast/opencast/tree/r/3.x/docs/upgrade/2.3_to_3.0).

Recreate Admin UI Search Index
------------------------------

MH-11861 needs the admin ui search index to be recreated. This can be achieved by calling POST /admin-ng/index/recreateIndex
using the REST-API Docs.
Note the rebuilding the search index may take hours depending on how much data needs to be reindexed.

Configuration Changes
---------------------

1. The parameter "karaf.shutdown.pid.file" is renamed to "karaf.pid.file" (MH-12188)
2. The optional organization property 'prop.adminui.user.listname' can be used to set the display format of users in
    the group editor (MH-12211)

Requirements Changes
--------------------

Note that Opencast 3.0 requires Java 1.8. In case you used Java 1.7 for Opencast 2.3, you will also need to update Java.
