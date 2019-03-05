Opencast 7: Release Notes
=========================

New Features
------------

- Overhaul of the permission management with the newly added possibility to define how access control lists are
  evaluated and how series permission changes are populated to episodes. For more details take a look at the [access
  control configuration guide](configuration/acl.md).
- Canvas user and role provider
- Update Elasticsearch and make is possible to run Elasticsearch as an external service.
- Per-Tenant Capture Agent Users
- Asset manager snaphots clean-up to remove older snapshots for a given media package. In some cases, this can
  drasticaly reduce Opencast's storage consumption. This feature is implemented as option to the [asset-delete workflow
  operation handler](workflowoperationhandlers/asset-delete-woh.md).
- Allows the workflow to select the audio track for composite video
- Improve scheduler performance, reducing the time for adding multiple schedules by up to 90% compared to the previous
  implementation.
- Add multi-tenant support for all list providers
- Make waveform size configurable
- Create a generic user interface configuration service
- Add link to series details, out of the eventstable-view
- Internationalization support for series LTI tools
- Display responsible person for workflows
- Allow the Ingest Service to make authenticated requests to other servers
- Crop service

Improvements
------------

A non-comprehensive list of improvements:

- Improvement of performance when scheduling new events or checking for conflicts. Please read the [upgrade guide] to
  make sure you migrate your data properly.

Configuration changes
---------------------

- …


Additional Notes about 6.1
--------------------------

Opencast 6.1 contains a number of bug fixes, some of which are security relevant. The following known vulnerabilities
within Opencast's `com.fasterxml.jackson.core:jackson-databind` dependency have been fixed by this release:
`CVE-2018-19361`, `CVE-2018-19362`, `CVE-2018-19360`.


Additional Notes about 6.2
--------------------------

Opencast 6.2 contains a number of bug fixes for the Opencast 6.x line of releases.


Additional Notes about 6.3
--------------------------

Opencast 6.3 contains a number of performance improvements and additional minor bug fixes for the Opencast 6.x line of
releases.


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
