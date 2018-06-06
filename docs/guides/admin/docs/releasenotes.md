Opencast 5: Release Notes
=========================

New Features
------------

* **Paella Player** - The Paella Player is already well known in the Opencast community as external plugin, but now the
  player is finally integrated into Opencast's core. This makes switching players as easy as changing one configuration
  property. Additionally, the player's own configuration is now well integrated to enable tenant specific runtime
  configuration with no need to rebuild the player. Additional improvements include more translations as well as caption
  support.

* **Animate Service** - The Animate Opencast service can be used to generate custom animated video sequences which can
  use Opencast metadata like the author's name or the event's title. This is useful, for example, to automatically
  generate animated intro sequences to ensure having a homogeneous corporate design for all video recordings.

* **Live Scheduler Service** - With the Live Scheduler Service you will be able to schedule live events. The students
  can then watch the live stream during the recording is happen.

* **OAI-PMH Publication Service** - OAI-PMH Publication Service was created to simplify and unify the publishing process
  to the OAI-PMH repository. The metadata update handling was also improved in speed and robustness.

* **Moodle User/Role Provider** - The Moodle user/role provider allows to query Moodle users and their roles. The
  [Opencast tool](https://github.com/unirz-tu-ilmenau/moodle-tool_opencast) must be installed on the Moodle side.

* **New Workflow Operation Handlers**
    * **clone** - The [clone](workflowoperationhandlers/clone-woh.md) workflow operation can be used to clone media
      package elements.
    * **duplicate-event** - The [duplicate-event](workflowoperationhandlers/duplicate-event-woh.md) operation can be
      used to duplicate an event by copying an existing one.
    * **log** - The [log](workflowoperationhandlers/log-woh.md) workflow operation can be used to log the current state
      of of a workflow and/or its media package for testing and debugging purposes.
    * **animate** - The [animate](workflowoperationhandlers/animate-woh.md) workflow operation handler is the entry
      point to the Animate Service.

Improvements
------------
* Java 8 support
* Removed unused hard-coded list providers
* Configurable License List Provider
* Series catalog will be updated if the series of an event has changed
* Allow customization of the username-to-user-role mapping
* FFmpeg Composer implementation improved
* Matomo (formerly Piwik) theodul plugin updated
* Performance
    * Series index rebuild performance increased
    * Workflow index rebuild performance increased
    * XACML parser performance increased
* User Interface
    * Cross-link column date in events table to enable the start date filter
    * Workflow controls added
    * Description added to the series dialog theme tab
    * Less technical representation of the Opencast version
    * Event can be scheduled by specifying the end time
    * Save button in the video editor do not close the page any more
    * Video editor can optionally play deleted segments
    * Event publications dialog tab and popup improved
    * Languages drop-down menu
    * Workflow drop-down menu
    * Default workflow is preselected in event create dialog
* Improved workflow operation handlers
    * WOH analyze-tracks
* Minor database fixes
* Documentation improvements
* Continious integration implemented
    * Travis build on each pull request
    * Travis Badge inicates the status of the build
* All mentions of Matterhorn are removed from the codebase
* Database tables renamed
* Workflow IDs renamed
* Several libraries updated
    * Karaf 4.0.10
    * AngularJS 1.5.11
    * …many other

Configuration changes
---------------------

* HTTP Basic authentication is enabled by default
    * Make sure you've enabled HTTPS support in Opencast or your prefered termination proxy (see
      [documentation](configuration/security.https.md))
* Paella Player URL has changed to `/paella/ui/watch.html`
    * Make sure to update your organization and security configuration on upgrade to Opencast 5
* `ng-` prefix was removed from the workflow IDs (also workflow filenames)
* Workflow control URLs are added to the security configuration


Release Schedule
----------------

|Date                   |Phase
|-----------------------|----------------------------------------
|April 3rd              |Feature Freeze
|April 3rd - May 14th   |Internal QA and bug fixing phase
|May 14th - May 27th    |Public QA phase
|May 21rd - May 27th    |Additional bug fixing phase
|May 21rd - May 27th    |Translation week
|May 28th - June 10th   |Final QA phase
|June 12th              |Release of Opencast 5.0


Release Managers
----------------

* Waldemar Smirnow (ELAN e.V.)
* Tobias Schiebeck (University of Manchester)
