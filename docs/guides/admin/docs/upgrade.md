Upgrading Opencast from 8.x to 9.x
==================================

This guide describes how to upgrade Opencast 8.x to 9.x. In case you need information about how to upgrade older
versions of Opencast, please refer to [older release notes](https://docs.opencast.org).

Configuration Changes
---------------------

1. The dispatch interval property is now called `dispatch.interval` and expects seconds instead of milliseconds
   `etc/org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.cfg`.

How to Upgrade
--------------

1. Stop your current Opencast instance
2. Replace Opencast with the new version
3. Back-up Opencast files and database (optional)
4. Upgrade the database
5. Remove search index data folder
6. Start Opencast
7. Rebuild Admin UI and External API index
