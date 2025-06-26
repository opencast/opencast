Opencast 3 Changelog
----------------------

## Opencast 3.7

*Released on Oct 16, 2018*

- [[MH-12982](https://opencast.jira.com/browse/MH-12982)] - 3.0 database upgrade error
- [[MH-13022](https://opencast.jira.com/browse/MH-13022)] - Fix LTI highly trusted keys being discarded
- [[MH-13034](https://opencast.jira.com/browse/MH-13034)] - Add lis_person_sourcedid back as LTI source field for the
  username
- [[MH-13082](https://opencast.jira.com/browse/MH-13082)] - Fix LTI security vulnerability and refactor LTI and OAuth
  classes
- [[MH-13152](https://opencast.jira.com/browse/MH-13152)] - Reduce Workflow Messages, backport of Lars fix for >=r/5.x
- [[MH-13156](https://opencast.jira.com/browse/MH-13156)] - Set the auth scheme to digest for inter-server
  communication


## Opencast 3.6

*Released on May 31, 2018*

- [[MH-12910]](https://opencast.jira.com/browse/MH-12910) - When switching between branches with different module
  naming schemes, the git tree is left unclean sometimes
- [[MH-12860]](https://opencast.jira.com/browse/MH-12860) - Opencast does not build at DEBUG logging level
- [[MH-12841]](https://opencast.jira.com/browse/MH-12841) - Opencast is ignoring permissions
- [[MH-12840]](https://opencast.jira.com/browse/MH-12840) - LTI user provider may allow LMS admins to become Opencast
  admins
- [[MH-12830]](https://opencast.jira.com/browse/MH-12830) - Fix mvn site generation
- [[MH-12743]](https://opencast.jira.com/browse/MH-12743) - OAIPMH-Republish-Operation tries to republish to ASW3
- [[MH-12441]](https://opencast.jira.com/browse/MH-12441) - Fix multi-server configuration docs and config details
- [[MH-12091]](https://opencast.jira.com/browse/MH-12091) - Create a Capture Agent digest user with its own role


## Opencast 3.5

*Released on February 6, 2018*

- [[MH-12620]](https://opencast.jira.com/browse/MH-12620) - Document ActiveMQ memory requirements
- [[MH-12606]](https://opencast.jira.com/browse/MH-12606) - Using "Start Task" with a workflow containing an embedded
  script in the configuration which somehow modifies the input parameters does not update those values properly
- [[MH-12582]](https://opencast.jira.com/browse/MH-12582) - Editor WOH should not encode videos unless it is strictly
  necessary (to save time and resources)
- [[MH-12495]](https://opencast.jira.com/browse/MH-12495) - Job dispatching with loads needs optimization
- [[MH-12487]](https://opencast.jira.com/browse/MH-12487) - Add job load settings to the default encoding profles
- [[MH-12399]](https://opencast.jira.com/browse/MH-12399) - Oaipmh Retract very slow


## Opencast 3.4

*Released on December 4, 2017*

- [[MH-12588]](https://opencast.jira.com/browse/MH-12588) - Stream Security Leaks Secrets
- [[MH-12587]](https://opencast.jira.com/browse/MH-12587) - ActiveMQ config ships with 3rd party tool enabled by default
- [[MH-12532]](https://opencast.jira.com/browse/MH-12532) - The bundle `workflow-workflowoperation` creates (and leaves)
  temporary files in`/tmp`
- [[MH-12516]](https://opencast.jira.com/browse/MH-12516) - Oversize job acceptance logic is incorrect
- [[MH-12505]](https://opencast.jira.com/browse/MH-12505) - composer operations need to set job load from profile load
  when creating jobs
- [[MH-12501]](https://opencast.jira.com/browse/MH-12501) - Incorrect logging in inbox scanner
- [[MH-12496]](https://opencast.jira.com/browse/MH-12496) - Feeds point to removed embed player
- [[MH-12494]](https://opencast.jira.com/browse/MH-12494) - JMX bean unregistration causing stack traces in unit tests
- [[MH-12478]](https://opencast.jira.com/browse/MH-12478) - Waveform filenames are not unique
- [[MH-12471]](https://opencast.jira.com/browse/MH-12471) - Workspace Cleaner Minor Fix
- [[MH-12464]](https://opencast.jira.com/browse/MH-12464) - Job dispatching can be slowed down excessively by host loads
  query
- [[MH-12439]](https://opencast.jira.com/browse/MH-12439) - WorkspaceCleaner Should Clean All Files
- [[MH-12437]](https://opencast.jira.com/browse/MH-12437) - Admin UI ng fails mvn clean install if the node_modules
  exists
- [[MH-12435]](https://opencast.jira.com/browse/MH-12435) - Race condition when workspace file deletion removes
  collection
- [[MH-12430]](https://opencast.jira.com/browse/MH-12430) - Update Crowdin translations for r/3.x
- [[MH-12422]](https://opencast.jira.com/browse/MH-12422) - Adjust documentation to new Crowdin Opencast project
- [[MH-12421]](https://opencast.jira.com/browse/MH-12421) - Job dispatching halts because of http connection hang
- [[MH-12415]](https://opencast.jira.com/browse/MH-12415) - Improve performance of /api/events?withpublications=true
- [[MH-12363]](https://opencast.jira.com/browse/MH-12363) - org.json.simple.parser.JSONParser is not thread safe
- [[MH-12000]](https://opencast.jira.com/browse/MH-12000) - Cross-tenant URL signing
- [[MH-11361]](https://opencast.jira.com/browse/MH-11361) - date in engage is the creation date, not the recording date
- [[MH-11042]](https://opencast.jira.com/browse/MH-11042) - Admin UI NG tests fail in +5:30 timezone


## Opencast 3.3

*Released on September 21, 2017*

- [[MH-12383]](https://opencast.jira.com/browse/MH-12383) - Upgrade/Unify Library Versions
- [[MH-12413]](https://opencast.jira.com/browse/MH-12413) - Don't present the user a previous/next item button if there
  is no previous/next item
- [[MH-12405]](https://opencast.jira.com/browse/MH-12405) - Catastrophic Oveload in Calendar generation
- [[MH-12400]](https://opencast.jira.com/browse/MH-12400) - Player: Embed Links disabled
- [[MH-12393]](https://opencast.jira.com/browse/MH-12393) - Retract workflow fails if run when a video is being played
  (with nfs storage)
- [[MH-12389]](https://opencast.jira.com/browse/MH-12389) - Set operation to failed when setting workflow to failed on
  exception path
- [[MH-12386]](https://opencast.jira.com/browse/MH-12386) - Update Postgresql Connector
- [[MH-12384]](https://opencast.jira.com/browse/MH-12384) - Catch possible NPE in FileSupport.delete()
- [[MH-12366]](https://opencast.jira.com/browse/MH-12366) - authorization-manager depends on download-impl
- [[MH-12365]](https://opencast.jira.com/browse/MH-12365) - Losing ActiveMQ connection spams the logs
- [[MH-12364]](https://opencast.jira.com/browse/MH-12364) - /broker/status endpoint returns incorrect 204 when ActiveMQ
  is shut down
- [[MH-12362]](https://opencast.jira.com/browse/MH-12362) - Less verbose logging for ExportWorkflowPropertiesWOH
- [[MH-12360]](https://opencast.jira.com/browse/MH-12360) - Race condition in workspace collection add and delete
- [[MH-12359]](https://opencast.jira.com/browse/MH-12359) - Milliseconds trim bug in videoeditor-workflowoperation
  formatTime() javaScript
- [[MH-12358]](https://opencast.jira.com/browse/MH-12358) - Only 6 series were displayed on the distribution node
- [[MH-12353]](https://opencast.jira.com/browse/MH-12353) - Theodul player does not load reliably after restart
- [[MH-12350]](https://opencast.jira.com/browse/MH-12350) - Recreate adminui-Index stops, if Asset of Event ist missing
- [[MH-12329]](https://opencast.jira.com/browse/MH-12329) - File copy can fail with jetty timeout
- [[MH-12326]](https://opencast.jira.com/browse/MH-12326) - Reduce log level for IllegalStateException in
  StaticResourceServlet
- [[MH-12317]](https://opencast.jira.com/browse/MH-12317) - AdminUI create every 5 seconds stats request and may crash
  on heavy server load
- [[MH-12303]](https://opencast.jira.com/browse/MH-12303) - Sort the REST endpoints alphabetically
- [[MH-12131]](https://opencast.jira.com/browse/MH-12131) - Migrate documentation of capture agent communication
  protocol to markdown
- [[MH-12085]](https://opencast.jira.com/browse/MH-12085) - Make file upload in Admin UI more flexible
- [[MH-11768]](https://opencast.jira.com/browse/MH-11768) - Timeline preview images


## Opencast 3.2

*Released on August 16, 2017*

- [[MH-12347]](https://opencast.jira.com/browse/MH-12347) - Opencast generates invalid XML catalogs when a "default"
  (empty) Namespace is used.
- [[MH-12345]](https://opencast.jira.com/browse/MH-12345) - Ingest fails because /recordings/{id}/acls returns 500 if
  event has not ACLs
- [[MH-12342]](https://opencast.jira.com/browse/MH-12342) - A "Scanner" instance in the ExecuteServiceImpl class is not
  properly closed: possible resource leak
- [[MH-12333]](https://opencast.jira.com/browse/MH-12333) - Feed generator separates lists of tags incorrectly
- [[MH-12327]](https://opencast.jira.com/browse/MH-12327) - CAS Authentication is not working
- [[MH-12324]](https://opencast.jira.com/browse/MH-12324) - Reduce frequency of index update messages for rebuilds
- [[MH-12318]](https://opencast.jira.com/browse/MH-12318) - Remove Webconsole Default Installation
- [[MH-12316]](https://opencast.jira.com/browse/MH-12316) - IllegalStateException: Committed
- [[MH-12315]](https://opencast.jira.com/browse/MH-12315) - Database Query of Users from UserlistProvider is very slow
- [[MH-12311]](https://opencast.jira.com/browse/MH-12311) - Update Admin UI build tools
- [[MH-12307]](https://opencast.jira.com/browse/MH-12307) - OAI-PMH REST endpoint docs fix
- [[MH-12305]](https://opencast.jira.com/browse/MH-12305) - Admin UI should stop polling event stats if the event tab
  isn't shown
- [[MH-12288]](https://opencast.jira.com/browse/MH-12288) - Set default max idle time if not configured and log key pool
  parameters
- [[MH-12280]](https://opencast.jira.com/browse/MH-12280) - Create an Opencast group for Sakai instructors
- [[MH-12278]](https://opencast.jira.com/browse/MH-12278) - NullPointerException in CleanupWorkflowOperationHandler
- [[MH-12275]](https://opencast.jira.com/browse/MH-12275) - MH-12261 / Avoid race condition between index and cleanup
  operations
- [[MH-12271]](https://opencast.jira.com/browse/MH-12271) - MH-12261 / Update WFR put action to update files atomically
- [[MH-12270]](https://opencast.jira.com/browse/MH-12270) - Don't swallow unknown SMIL exceptions
- [[MH-12263]](https://opencast.jira.com/browse/MH-12263) - MH-12261 / FileSupport > link - copy file action should use
  overwrite argument (Throws FileFileAlreadyExists)
- [[MH-12261]](https://opencast.jira.com/browse/MH-12261) - Race condition leads to FileAlreadyExistsException and
  FileNotFoundException
- [[MH-12079]](https://opencast.jira.com/browse/MH-12079) - Misleading logging in some indexing message receivers
- [[MH-12007]](https://opencast.jira.com/browse/MH-12007) - Revive the Execute Service
- [[MH-11542]](https://opencast.jira.com/browse/MH-11542) - Failed test: Process video after cutting (Safari)
- [[MH-10650]](https://opencast.jira.com/browse/MH-10650) - Intermittent failure to detect hard links when starting a
  cluster
- [[MH-10523]](https://opencast.jira.com/browse/MH-10523) - Misleading exception parameter in getFileFromCollection


## Opencast 3.1

*Released on July 14, 2017*

- [[MH-12296]](https://opencast.jira.com/browse/MH-12296) - getSeries Performance Issue
- [[MH-12295]](https://opencast.jira.com/browse/MH-12295) - Update Karaf to 4.0.9
- [[MH-12291]](https://opencast.jira.com/browse/MH-12291) - Remove obsolete Speech Recognition API
- [[MH-12279]](https://opencast.jira.com/browse/MH-12279) - As a user, I expect the video editor to correctly visualize
  the audio track
- [[MH-12253]](https://opencast.jira.com/browse/MH-12253) - Example workflows are inconsistent in Formatting and
  Configuration of Publication Options
- [[MH-12215]](https://opencast.jira.com/browse/MH-12215) - Extended metadata should be applied on event create wizard
- [[MH-12157]](https://opencast.jira.com/browse/MH-12157) - Series index query performs bad on system with many series
- [[MH-11742]](https://opencast.jira.com/browse/MH-11742) - Document criteria for inclusion and exclusion of
  translations


## Opencast 3.0

*Released on June 13, 2017*

- [[MH-12257]](https://opencast.jira.com/browse/MH-12257) - HttpsFilter is not called before
  OAuthProviderProcessingFilter
- [[MH-12255]](https://opencast.jira.com/browse/MH-12255) - OC cannot add PyCA capture agent when server ending with /
- [[MH-12252]](https://opencast.jira.com/browse/MH-12252) - LTI default launch goes to the wrong URL for sample tool
- [[MH-12249]](https://opencast.jira.com/browse/MH-12249) - Media Module: Paging forgets search parameters
- [[MH-12248]](https://opencast.jira.com/browse/MH-12248) - Capture Calendar Modification Caching Implementation is very
  Inefficient
- [[MH-12247]](https://opencast.jira.com/browse/MH-12247) - Archive Synchronization fix doesn't working in \>=2.3
- [[MH-12235]](https://opencast.jira.com/browse/MH-12235) - WOH partial-import: No track matching smil Track-id
- [[MH-12230]](https://opencast.jira.com/browse/MH-12230) - Notifications appear again although the user has closed them
- [[MH-12228]](https://opencast.jira.com/browse/MH-12228) - player controls: use dropup instead of a dropdown if
  controls are below the video
- [[MH-12226]](https://opencast.jira.com/browse/MH-12226) - Add documentation about configuration of publication channel
  names and icons
- [[MH-12222]](https://opencast.jira.com/browse/MH-12222) - As a user, I don't want an empty tab be presented to me
  since I don't necessarily understand, what that means
- [[MH-12221]](https://opencast.jira.com/browse/MH-12221) - As a user, I expect meaningful placeholder texts in the
  filter selection components
- [[MH-12213]](https://opencast.jira.com/browse/MH-12213) - Internal distribution fails if download url is not default
- [[MH-12211]](https://opencast.jira.com/browse/MH-12211) - As a service provider, I need to be able to deal with
  multiple users that have the same name
- [[MH-12207]](https://opencast.jira.com/browse/MH-12207) - Incorrect comment identifiers in some workflows
- [[MH-12205]](https://opencast.jira.com/browse/MH-12205) - Update version of javax.ws.rs - jsr311-api
- [[MH-12204]](https://opencast.jira.com/browse/MH-12204) - Rearrange the config
- [[MH-12202]](https://opencast.jira.com/browse/MH-12202) - ProxyMiddleware does ignore host port
- [[MH-12199]](https://opencast.jira.com/browse/MH-12199) - 3.x release notes mention "comprehensive" LDAP support,
  which is not (yet) true
- [[MH-12198]](https://opencast.jira.com/browse/MH-12198) - Remove outdated file location in LDAP documentation
- [[MH-12197]](https://opencast.jira.com/browse/MH-12197) - IllegalStateException: Response is committed
- [[MH-12195]](https://opencast.jira.com/browse/MH-12195) - Unprivileged users cannot view media package element details
  on Recordings-\>Events-\>"Event Details"-\>Assets-\>Media
- [[MH-12193]](https://opencast.jira.com/browse/MH-12193) - OAI-PMH distribution fails on adaptive streaming artifacts
- [[MH-12189]](https://opencast.jira.com/browse/MH-12189) - Sakai userdirectory provider is not properly bundled
- [[MH-12183]](https://opencast.jira.com/browse/MH-12183) - Theodul does not load
- [[MH-12181]](https://opencast.jira.com/browse/MH-12181) - As a course admin, I want to allow roles in the UI for ACLs
  that match a pattern
- [[MH-12180]](https://opencast.jira.com/browse/MH-12180) - Cannot specify ValuefFor probe-resolution woh
- [[MH-12174]](https://opencast.jira.com/browse/MH-12174) - The Admin UI temporarily displays wrong table content
  because data is not cleared upon page navigation
- [[MH-12173]](https://opencast.jira.com/browse/MH-12173) - The Admin UI temporarily displays wrong table content
  because data requests are not cancelled
- [[MH-12170]](https://opencast.jira.com/browse/MH-12170) - Safari does not display metadata once entered
- [[MH-12169]](https://opencast.jira.com/browse/MH-12169) - As a user, I expect search strings to match non-word
  boundaries in searchable dropdown lists
- [[MH-12167]](https://opencast.jira.com/browse/MH-12167) - As a user, I need to be able to search for values offered by
  the filters, so that I actually find the value I am looking for
- [[MH-12156]](https://opencast.jira.com/browse/MH-12156) - Fix version of matterhorn-engage-theodul-plugin-custom-piwik
- [[MH-12153]](https://opencast.jira.com/browse/MH-12153) - Reduce Database Space usage
- [[MH-12149]](https://opencast.jira.com/browse/MH-12149) - Upgrade Elastic Search to 1.7.6
- [[MH-12148]](https://opencast.jira.com/browse/MH-12148) - Undocumented Archive WOH Requirements
- [[MH-12147]](https://opencast.jira.com/browse/MH-12147) - TOC links in REST docs overlap
- [[MH-12142]](https://opencast.jira.com/browse/MH-12142) - As a system administrator, I would like a documented hint
  that the user running Opencast needs RW access to the optional storage directory
- [[MH-12141]](https://opencast.jira.com/browse/MH-12141) - As service provider, I want to restrict access granted to
  tenant administrators
- [[MH-12138]](https://opencast.jira.com/browse/MH-12138) - Added release notes
- [[MH-12137]](https://opencast.jira.com/browse/MH-12137) - AWS S3 tries to distribute attachments from OAI-PMH
  distribution
- [[MH-12133]](https://opencast.jira.com/browse/MH-12133) - OAI-PMH Tests Fails Regularly
- [[MH-12130]](https://opencast.jira.com/browse/MH-12130) - Filters set by selecting a category in the dashboard are not
  shown
- [[MH-12128]](https://opencast.jira.com/browse/MH-12128) - REST docs are too eager to check for a valid value
- [[MH-12126]](https://opencast.jira.com/browse/MH-12126) - Fast workflow needs AWS distribution to default to false.
- [[MH-12124]](https://opencast.jira.com/browse/MH-12124) - Cutting a video multiple times results in multiple
  smil/cutting catalogs
- [[MH-12121]](https://opencast.jira.com/browse/MH-12121) - Update grunt-ng-annotate to 3.0.0 and grunt-contrib-uglify
  to 2.2.0
- [[MH-12120]](https://opencast.jira.com/browse/MH-12120) - pub service oaipmh wants distribution api
- [[MH-12117]](https://opencast.jira.com/browse/MH-12117) - As an adopter I would like to get collect data with Piwik
- [[MH-12115]](https://opencast.jira.com/browse/MH-12115) - Republish Metadata to OAI-PMH fails
- [[MH-12113]](https://opencast.jira.com/browse/MH-12113) - Update outdated comment about the "lifecycle-mapping" plugin
  in the main pom.xml
- [[MH-12112]](https://opencast.jira.com/browse/MH-12112) - Update Node Version
- [[MH-12110]](https://opencast.jira.com/browse/MH-12110) - frontend-maven-plugin is executed on every module
- [[MH-12109]](https://opencast.jira.com/browse/MH-12109) - Creating comments does not work anymore
- [[MH-12108]](https://opencast.jira.com/browse/MH-12108) - Set Workflow Variables Based On Resolution
- [[MH-12104]](https://opencast.jira.com/browse/MH-12104) - As a producer, I want to access assets of my tenant while a
  workflow is running
- [[MH-12103]](https://opencast.jira.com/browse/MH-12103) - As a producer, I want to be able to execute WOH
  partial-import on archived sources
- [[MH-12102]](https://opencast.jira.com/browse/MH-12102) - Add Workflow Variables Based On Media Properties
- [[MH-12084]](https://opencast.jira.com/browse/MH-12084) - The class "AsyncTimeoutRedirectFilter" swallows almost all
  the exceptions
- [[MH-12074]](https://opencast.jira.com/browse/MH-12074) - Remove workflow MissedCaptureScanner and MissedIngestScanner
- [[MH-12073]](https://opencast.jira.com/browse/MH-12073) - Typo in rest\_docs entry box
- [[MH-12070]](https://opencast.jira.com/browse/MH-12070) - Order the event counters to reflect the event lifecycle
- [[MH-12067]](https://opencast.jira.com/browse/MH-12067) - Initial REST Docs Search
- [[MH-12066]](https://opencast.jira.com/browse/MH-12066) - Missing feature.xml Installation
- [[MH-12065]](https://opencast.jira.com/browse/MH-12065) - Fix bundle info REST endpoint description
- [[MH-12064]](https://opencast.jira.com/browse/MH-12064) - Handle missing meta.abstract gracefully
- [[MH-12060]](https://opencast.jira.com/browse/MH-12060) - Simplify Default WOH
- [[MH-12056]](https://opencast.jira.com/browse/MH-12056) - As an Administrator, I'd like to add some custom roles for
  managing access
- [[MH-12055]](https://opencast.jira.com/browse/MH-12055) - Update REST Documentation Template
- [[MH-12054]](https://opencast.jira.com/browse/MH-12054) - Incorrect or misleading documentation about WOH conditional
  execution
- [[MH-12049]](https://opencast.jira.com/browse/MH-12049) - Update REST Documentation Overview
- [[MH-12043]](https://opencast.jira.com/browse/MH-12043) - Allow more then one additional authentication algorithms
  beside digest
- [[MH-12038]](https://opencast.jira.com/browse/MH-12038) - Fallback decoding for mediapackage date values in unixtime
  rather than W3CDTF
- [[MH-12037]](https://opencast.jira.com/browse/MH-12037) - NullPoiinterException when starting embedded Solr
- [[MH-12035]](https://opencast.jira.com/browse/MH-12035) - Setting Default Download Directory
- [[MH-12034]](https://opencast.jira.com/browse/MH-12034) - Make the UserAndRoleDirectoryService cache configurable
- [[MH-12033]](https://opencast.jira.com/browse/MH-12033) - Add indicator lights for capture agent status
- [[MH-12032]](https://opencast.jira.com/browse/MH-12032) - Add an authenticated ACL template
- [[MH-12031]](https://opencast.jira.com/browse/MH-12031) - Add additional docs for inspection WOH
- [[MH-12029]](https://opencast.jira.com/browse/MH-12029) - As a user, I want to use my existing AAI login for Opencast,
  too
- [[MH-12023]](https://opencast.jira.com/browse/MH-12023) - Make development builds faster
- [[MH-12022]](https://opencast.jira.com/browse/MH-12022) - /ingest/addTrackURL broken
- [[MH-12019]](https://opencast.jira.com/browse/MH-12019) - Ensure Test Files Are Deleted
- [[MH-12017]](https://opencast.jira.com/browse/MH-12017) - CoverImage WOH should provide metadata for recording
  start/end time
- [[MH-12016]](https://opencast.jira.com/browse/MH-12016) - Fix and improve user, group, role and provider handling
- [[MH-12015]](https://opencast.jira.com/browse/MH-12015) - Typo in External API role name
- [[MH-12014]](https://opencast.jira.com/browse/MH-12014) - Incorrect number of roles returned when limit is specified
- [[MH-12013]](https://opencast.jira.com/browse/MH-12013) - Contribute OAI-PMH work (ETH)
- [[MH-12002]](https://opencast.jira.com/browse/MH-12002) - Date & time format should be customizable in cover images
- [[MH-11994]](https://opencast.jira.com/browse/MH-11994) - UserIdRoleProvider should check user existence from user
  providers
- [[MH-11993]](https://opencast.jira.com/browse/MH-11993) - WOH partial-import should support output framerate
- [[MH-11990]](https://opencast.jira.com/browse/MH-11990) - Remove configuration file of removed module
  matterhorn-load-test
- [[MH-11982]](https://opencast.jira.com/browse/MH-11982) - As an Opencast administrator, I would like a dashboard
  counter for active recordings
- [[MH-11979]](https://opencast.jira.com/browse/MH-11979) - The video editor does not highlight the selected segment if
  it is cut
- [[MH-11978]](https://opencast.jira.com/browse/MH-11978) - Hotkeys for common tasks in Admin UI
- [[MH-11977]](https://opencast.jira.com/browse/MH-11977) - Remove Unused OSGI Bindings From IndexService
- [[MH-11976]](https://opencast.jira.com/browse/MH-11976) - Adjust DownloadDistribution Logs
- [[MH-11975]](https://opencast.jira.com/browse/MH-11975) - Update some maven plugins
- [[MH-11971]](https://opencast.jira.com/browse/MH-11971) - Update maven-surfire-test plugin to latest version
- [[MH-11969]](https://opencast.jira.com/browse/MH-11969) - Fullscreen button in embedded view of Theodul player missing
  after update to 2.2.4
- [[MH-11967]](https://opencast.jira.com/browse/MH-11967) - Publish internal fails on Distrubuted System Admin/Engage
- [[MH-11965]](https://opencast.jira.com/browse/MH-11965) - Update to Karaf 4.0.8
- [[MH-11957]](https://opencast.jira.com/browse/MH-11957) - Make availability check of WOH publish-configure
  configurable
- [[MH-11956]](https://opencast.jira.com/browse/MH-11956) - Allow fine-grained control of accurate frame count
- [[MH-11954]](https://opencast.jira.com/browse/MH-11954) - Fixing Javadoc Build
- [[MH-11952]](https://opencast.jira.com/browse/MH-11952) - HTML in Translations
- [[MH-11944]](https://opencast.jira.com/browse/MH-11944) - MH-11817 use keyboard shortcuts to control the editor
- [[MH-11916]](https://opencast.jira.com/browse/MH-11916) - Add convenience workflow instance variable to indicate
  whether a theme is involved
- [[MH-11910]](https://opencast.jira.com/browse/MH-11910) - WOH composite should be able to respect resolution of its
  input
- [[MH-11904]](https://opencast.jira.com/browse/MH-11904) - Missing IDClass Warnings
- [[MH-11903]](https://opencast.jira.com/browse/MH-11903) - Cannot Configure Authentication For Webconsole
- [[MH-11902]](https://opencast.jira.com/browse/MH-11902) - Update to latest 5.x MySQL connector
- [[MH-11894]](https://opencast.jira.com/browse/MH-11894) - Suppress context menu on video element
- [[MH-11885]](https://opencast.jira.com/browse/MH-11885) - Add support for search and filtering to
  Organization-\>Access Policies
- [[MH-11881]](https://opencast.jira.com/browse/MH-11881) - ArchiveRestEndpoint has conflicting endpoints
- [[MH-11880]](https://opencast.jira.com/browse/MH-11880) - Multiple issues with LDAP in branch 2.3.x
- [[MH-11873]](https://opencast.jira.com/browse/MH-11873) - org.ops4j.pax.web.pax-web-extender-whiteboard causes
  exception when shutting down
- [[MH-11868]](https://opencast.jira.com/browse/MH-11868) - redesign loginpages
- [[MH-11861]](https://opencast.jira.com/browse/MH-11861) - MH-11817 Change default view to editor in admin ui tools
  area
- [[MH-11849]](https://opencast.jira.com/browse/MH-11849) - Edit metadata fields by click inside and focus cursor in
  field
- [[MH-11822]](https://opencast.jira.com/browse/MH-11822) - Admin UI Video Editor - Improved Segment Controls
- [[MH-11821]](https://opencast.jira.com/browse/MH-11821) - Admin UI Video Editor - Comment and Metadata Editing
- [[MH-11818]](https://opencast.jira.com/browse/MH-11818) - Admin UI Video Editor - Improved playback and timeline
- [[MH-11806]](https://opencast.jira.com/browse/MH-11806) - Output Frame Rate on Concat Operation
- [[MH-11797]](https://opencast.jira.com/browse/MH-11797) - Upgrade Karaf to 4.0.6
- [[MH-11796]](https://opencast.jira.com/browse/MH-11796) - Add support for watermarks to themes
- [[MH-11782]](https://opencast.jira.com/browse/MH-11782) - MH-11780 Create configure-by-dcterm workflow operation
  handler
- [[MH-11781]](https://opencast.jira.com/browse/MH-11781) - MH-11780 Create tag-by-dcterm workflow operation handler
- [[MH-11780]](https://opencast.jira.com/browse/MH-11780) - As a developer I want to be able to manipulate a workflow
  based on metadata in the Mediapackage
- [[MH-11766]](https://opencast.jira.com/browse/MH-11766) - enhance REST Ingest/addTrack Ingest/addCatalog
  Ingest/AddAttachment to add tags
- [[MH-11761]](https://opencast.jira.com/browse/MH-11761) - Captions for player
- [[MH-11732]](https://opencast.jira.com/browse/MH-11732) - Make distribution and retraction efficient
- [[MH-11719]](https://opencast.jira.com/browse/MH-11719) - When configuring LDAP with default file things are broken
- [[MH-11717]](https://opencast.jira.com/browse/MH-11717) - MH-11713 Not possible to add external roles to an ACL
  through the admin UI
- [[MH-11715]](https://opencast.jira.com/browse/MH-11715) - MH-11713 Externally provisioned roles should not be
  persisted
- [[MH-11713]](https://opencast.jira.com/browse/MH-11713) - Users may have roles in Opencast which are granted from an
  external system (e.g. LMS)
- [[MH-11684]](https://opencast.jira.com/browse/MH-11684) - WOH silence does not support tags
- [[MH-11474]](https://opencast.jira.com/browse/MH-11474) - Assigning a user to a certain "ROLE\_GROUP\_<name\>" role
  does not really put the user in such group
- [[MH-11466]](https://opencast.jira.com/browse/MH-11466) - Improve handling of long strings in cover images
- [[MH-11379]](https://opencast.jira.com/browse/MH-11379) - Service to distribute delivery files to AWS S3
- [[MH-11229]](https://opencast.jira.com/browse/MH-11229) - workflowoperation unit tests are incredible slow
- [[MH-11036]](https://opencast.jira.com/browse/MH-11036) - Adapt Fast Testing Workflow for Admin NG
- [[MH-10871]](https://opencast.jira.com/browse/MH-10871) - Sakai User Provider for Opencast-Sakai integration
- [[MH-10819]](https://opencast.jira.com/browse/MH-10819) - When creating a new event, metadata field can only be edited
  by clicking on the pencil icon
- [[MH-10753]](https://opencast.jira.com/browse/MH-10753) - Stale database connection causes job failure
- [[MH-10310]](https://opencast.jira.com/browse/MH-10310) - Add ERROR state for capture agent