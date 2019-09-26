Upgrading Opencast from 7.x to 8.x
==================================

This guide describes how to upgrade Opencast 7.x to 8.x. In case you need information about how to upgrade older
versions of Opencast, please refer to [older release notes](https://docs.opencast.org).

How to Upgrade
--------------

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Back-up Opencast files and database (optional)
4. [Upgrade the database](#database-migration)
5. Remove search index data folder
6. Start Opencast
7. Rebuild Admin UI and External API index

You can find the database upgrade script in `docs/upgrade/7_to_8/`.
