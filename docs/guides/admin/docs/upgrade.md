# Upgrading Opencast From 2.1 To 2.2

This guide describes how to upgrade Opencast 2.1.x to 2.2.x. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).

## Database Migration

You will find the database migration script in `/docs/upgrade/2.1_to_2.2/<vendor>.sql`

*Notice:* We highly recommend to use MariaDB 10.0+ instead of MySQL 5.5+, as the migration scripts for MariaDB are
much more robust.

Switch to the directory that contains the mysql5.sql or mariadb10.sql file and run the MySQL/MariaDB client with the
database user and switch to the database you want to use (`opencast`):

    mysql -u opencast -p opencast

Run the ddl script:

    mysql> source mariadb10.sql;

or

    mysql> source mysql5.sql;

With the mysql5 script, you might encounter errors in the logs about missing INDEXes and non existing FOREIGN KEYS. You
can probably ignore these errors. If your MySQL database is not working afterwards we can only recommend trying MariaDB.
You should not use the shell import of the SQL script for MySQL, as unlike with the SOURCE command, the processing of
the script in the shell will stop, as soon as an error is encountered.

## System Account

If you are upgrading from Opencast 1.x or 2.0 to 2.2 you should notice that the default username of the digest user
has changed. You should stick to your current username (`matterhorn_system_account`) as otherwise the creating user for
some jobs cannot be found.

## Scheduled Events

It is highly recommended that you don't have scheduled jobs left in your system before the migration, as the migration
of scheduled events sometimes fails.

## Distribution Artifacts Migration

With the introduction of stream security in Opencast 2.2, the distribution URLs have changed to provide a clearer
separation between artefacts of different tenants present in the system. In particular, now the tenant's name will be
part of all its artifacts' URLs. This change makes it easier to protect the distributed artifacts from being accessed by
a tenant they do not belong to. It affects all the Opencast deployments, even if only one tenant (the "default
organization") is defined or even if the Stream Security mechanism is not active.

## Configuration Changes

* In `org.opencastproject.organization-mh_default_org.cfg` the keyboard shortcut identifiers (starting with
  `prop.player.shortcut.*`) have been changed and some new shortcuts for the zoom feature have been added.  If you want
  to re-use the config file from your 2.1 installation, please update this section.

* The configuration key `promiscuous-audio-muxing` of WOH prepare-av has been replaced by the more generic configuration
  key `audio-muxing-source-flavors`. In case your custom workflows contain `promiscuous-audio-muxing=true`, replace that
  key by `audio-muxing-source-flavors=?/*,*/*` which is semantically equivalent.

* In case that you want to use a custom workflow for retracting events, you must tag it `delete-ng`.You need such a
  workflow to be able to delete events that have been published. There is a default workflow for this.

* The names of the following configuration properties found in `org.opencastproject.organization-mh_default_org.cfg`
  have been changed:

    |Old name                                             |New name                                               |
    |-----------------------------------------------------|-------------------------------------------------------|
    |prop.org.opencastproject.admin.documentation.url     | prop.org.opencastproject.admin.help.documentation.url |
    |prop.org.opencastproject.admin.restdocs.url          | prop.org.opencastproject.admin.help.restdocs.url      |

* The Opencast logos defined in `org.opencastproject.organization-mh_default_org.cfg` have been changed.  The old logos
  have been removed from Opencast.

* Several default values (mostly `custom.properties`) have been moved from the configuration files into the code and
  have been commented out in the configuration.

## How to Upgrade

1. Check out/download Opencast 2.2
2. Stop your current Opencast instance
3. Back up Opencast files and database (optional)
4. Run the appropriate database upgrade script
     - `/docs/upgrade/2.1_to_2.2/`
5. Review the configuration changes and adjust your configuration accordingly
6. Update the third party tools as documented in the [installation docs](installation/index.md).
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
