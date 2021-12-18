Opencast 9: Release Notes
=========================

Important Changes
-----------------

- Opencast now requires an external Elasticsearch server to function
- Paella is now the default video player
- The `encode` worfklow operation now completely replaces `compose`,
  which is dropped now
- MariaDB JDBC driver
- New LTI-Tools
- A [completely new system for securing static file content](configuration/serving-static-files.md) has been integrated
  into Opencast 9.2. While is is not active by default for compatibility to older 9.x releases, it can be easily enabled
  and used to ensure access to static files has the same security checks you are already used to from the player and
  from other user interfaces.


Features
--------

- Get notified when there is a new version of Opencast,
  and if your current version reached its end of life
- Sort events in the main event table
  by the number of publications they have
- Publish streaming elements using `publish-configure`
- Generate embed codes for videos right from the admin UI
- Extend support for resolution based encoding profiles
  through new conditional commands
- Allow filtering series and events by write access
  in the external API
- Support NUT container format
- Support playing 360 degree videos in Paella
- Optionally persist LTI users for the purpose of
  longer running processes like uploads using Opencast Studio
- Register as an adopter to potentially help us gather data
  and to receive news about the project
- Configure Opencast to trim the start and end automatically
  to help with issues of synchronizing audio and video streams
- Automatically generate transcriptions with the new AmberScript integration
- Do more with LTI: Create new events, edit or delete existing ones,
  or move them between series
- Export statistics to CSV using a new External API endpoint
- Edit event meta data in bulk with a new dialog in the admin UI
- Create Shibboleth users dynamically using configuration
- Support Serverless HLS
- Retract only specific flavors/tags with a new WOH
- New download button in the Theodul player
- New VideoGrid-WOH to handle grids of videos as presented for example
  by BigBlueButton
- Allow specifying role prefixes in various user providers to better
  distinguish users from different sources
- Adopter registration
- New version of Opencast Studio
- User and role provider for the Canvas LMS
- The partial import WOH can now do certain kinds of preprocessings
  to simplify further processing down the line
- Support for presigned S3 URLs

Improvements
------------

- New Paella Player version
- Make the `partial-import` workflow operation more robust
  in the face of containers with multiple videos in them
- Multiple workflows with the same ID no longer bring Opencast down
- Respect LTI user data passed by the consumer
  when persistence is enabled
- Fix entering custom roles in the admin UI
- Support odd widths/heights in the default encoding profiles
- Make image extraction more robust for streams wihtout known duration
- Fix image extraction at frame 0
- The external API now returns the bibliographic start date
  instead of the technical one
- Silence detection now ignores the video streams
  making it faster in cases there are some
- Compatibility with Java 11
- Force-delete workflows with the workflow service API,
  for example when they are stuck for some reason
- Fix the hourly statistics export
- More logging during Solr search index rebuilds
  to enable catching issues more easily
- Configuration to enable tenant-specific capture agent users
- Make the ExecuteMany-WOH more flexible in its in- and output
- Fix processing videos with only one frame,
  like MP3s with an embedded image
- Wowza configuration can now be tenant-specific
- The Theodul embed code is now fully responsive
- Override certain Karaf/Opencast settings
  using environment variables directly
- The admin UI can now handle very long ACLs (> 300 entries)
  without freezing the browser
- Verifying the success of a "proxied" log in from an external application
  is now possible without relying on fragile internal knowledge
- Make the Theodul player zoom function more obvious
  using an overlay similar to the one in Google Maps
- Various player fixes related to tracks not having any tags
- Make searching for roles in the admin UI more robust
  by loading them all at once
- Improved compatibility with S3 compatibles/alternatives
- Display notifications as overlay instead of as "flash messages"
- The `tag` workflow operation now allows wildcards in the target flavor
- Use series ACL as default ACL for events in LTI upload tool if available

API changes
-----------

- The external API now returns the bibliographic start date
  instead of the technical one
- Allow updating processing information using the Events API
- Export statistics to CSV
- The filter in the Events API is "additive" now, allowing you to get
  different types of events in one request
- The Series API can now return ACLs within its response, if you tell it to


