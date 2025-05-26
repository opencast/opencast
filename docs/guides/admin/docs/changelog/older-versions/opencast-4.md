Opencast 4
------------

### Opencast 4.5

*Released on Oktober 30, 2018*

- [[NOJIRA]](https://github.com/opencast/opencast/pull/453) - Fix wrong example in publish-configure documentation
- [[MH-13075]](https://opencast.jira.com/browse/MH-13075) - make ACL entries unique prior to running ACL comparisons
- [[MH-13068]](https://opencast.jira.com/browse/MH-13068) - workflow delete instance stability improvement
- [[MH-13055]](https://opencast.jira.com/browse/MH-13055) - Stop making events with no ACL public on ingest
- [[MH-13032]](https://opencast.jira.com/browse/MH-13032) - Asset Upload fix for missing reset()
- [[MH-12953]](https://opencast.jira.com/browse/MH-12953) - stop loading editor.json twice
- [[NOJIRA]](https://github.com/opencast/opencast/pull/273) - Update the release process docs

### Opencast 4.4

*Released on May 31, 2018*

- [[MH-12923]](https://opencast.jira.com/browse/MH-12923) - ServiceRegistry does not close db connction
- [[MH-12841]](https://opencast.jira.com/browse/MH-12841) - Opencast is ignoring permissions
- [[MH-12840]](https://opencast.jira.com/browse/MH-12840) - LTI user provider may allow LMS admins to become Opencast
  admins

### Opencast 4.3

*Released on March 28, 2018*

- [[MH-12774]](https://opencast.jira.com/browse/MH-12744) - Fix differences in provided security configurations
- [[MH-12773]](https://opencast.jira.com/browse/MH-12773) - Fix that non-admins cannot add new assets
- [[MH-12772]](https://opencast.jira.com/browse/MH-12772) - Fix acces to assets for non-admins
- [[MH-12789]](https://opencast.jira.com/browse/MH-12789) - Remove tabs and trailing spaces in LTI tools
- [[MH-12790]](https://opencast.jira.com/browse/MH-12790) - Make LTI respect player configuration

### Opencast 4.2

*Released on March 14, 2018*

- [[MH-12766]](https://opencast.jira.com/browse/MH-12766) - Metadata view and edit roles where at some places set
  incorrectly
- [[MH-12765]](https://opencast.jira.com/browse/MH-12765) - Navigating through series in the series details modal causes
  failing attempts to save ACLs
- [[MH-12758]](https://opencast.jira.com/browse/MH-12758) - Changing the ACLs does not trigger AssetManagerDecorators
- [[MH-12747]](https://opencast.jira.com/browse/MH-12747) - Heartbeat is broken
- [[MH-12745]](https://opencast.jira.com/browse/MH-12745) - Fix heartbeat config logging
- [[MH-12743]](https://opencast.jira.com/browse/MH-12743) - OAIPMH-Republish-Operation tries to republish to ASW3
- [[MH-12728]](https://opencast.jira.com/browse/MH-12728) - Add LAST-MODIFIED to ical event properties
- [[MH-12727]](https://opencast.jira.com/browse/MH-12727) - OptimisticLockException on worker node can cause jobs to be
  stuck in DISPATCHING state
- [[MH-12725]](https://opencast.jira.com/browse/MH-12725) - Series/Events ACL update causes scheduled recordings in the
  series/the events to disappear from CA calendar
- [[MH-12717]](https://opencast.jira.com/browse/MH-12717) - Series metadata update causes scheduled recordings in the
  series to disappear from CA calendar
- [[MH-12711]](https://opencast.jira.com/browse/MH-12711) - XACML Parser should be more robust
- [[MH-12707]](https://opencast.jira.com/browse/MH-12707) - Fix problem with non-strict mode in URL-Signing
- [[MH-12706]](https://opencast.jira.com/browse/MH-12706) - Old zombie workflows cannot be stopped, suspended etc.
- [[MH-12668]](https://opencast.jira.com/browse/MH-12668) - Update admin ui build pipeline
- [[MH-12651]](https://opencast.jira.com/browse/MH-12651) - Scheduling repeating events through Admin UI is very slow

### Opencast 4.1

*Released on Februar 7, 2018*

- [[MH-12695]](https://opencast.jira.com/browse/MH-12695) - Improve Synchronization in WorkflowService
- [[MH-12689]](https://opencast.jira.com/browse/MH-12689) - Flickering filter: When loading the page, all filters
  briefly appear and disappear again
- [[MH-12687]](https://opencast.jira.com/browse/MH-12687) - Date filters not working
- [[MH-12685]](https://opencast.jira.com/browse/MH-12685) - Performance issue in filters
- [[MH-12682]](https://opencast.jira.com/browse/MH-12682) - TimelinePreview Concurrency Problem
- [[MH-12676]](https://opencast.jira.com/browse/MH-12676) - List provider service implementation is not thread-safe
- [[MH-12673]](https://opencast.jira.com/browse/MH-12673) - Content-Type is not set for JavaScript files
- [[MH-12664]](https://opencast.jira.com/browse/MH-12664) - Ensure series can be deleted
- [[MH-12662]](https://opencast.jira.com/browse/MH-12662) - Special characters in modal window titles are double-escaped
- [[MH-12657]](https://opencast.jira.com/browse/MH-12657) - Users of non-admin groups cannot create events
- [[MH-12652]](https://opencast.jira.com/browse/MH-12652) - Scheduler service needs to restrict queries to episodes
  owned by it
- [[MH-12641]](https://opencast.jira.com/browse/MH-12641) - Asset manager conflict checks are very slow
- [[MH-12638]](https://opencast.jira.com/browse/MH-12638) - Migration bundle needs to have a higher runlevel
- [[MH-12637]](https://opencast.jira.com/browse/MH-12637) - Remove event id from episode DC catalog during migration
- [[MH-12632]](https://opencast.jira.com/browse/MH-12632) - Make index rebuild robust
- [[MH-12631]](https://opencast.jira.com/browse/MH-12631) - Drop the ORGANIZER field from the ical feed
- [[MH-12627]](https://opencast.jira.com/browse/MH-12627) - Start Task copies files into workspace
- [[MH-12620]](https://opencast.jira.com/browse/MH-12620) - Document ActiveMQ memory requirements
- [[MH-12610]](https://opencast.jira.com/browse/MH-12610) - Navigating through events in the event details modal causes
  failing attempts to save ACLs
- [[MH-12609]](https://opencast.jira.com/browse/MH-12609) - As a user, I expect scheduling of events to be working
- [[MH-12606]](https://opencast.jira.com/browse/MH-12606) - Using "Start Task" with a workflow containing an embedded
  script in the configuration which somehow modifies the input parameters does not update those values properly
- [[MH-12602]](https://opencast.jira.com/browse/MH-12602) - External API gives 500 error for migrated series that do not
  have creator field
- [[MH-12601]](https://opencast.jira.com/browse/MH-12601) - Fast Workflow Does Not Attach Series Metadata
- [[MH-12582]](https://opencast.jira.com/browse/MH-12582) - Editor WOH should not encode videos unless it is strictly
  necessary (to save time and resources)
- [[MH-12495]](https://opencast.jira.com/browse/MH-12495) - Job dispatching with loads needs optimization
- [[MH-12476]](https://opencast.jira.com/browse/MH-12476) - Delay start of job dispatching on startup
- [[MH-10016]](https://opencast.jira.com/browse/MH-10016) - Cannot Change Default Workflow

### Opencast 4.0

*Released on December 8, 2017*

- [[MH-12597]](https://opencast.jira.com/browse/MH-12597) - When reindexing, some events may incorrectly be displayed as
  "Scheduled" instead of "Processed" or "Failed"
- [[MH-12596]](https://opencast.jira.com/browse/MH-12596) - Video Editor Ignores Workspace
- [[MH-12594]](https://opencast.jira.com/browse/MH-12594) - Description field in metadata editor doesn't handle newlines
  properly
- [[MH-12591]](https://opencast.jira.com/browse/MH-12591) - AssetManager reindex produces "No organization found!"
  warnings
- [[MH-12590]](https://opencast.jira.com/browse/MH-12590) - Fix Workflow WOH Workspace Mock
- [[MH-12589]](https://opencast.jira.com/browse/MH-12589) - Fix Timelinepreview Dependencies
- [[MH-12588]](https://opencast.jira.com/browse/MH-12588) - Stream Security Leaks Secrets
- [[MH-12587]](https://opencast.jira.com/browse/MH-12587) - ActiveMQ config ships with 3rd party tool enabled by default
- [[MH-12583]](https://opencast.jira.com/browse/MH-12583) - Reduce frequency of index rebuild messages for comments and
  asset manager
- [[MH-12579]](https://opencast.jira.com/browse/MH-12579) - Simplify XACML Handling
- [[MH-12578]](https://opencast.jira.com/browse/MH-12578) - Color of Crosslinks Makes Tables Look Noisy
- [[MH-12574]](https://opencast.jira.com/browse/MH-12574) - Audio keeps playing when leaving the playback or editor page
- [[MH-12573]](https://opencast.jira.com/browse/MH-12573) - Unprivileged users cannot delete events
- [[MH-12572]](https://opencast.jira.com/browse/MH-12572) - Dependency Fixes
- [[MH-12570]](https://opencast.jira.com/browse/MH-12570) - Admin UI Regressions And Minor Bugs
- [[MH-12569]](https://opencast.jira.com/browse/MH-12569) - Don't fail hard if attempting to distribute a non-track
  media package element to streaming server
- [[MH-12568]](https://opencast.jira.com/browse/MH-12568) - EditableSingleValue Has Focus Issues
- [[MH-12567]](https://opencast.jira.com/browse/MH-12567) - Index Service Dependencies
- [[MH-12566]](https://opencast.jira.com/browse/MH-12566) - Remove Unused Participation List Provider
- [[MH-12560]](https://opencast.jira.com/browse/MH-12560) - Streaming media distribution does not work in a distributed
  cluster
- [[MH-12559]](https://opencast.jira.com/browse/MH-12559) - CSS: Delete And Retract Dialogs For Events Are Messed up
- [[MH-12558]](https://opencast.jira.com/browse/MH-12558) - CSS: Buttons in Confirm Modals Too Big
- [[MH-12557]](https://opencast.jira.com/browse/MH-12557) - CSS: Checkbox Alignment in Tables
- [[MH-12556]](https://opencast.jira.com/browse/MH-12556) - Video Editor CSS Enhancements
- [[MH-12554]](https://opencast.jira.com/browse/MH-12554) - Downloading translations from Crowdin doesn't work anymore
- [[MH-12553]](https://opencast.jira.com/browse/MH-12553) - As an administrator, I want to configure the order in which
  the different adaptive streaming video qualities are listed
- [[MH-12552]](https://opencast.jira.com/browse/MH-12552) - The "delete" button in the Admin UI may leave the "preview"
  artifacts undeleted
- [[MH-12551]](https://opencast.jira.com/browse/MH-12551) - Redo changes of MH-11660 that got lost in means of a
  regression
- [[MH-12550]](https://opencast.jira.com/browse/MH-12550) - hasActiveTransaction is triggered permantly for edited jobs
- [[MH-12548]](https://opencast.jira.com/browse/MH-12548) - Matterhorn Kernel Test Issues
- [[MH-12547]](https://opencast.jira.com/browse/MH-12547) - Group related settings in custom.properties
- [[MH-12546]](https://opencast.jira.com/browse/MH-12546) - 3.x to 4.0 upgrade is ugly
- [[MH-12545]](https://opencast.jira.com/browse/MH-12545) - Multi Value Editable Loses Value on Blur
- [[MH-12543]](https://opencast.jira.com/browse/MH-12543) - Adjust Log Level During Build Time
- [[MH-12542]](https://opencast.jira.com/browse/MH-12542) - Fix Ingest Service API Dependencies
- [[MH-12541]](https://opencast.jira.com/browse/MH-12541) - Events not searchable after migration if event was subject
  to a workflow with two publish-engage operations
- [[MH-12540]](https://opencast.jira.com/browse/MH-12540) - Add documentation for WOH failing
- [[MH-12539]](https://opencast.jira.com/browse/MH-12539) - Add documentation for WOH include
- [[MH-12537]](https://opencast.jira.com/browse/MH-12537) - Admin UI Asset upload: Order Assets as listed in properties
  file (vs alphabetical)
- [[MH-12535]](https://opencast.jira.com/browse/MH-12535) - Add language support for Hebrew
- [[MH-12534]](https://opencast.jira.com/browse/MH-12534) - Broken Labels In Default Workflow
- [[MH-12532]](https://opencast.jira.com/browse/MH-12532) - The bundle `workflow-workflowoperation` creates (and leaves)
  temporary files in `/tmp`
- [[MH-12529]](https://opencast.jira.com/browse/MH-12529) - External API returns negative Event duration
- [[MH-12526]](https://opencast.jira.com/browse/MH-12526) - External (LDAP) users cannot not see their own role
  (ROLE_USER_XXXX) in the access policy of the events they create.
- [[MH-12525]](https://opencast.jira.com/browse/MH-12525) - Non-admin users cannot modify ACLs in their own events
- [[MH-12523]](https://opencast.jira.com/browse/MH-12523) - "Submit" button in retract modal is always disabled
- [[MH-12522]](https://opencast.jira.com/browse/MH-12522) - Improve Waveform Service Dependency Specification
- [[MH-12520]](https://opencast.jira.com/browse/MH-12520) - Duplicate Series When Double Clicking Create Button
- [[MH-12519]](https://opencast.jira.com/browse/MH-12519) - Improve Admin-NG Dependency Specification
- [[MH-12518]](https://opencast.jira.com/browse/MH-12518) - Ugly exception appears in stdout/Karaf console
- [[MH-12517]](https://opencast.jira.com/browse/MH-12517) - Some job data is not copied correctly
- [[MH-12514]](https://opencast.jira.com/browse/MH-12514) - Opencast Allows Multiple Simultaneous Workflows For Same
  Media Package
- [[MH-12513]](https://opencast.jira.com/browse/MH-12513) - MigrationService fails
- [[MH-12512]](https://opencast.jira.com/browse/MH-12512) - Frontend-Maven-Plugin configuration is missing the mandatory
  "versionRange" parameter
- [[MH-12511]](https://opencast.jira.com/browse/MH-12511) - Deleting an event with inconsistent search index state
  doesn't work
- [[MH-12510]](https://opencast.jira.com/browse/MH-12510) - System doesn't recover from ActiveMQ downtime
- [[MH-12507]](https://opencast.jira.com/browse/MH-12507) - Textanalyzer Has Nondeclared Dependencies
- [[MH-12503]](https://opencast.jira.com/browse/MH-12503) - Log statements do not require Object or String arrays to
  provide 3 parameters or more
- [[MH-12500]](https://opencast.jira.com/browse/MH-12500) - Fix incorrect usage of method "URL#getFile()"
- [[MH-12499]](https://opencast.jira.com/browse/MH-12499) - Admin UI event tools dialog can't be closed with the close
  button
- [[MH-12498]](https://opencast.jira.com/browse/MH-12498) - External API: Cannot get series if description field is
  empty
- [[MH-12497]](https://opencast.jira.com/browse/MH-12497) - Improve usability of admin UI forms
- [[MH-12492]](https://opencast.jira.com/browse/MH-12492) - AssetManager endpoint return server error on assets, which
  the user not allowed to read
- [[MH-12489]](https://opencast.jira.com/browse/MH-12489) - Failed test: MySQL DDL Scripts (Update) ￼
- [[MH-12488]](https://opencast.jira.com/browse/MH-12488) - Publish worklow always fail
- [[MH-12480]](https://opencast.jira.com/browse/MH-12480) - Waveform Operation Should Have Tests
- [[MH-12479]](https://opencast.jira.com/browse/MH-12479) - Waveform Operation Should Not leave Files In Workspace
- [[MH-12475]](https://opencast.jira.com/browse/MH-12475) - Make mimetypes consistent
- [[MH-12470]](https://opencast.jira.com/browse/MH-12470) - Prematurely deleted scheduler properties lead to undeletable
  events
- [[MH-12469]](https://opencast.jira.com/browse/MH-12469) - Auto Update OAIPMH republishes deleted Events
- [[MH-12467]](https://opencast.jira.com/browse/MH-12467) - Scheduled event fails due to not finding a workflow
  definition to use
- [[MH-12465]](https://opencast.jira.com/browse/MH-12465) - Propagate Changes of Series Extended Metadata to Events and
  OAI-PMH
- [[MH-12463]](https://opencast.jira.com/browse/MH-12463) - Hyphens in event/series search return no results
- [[MH-12456]](https://opencast.jira.com/browse/MH-12456) - Clean Up PathSupport
- [[MH-12455]](https://opencast.jira.com/browse/MH-12455) - FFmpeg does not terminate when Opencast is shut down
- [[MH-12454]](https://opencast.jira.com/browse/MH-12454) - PathSupport.changeFileExtension does not properly handle
  files with no extension
- [[MH-12453]](https://opencast.jira.com/browse/MH-12453) - TimelinePreview Path Handling
- [[MH-12451]](https://opencast.jira.com/browse/MH-12451) - Lock file utility method should throw exceptions
- [[MH-12450]](https://opencast.jira.com/browse/MH-12450) - Clean up \*EncoderEngine code
- [[MH-12449]](https://opencast.jira.com/browse/MH-12449) - Ensure temporary files are deleted on composer failure
- [[MH-12448]](https://opencast.jira.com/browse/MH-12448) - Remove unconfigured send-mail WOH
- [[MH-12447]](https://opencast.jira.com/browse/MH-12447) - OAI-PMH autorepublish fails if series was deleted
- [[MH-12446]](https://opencast.jira.com/browse/MH-12446) - Do not leave ZIP files in workspace when a Workflow fails
- [[MH-12445]](https://opencast.jira.com/browse/MH-12445) - underlying code showing on metadata source tab when creating
  event
- [[MH-12443]](https://opencast.jira.com/browse/MH-12443) - editing event changes status from scheduled to finished
- [[MH-12442]](https://opencast.jira.com/browse/MH-12442) - Maven site is broken
- [[MH-12436]](https://opencast.jira.com/browse/MH-12436) - Add Christian Greweling to Comitters list
- [[MH-12431]](https://opencast.jira.com/browse/MH-12431) - Update Crowdin translations for r/4.x
- [[MH-12428]](https://opencast.jira.com/browse/MH-12428) - Performance Issue In Event Metadata
- [[MH-12427]](https://opencast.jira.com/browse/MH-12427) - Submit button in Editor typo
- [[MH-12423]](https://opencast.jira.com/browse/MH-12423) - Date Parse Error When Changing Certain Metadata
- [[MH-12420]](https://opencast.jira.com/browse/MH-12420) - Update frontend-maven-plugin
- [[MH-12417]](https://opencast.jira.com/browse/MH-12417) - Poor performace on scheduler /recordings/calendars
- [[MH-12411]](https://opencast.jira.com/browse/MH-12411) - Database user requires additional permissions
- [[MH-12409]](https://opencast.jira.com/browse/MH-12409) - Conductor logs ClassCastException when receiving
  DeleteSnapshot
- [[MH-12407]](https://opencast.jira.com/browse/MH-12407) - "The task could not be created" message by starting task on
  multiple events
- [[MH-12406]](https://opencast.jira.com/browse/MH-12406) - Splitting in the video editor while a video is playing
  causes time jump
- [[MH-12401]](https://opencast.jira.com/browse/MH-12401) - Video editor segment times stay blank (timing)
- [[MH-12399]](https://opencast.jira.com/browse/MH-12399) - Oaipmh Retract very slow
- [[MH-12396]](https://opencast.jira.com/browse/MH-12396) - Cannot select filter two times in a row from dropdown
- [[MH-12395]](https://opencast.jira.com/browse/MH-12395) - REST: Handle Scheduling Conflict
- [[MH-12394]](https://opencast.jira.com/browse/MH-12394) - Video editor allows the submission of an event with no
  active segments
- [[MH-12390]](https://opencast.jira.com/browse/MH-12390) - Gracefully handle unregistration of non-existing host
- [[MH-12385]](https://opencast.jira.com/browse/MH-12385) - Ingest Code Cleanup
- [[MH-12382]](https://opencast.jira.com/browse/MH-12382) - As a system administrator, I want to see the capture agent
  configuration in the user interface, so that I don't need to look into the database directly
- [[MH-12380]](https://opencast.jira.com/browse/MH-12380) - External API v1.0.0 Broken Due To StartDate Format Change
- [[MH-12372]](https://opencast.jira.com/browse/MH-12372) - Make waveform service more flexible by allowing pre- and
  post-filters to be configured
- [[MH-12366]](https://opencast.jira.com/browse/MH-12366) - authorization-manager depends on download-impl
- [[MH-12365]](https://opencast.jira.com/browse/MH-12365) - Losing ActiveMQ connection spams the logs
- [[MH-12356]](https://opencast.jira.com/browse/MH-12356) - As an administrator, I'd like to resolve or delete comments
  in workflows by comment reason only
- [[MH-12355]](https://opencast.jira.com/browse/MH-12355) - Include Wowza Adaptive Streaming Module in Opencast
- [[MH-12354]](https://opencast.jira.com/browse/MH-12354) - Admin UI Video Editor wont let you edit segements at the end
- [[MH-12352]](https://opencast.jira.com/browse/MH-12352) - Include support for user Groups in LDAP
- [[MH-12350]](https://opencast.jira.com/browse/MH-12350) - Recreate adminui-Index stops, if Asset of Event ist missing
- [[MH-12349]](https://opencast.jira.com/browse/MH-12349) - Exception handler should not throw an IO exception on
  deleting temporary directory
- [[MH-12348]](https://opencast.jira.com/browse/MH-12348) - As an administrator, I want to use the "send-email" WOH with
  multiple recipients and also use the CC and BCC fields
- [[MH-12346]](https://opencast.jira.com/browse/MH-12346) - Publications are not shown in the admin interface
- [[MH-12330]](https://opencast.jira.com/browse/MH-12330) - The series WOH only updates the series' title and ID on the
  episode's catalog, but sometimes more fields should be updated
- [[MH-12328]](https://opencast.jira.com/browse/MH-12328) - Update AngularJS from 1.3.x to 1.4.x
- [[MH-12325]](https://opencast.jira.com/browse/MH-12325) - Maven warning when building r/3.x
- [[MH-12314]](https://opencast.jira.com/browse/MH-12314) - As a developer, I expect the Admin UI tests being skipped if
  I build Opencast using -DskipTests
- [[MH-12312]](https://opencast.jira.com/browse/MH-12312) - Event Counter For "Today"
- [[MH-12309]](https://opencast.jira.com/browse/MH-12309) - Use Matching FontAwesome Icons
- [[MH-12304]](https://opencast.jira.com/browse/MH-12304) - Configurable Notification Durations
- [[MH-12302]](https://opencast.jira.com/browse/MH-12302) - Do Not Warn About Default Configuration
- [[MH-12289]](https://opencast.jira.com/browse/MH-12289) - Publish extended metadata to OAI-PMH
- [[MH-12287]](https://opencast.jira.com/browse/MH-12287) - prevent reload of Admin UI when opening the editor
- [[MH-12286]](https://opencast.jira.com/browse/MH-12286) - As an Opencast admin, I want to set workflow properties from
  an external script
- [[MH-12284]](https://opencast.jira.com/browse/MH-12284) - Unprivileged users cannot upload any files when creating or
  editing a theme
- [[MH-12283]](https://opencast.jira.com/browse/MH-12283) - Support MPEG DASH in Player
- [[MH-12278]](https://opencast.jira.com/browse/MH-12278) - NullPointerException in CleanupWorkflowOperationHandler
- [[MH-12274]](https://opencast.jira.com/browse/MH-12274) - Ingest service REST endpoint should be verbosable and expect
  input UTF-8 encoded
- [[MH-12266]](https://opencast.jira.com/browse/MH-12266) - As a user, I expect metadata changes to be propagated to
  third-party applications
- [[MH-12259]](https://opencast.jira.com/browse/MH-12259) - Ingest-download WOH fail on downloading publication elements
- [[MH-12258]](https://opencast.jira.com/browse/MH-12258) - Update angular-translate to version 2.15.2
- [[MH-12250]](https://opencast.jira.com/browse/MH-12250) - Synchronize Dublin Core date created and start date in DC
  temporal
- [[MH-12242]](https://opencast.jira.com/browse/MH-12242) - Theodul: Quality selector does not display/load
- [[MH-12234]](https://opencast.jira.com/browse/MH-12234) - Cleanup WOH does not remove all files as it should do
- [[MH-12227]](https://opencast.jira.com/browse/MH-12227) - As a user, I don't want to be informed about services not
  being working correctly
- [[MH-12223]](https://opencast.jira.com/browse/MH-12223) - Oaipmh Publish is very slow
- [[MH-12200]](https://opencast.jira.com/browse/MH-12200) - Improve LDAP integration after the changes brought by
  MH-12016
- [[MH-12196]](https://opencast.jira.com/browse/MH-12196) - Use a date and time picker instead of separate inputs for
  date and time in admin UI
- [[MH-12191]](https://opencast.jira.com/browse/MH-12191) - Add support for automated captions/transcripts (IBM Watson)
- [[MH-12168]](https://opencast.jira.com/browse/MH-12168) - As a user, I need cross-page links that help me to work more
  efficiently
- [[MH-12166]](https://opencast.jira.com/browse/MH-12166) - As a user, I'm not willing to perform that many clicks to
  actually use the filters
- [[MH-12111]](https://opencast.jira.com/browse/MH-12111) - Require Java 8
- [[MH-12104]](https://opencast.jira.com/browse/MH-12104) - As a producer, I want to access assets of my tenant while a
  workflow is running
- [[MH-12099]](https://opencast.jira.com/browse/MH-12099) - Wrong started date/time on workflow details view
- [[MH-12082]](https://opencast.jira.com/browse/MH-12082) - Contribute Asset Manager/Scheduler work (ETH)
- [[MH-12052]](https://opencast.jira.com/browse/MH-12052) - As an Administrator, I'd like to know that ActiveMQ is
  running properly
- [[MH-12000]](https://opencast.jira.com/browse/MH-12000) - Cross-tenant URL signing
- [[MH-11703]](https://opencast.jira.com/browse/MH-11703) - Service error states not immediately visible in admin UI
- [[MH-11458]](https://opencast.jira.com/browse/MH-11458) - Update translations from crowdin
- [[MH-11274]](https://opencast.jira.com/browse/MH-11274) - Workflow Operations of Scheduled Event are not editable
- [[MH-11195]](https://opencast.jira.com/browse/MH-11195) - Ability to Search on part of a Series Identifier, instead of
  just exact match
- [[MH-11042]](https://opencast.jira.com/browse/MH-11042) - Admin UI NG tests fail in +5:30 timezone
- [[MH-10156]](https://opencast.jira.com/browse/MH-10156) - Misspelling in LtiLaunchAuthenticationHandler.java
