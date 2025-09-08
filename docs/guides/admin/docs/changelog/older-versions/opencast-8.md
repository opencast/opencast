Opencast 8 Changelog
--------------------

## Opencast 8.11

*Released on April 6th, 2021*

- [[#2418](https://github.com/opencast/opencast/pull/2418)] -
  Update Test Server Builds
- [[#2416](https://github.com/opencast/opencast/pull/2416)] -
  Restore "8.x specific fixes for the markdown generation code"
- [[#2415](https://github.com/opencast/opencast/pull/2415)] -
  Revert "8.x specific fixes for the markdown generation code"
- [[#2409](https://github.com/opencast/opencast/pull/2409)] -
  8.x specific fixes for the markdown generation code
- [[#2310](https://github.com/opencast/opencast/pull/2310)] -
  Gracefully handle missing Shibboleth User References
- [[#2261](https://github.com/opencast/opencast/pull/2261)] -
  Handle broken encoding profiles without killing the ComposerService
- [[#2253](https://github.com/opencast/opencast/pull/2253)] -
  Configure POST api/groups access correctly
- [[#2239](https://github.com/opencast/opencast/pull/2239)] -
  Fix Typo in Branding Properties
- [[#2214](https://github.com/opencast/opencast/pull/2214)] -
  Validate Ingested DublinCore Catalogs
- [[#2177](https://github.com/opencast/opencast/pull/2177)] -
  Fix Sorting Series by Creators
- [[#2071](https://github.com/opencast/opencast/pull/2071)] -
  Properly configure AWS S3 distribution on startup by default


## Opencast 8.10

*Released on December 23rd, 2020*

- [[#2160](https://github.com/opencast/opencast/pull/2160)] -
  Fix Ingest by Non-privileged User
- [[#2049](https://github.com/opencast/opencast/pull/2049)] -
  Endtime of segments fixed in the editor


## Opencast 8.9

*Released on December 8th, 2020*

- [[#2099](https://github.com/opencast/opencast/pull/2099)] -
  Support faster playback rates in paella video player
- [[#2087](https://github.com/opencast/opencast/pull/2087)] -
  Move from Travis CI to GitHub Actions (8.x)
- [[#2075](https://github.com/opencast/opencast/pull/2075)] -
  Reduce IO Load When Starting a Workflow
- [[#2068](https://github.com/opencast/opencast/pull/2068)] -
  JDK Support proposal (2020-11-11) documentation for 8.x
- [[#1988](https://github.com/opencast/opencast/pull/1988)] -
  #1987 Admin UI event start filter datepicker timezone patch


## Opencast 8.8

*Released on November 19th, 2020*

- [[#2075](https://github.com/opencast/opencast/pull/2075)] -
  Reduce IO Load When Starting a Workflow
- [[#2072](https://github.com/opencast/opencast/pull/2072)] -
  Update player.matomo.tracking.md
- [[#2067](https://github.com/opencast/opencast/pull/2067)] -
  Solves #2034 (Execute-once subprocess issue)
- [[#2051](https://github.com/opencast/opencast/pull/2051)] -
  Limit OpenMP Threads to Prevent Tesseract Blocking the System
- [[#2040](https://github.com/opencast/opencast/pull/2040)] -
  Drop user login log level
- [[#2020](https://github.com/opencast/opencast/pull/2020)] -
  Set the RFC 822 "Date" header field when sending an email (r/8.x)
- [[#2008](https://github.com/opencast/opencast/pull/2008)] -
  Speed up preview encoding
- [[#1988](https://github.com/opencast/opencast/pull/1988)] -
  #1987 Admin UI event start filter datepicker timezone patch
- [[#1954](https://github.com/opencast/opencast/pull/1954)] -
  Fix Paella Player assuming track is audio-only when it's actually video and audio
- [[#1894](https://github.com/opencast/opencast/pull/1894)] -
  Remove references to removed modules
- [[#1891](https://github.com/opencast/opencast/pull/1891)] -
  Creating a new series doesn't send user collections anymore
- [[#1887](https://github.com/opencast/opencast/pull/1887)] -
  Remove Dead OpenID Module


## Opencast 8.7

*Released on September 27th, 2020*

- [[#1851](https://github.com/opencast/opencast/pull/1851)] -
  Fix conflict checking for scheduled events in multitenant systems
- [[#1848](https://github.com/opencast/opencast/pull/1848)] -
  Fix capture agent dropdown menus
- [[#1837](https://github.com/opencast/opencast/pull/1837)] -
  Adding bugfix from #1668 to 8.x
- [[#1828](https://github.com/opencast/opencast/pull/1828)] -
  Recommend https
- [[#1827](https://github.com/opencast/opencast/pull/1827)] -
  Clean up basic configuration
- [[#1812](https://github.com/opencast/opencast/pull/1812)] -
  Missing ACL after asset upload
- [[#1786](https://github.com/opencast/opencast/pull/1786)] -
  Removed servicewarnings endpoint
- [[#1770](https://github.com/opencast/opencast/pull/1770)] -
  Paella player should only list http(s) URLs in the download plugin
- [[#1756](https://github.com/opencast/opencast/pull/1756)] -
  Change markdown CI checks to bash
- [[#1753](https://github.com/opencast/opencast/pull/1753)] -
  Admin interface event API logs on user error
- [[#1735](https://github.com/opencast/opencast/pull/1735)] -
  Added reloading of filters in users page

## Opencast 8.6

*Released on August 5th, 2020*

- [[#1744](https://github.com/opencast/opencast/pull/1744)] -
  Fix for issue 1616: User keep write permission on ACL template selection
- [[#1720](https://github.com/opencast/opencast/pull/1720)] -
  Corrected title of user delete button
- [[#1711](https://github.com/opencast/opencast/pull/1711)] -
  Fixes issue #1642: Drop-down menu does not disappear
- [[#1662](https://github.com/opencast/opencast/pull/1662)] -
  Update Studio from 2020-05-20 to 2020-06-25
- [[#1639](https://github.com/opencast/opencast/pull/1639)] -
  Don't raise a NPE when the workflow creator was deleted


## Opencast 8.5

*Released on June 15th, 2020*

- [[#1633](https://github.com/opencast/opencast/pull/1633)] -
  Using ConcurrentHashMap for synchronizing LTI user login
- [[#1622](https://github.com/opencast/opencast/pull/1622)] -
  Fix LTI Without Persistence
- [[#1620](https://github.com/opencast/opencast/pull/1620)] -
  Fix Formatting
- [[#1612](https://github.com/opencast/opencast/pull/1612)] -
  Use normal file appender instead of RandomAccessFile for logging
- [[#1611](https://github.com/opencast/opencast/pull/1611)] -
  Fix LDAP Debug Logging
- [[#1593](https://github.com/opencast/opencast/pull/1593)] -
  Update paella to 6.2.7

## Opencast 8.4

*Released on May 22nd, 2020*

- [[#1593](https://github.com/opencast/opencast/pull/1593)] -
  Update player Paella to 6.2.7
- [[#1592](https://github.com/opencast/opencast/pull/1592)] -
  Update Studio version to 2020-05-20
- [[#1581](https://github.com/opencast/opencast/pull/1581)] -
  Update Studio to 2020-05-14
- [[#1578](https://github.com/opencast/opencast/pull/1578)] -
  Add cutting to the default Studio workflow
- [[#1570](https://github.com/opencast/opencast/pull/1570)] -
  Partial import muxing fix
- [[#1568](https://github.com/opencast/opencast/pull/1568)] -
  Fix URL to Wowza documentation
- [[#1565](https://github.com/opencast/opencast/pull/1565)] -
  Avoids normal user to delete series with events when option series.hasEvents.delete.allow is set to false
- [[#1550](https://github.com/opencast/opencast/pull/1550)] -
  Be more lenient towards service failures
- [[#1543](https://github.com/opencast/opencast/pull/1543)] -
  403 Logout Option
- [[#1542](https://github.com/opencast/opencast/pull/1542)] -
  Allow Admin UI Users to Access /
- [[#1540](https://github.com/opencast/opencast/pull/1540)] -
  Studio workflow should archive prepared versioned of videos
- [[#1535](https://github.com/opencast/opencast/pull/1535)] -
  Opencast server node name should be optional
- [[#1534](https://github.com/opencast/opencast/pull/1534)] -
  Admin UI systems filters should be accessible by the corresponding roles
- [[#1532](https://github.com/opencast/opencast/pull/1532)] -
  Fix race condition when creating or updating user references from LTI
- [[#1516](https://github.com/opencast/opencast/pull/1516)] -
  Studio encoding profile performance improved
- [[#1515](https://github.com/opencast/opencast/pull/1515)] -
  Studio-upload workflow should generate search preview images
- [[#1509](https://github.com/opencast/opencast/pull/1509)] -
  Allow anonymous access to static Studio files
- [[#1508](https://github.com/opencast/opencast/pull/1508)] -
  Fixed a typo in the Theodul Config
- [[#1500](https://github.com/opencast/opencast/pull/1500)] -
  Adding documentation on how to use the multiserver docker-compose
- [[#1496](https://github.com/opencast/opencast/pull/1496)] -
  Fix Adaptive Encoding
- [[#1492](https://github.com/opencast/opencast/pull/1492)] -
  Update Studio (for bug fixes) and add docs for Studio
- [[#1488](https://github.com/opencast/opencast/pull/1488)] -
  Make encoding profiles support odd widths/heights
- [[#1479](https://github.com/opencast/opencast/pull/1479)] -
  Fix Theodul quality selection

## Opencast 8.3

*Released on March 26th, 2020*

- [[#1492](https://github.com/opencast/opencast/pull/1492)] -
  Update Studio (for bugfixes) and add docs for Studio
- [[#1489](https://github.com/opencast/opencast/pull/1489)] -
  Workaround early lti session timeout
- [[#1488](https://github.com/opencast/opencast/pull/1488)] -
  Make encoding profiles support odd widths/heights
- [[#1487](https://github.com/opencast/opencast/pull/1487)] -
  Fix Studio login redirect
- [[#1485](https://github.com/opencast/opencast/pull/1485)] -
  Custom Roles for LTI users
- [[#1483](https://github.com/opencast/opencast/pull/1483)] -
  Read published files direct from filesystem if possible  (completed)
- [[#1482](https://github.com/opencast/opencast/pull/1482)] -
  Create JpaUserReference for LTI user (backport)
- [[#1480](https://github.com/opencast/opencast/pull/1480)] -
  Preserve ACL On Workflow Errors
- [[#1479](https://github.com/opencast/opencast/pull/1479)] -
  Fix Theodul quality selection
- [[#1478](https://github.com/opencast/opencast/pull/1478)] -
  Studio upload optimized workflows
- [[#1476](https://github.com/opencast/opencast/pull/1476)] -
  Fix Studio Redirect discarding GET Parameters
- [[#1475](https://github.com/opencast/opencast/pull/1475)] -
  Fix Theodul Matomo plugin after configuration data structure change
- [[#1473](https://github.com/opencast/opencast/pull/1473)] -
  Move OC Studio configuration to `etc/ui-config` and update Studio
- [[#1468](https://github.com/opencast/opencast/pull/1468)] -
  Studio in admin-presentation


## Opencast 8.2

*Released on March 14th, 2020*

- [[#1458](https://github.com/opencast/opencast/pull/1458)] -
  Fix Image Extraction At Position Zero
- [[#1457](https://github.com/opencast/opencast/pull/1457)] -
  Image Extraction Without Stream Duration
- [[#1454](https://github.com/opencast/opencast/pull/1454)] -
  Fix HLS on iOS
- [[#1448](https://github.com/opencast/opencast/pull/1448)] -
  Adding link to community workflow repository to the markdown docs
- [[#1446](https://github.com/opencast/opencast/pull/1446)] -
  Disable 2 more instances of jmxremote param, #1445
- [[#1441](https://github.com/opencast/opencast/pull/1441)] -
  Remove databasemigration for Opencast 7
- [[#1436](https://github.com/opencast/opencast/pull/1436)] -
  integrate opencast studio
- [[#1433](https://github.com/opencast/opencast/pull/1433)] -
  Series ACLs not propagating to individual events
- [[#1414](https://github.com/opencast/opencast/pull/1414)] -
  Gracefully crash if there is no password stored
- [[#1409](https://github.com/opencast/opencast/pull/1409)] -
  Asset Upload Title
- [[#1408](https://github.com/opencast/opencast/pull/1408)] -
  Simplify Asset Upload Workflow
- [[#1399](https://github.com/opencast/opencast/pull/1399)] -
  Gracefully Fail Hash Verification
- [[#1364](https://github.com/opencast/opencast/pull/1364)] -
  Temination state service test
- [[#1359](https://github.com/opencast/opencast/pull/1359)] -
  Fix workflow dropdown in start task
- [[#1327](https://github.com/opencast/opencast/pull/1327)] -
  Video Segemntation On Short Videos
- [[#1301](https://github.com/opencast/opencast/pull/1301)] -
  Fix event delete with existing publications
- [[#1248](https://github.com/opencast/opencast/pull/1248)] -
  Fix conflict detection for non-admin users and for multiple events


## Opencast 8.1

*Released on January 29, 2020*

- [[#1341](https://github.com/opencast/opencast/pull/1341)] -
  Spring Framework Dependency Specification
- [[#1340](https://github.com/opencast/opencast/pull/1340)] -
  LDAP User Directory Dependencies
- [[#1339](https://github.com/opencast/opencast/pull/1339)] -
  Add Missing Karaf Features
- [[#1338](https://github.com/opencast/opencast/pull/1338)] -
  Sakai User Directory Dependencies
- [[#1328](https://github.com/opencast/opencast/pull/1328)] -
  AngularJS Components 1.7.9
- [[#1326](https://github.com/opencast/opencast/pull/1326)] -
  Fix Image Extraction From Short Videos
- [[#1321](https://github.com/opencast/opencast/pull/1321)] -
  Fix URL Parameters in Theodul Player
- [[#1300](https://github.com/opencast/opencast/pull/1300)] -
  Allow Root In Bower
- [[#1299](https://github.com/opencast/opencast/pull/1299)] -
  Fix AWS WOH OSGI dependencies
- [[#1266](https://github.com/opencast/opencast/pull/1266)] -
  Allow capture agent users to read properties of series

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


## Opencast 8.0

*Released on December 17, 2019*

- [[#1292](https://github.com/opencast/opencast/pull/1292)] -
  Release notes for Opencast 8.0
- [[#1290](https://github.com/opencast/opencast/pull/1290)] -
  Fix for MP3 with embedded image
- [[#1286](https://github.com/opencast/opencast/pull/1286)] -
  Fix Role For Assets Quick Access
- [[#1278](https://github.com/opencast/opencast/pull/1278)] -
  Editor Thumbnail Default
- [[#1274](https://github.com/opencast/opencast/pull/1274)] -
  Update Security Configuration
- [[#1269](https://github.com/opencast/opencast/pull/1269)] -
  Fix processing of odd video width
- [[#1256](https://github.com/opencast/opencast/pull/1256)] -
  Remove publishedhours default statistics provider
- [[#1245](https://github.com/opencast/opencast/pull/1245)] -
  AngularJS 1.7.9 Security Update
- [[#1216](https://github.com/opencast/opencast/pull/1216)] -
  Simplify Editor URL Signing
- [[#1212](https://github.com/opencast/opencast/pull/1212)] -
  Update paella player to 6.2.4
- [[#1207](https://github.com/opencast/opencast/pull/1207)] -
  Enable Browser Tests
- [[#1206](https://github.com/opencast/opencast/pull/1206)] -
  Temporarily Ignore Failing Test
- [[#1203](https://github.com/opencast/opencast/pull/1203)] -
  Warn about using H2
- [[#1202](https://github.com/opencast/opencast/pull/1202)] -
  Overhaul RPM Installation Guide
- [[#1199](https://github.com/opencast/opencast/pull/1199)] -
  Fix Crowdin Upload
- [[#1197](https://github.com/opencast/opencast/pull/1197)] -
  Fix Theodul Embed Configuration
- [[#1167](https://github.com/opencast/opencast/pull/1167)] -
  Migrate IBM Watson transcription to shared persistence
- [[#1153](https://github.com/opencast/opencast/pull/1153)] -
  Keep generated SMIL for partial tracks
- [[#1151](https://github.com/opencast/opencast/pull/1151)] -
  (#1008): Better crop detect test #1085
- [[#1146](https://github.com/opencast/opencast/pull/1146)] -
  Remove unnecessary global package-lock.json
- [[#1141](https://github.com/opencast/opencast/pull/1141)] -
  Consider file extension of uploaded asset
- [[#1134](https://github.com/opencast/opencast/pull/1134)] -
  Do not use stack-overflow logo
- [[#1131](https://github.com/opencast/opencast/pull/1131)] -
  Issue1123 TEMP FIX for Paella Player Build error
- [[#1110](https://github.com/opencast/opencast/pull/1110)] -
  Build failed on captions-impl tests for non english OS
- [[#1108](https://github.com/opencast/opencast/pull/1108)] -
  Fix external API versioning for EventsEndpoint
- [[#1103](https://github.com/opencast/opencast/pull/1103)] -
  Fix PostreSQL Support
- [[#1102](https://github.com/opencast/opencast/pull/1102)] -
  Clean-up Fast Testing Workflow
- [[#1101](https://github.com/opencast/opencast/pull/1101)] -
  Filter jobs by transcription service provider ID
- [[#1073](https://github.com/opencast/opencast/pull/1073)] -
  close esc function for new event and new series modals
- [[#1067](https://github.com/opencast/opencast/pull/1067)] -
  Publication Button show fix
- [[#1100](https://github.com/opencast/opencast/pull/1100)] -
  Player Scroll/Zoom Overlay
- [[#1098](https://github.com/opencast/opencast/pull/1098)] -
  Fix displaying tracks with no tags in player
- [[#1095](https://github.com/opencast/opencast/pull/1095)] -
  Add a new optional date_expected column to the transcription job table
- [[#1094](https://github.com/opencast/opencast/pull/1094)] -
  Smarter etc/ hints in documentation
- [[#1093](https://github.com/opencast/opencast/pull/1093)] -
  Provide access to file contents in the WFR
- [[#1091](https://github.com/opencast/opencast/pull/1091)] -
  Remove inaccurate url-pattern ${element_uri}
- [[#1090](https://github.com/opencast/opencast/pull/1090)] -
  Elasticsearch access_policy field increased in size
- [[#1086](https://github.com/opencast/opencast/pull/1086)] -
  Fix CI Builds (Crop Tests)
- [[#1084](https://github.com/opencast/opencast/pull/1084)] -
  Fix Player ID Parameter Parsing
- [[#1082](https://github.com/opencast/opencast/pull/1082)] -
  Docs readme extended.
- [[#1079](https://github.com/opencast/opencast/pull/1079)] -
  Remove Workflow Operations from Worker
- [[#1078](https://github.com/opencast/opencast/pull/1078)] -
  Fix database docs
- [[#1075](https://github.com/opencast/opencast/pull/1075)] -
  Remove State Mapping “Importing”
- [[#1074](https://github.com/opencast/opencast/pull/1074)] -
  Navbar icons toggle
- [[#1071](https://github.com/opencast/opencast/pull/1071)] -
  Fix Pull Request Template
- [[#1070](https://github.com/opencast/opencast/pull/1070)] -
  Temporarily Ignore Service Registry Test
- [[#1066](https://github.com/opencast/opencast/pull/1066)] -
  Major developer docs update
- [[#1065](https://github.com/opencast/opencast/pull/1065)] -
  Remove the RoleProvider.getRoles() method
- [[#1063](https://github.com/opencast/opencast/pull/1063)] -
  Only events with write access
- [[#1062](https://github.com/opencast/opencast/pull/1062)] -
  start on used port
- [[#1059](https://github.com/opencast/opencast/pull/1059)] -
  Hide Column `Stop` By Default
- [[#1058](https://github.com/opencast/opencast/pull/1058)] -
  Custom LTI Series Tool Styles
- [[#1057](https://github.com/opencast/opencast/pull/1057)] -
  Update ESLint
- [[#1055](https://github.com/opencast/opencast/pull/1055)] -
  Move to GitHub Issues
- [[#1053](https://github.com/opencast/opencast/pull/1053)] -
  Update mustache
- [[#1052](https://github.com/opencast/opencast/pull/1052)] -
  Update bootbox
- [[#1050](https://github.com/opencast/opencast/pull/1050)] -
  && MH-13425 - Feeds-Tab / adds a new tab in series properties.
- [[#1048](https://github.com/opencast/opencast/pull/1048)] -
  Add an optional build step to clean easily clean the frontend caches
- [[#1047](https://github.com/opencast/opencast/pull/1047)] -
  ServiceRegistry not updating database correctly when dispatching jobs
- [[#1044](https://github.com/opencast/opencast/pull/1044)] -
  clean node, node_modules and bower_components folders
- [[#1042](https://github.com/opencast/opencast/pull/1042)] -
  Update Admin Interface JS Test Libraries
- [[#1041](https://github.com/opencast/opencast/pull/1041)] -
  Update ESLint
- [[#1039](https://github.com/opencast/opencast/pull/1039)] -
  paella can filter which tracks to load depending on the user's device
- [[#1037](https://github.com/opencast/opencast/pull/1037)] -
  Update paella player to 6.2.0
- [[#1034](https://github.com/opencast/opencast/pull/1034)] -
  Update Translation Key for Published Hours
- [[#1033](https://github.com/opencast/opencast/pull/1033)] -
  Direct link to assets tab
- [[#1030](https://github.com/opencast/opencast/pull/1030)] -
  Configure max open files
- [[#1029](https://github.com/opencast/opencast/pull/1029)] -
  Update admin interface JS libraries
- [[#1028](https://github.com/opencast/opencast/pull/1028)] -
  Update Engage JS Libraries
- [[#1027](https://github.com/opencast/opencast/pull/1027)] -
  Update Markdownlint
- [[#1023](https://github.com/opencast/opencast/pull/1023)] -
  fix invisible icon for specific zoom level
- [[#1022](https://github.com/opencast/opencast/pull/1022)] -
  Automatic publication of streaming URLs
- [[#1021](https://github.com/opencast/opencast/pull/1021)] -
  Moving mediapackages needs to handle missing version information
- [[#1020](https://github.com/opencast/opencast/pull/1020)] -
  Logging
- [[#1016](https://github.com/opencast/opencast/pull/1016)] -
  Update Deprecated EqualsUtil.hash(…)
- [[#1015](https://github.com/opencast/opencast/pull/1015)] -
  IDEA Settings
- [[#1014](https://github.com/opencast/opencast/pull/1014)] -
  Don't start opencast on a used port
- [[#1009](https://github.com/opencast/opencast/pull/1009)] -
  Shell information for developer distribution
- [[#1008](https://github.com/opencast/opencast/pull/1008)] -
  Crop service
- [[#1007](https://github.com/opencast/opencast/pull/1007)] -
  Update several JS libraries
- [[#1006](https://github.com/opencast/opencast/pull/1006)] -
  Improve metadata handling in backend
- [[#1005](https://github.com/opencast/opencast/pull/1005)] -
  Fix dropdown menus
- [[#1004](https://github.com/opencast/opencast/pull/1004)] -
  eslint 6.1.0
- [[#1003](https://github.com/opencast/opencast/pull/1003)] -
  Update karma
- [[#1001](https://github.com/opencast/opencast/pull/1001)] -
  Access org properties from publish-configure WOH
- [[#998](https://github.com/opencast/opencast/pull/998)] -
  Concat Operation Graphics
- [[#997](https://github.com/opencast/opencast/pull/997)] -
  Update Development Process Documentation
- [[#996](https://github.com/opencast/opencast/pull/996)] -
  Update commons-text
- [[#995](https://github.com/opencast/opencast/pull/995)] -
  Composer Should Not Overwrite Files
- [[#994](https://github.com/opencast/opencast/pull/994)] -
  Added name of the configuration file where properties of login details are modified
- [[#992](https://github.com/opencast/opencast/pull/992)] -
  switch to compatible file type filter definitions
- [[#990](https://github.com/opencast/opencast/pull/990)] -
  Upgrade chromedriver
- [[#985](https://github.com/opencast/opencast/pull/985)] -
  Update grunt-concurrent
- [[#983](https://github.com/opencast/opencast/pull/983)] -
  Update ESLint
- [[#978](https://github.com/opencast/opencast/pull/978)] -
  Mh 13617 Duplicate encoding profiles for PrepareAV/SelectStreams
- [[#973](https://github.com/opencast/opencast/pull/973)] -
  Don't consider raw fields updated
- [[#972](https://github.com/opencast/opencast/pull/972)] -
  Improve setting values from dublin core catalog
- [[#971](https://github.com/opencast/opencast/pull/971)] -
  NOJIRA: Add `ALTER` to necessary MySQL permissions
- [[#970](https://github.com/opencast/opencast/pull/970)] -
  Fix hello-world modules
- [[#968](https://github.com/opencast/opencast/pull/968)] -
  Resolution Based, Conditional Encoding
- [[#967](https://github.com/opencast/opencast/pull/967)] -
  Introduce general CatalogUIAdapter
- [[#966](https://github.com/opencast/opencast/pull/966)] -
  Update frontend-maven-plugin
- [[#965](https://github.com/opencast/opencast/pull/965)] -
  Update Logger
- [[#964](https://github.com/opencast/opencast/pull/964)] -
  Update Checkstyle
- [[#963](https://github.com/opencast/opencast/pull/963)] -
  Update Paella Build Dependencies
- [[#962](https://github.com/opencast/opencast/pull/962)] -
  Update Chromedriver
- [[#961](https://github.com/opencast/opencast/pull/961)] -
  Update autoprefixer to 9.6.0
- [[#960](https://github.com/opencast/opencast/pull/960)] -
  Update Markdownlint
- [[#959](https://github.com/opencast/opencast/pull/959)] -
  Update Admin Interface Test Framework
- [[#957](https://github.com/opencast/opencast/pull/957)] -
  Clean-up Static Resource Servlet
- [[#956](https://github.com/opencast/opencast/pull/956)] -
  Re-introduce Prepare AV
- [[#954](https://github.com/opencast/opencast/pull/954)] -
  Fix bundle versions
- [[#952](https://github.com/opencast/opencast/pull/952)] -
  Cleanup workflows
- [[#951](https://github.com/opencast/opencast/pull/951)] -
  More Dependency Checks…
- [[#950](https://github.com/opencast/opencast/pull/950)] -
  Tag elements retrieved from asset manager
- [[#949](https://github.com/opencast/opencast/pull/949)] -
  Termination State Service to integrate with AWS AutoScaling Lifecycle
- [[#948](https://github.com/opencast/opencast/pull/948)] -
  add health-check endpoint
- [[#945](https://github.com/opencast/opencast/pull/945)] -
  -publication
- [[#943](https://github.com/opencast/opencast/pull/943)] -
  color "blue" for links in the admin ui
- [[#942](https://github.com/opencast/opencast/pull/942)] -
  Theodul player ui config
- [[#941](https://github.com/opencast/opencast/pull/941)] -
  More dependency fixes
- [[#937](https://github.com/opencast/opencast/pull/937)] -
  Workflow Condition Parser Location
- [[#936](https://github.com/opencast/opencast/pull/936)] -
  Drop distribution-service-streaming
- [[#935](https://github.com/opencast/opencast/pull/935)] -
  Drop Distribution “adminworker”
- [[#934](https://github.com/opencast/opencast/pull/934)] -
  Drop Migration Distribution
- [[#931](https://github.com/opencast/opencast/pull/931)] -
  Assembly Configuration
- [[#929](https://github.com/opencast/opencast/pull/929)] -
  Check dependencies at build time
- [[#928](https://github.com/opencast/opencast/pull/928)] -
  Admin Interface Browser Tests
- [[#927](https://github.com/opencast/opencast/pull/927)] -
  Metadata Transfer Operation
- [[#926](https://github.com/opencast/opencast/pull/926)] -
  Remove unused code
- [[#925](https://github.com/opencast/opencast/pull/925)] -
  Media Module Dependency Management
- [[#924](https://github.com/opencast/opencast/pull/924)] -
  Jettison Dependency Management
- [[#923](https://github.com/opencast/opencast/pull/923)] -
  Introduce ESLint to Media Module
- [[#922](https://github.com/opencast/opencast/pull/922)] -
  Support for exclusion pattern for URL signing
- [[#921](https://github.com/opencast/opencast/pull/921)] -
  Officially support URL signing keys that handle multiple URL prefixes
- [[#920](https://github.com/opencast/opencast/pull/920)] -
  Streaming Module Cleanup
- [[#919](https://github.com/opencast/opencast/pull/919)] -
  Fix dependencies for statistics- and workflow-condition-parser
- [[#918](https://github.com/opencast/opencast/pull/918)] -
  Remove module 'dataloader'
- [[#917](https://github.com/opencast/opencast/pull/917)] -
  Remove obviously unused classes
- [[#908](https://github.com/opencast/opencast/pull/908)] -
  Admin interface dependency update
- [[#906](https://github.com/opencast/opencast/pull/906)] -
  Media Module Configuration
- [[#899](https://github.com/opencast/opencast/pull/899)] -
  Fix Login Page
- [[#898](https://github.com/opencast/opencast/pull/898)] -
  Fix Spelling of Flavor
- [[#895](https://github.com/opencast/opencast/pull/895)] -
  Update Tesseract Code
- [[#894](https://github.com/opencast/opencast/pull/894)] -
  NOJIRA Speed up statistics api tests
- [[#893](https://github.com/opencast/opencast/pull/893)] -
  Dependency Fixes
- [[#892](https://github.com/opencast/opencast/pull/892)] -
  Drop Custom Logger Configuration
- [[#891](https://github.com/opencast/opencast/pull/891)] -
  Unnecessary LineReader
- [[#890](https://github.com/opencast/opencast/pull/890)] -
  NOJIRA: Remove statistics provider configs
- [[#889](https://github.com/opencast/opencast/pull/889)] -
  Limit accepted file types when uploading assets
- [[#887](https://github.com/opencast/opencast/pull/887)] -
  Collect and visualize published hours of video
- [[#885](https://github.com/opencast/opencast/pull/885)] -
  Rework workflow conditions, add string data type
- [[#883](https://github.com/opencast/opencast/pull/883)] -
  Remove inclusion of non-existent scripts in Admin UI
- [[#882](https://github.com/opencast/opencast/pull/882)] -
  Navigation of statistics broken
- [[#881](https://github.com/opencast/opencast/pull/881)] -
  JavaScript Dependency Management
- [[#880](https://github.com/opencast/opencast/pull/880)] -
  Improve icons and wording in video editor
- [[#879](https://github.com/opencast/opencast/pull/879)] -
  statistics csv export
- [[#876](https://github.com/opencast/opencast/pull/876)] -
  Add Hourly Data Resolution For Statistics
- [[#874](https://github.com/opencast/opencast/pull/874)] -
  Role support for workflows
- [[#872](https://github.com/opencast/opencast/pull/872)] -
  Remove pseudo-mechanism for workflow definition registration
- [[#869](https://github.com/opencast/opencast/pull/869)] -
  Remove unused method WorkflowDefinition.isPublished
- [[#865](https://github.com/opencast/opencast/pull/865)] -
  Empty node name causes exception
- [[#864](https://github.com/opencast/opencast/pull/864)] -
  Multitenancy support for workflows
- [[#863](https://github.com/opencast/opencast/pull/863)] -
  Improve URL signing performance
- [[#862](https://github.com/opencast/opencast/pull/862)] -
  add single step event deletion
- [[#861](https://github.com/opencast/opencast/pull/861)] -
  Add option to configure state mappings for workflows
- [[#860](https://github.com/opencast/opencast/pull/860)] -
  Remove unused fields from search index
- [[#858](https://github.com/opencast/opencast/pull/858)] -
  Improve navigation in video editor when zoom is active
- [[#857](https://github.com/opencast/opencast/pull/857)] -
  resume on past table page when leaving video editor
- [[#854](https://github.com/opencast/opencast/pull/854)] -
  move ingest-download Operation to worker
- [[#851](https://github.com/opencast/opencast/pull/851)] -
  Highlight main table rows on hover
- [[#850](https://github.com/opencast/opencast/pull/850)] -
  Add node name to host registration as a UI searchable alternative to hostname
- [[#849](https://github.com/opencast/opencast/pull/849)] -
  Upgrade Admin Interface Libraries (Including AngularJS)
- [[#848](https://github.com/opencast/opencast/pull/848)] -
  Remove method canLogin from interface User
- [[#847](https://github.com/opencast/opencast/pull/847)] -
  Fix License and Documentation Links
- [[#846](https://github.com/opencast/opencast/pull/846)] -
  Automatically Launch Logs for `dist-develop`
- [[#842](https://github.com/opencast/opencast/pull/842)] -
  Harmonizing the column names
- [[#841](https://github.com/opencast/opencast/pull/841)] -
  Expand log messages to add error detail
- [[#834](https://github.com/opencast/opencast/pull/834)] -
  Introduce basic statistics visualization capabilities
- [[#831](https://github.com/opencast/opencast/pull/831)] -
  userprovider for the d2l brightspace LMS
- [[#826](https://github.com/opencast/opencast/pull/826)] -
  url query string incorrect
- [[#825](https://github.com/opencast/opencast/pull/825)] -
  Remove leftover service
- [[#824](https://github.com/opencast/opencast/pull/824)] -
  Use Username In Workflows
- [[#823](https://github.com/opencast/opencast/pull/823)] -
  Automatic caption using Google speech to text api
- [[#816](https://github.com/opencast/opencast/pull/816)] -
  Change the default composer job load from 0.8 to 1.5
- [[#784](https://github.com/opencast/opencast/pull/784)] -
  Admin UI new event media upload progress bar
- [[#757](https://github.com/opencast/opencast/pull/757)] -
  Timelinepreviews process first one only