Additional Notes about 9.12
---------------------------

This release contains a security fix:

- Further mitigation for Log4Shell (CVE-2021-45105)


Additional Notes about 9.11
---------------------------

This release contains two security fixes:

- Further mitigation for Log4Shell (CVE-2021-45046)
- Files Accessible to External Parties (CVE-2021-43821; Backport from 10.x)


Additional Notes about 9.10
---------------------------

This release contains two important security fixes.
Please take a look at the changelog for more information.


Additional Notes about 9.9
--------------------------

Minor fixes and improvements regarding the documentation, the translations,
and the handling of extended metadata catalogs.

Additional Notes about 9.8
--------------------------

This release contains a few minor bugfixes.  Notably this contains a fix which
resolves an issue related to Extron's SMP351 devices.  This fix is temporary,
and will be removed in future Opencast versions.

Additional Notes about 9.7
--------------------------

Bugfixes, quality of life improvements for developers, and even some features!
Some highlights:

- OAI-PMH sets
- workflow configuration panels can now use more `input`-types
- the pagination in the admin UI series page works properly agian
- you can now pass metadata fields directly to the series endpoint
- ACEs no longer need to specify that they are "allow entries"
- events ingested with the `fast` workflow can be processed with other workflows again
- the filename length of ingested tracks is now limited

Additional Notes about 9.6
--------------------------

Another release with many bug fixes, but also some new features,
including, but not limited to:

- The LTI-Tools now inlude the iFrameResizer library
  to let hosts control the size of the embedded tool.
- Elasticsearch can now be used with basic authentication

Additional Notes about 9.5
--------------------------

This release contains many bugfixes. In particular, a long standing issue
with 9's automatic database schema generation is finally resolved.

There is also a "Download" dropdown in the LTI tools, now.

Additional Notes about 9.4
--------------------------

Besides many fixes and improvements, Opencast 9.4 comes with the following new features:

- [The standalone video editor](modules/editor.md) received an update, featuring optional metadata editing,
hotkeys for the cutting controls and internationalization support.

Additional Notes about 9.3
--------------------------

Besides many fixes and improvements, Opencast 9.3 comes with the following new features:

- The [metrics endpoint](modules/metrics.md) now provides information about the number of events per organization.
- A first beta version of [the standalone video editor](modules/editor.md).

Additional Notes about 9.2
--------------------------

Opencast 9.2 comes with a few new features, non of which require any migrations or change the default bohavior of
Opencast 9.x.

A [metrics endpoint has been added](modules/metrics.md). It supports the [OpenMetrics format](https://openmetrics.io)
and can be used by tools like [Prometheus](https://prometheus.io). The endpoint is available at `/metrics`.

A [completely new system for securing static file content](configuration/serving-static-files.md) has been integrated.
While is is not active by default for compatibility to older 9.x releases, it can be easily enabled and used to ensure
access to static files has the same security checks you are already used to from the player and from other user
interfaces.

A new [workflow operation `cut-marks-to-smil`](workflowoperationhandlers/cut-marks-to-smil-woh.md) has been added to
allow a simpler specification of cutting information for the video editor. This was also the last piece missing from the
[BigBlueButton recordings integration](https://github.com/elan-ev/opencast-bigbluebutton-integration) in Opencast.

Some problems with the new adopter registration and update reporting have been resolved. If you stumbled over these
before, this is the release to try again.

A [security problem](https://github.com/opencast/opencast/security/advisories/GHSA-vpc2-3wcv-qj4w) where chaning access
ights to an individual video lead to cooresponding changes to its entire series was fixed.


Additional Notes about 9.1
--------------------------

This release contains an important bugfix where unprivileged users
could not upload videos, before.

Release Schedule
----------------

| Date                        | Phase                   |
|-----------------------------|-------------------------|
| October 5th                 | Feature freeze          |
| November 9th–November 15th  | Translation week        |
| November 16th–November 29th | Public QA phase         |
| December 15th               | Release of Opencast 9.0 |

Release managers
----------------

- Julian Kniephoff (ELAN e.V.)
- Carlos Turro Ribalta (Universitat Politècnica de València)
