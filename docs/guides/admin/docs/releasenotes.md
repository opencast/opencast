Opencast 7: Release Notes
=========================

New Features
------------

- Overhaul of the permission management with the newly added possibility to define how access control lists are
  evaluated and how series permission changes are populated to episodes. For more details take a look at the [access
  control configuration guide](configuration/acl.md).
- Update Elasticsearch and make is possible to run Elasticsearch as an external service.
- Per-Tenant Capture Agent Users
- Asset manager snaphots clean-up to remove older snapshots for a given media package. In some cases, this can
  drastically reduce Opencast's storage consumption. This feature is implemented as an option for the [asset-delete workflow
  operation handler](workflowoperationhandlers/asset-delete-woh.md).
- Allow the workflow to select the audio track for composite video
- Add multi-tenant support for all list providers
- Make waveform size configurable
- Create a generic user interface configuration service
- Add link to series details to the events table
- Add Internationalization support for series LTI tools
- Display responsible person for workflows
- Allow the Ingest Service to make authenticated requests to other servers
- Some modules are now plugins. These are not started by default to reduce the amount of code running unnecessarily.
  They can easily be enabled in `etc/org.apache.karaf.features.cfg`. Modified modules are:
    - Moodle user directory
    - Sakai user directory

Improvements
------------

A non-comprehensive list of improvements:

- Improvement of performance when scheduling new events or checking for conflicts, reducing the time for adding
multiple schedules by up to 90% compared to the previous implementation. Please read the [upgrade guide](upgrade.md)
to make sure you migrate your data properly.

Configuration changes
---------------------

- `KARAF_NOROOT` is now set to `true` by default, preventing Opencast to be started as root user unless the
  configuration is changed.
- The default configuration for the Paella player has been moved to `etc/ui-config/mh_default_org/paella/config.json`
- By default, metadata catalogs and attachments sent by capture agents are discarded since this data is usually
  controlled by Opencast and the routing through capture agents which existed for historical reasons was just an
  additional source for errors. If you rely on the old behavior, it can be configured in
  `etc/org.opencastproject.ingest.impl.IngestServiceImpl.cfg`.

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

Additional Notes about 6.4
--------------------------

Opencast 6.4 contains a number of bug fixes, some of which are security relevant. The following known vulnerability
within Opencast's `org.springframework.security.oauth:spring-security-oauth` dependency have been fixed by this release:
`CVE-2019-3778`.


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
