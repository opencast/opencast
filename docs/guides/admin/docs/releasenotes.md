Opencast 6: Release Notes
=========================

New Features and Improvements
-----------------------------

- Deleting Series now warns if series contains events. You can configure if the user is allowed to
  delete a series containing events in the series endpoint config file.
- Update Paella Player to 6.0.x
- Add Moodle groups to Moodle role provider
- Add start-workflow operation
- Ability to send HTML emails
- Ability for blacklisting languages from the admin UI
- Update LTI Series Tool
- External API 1.1.0
    - Add filters for new fields
- Fill creator metadata field with actual user when new event
- AWS S3 Asset Storage
- Tiered Storage for the Asset Manager
- image-convert operation
- Multiencode
- Improved keyboard/hotkey support
- Video editor
    - Audio and video track selection in video editor
    - Intuitive Merging of Video Segments
- Alternative video editor backend (process-smil)
- Capture Agent Access Management
- Add new modal to edit multiple scheduled events at once
- Manually Select And Upload Thumbnails
- As an unprivileged user, I only want to see series and events that I have write access to.
- Make workflow processing settings persistent
- Demux Operation
- Lossless Concat Operation
- Mattermost-notification-workflowoperationhandler
- Per-tenant digest user for capture agents


Configuration changes
---------------------

- The tracking options defaults have changed to be more aware of the European Union's General Data Protection
  Regulation. Note that you can [still use the old settings if you want to](configuration/user-statistics.and.privacy.md).

- The role ROLE_UI_EVENTS_DETAILS_GENERAL_VIEW for viewing the publications (previously general) tab in the event
  details modal has been renamed to ROLE_UI_EVENTS_DETAILS_PUBLICATIONS_VIEW for consistency.


Release Schedule
----------------

|Date                         |Phase
|-----------------------------|------------------------------------------
|Sep 25th 2018                |Feature Freeze
|Oct 29th - Nov 4th 2018      |Translation week
|Nov 5th - Nov 18th 2018      |Public QA phase
|Dec 10th 2018                |Release of Opencast 6.0


Release Managers
----------------

- Matthias Neugebauer (University of Münster)
- Lars Kiesow (ELAN e.V.)
