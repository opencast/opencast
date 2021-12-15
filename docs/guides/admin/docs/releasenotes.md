# Opencast 11: Release Notes


Features
--------

- New ElasticSearch architecture, now there is one elastic search for the admin ui and the external api. This  
  simplifies the maintenance and double the speed of index rebuild.
- New *select-version* workflow operation handler, used to replace the media package in the currently
  running workflow with an older version from the asset manager.
- Additional S3 distribution workflow operation handlers: *retract-partial-aws*, *publish-configure-aws*,
  *retract-configure-aws*
- Wowza stream security configuration now allows the configuration "prefix:secret"
- Allows upload of tracks (e.g. subtitles) as assets for new and existing
  events.

Improvements
------------

- Save buttons for the metadata of existing series and events in the Admin UI.
- Moved the configuration of the user interface configuration service (providing the /ui-config endpoint)
  from the global configuration to a service configuration file.
- Extracting preview images are now from the source material instead, instead of processed files.
- The groups are always updated regarded their user reference provider.
- `execute-many` and `execute-once` now print all the output in the logs
- Add support for `WebP` and `Advanced SubStation Alpha` mime types

Behavior changes
-----------------

- There is now only one Elasticsearch index for both the Admin UI and the External API. Its structure is identical
  to the old Admin UI index, thus migration is possible, alternatively the index has to be rebuilt or the old Admin UI
  index can be configured (see upgrade guide).
  With this change the index rebuild should now be twice as fast as before. The index endpoints to clear and rebuild
  the index were moved to /index.
- There is a completely new set of workflows. Please make sure to check your local configuration and adapt
  it accordingly if you made changes to your workflows before. Opencast will also continue to work with the old set of
  workflows. The new ones just remove a lot of redundancies, making the whole process more efficient.

  Some of the new workflows (e.g. `fast`) now use slightly different workflow configurations. This could potentially
  cause problems if you scheduled recordings using the old workflows but have the events processed using the new 
  workflows. Please make sure the workflow you use work fine, or do not have anything scheduled via the upgrade.
- Changes to the service registry config at `ServiceRegistryJpaImpl.cfg`:
    - The usage of `max.attempts` is modified in the sense that if you set -1, you can disable services going into error
      state completely. Before, this was equivalent to 0, which would have the service go into error state after one
      attempt, though this was undocumented. Check your configuration to be sure you didn't rely on this behavior.

    - `no.error.state.service.types` was added. With this, you can define service types that should never go into error
      state.
- The default location of the user interface configuration service configuration is now
  `etc/org.opencastproject.uiconfig.UIConfigRest.cfg`. For more details, take a look at
  [pull request #2860](https://github.com/opencast/opencast/pull/2860).
- There are changes to how hosts are mapped to tenants. If you use a multi-tenant system you therefore need to update
  your `org.opencastproject.organization-*.cfg` configuration files:

  Before Opencast 11 the domain names were mapped to tenants and a common port number was assumed for all domains. Now
  you need to configure a URL per instance you want to map to a tenant.

  Before:
  ```
  port=8080
  prop.org.opencastproject.host.admin.example.org=tenant1-admin.example.org
  prop.org.opencastproject.host.presentation.example.org=tenant1-presentation.example.org
  ```

  Now:
  ```
  prop.org.opencastproject.host.admin.example.org=https://tenant1-admin.example.org
  prop.org.opencastproject.host.presentation.example.org=https://tenant1-presentation.example.org:8443
  ```
- Support for automatically setting up an HLS encoding ladder via the `{video,audio}.bitrates.mink`
  and `{video,audio}.bitrates.maxk` encoding profile options was removed. Instead, users should now explicitly specify
  the bit rate and bit rate control mechanism in the `ffmpeg.command`.
- Some S3 distribution workflow operation handlers have been renamed: *publish-aws* to *publish-engage-aws* and
  *retract-aws* to *retract-engage-aws*
- If an URL does not exist, it returns to the welcome page.
- The amount of job statistics for servers displayed in the admin interface was reduced to running and queued jobs to
  avoid performance problems and remove incorrect and/or misleading data.


API changes
-----------
- [[#2814](https://github.com/opencast/opencast/pull/2814)] - Add track fields `is_master_playlist` and `is_live` to
  external API
- [[#2878](https://github.com/opencast/opencast/pull/2878)] - Add endpoint to resume Index Rebuild for specified service
- [[#3002](https://github.com/opencast/opencast/pull/3002)] - Sign publication URL of events in External API
- [[#3148](https://github.com/opencast/opencast/pull/3148)] - Allow empty track duration


Release Schedule
----------------

| Date                        | Phase                    |
|-----------------------------|--------------------------|
| November 17, 2021           | Feature freeze           |
| November 22, 2021           | Translation week         |
| November 29, 2021           | Public QA phase          |
| December 15, 2021           | Release of Opencast 11.0 |


Release managers
----------------

- Maximiliano Lira Del Canto (University of Cologne)
- Jonathan Neugebauer (University of Münster)
