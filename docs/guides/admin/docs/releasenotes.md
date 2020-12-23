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
- **Opencast 8.2: Opencast Studio (browser-based recording tool)**

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

Additional Notes about 8.10
---------------------------

This release contains an important bugfix where unprivileged users could not upload videos.


Additional Notes about 8.9
--------------------------

Opencast 8.9 contains an important security fix regarding host verification. Upgrading is strongly recommended. If you
use self-signed certificates, you now need to properly import them.
Additionally, the UI of the docs were improved and and the Spring snapshot repository was removed to resolve build
problems.
5 other patches have been merged.

Additional Notes About 8.8
--------------------------

This maintenance includes 12 patches.

Additional Notes About 8.7
--------------------------

This maintenance release fixes severals bugs including problems with the scheduler in multitenant systems and adds
missing ACLs after asset uploads.

Additional Notes About 8.6
--------------------------

This maintenance release contains some bugfixes.

Additional Notes About 8.5
--------------------------

This maintenance release contains mainly fixes for LTI issues.

Additional Notes About 8.4
--------------------------

This maintenance release contains several Opencast Studio bug fixes and enhancements, plus additional security filters.

### Configuration Changes in 8.4

- Updated studio workflows
- Additional security configuration in `etc/security/mh_default_org.xml` include
    - 403 Logout redirect
    - admin access to "/"
    - role access to system filters
    - prevent normal user from deleting series
    - additional studio access filters
- Muxing fix in encoding files

    - `etc/encoding/adaptive-streaming-movies.properties`
    - `etc/encoding/opencast-movies.properties`
- Increased preview image resolution in
    - `etc/encoding/engage-images.properties`
- A new editor preview profile in
    - `etc/encoding/opencast-movies.properties`
- Default maximum job attempt increases 1 to 10 in
    - `etc/org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.cfg`


Additional Notes About 8.3
--------------------------

To improve the integration of Opencast Studio we introduced the custom roles plugin for LTI.

There are also several changes in Opencast Studio:

- Added
    - Add detection of cameras. If none are detected, the buttons for "Camera" and "Screen & Camera"
    are grayed out and not clickable.
    - Add configuration options for the ACL sent by Studio on upload (the defaults haven't changed).
    - Add configurations to limit the maximum video size and frame rate (the default haven't changed).
    - Add configurations for video bitrate and preferred MIME types
    - Add option to pass full configuration as hex encoded GET parameter.
    - Show "Your recording will now be processed by Opencast." after upload.
    - Full German translation
    - Add build-time configuration of the settings.json path

- Changed
    - Remove "beta" status of Studio
    - Update dependencies
    - Settings are now validated against a fixed schema
    - An invalid Content-Type for the ACL XML request does not lead to the ACL template being ignored.
    Now it's used anyway, but a warning is still printed.
    - Source map files are now included in the released tarballs.

- Fixed
    - Fix bug in parsing GET parameters that would lead to only the last parameter being used
    - Errors during upload are now shown to the user (previously, it would just show "Opencast connection not
    configured").
    - Fixed very short recordings on weak devices leading to an error
    - Fix MIME type to file extension detection
    - Fix Safari detection
    - GET parameters are now retained all the time
    - text/xml is now also accepted as valid Content-Type for the ACL XML request (previously only application/xml
    was accepted)


### Configuration Changes in 8.3

- The configuration file `etc/org.opencastproject.kernel.security.LtiLaunchAuthenticationHandler.cfg` was moved
   to `etc/org.opencastproject.security.lti.LtiLaunchAuthenticationHandler.cfg`.
- The configuration file `etc/org.ops4j.pax.web.cfg` was updated to include the `org.ops4j.pax.web.session.timeout`
   option with value `240`.
- New workflows and encodings for Opencast Studio
- New configuration file `etc/ui-config/mh_default_org/studio/settings.json`



Additional Notes About 8.2
--------------------------

Unlike in other maintainance releases, the release managers decided to break the rule to include features only
in major releases. So Opencast 8.2 includes **Opencast Studio** a browser-based recording tool. We felt that
because of the Corona Crisis this tool would be to many Opencast users as soon as possible.

Additionally this release also includes 17 other patches.

### Configuration Changes in 8.2

- `/etc/encoding/engage-images.properties` has a few optimizations that are needed especially for Studio
- `etc/security/mh_default_org.xml` has some changes for Studio and a new ROLE_STUDIO is introduced
- `etc/workflows/publish-uploaded-assets.xml` has some changes.

Aditional Notes About 8.1
-------------------------

Opencast 8.1 fixes a number of security issues. Upgrading is strongly recommended.
Take a look at the [security advisories](https://github.com/opencast/opencast/security/advisories) for more details.

One change is that the OAI-PMH endpoint is no longer publicly accessible by default.
If you need it to be, you can easily change that in the security configuration at `etc/security/mh_default_org.xml`.

### Configuration Changes in 8.1

- `etc/security/mh_default_org.xml`
    - `/oaipmh` URL is now restricted to `ROLE_ADMIN`
    - Removed `ROLE_COURSE_ADMIN` that was used for `/` and `/admin-ng` URLs
    - replaced `<sec:remember-me key="opencast" user-service-ref="userDetailsService" />`
with `<sec:remember-me services-ref="rememberMeServices" />`
    - Added configuration for rememberMeServices
    - Added CustomPasswordEncoder configuration instead of MD5
- changed `exception-handler-workflow="error"` to `exception-handler-workflow="partial-error"` in several workflow definitions

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
