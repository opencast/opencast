Opencast 7: Release Notes
=========================

New Features
------------

<<<<<<< HEAD
- Overhaul of the permission management with the newly added possibility to define how access control lists are
  evaluated and how series permission changes are populated to episodes. For more details take a look at the [access
  control configuration guide](configuration/acl.md).
=======
- **Asset Manager Storage Layers** - The Asset Manager now supports multiple storage layers natively.  This allows
  users to move data from local storage to remote storage within Opencast.  These moves can be triggered manually, or
  via a built-in timer.  Currently we support local file storage, and AWS S3 storage.

- **Video Editor - Thumbnails** - When displaying lists of videos, thumbnails are usually used to make such lists more
  appealing. The video editor allows the user to choose between the default thumbnail (automatically created), a
  snapshot thumbnail (extract at current position from video stream) or an uploaded thumbnail.  This allows the user to
  get the best thumbnails for his videos with as little effort as possible.

- **Video Editor - Track selection** - In case multiple tracks have been ingested, the video editor allows the user to
  choose which tracks should be processed. In case of a dual track lecture recording, the presenter track could be
  exluded from publication in case the recorded person would want this.

- **Keyboard Shortcuts** - A per page keyboard shortcut list is available so that users can at any time and page see
  what keyboard shortcuts are currenlty available.

- **External API 1.1.0** - For the first time, Opencast takes advantage of its state-of-the-art versioned API by
  introducing the External API 1.1.0.  The new version extends the API to support scheduling of events and access to
  workflows.  The filter and sort facilities have been extended and additional fields are directly delivered by some
  core requests with allows clients to access them way faster.  The External API 1.0.0 is still supported relieving
  existing clients from the need to be timely adapted.

- **Processing Settings Persistency** - A new persistence layer for processing settings has been introduced. This allows
  the Admin UI to not just provide processing settings as input when starting workflows, but also to display processing
  settings on event basis.

- **Capture Agent Access Management** - The Admin UI now supports access management for capture agents. This features
  addresses the need to allow unprivileged users to access the Admin UI to manage their own content and cut videos
  without allowing them to schedule events or change the scheduling as this task is usually in the responsibility of a
  dedicated team.  It is also possible to permit users access to specific subsets of the available capture agents.

- **New Workflow Operation Handler**
    - **demux** can be used to demux multiple streams from a container into seperate containers
    - **image-convert** can convert multiple source images into multiple target images with different encoding
    - **mattermost-notify** sends notification to services like Mattermost or Slack
    - **move-to-remote** moves files in the asset manager from one storage system to another
    - **multi-encode** allows encoding of multiple source tracks into multiple target tracks with differnet encoding
    - **process-smil** edits media files based on descriptions from a SMIL file
    - **select-tracks** can filter source tracks based on workflow properties
    - **start-workflow** allows a workflow to start another workflow
>>>>>>> r/6.x

Improvements
------------

A non-comprehensive list of improvements:

<<<<<<< HEAD
- improvement 1
- improvement 2
=======
- The event counters are now fully configurable
- Workflows can be stopped and deleted in the Admin UI
- Multiple scheduled events can be edited at once
- Deleting Series now warns if series contains events. You can configure if the user is allowed to
  delete a series containing events in the series endpoint config file.
- The video editor can be opened while an event is processed
- Add Moodle groups to Moodle role provider
- The order of appearance of workflows can be configured
- Ability to send HTML emails
- Ability for blacklisting languages from the admin UI
- Update LTI Series Tool
- Fill creator metadata field with actual user when new event
- Video editor
- Intuitive Merging of Video Segments
- Add new modal to edit multiple scheduled events at once
- As an unprivileged user, I only want to see series and events that I have write access to.
- Lossless Concat Operation
- Update Paella Player to 6.0.x
>>>>>>> r/6.x

Configuration changes
---------------------

- …

Release Schedule
----------------

*To be defined*


Release Managers
----------------

*Looking for release managers*
