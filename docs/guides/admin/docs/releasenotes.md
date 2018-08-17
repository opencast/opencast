Opencast 5: Release Notes
=========================

New Features
------------

* **Paella Player** - The Paella Player is already well known in the Opencast community as external plugin, but now the
  player is finally integrated into Opencast's core. This makes switching players as easy as changing one configuration
  property. Additionally, the player's own configuration is now well integrated to enable tenant specific runtime
  configuration with no need to rebuild the player. Additional improvements include more translations as well as caption
  support.

* **Animate Service** - The animate service can be used to generate custom animated video sequences which can use
  Opencast's metadata like the author's name or the event's title. This is useful, for example, to automatically generate
  animated intro sequences to ensure having a homogeneous corporate design for all video recordings.

* **Live Scheduler Service** - With the live scheduler service you are able to schedule and publish live events. The
  students can then watch the stream while the recording is happening.

* **OAI-PMH Publication Service** - OAI-PMH publication service was created to simplify and unify the publishing process
  to OAI-PMH repositories. The metadata update handling was also improved in speed and robustness.

* **Moodle User/Role Provider** - The Moodle user/role provider allows to query Moodle users and their roles. The Moodle
  [Opencast tool](https://github.com/unirz-tu-ilmenau/moodle-tool_opencast) needs to be installed for this to work.

* **New Workflow Operation Handlers**
    * **clone** - The [clone](workflowoperationhandlers/clone-woh.md) workflow operation can be used to clone media
      package elements.
    * **duplicate-event** - The [duplicate-event](workflowoperationhandlers/duplicate-event-woh.md) operation can be
      used to duplicate an event by copying an existing one.
    * **log** - The [log](workflowoperationhandlers/log-woh.md) workflow operation can be used to log the current state
      of of a workflow and/or its media package for testing and debugging purposes.
    * **animate** - The [animate](workflowoperationhandlers/animate-woh.md) workflow operation handler is the entry
      point to the new animate service.


Improvements
------------

* Support for Java 8 runtime features
* Removed unused hard-coded list providers
* Configurable license list provider
* Series catalog will be updated if the series of an event has changed
* Allow customization of the username-to-user-role mapping
* Improved FFmpeg composer implementation
* Matomo (formerly Piwik) Theodul plugin updated
* Performance
    * Series index rebuild performance increased
    * Workflow index rebuild performance increased
    * XACML parser performance increased
* User Interface
    * New translations added: Filipino, Tagalog and Turkish
    * Cross-link column date in events table so the start date filter can be used
    * Additional workflow controls added
    * Description added to the series dialog theme tab
    * Less technical representation of the Opencast version
    * Event can be scheduled by specifying the end time
    * Save button in the video editor does not close the page anymore
    * Video editor can optionally play deleted segments
    * Event publications dialog tab and pop-up improved
    * Languages drop-down menu is now sorted and allows to define a default language
    * Workflow drop-down menu is now sorted by workflow titles, workflow filtering is only enabled if are enough
      workflows available
    * Default workflow is preselected in event create dialog
* Improved workflow operation handlers
    * [analyze-tracks](workflowoperationhandlers/analyze-tracks-woh.md) workflow operation handler set the video
      framerate as the workflow instance property too
* Minor database fixes
* Documentation improvements
* Continuous integration implemented
    * Travis build on each pull request
    * Travis Badge indicates the status of the build
* All mentions of Matterhorn are removed from the codebase
* Database tables renamed
* Workflow definitions renamed
* Several libraries updated
    * Karaf 4.0.10
    * AngularJS 1.5.11
    * …many other


Configuration changes
---------------------
Please check the [configuration changes](upgrade.md#configuration-changes) section in the upgrade notes.


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
