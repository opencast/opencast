Opencast 7: Release Notes
=========================

New Features
------------

- Overhaul of the permission management with the newly added possibility to define how access control lists are
  evaluated and how series permission changes are populated to episodes. For more details take a look at the [access
  control configuration guide](configuration/acl.md).
- Update Elasticsearch and make it possible to run Elasticsearch as an external service.
- Per-Tenant Capture Agent Users
- Asset manager snaphots clean-up to remove older snapshots for a given media package. In some cases, this can
  drastically reduce Opencast's storage consumption. This feature is implemented as an option for the
  [asset-delete workflow operation handler](workflowoperationhandlers/asset-delete-woh.md).
- Allow the workflow to select the audio track for composite video
- Add multi-tenant support for all list providers
- Make waveform size configurable
- Create a generic user interface configuration service
- In the events table add a link to the series details
- Add internationalization support for series LTI tools
- Display creator of workflow
- Allow the ingest service to make authenticated requests to other servers
- Some modules are now plugins. These are not started by default to reduce the amount of code running unnecessarily.
  They can easily be enabled in `etc/org.apache.karaf.features.cfg`. Modified modules are:
    - Moodle user directory
    - Sakai user directory

Improvements
------------

- Improved performance when scheduling new events or checking for conflicts (reducing the time for adding
multiple schedules by up to 90%).

Configuration changes
---------------------

- `etc/org.opencastproject.scheduler.impl.SchedulerServiceImpl.cfg` has a new option `maintenance` which temporarily
  disables the scheduler if set to `true`.
- `KARAF_NOROOT` is now set to `true` by default, preventing Opencast to be started as root user unless the
  configuration is changed.
- The default configuration for the Paella player has been moved to `etc/ui-config/mh_default_org/paella/config.json`
- By default, metadata catalogs and attachments sent by capture agents are discarded since this data is usually
  controlled by Opencast and the routing through capture agents which existed for historical reasons was just an
  additional source for errors. If you rely on the old behavior, it can be configured in
  `etc/org.opencastproject.ingest.impl.IngestServiceImpl.cfg`.
- By default, the Paella player now respects all tracks published to engage instead of having a hard-coded filter for
  tracks with the sub-flavor `delivery` only.
- The structure of the configuration files concerning URL signing has changed. See the
[stream security configuration](./configuration/stream-security.md) for more details.

API changes
-----------

Due to [MH-13397](https://opencast.jira.com/browse/MH-13397):

- Modified GET /recordings/{id}/technical.json: Removed field `optOut`
- Removed GET /recordings/{id}/optOut
- Removed GET /recordings/{id}/reviewStatus
- Removed PUT /recordings/{id}/reviewStatus
- Modified POST /recordings: Removed form parameter `optOut`
- Modified POST /recordings/multiple: Removed form parameter `optOut`
- Modified PUT /recordings/{id}: Removed form parameters `optOut` and `updateOptOut`
- Removed GET /series/{id}/optOut

Due to [MH-13446](https://opencast.jira.com/browse/MH-13446):

- Removed GET /acl-manager/transitions.json
- Removed GET /acl-manager/transitionsfor.json
- Removed POST /acl-manager/episode/{id}
- Removed POST /acl-manager/series/{id}
- Modified POST /acl-manager/apply/episode/{id}: Removed form parameters workflowDefinitionId and workflowParams
- Modified POST /acl-manager/apply/series/{id}: Removed form parameters workflowDefinitionId and workflowParams
- Removed DELETE /acl-manager/episode/{transitionId}
- Removed DELETE /acl-manager/series/{transitionId}
- Removed PUT /acl-manager/episode/{transitionId}
- Removed PUT /acl-manager/series/{transitionId}

Additional Notes About 7.1
--------------------------

Opencast 7.1 is the first maintenance release for Opencast 7. It fixes a bug with the scheduler migration which may have
caused minor issues for old, process events which were missing some meta-data. If you have already migrated to Opencast
7.0 and experience this problem, simply re-start the scheduler migration and re-build the index once more.


Additional Notes About 7.2
--------------------------

Opencast 7.2 fixes a bug in the video editor configuration present in Opencast 7.0 to 7.1 which will cause Opencast to
always silently skip the video editor and publish the whole video. The problem was introduced by [a fix in the default
workflows](https://github.com/opencast/opencast/pull/944) and later fixed again by a [configuration change therein
](https://github.com/opencast/opencast/pull/1013). If you use the default workflows, please make sure to update to the
latest state of the workflows.

If you use your own workflow and did not adapt the first patch, you should not be affected by this problem at all. If
you are, just make sure that source and target smil flavor for the editor workflow operation are identical like it is
ensured [by the official fix](https://github.com/opencast/opencast/pull/1013). A proper solution not relying on specific
configurations and less error prone is in work and will be added to the upcoming major Opencast release.


Release Schedule
----------------

|Date                         |Phase
|-----------------------------|------------------------------------------
|April 1st 2019               |Feature Freeze
|May 6th - May 12th 2019      |Translation week
|May 13th - May 26th 2019     |Public QA phase
|June 13th 2019               |Release of Opencast 7.0

Release Managers
----------------

- Maximiliano Lira Del Canto (University of Cologne)
- Katrin Ihler (ELAN e.V.)
