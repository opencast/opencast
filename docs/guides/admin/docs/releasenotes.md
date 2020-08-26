Opencast 9: Release Notes
=========================

**TODO** The following is as of 2020/08/26, and not finished, yet.

Important Changes
-----------------

- Opencast now requires an external Elasticsearch server to function
- Paella is now the default video player
- The `encode` worfklow operation now completely replaces `compose`,
  which is dropped now
- MariaDB JDBC driver
- ...

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
- ...

Improvements
------------

- Paella Player version **TODO**
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
- ...

API changes
-----------

- The external API now returns the bibliographic start date
  instead of the technical one
- ...

Configuration changes
---------------------

*TBD*


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
