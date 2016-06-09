# Upgrading Opencast to 2.2

## Database Migration
You will find the database migration script in /docs/upgrade/2.1_to_2.2/<vendor>.sql

## Distribution artifacts migration
With the introduction of the stream security, the distribution URLs he distribution URLs in Opencast 2.2 have changed
to provide a clearer separation between the artifacts belonging to the different tenants present in the system. 
In particular, now the tenant's name will be part of all its artifacts' URLs. This change makes it easier to protect 
the distributed artifacts from being accessed by a tenant they do not belong to. It affects all the Opencast 
deployments, even if only one tenant (the "default organization") is defined or even if the Stream Security 
mechanism is not active.

## Configuration changes

* In `org.opencastproject.organization-mh_default_org.cfg` the keyboard shortcut identifiers (starting with 
prop.player.shortcut.*) have been changed and some new shortcuts for the zoom feature have been added. 
If you want to re-use this config file from your 2.1 installation, please update this section.

## How to Upgrade

1. Check out/download Opencast 2.2
2. Stop your current Opencast instance
3. Back up Opencast files and database (optional)
4. Run the appropriate database upgrade script
     - `/docs/upgrade/2.1_to_2.2/`
5. Review the configuration changes and adjust your configuration accordingly
6. Update the third party tools as documented in the [installation docs](../installation/index.md).
7. Build Opencast 2.1
8. Delete the `adminui` directory in your Opencast data directory.
9. Start Opencast using the interactive script in `bin/start-opencast`
10. Log-in to the Karaf console on your node where the search service is running (usually presentation node) 
    and install the opencast-migration feature by entering: `feature:install opencast-migration`

    > *Make sure that your current engage search index contains all episodes. Retracted recordings will not be 
    migrated.*

11. Check the logs for errors!
12. Restart Opencast service - you do not need to use the interactive start script.
13. Reconstruct the Admin UI search index. There are two ways to reconstruct the index:

       * By opening `http://my.opencast-server.tld:8080/admin-ng/index/recreateIndex` in your browser.
         The resulting page is empty but should return an HTTP status 200 (OK).
       * By using the REST documentation, open "Admin UI - Index Endpoint" and use the testing form on `/recreateIndex`.
         The resulting page is empty but should return an HTTP status 200 (OK).
         You can find the REST documentation in the help-section of the Admin UI behind the *?*-symbol.

