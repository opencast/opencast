Upgrading Opencast
==================

Upgrading Opencast from 4.x to 5.0
----------------------------------

This guide describes how to upgrade Opencast 4.x to 5.0. In case you need information about how to upgrade older
versions of Opencast, please refer to the [old release notes](https://docs.opencast.org).

### How to Upgrade

1. Stop your current Opencast instance
2. Replace Opencast 4.x with 5.0
3. Back-up Opencast files and database (optional)
4. [Upgrade the database](#database-migration)
5. [Upgrade the ActiveMQ configuration](#activemq-migration)
6. Review the [configuration changes](#configuration-changes) and adjust your configuration accordingly


### Database Migration

As part of removing Matterhorn mentions, all database table names have been changed and are now prefixed with `oc_`.
This requires an database update. As with all database migrations, we recommend to make a database backup before
attempting the upgrade.

You can find the database upgrade script at `docs/upgrade/4_to_5/mysql5.sql`.

### ActiveMQ Migration

Opencast 5.0 needs a new ActiveMQ message broker configuration. Please follow the steps of the [message broker
configuration guide](configuration/message-broker/) to deploy a new configuration. No data migration is required for
this since the message broker only contains temporary data.

### Configuration Changes

HTTP Basic authentication is enabled by default (see `etc/security/mh_default_org.xml`). Make sure you've enabled
HTTPS support in Opencast or your preferred HTTP proxy (see [documentation](configuration/security.https.md)).

Paella Player has been included in Opencast 5.0. So you can choose between the Theodul and Paella player.
This can be done by setting the `prop.player` property in `etc/org.opencastproject.organization-mh_default_org.cfg`.
Additional the path to the Paella player configuration folder is added in `etc/custom.properties`
as `org.opencastproject.engage.paella.config.folder` with the default value of `${karaf.etc}/paella`,
where you will find the default configuration. The Paella URL pattern was also added to the security configuration
`etc/security/mh_default_org.xml`.

Workflow definition IDs has been changed and does not prefixed with `ng-` any more. If you are using the default
workflows or include them as part of your custom workflow, please adapt the changes. You can find the workflow
definitions in `etc/workflows/*.xml`. You may also adapt the workflow ID changes in the following configuration files:

Configuration file name | Property name
------------------------|-------------------
custom.properties                                 | org.opencastproject.workflow.default.definition
org.opencastproject.ingest.scanner.InboxScannerService-inbox.cfg              | workflow.definition
org.opencastproject.transcription.ibmwatson.IBMWatsonTranscriptionService.cfg | workflow

The new workflow control functionality includes some REST endpoints that needs to be secured.
Therefore the new URL patterns has been added to the `etc/security/mh_default_org.xml`.

As Piwik has been renamed to Matomo Opencast changed the name for the plugin and configuration keys too.
So if you already configured an Piwik Server please adapt in `etc/org.opencastproject.organization-mh_default_org.cfg`
the following keys:

* `prop.player.piwik.server` -> `prop.player.matomo.server`
* `prop.player.piwik.site_id` -> `prop.player.matomo.site_id`
* `prop.player.piwik.heartbeat` -> `prop.player.matomo.heartbeat`
* `prop.player.piwik.track_events` -> `prop.player.matomo.track_events`

The publication channels listprovider configurations `etc/listproviders/publication.channel.labels.properties` and
`etc/listproviders/publication.channel.icons.properties` has been merged in
`etc/listproviders/publication.channels.properties`.

Several service job load values were updated. You can find these configuration files by running the command
`grep job.load etc/org.opencastproject.*` on the command line or search for `job.load` string in
service configuration files.
