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
- Automatically generate subscriptions with the new AmberScript integration
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
