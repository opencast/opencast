Opencast 8: Release Notes
=========================

Features
--------

- Improved integration of external statistics providers (InfluxDB)
    - Hourly scale for data exports
    - Visualization of statistics in the Admin UI
- Limit accepted file types when uploading assets
- Support for exclusion pattern for URL signing
- Add option to configure state mappings for workflows
- Assembly configuration
- Multi-tenancy support for workflows
- Role support for workflows
- Video-crop service
- Paella Player 6.2.4
- Multiple audio tracks support on Paella Player
- Feeds-tab: adds a new tab in series properties for ATOM and RSS Feeds
- Custom LTI series tool styles
- Show only events with write access in the Admin UI
- Access org properties from publish-configure WOH
- Resolution based, conditional encoding
- Termination State Service to integrate with AWS AutoScaling Lifecycle
- Health check endpoint
- Officially support URL signing keys that handle multiple URL prefixes
- Support for exclusion pattern for URL signing
- User-provider for the d2l Brightspace LMS
- Provide access to file contents in the Working File Repository
- Automatic caption using Google speech to text API
- Admin UI: new event media upload progress bar
- Opencast Plug-in features for Karaf
- Single step delete of events


Improvements
------------

- Resume on past table page when leaving video editor
- JavaScript dependency management
- Highlight main table rows on hover
- Reduces job payload size in database
- Improved URL signing performance
- ingest-download operation moved to worker
- Media Module configuration now in the organization configuration file
- Sensible names for hosts instead of URLs
- Improved icons and wording in video editor
- Improved delete-event submit button
- Extended the ingest-download-woh
- Tag elements retrieved from asset manager
- Improve navigation in video editor when zoom is active
- Switch to compatible file type filter definitions
- Improved setting values from Dublin Core catalog
- Don't start Opencast on a used port
- ESLint used in Theodul Core
- Theodul Player scroll/zoom overlay to use shift + scroll wheel zoom
- Removed State Mapping "Importing"
- Hide column "Stop" by default
- Fixed Workflow Index rebuild ACL handling
- Reduced memory needed for Workflow Index rebuild
- Ansible script documentation
- Automatic publication of streaming URLs
- S3 compatibility - Endpoint configuration for Amazon S3 alternatives added  
- Theodul player ui config
- Re-introduce ability to avoid data loss during ingest

Configuration changes
---------------------
- `etc/org.opencastproject.adminui.cfg` has a new option `retract.workflow.id` which contains the id of the workflow used
  to retract events when deleting.


API changes
-----------

- Removed REST endpoints for modifying workflow definitions
    - DELETE /workflow/definition/{id}
    - PUT /workflow/definition

Aditional Notes About 8.1
-------------------------

Opencast 8.1 fixes a number of security issues. Upgrading is strongly recommended.
Take a look at the [security advisories](https://github.com/opencast/opencast/security/advisories) for more details.

One change is that the OAI-PMH endpoint is no longer publicly accessible by default.
If you need it to be, you can easily change that in the security configuration at `etc/security/mh_default_org.xml`.

### Fixed Security Issues

- CVE-2020-5231 – [Users with ROLE\_COURSE\_ADMIN can create new users
  ](https://github.com/opencast/opencast/security/advisories/GHSA-94qw-r73x-j7hg)
- CVE-2020-5206 – [Authentication Bypass For Endpoints With Anonymous Access
  ](https://github.com/opencast/opencast/security/advisories/GHSA-vmm6-w4cf-7f3x)
- CVE-2020-5222 – [Hard-Coded Key Used For Remember-me Token
  ](https://github.com/opencast/opencast/security/advisories/GHSA-mh8g-hprg-8363)
- CVE-2020-5230 – [Unsafe Identifiers
  ](https://github.com/opencast/opencast/security/advisories/GHSA-w29m-fjp4-qhmq)
- CVE-2020-5229 – [Replace MD5 with bcrypt for password hashing
  ](https://github.com/opencast/opencast/security/advisories/GHSA-h362-m8f2-5x7c)
- CVE-2020-5228 – [Public Access Via OAI-PMH
  ](https://github.com/opencast/opencast/security/advisories/GHSA-6f54-3qr9-pjgj)


Release Schedule
----------------

|Date                         |Phase
|-----------------------------|------------------------------------------
|October 1st 2019             |Feature Freeze
|Nov 4th - Nov 10th 2019      |Translation week
|Nov 11th - Nov 24th 2019     |Public QA phase
|December 17th 2019           |Release of Opencast 8.0


Release managers
----------------

- Karen Dolan (Harvard University DCE)
- Rüdiger Rolf (Osnabrück University)
