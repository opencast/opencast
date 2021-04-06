Changelog
=========

Opencast 8
----------

### Opencast 8.11

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


### Opencast 8.10

*Released on December 23rd, 2020*

- [[#2160](https://github.com/opencast/opencast/pull/2160)] -
  Fix Ingest by Non-privileged User
- [[#2049](https://github.com/opencast/opencast/pull/2049)] -
  Endtime of segments fixed in the editor


### Opencast 8.9

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


### Opencast 8.8

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


### Opencast 8.7

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

### Opencast 8.6

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


### Opencast 8.5

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

### Opencast 8.4

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

### Opencast 8.3

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


### Opencast 8.2

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


### Opencast 8.1

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

#### Fixed Security Issues

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


### Opencast 8.0

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

Opencast 7
----------

### Opencast 7.9

*Released on December 8, 2020*

- [[GHSA-44cw-p2hm-gpf6](https://github.com/opencast/opencast/security/advisories/GHSA-44cw-p2hm-gpf6)]
  Security: Disabled Hostname Verification
- [[#1964](https://github.com/opencast/opencast/pull/1964)] -
  Docs: When the sidebar is hidden, the navigation links are disabled now
- [[#1922](https://github.com/opencast/opencast/pull/1922)] -
  Remove Spring snapshot repository from main pom

### Opencast 7.8

*Released on August 17, 2020*

- [[#1667](https://github.com/opencast/opencast/pull/1667)] -
  Fix managed ACL filter in API
- [[#1659](https://github.com/opencast/opencast/pull/1659)] -
  Updating Guava version
- [[#1657](https://github.com/opencast/opencast/pull/1657)] -
  Fixed the video outline in the Theodul Player
- [[#1641](https://github.com/opencast/opencast/pull/1641)] -
  Capture Agent Authentication Method
- [[#1595](https://github.com/opencast/opencast/pull/1595)] -
  Gracefully Handle Missing Timeline Preview
- [[#1594](https://github.com/opencast/opencast/pull/1594)] -
  Fix Calculation of Aspect Ratio
- [[#1589](https://github.com/opencast/opencast/pull/1589)] -
  Engage: Theodul: Audio: Refer to correct items

### Opencast 7.7

*Released on April 27, 2020*

- [[#1539](https://github.com/opencast/opencast/pull/1539)] -
  Fix Karma Safari test in mac OS
- [[#1474](https://github.com/opencast/opencast/pull/1474)] -
  Add comment to document remember me keys
- [[#1442](https://github.com/opencast/opencast/pull/1442)] -
  Fix Remember-Me Authentication
- [[#1427](https://github.com/opencast/opencast/pull/1427)] -
  1281: Ignoring test which randomly fails in CI testing
- [[#1426](https://github.com/opencast/opencast/pull/1426)] -
  Autodetect browsers for Admin UI tests, fix phantomJS OpenSSL issue
- [[#1425](https://github.com/opencast/opencast/pull/1425)] -
  Don't duplicate publications
- [[#1380](https://github.com/opencast/opencast/pull/1380)] -
  In fast, don't cleanup ACLs (see other workflows)
- [[#1379](https://github.com/opencast/opencast/pull/1379)] -
  Use active, not episode ACL in scheduler service

### Opencast 7.6

*Released on January 29, 2020*

- [CVE-2020-5231](https://github.com/opencast/opencast/security/advisories/GHSA-94qw-r73x-j7hg) -
   Users with ROLE\_COURSE\_ADMIN can create new users
- [CVE-2020-5206](https://github.com/opencast/opencast/security/advisories/GHSA-vmm6-w4cf-7f3x) -
   Authentication Bypass For Endpoints With Anonymous Access
- [CVE-2020-5222](https://github.com/opencast/opencast/security/advisories/GHSA-mh8g-hprg-8363) -
   Hard-Coded Key Used For Remember-me Token
- [CVE-2020-5230](https://github.com/opencast/opencast/security/advisories/GHSA-w29m-fjp4-qhmq) -
   Unsafe Identifiers
- [CVE-2020-5228](https://github.com/opencast/opencast/security/advisories/GHSA-6f54-3qr9-pjgj) -
   Public Access Via OAI-PMH
- [[#1358](https://github.com/opencast/opencast/pull/1358)] -
  Switch To HTTPS Maven Repository
- [[#1353](https://github.com/opencast/opencast/pull/1353)] -
  Handle empty fields from REST docs in EventHttpServletRequest
- [[#1352](https://github.com/opencast/opencast/pull/1352)] -
  Remove unsafe option in ffmpeg command for SMIL processing
- [[#1343](https://github.com/opencast/opencast/pull/1343)] -
  Fixes Admin-UI Presenter's column
- [[#1333](https://github.com/opencast/opencast/pull/1333)] -
  Switch to mvn.opencast.org
- [[#1329](https://github.com/opencast/opencast/pull/1329)] -
  Remove Spring Request Logger Configuration
- [[#1325](https://github.com/opencast/opencast/pull/1325)] -
  Secure FPS For SMIL Processing
- [[#1318](https://github.com/opencast/opencast/pull/1318)] -
  Remove Custom Plugin Repositories
- [[#1315](https://github.com/opencast/opencast/pull/1315)] -
  Bump spring-security-oauth from 2.3.6.RELEASE to 2.3.7.RELEASE
- [[#1276](https://github.com/opencast/opencast/pull/1276)] -
  Don't add the internal publication of the original event twice
- [[#1271](https://github.com/opencast/opencast/pull/1271)] -
  Wrong encoding in video editor zoom box
- [[#1270](https://github.com/opencast/opencast/pull/1270)] -
  S3 Distribution Fails
- [[#1265](https://github.com/opencast/opencast/pull/1265)] -
  Some error operations referencing the wrong error-handler.
- [[#1246](https://github.com/opencast/opencast/pull/1246)] -
  Remove default storage_id setting from asset Manager

### Opencast 7.5

*Released on December 10, 2019*

- [[#1233](https://github.com/opencast/opencast/pull/1233)] -
  Change bibliographicdate if technicaldate is changed.
- [[#1220](https://github.com/opencast/opencast/pull/1220)] -
  Make Thumbnail Optional
- [[#1218](https://github.com/opencast/opencast/pull/1218)] -
  [Documentation] Added path hint to upgrade.md
- [[#1170](https://github.com/opencast/opencast/pull/1170)] -
  MH-9753: Prepare AV WOH can throw a NPE
- [[#1164](https://github.com/opencast/opencast/pull/1164)] -
  CentOS basic installation guide rewording
- [[#1148](https://github.com/opencast/opencast/pull/1148)] -
  VideoEditorServiceImpl: Fixed the file extension duplication with removeExtention from FilenameUtils.
- [[#1122](https://github.com/opencast/opencast/pull/1122)] -
  fixes #1069 workflow tab shows scheduling info instead of workflows

### Opencast 7.4

*Released on October 02, 2019*

- [[MH-13517](https://opencast.jira.com/browse/MH-13517)][[#1092](https://github.com/opencast/opencast/pull/1092)] -
  Set an absolute limit on Solr query size
- [[MH-13476](https://opencast.jira.com/browse/MH-13476)][[#1088](https://github.com/opencast/opencast/pull/1088)] -
  Filter capture agent roles for ACLs
- [[#1087](https://github.com/opencast/opencast/pull/1087)] -
  Issue 1068, Stop job dispatcher before unregistering hosts, junit MH-13675
- [[MH-13706](https://opencast.jira.com/browse/MH-13706)][[#1072](https://github.com/opencast/opencast/pull/1072)] -
  fix the date cell of the events overview table in the admin UI
- [[#1056](https://github.com/opencast/opencast/pull/1056)] -
  NOISSUE: CAS security example is very out of date

### Opencast 7.3

*Released on September 19, 2019*

- [[MH-13716](https://opencast.jira.com/browse/MH-13716)][[#1061](https://github.com/opencast/opencast/pull/1061)] -
  Update xmlsec
- [[MH-13715](https://opencast.jira.com/browse/MH-13715)][[#1060](https://github.com/opencast/opencast/pull/1060)] -
  Check Markdown for newline character
- [[#1056](https://github.com/opencast/opencast/pull/1056)] -
  CAS security example is very out of date
- [[MH-13707](https://opencast.jira.com/browse/MH-13707)][[#1051](https://github.com/opencast/opencast/pull/1051)] -
  Watermark missing
- [[MH-13706](https://opencast.jira.com/browse/MH-13706)][[#1049](https://github.com/opencast/opencast/pull/1049)] -
  Show bibliographic event dates on the events overview page
- [[MH-13701](https://opencast.jira.com/browse/MH-13701)][[#1040](https://github.com/opencast/opencast/pull/1040)] -
  Interpret source-audio-name correctly for composite operation
- [[MH-13699](https://opencast.jira.com/browse/MH-13699)][[#1038](https://github.com/opencast/opencast/pull/1038)] -
  Fix Workflow Index Rebuild ACL Handling
- [[MH-13697](https://opencast.jira.com/browse/MH-13697)][[#1036](https://github.com/opencast/opencast/pull/1036)] -
  Workflow Index Rebuild Memory
- [[MH-13684](https://opencast.jira.com/browse/MH-13684)][[#1024](https://github.com/opencast/opencast/pull/1024)] -
  Do not include auth token in republished URLs
- [[MH-12533](https://opencast.jira.com/browse/MH-12533)][[#714](https://github.com/opencast/opencast/pull/714)] -
  Re-introduce ability to avoid data loss during ingest

### Opencast 7.2

*Released on August 02, 2019*

- [[MH-13662](https://opencast.jira.com/browse/MH-13662)][[#1000](https://github.com/opencast/opencast/pull/1000)] -
  Update LTI Information

### Opencast 7.1

*Released on July 09, 2019*

- [[MH-13656](https://opencast.jira.com/browse/MH-13656)][[#993](https://github.com/opencast/opencast/pull/993)] -
  Fix Scheduler Index Rebuild
- [[MH-13655](https://opencast.jira.com/browse/MH-13655)][[#991](https://github.com/opencast/opencast/pull/991)] -
  Scheduler Message Logging
- [[MH-13653](https://opencast.jira.com/browse/MH-13653)][[#989](https://github.com/opencast/opencast/pull/989)] -
  Fully Migrate Scheduled Events
- [[MH-13652](https://opencast.jira.com/browse/MH-13652)][[#988](https://github.com/opencast/opencast/pull/988)] -
  Don't save unchanged values in dropdown menus
- [[MH-13651](https://opencast.jira.com/browse/MH-13651)][[#987](https://github.com/opencast/opencast/pull/987)] -
  Don't call submit of SingleSelect twice
- [[MH-13650](https://opencast.jira.com/browse/MH-13650)][[#986](https://github.com/opencast/opencast/pull/986)] -
  Scheduler Migration Performance
- [[MH-13646](https://opencast.jira.com/browse/MH-13646)][[#982](https://github.com/opencast/opencast/pull/982)] -
  Delete scheduled events
- [[MH-13645](https://opencast.jira.com/browse/MH-13645)][[#981](https://github.com/opencast/opencast/pull/981)] -
  Only send delete comments message if we delete something
- [[MH-13642](https://opencast.jira.com/browse/MH-13642)][[#977](https://github.com/opencast/opencast/pull/977)] -
  Fix Index Update Logging
- [[MH-13639](https://opencast.jira.com/browse/MH-13639)][[#976](https://github.com/opencast/opencast/pull/976)] -
  Admin interface does not handle missing metadata well
- [[MH-13638](https://opencast.jira.com/browse/MH-13638)][[#975](https://github.com/opencast/opencast/pull/975)] -
  Update NPM
- [[MH-13619](https://opencast.jira.com/browse/MH-13619)][[#958](https://github.com/opencast/opencast/pull/958)] -
  Fix Logging in Video Segmenter
- [[MH-13615](https://opencast.jira.com/browse/MH-13615)][[#953](https://github.com/opencast/opencast/pull/953)] -
  Fix Italian Translation
- [[MH-13610](https://opencast.jira.com/browse/MH-13610)][[#947](https://github.com/opencast/opencast/pull/947)] -
  LDAP User Directory Fixes

### Opencast 7.0

*Released on June 13, 2019*

- [[MH-13615](https://opencast.jira.com/browse/MH-13615)][[#953](https://github.com/opencast/opencast/pull/953)] -
  Fix Italian Translation
- [[MH-13602](https://opencast.jira.com/browse/MH-13602)][[#940](https://github.com/opencast/opencast/pull/940)] -
  Update jackson-databind to fix CVE-2019-12086
- [[MH-13599](https://opencast.jira.com/browse/MH-13599)][[#938](https://github.com/opencast/opencast/pull/938)] -
  Select well supported mime type by default
- [[MH-13593](https://opencast.jira.com/browse/MH-13593)][[#933](https://github.com/opencast/opencast/pull/933)] -
  Incorrect default waveform colors
- [[MH-13569](https://opencast.jira.com/browse/MH-13569)][[#913](https://github.com/opencast/opencast/pull/913)] -
  Change of PlayerRedirection variable from {{id}} to #{id}
- [[MH-13568](https://opencast.jira.com/browse/MH-13568)][[#911](https://github.com/opencast/opencast/pull/911)] -
  Catch exception from overlapping RRule and return bad request
- [[MH-13566](https://opencast.jira.com/browse/MH-13566)][[#910](https://github.com/opencast/opencast/pull/910)] -
  Accept duration as either string or number in scheduling JSON
- [[MH-13385](https://opencast.jira.com/browse/MH-13385)][[#909](https://github.com/opencast/opencast/pull/909)] -
  Add release note about URL signing configuration changes
- [[MH-13375](https://opencast.jira.com/browse/MH-13375)][[#907](https://github.com/opencast/opencast/pull/907)] -
  Handle empty-range errors correctly
- [[MH-13563](https://opencast.jira.com/browse/MH-13563)][[#905](https://github.com/opencast/opencast/pull/905)] -
  Duplicated Variables in Media Module
- [[MH-13562](https://opencast.jira.com/browse/MH-13562)][[#904](https://github.com/opencast/opencast/pull/904)] -
  ReferenceError in Media Module
- [[MH-13561](https://opencast.jira.com/browse/MH-13561)][[#903](https://github.com/opencast/opencast/pull/903)] -
  Access to UI Configuration
- [[MH-13558](https://opencast.jira.com/browse/MH-13558)][[#900](https://github.com/opencast/opencast/pull/900)] -
  Paella Track Filter
- [[MH-13554](https://opencast.jira.com/browse/MH-13554)][[#897](https://github.com/opencast/opencast/pull/897)] -
  Theodul Zoom
- [[MH-13553](https://opencast.jira.com/browse/MH-13553)][[#896](https://github.com/opencast/opencast/pull/896)] -
  Fix Paella Track Selection
- [[MH-13538](https://opencast.jira.com/browse/MH-13538)][[#878](https://github.com/opencast/opencast/pull/878)] -
  Update jQuery
- [[MH-13531](https://opencast.jira.com/browse/MH-13531)][[#873](https://github.com/opencast/opencast/pull/873)] -
  upgrade spring-security and jasig cas library to fix issue…
- [[MH-13529](https://opencast.jira.com/browse/MH-13529)][[#871](https://github.com/opencast/opencast/pull/871)] -
  Don't warn about expected behavior
- [[MH-13528](https://opencast.jira.com/browse/MH-13528)][[#870](https://github.com/opencast/opencast/pull/870)] -
  Non-Interactive FFmpeg
- [[MH-13525](https://opencast.jira.com/browse/MH-13525)][[#867](https://github.com/opencast/opencast/pull/867)] -
  Update Admin Interface Libraries
- [[MH-13519](https://opencast.jira.com/browse/MH-13519)][[#855](https://github.com/opencast/opencast/pull/855)] -
  Migrate mappings to Elastic Search 5.x
- [[MH-13505](https://opencast.jira.com/browse/MH-13505)][[#844](https://github.com/opencast/opencast/pull/844)] -
  Update Admin Interface JavaScript Libraries
- [[MH-13504](https://opencast.jira.com/browse/MH-13504)][[#843](https://github.com/opencast/opencast/pull/843)] -
  JavaScript Library Update
- [[MH-12047](https://opencast.jira.com/browse/MH-12047)][[#832](https://github.com/opencast/opencast/pull/832)] -
  MH-13380 MH-13490 MH-13489 Add missing indexes
- [[MH-13477](https://opencast.jira.com/browse/MH-13477)][[#819](https://github.com/opencast/opencast/pull/819)] -
  Faster Asset Manager Property Access
- [[MH-13465](https://opencast.jira.com/browse/MH-13465)][[#807](https://github.com/opencast/opencast/pull/807)] -
  Prevent NullPointerException
- [[MH-13389](https://opencast.jira.com/browse/MH-13389)][[#815](https://github.com/opencast/opencast/pull/815)] -
  More informative job load logging
- [[MH-13472](https://opencast.jira.com/browse/MH-13472)][[#813](https://github.com/opencast/opencast/pull/813)] -
  Permissions for /play/ missing
- [[MH-13471](https://opencast.jira.com/browse/MH-13471)][[#812](https://github.com/opencast/opencast/pull/812)] -
  Shibboleth SSO plugin to add roles for users on OC according to their EDUPERSONAFFILIATION. eg:
  "ROLE_AAI_USER_AFFILIATION_student" for "student"
- [[MH-13469](https://opencast.jira.com/browse/MH-13469)][[#811](https://github.com/opencast/opencast/pull/811)] -
  Drop LastHeardFrom On Scheduler Messages
- [[MH-13468](https://opencast.jira.com/browse/MH-13468)][[#810](https://github.com/opencast/opencast/pull/810)] -
  Capture Agent Registration Exception
- [[MH-13466](https://opencast.jira.com/browse/MH-13466)][[#809](https://github.com/opencast/opencast/pull/809)] -
  Prevent Capture Agents From Modifying Metadata
- [[MH-13467](https://opencast.jira.com/browse/MH-13467)][[#808](https://github.com/opencast/opencast/pull/808)] -
  opencast-security-cas feature can not be started
- [[#806](https://github.com/opencast/opencast/pull/806)] -
  extend the ingest-download-woh
- [[MH-12643](https://opencast.jira.com/browse/MH-12643)][[#804](https://github.com/opencast/opencast/pull/804)] -
  Allow workspace to read from asset manager
- [[MH-13462](https://opencast.jira.com/browse/MH-13462)][[#802](https://github.com/opencast/opencast/pull/802)] -
  Prevent Being Started By Root
- [[MH-13461](https://opencast.jira.com/browse/MH-13461)][[#801](https://github.com/opencast/opencast/pull/801)] -
  Dependency Fixes & Dependency Checks
- [[MH-13460](https://opencast.jira.com/browse/MH-13460)][[#800](https://github.com/opencast/opencast/pull/800)] -
  Update JavaScript Dependencies
- [[MH-13459](https://opencast.jira.com/browse/MH-13459)][[#799](https://github.com/opencast/opencast/pull/799)] -
  Make Paella Use UI Configuration Service
- [[MH-13458](https://opencast.jira.com/browse/MH-13458)][[#798](https://github.com/opencast/opencast/pull/798)] -
  Live Scheduler Dependencies
- [[MH-13457](https://opencast.jira.com/browse/MH-13457)][[#797](https://github.com/opencast/opencast/pull/797)] -
  Dependency Update
- [[MH-13456](https://opencast.jira.com/browse/MH-13456)][[#796](https://github.com/opencast/opencast/pull/796)] -
  Move Log Workflow Operation To Admin
- [[MH-13455](https://opencast.jira.com/browse/MH-13455)][[#795](https://github.com/opencast/opencast/pull/795)] -
  Opencast Plug-in Features
- [[MH-13454](https://opencast.jira.com/browse/MH-13454)][[#794](https://github.com/opencast/opencast/pull/794)] -
  Drop Unused Configuration Option Maps
- [[MH-13453](https://opencast.jira.com/browse/MH-13453)][[#793](https://github.com/opencast/opencast/pull/793)] -
  Add more log output to WOH select-streams
- [[MH-13452](https://opencast.jira.com/browse/MH-13452)][[#792](https://github.com/opencast/opencast/pull/792)] -
  Show creators correctly in delete modals
- [[MH-13450](https://opencast.jira.com/browse/MH-13450)][[#790](https://github.com/opencast/opencast/pull/790)] -
  Remove unused class org.opencastproject.adminui.api.SortType
- [[MH-13448](https://opencast.jira.com/browse/MH-13448)][[#789](https://github.com/opencast/opencast/pull/789)] -
  Make translation of creators consistent
- [[MH-13446](https://opencast.jira.com/browse/MH-13446)][[#788](https://github.com/opencast/opencast/pull/788)] -
  Removed unfinished feature "ACL transitions"
- [[MH-13445](https://opencast.jira.com/browse/MH-13445)][[#787](https://github.com/opencast/opencast/pull/787)] -
  Update Checkstyle
- [[MH-13443](https://opencast.jira.com/browse/MH-13443)][[#783](https://github.com/opencast/opencast/pull/783)] -
  Don't use deprecated $http.success and $http.error methods
- [[MH-13439](https://opencast.jira.com/browse/MH-13439)][[#782](https://github.com/opencast/opencast/pull/782)] -
  Dynamic Player Redirect
- [[MH-13438](https://opencast.jira.com/browse/MH-13438)][[#781](https://github.com/opencast/opencast/pull/781)] -
  Simplify Streaming Format Check
- [[#780](https://github.com/opencast/opencast/pull/780)] -
  ACL documentation pointed to wrong config file
- [[MH-13436](https://opencast.jira.com/browse/MH-13436)][[#778](https://github.com/opencast/opencast/pull/778)] -
  Improve error message for out of bounds image extraction
- [[MH-13421](https://opencast.jira.com/browse/MH-13421)][[#776](https://github.com/opencast/opencast/pull/776)] -
  Remove unused workflowservice exceptions
- [[MH-13434](https://opencast.jira.com/browse/MH-13434)][[#775](https://github.com/opencast/opencast/pull/775)] -
  Opencast Common Clean-up
- [[MH-13381](https://opencast.jira.com/browse/MH-13381)][[#771](https://github.com/opencast/opencast/pull/771)] -
  Use Organization Identifier In Roles
- [[MH-13432](https://opencast.jira.com/browse/MH-13432)][[#770](https://github.com/opencast/opencast/pull/770)] -
  Remove unused modals "Job Details" and "Server Details"
- [[MH-13431](https://opencast.jira.com/browse/MH-13431)][[#769](https://github.com/opencast/opencast/pull/769)] -
  Remove unfinished feature "Bulk Messaging"
- [[MH-13430](https://opencast.jira.com/browse/MH-13430)][[#768](https://github.com/opencast/opencast/pull/768)] -
  Fix Opencast Offline Builds
- [[MH-13428](https://opencast.jira.com/browse/MH-13428)][[#766](https://github.com/opencast/opencast/pull/766)] -
  Remove unused library angular-scenario from admin ui tests
- [[MH-13426](https://opencast.jira.com/browse/MH-13426)][[#765](https://github.com/opencast/opencast/pull/765)] -
  Remove unused Protractor end-to-end tests
- [[MH-13427](https://opencast.jira.com/browse/MH-13427)][[#764](https://github.com/opencast/opencast/pull/764)] -
  Remove unused test resources
- [[MH-13381](https://opencast.jira.com/browse/MH-13381)][[#763](https://github.com/opencast/opencast/pull/763)] -
  Use Organization Identifier in Workflows
- [[MH-13424](https://opencast.jira.com/browse/MH-13424)][[#762](https://github.com/opencast/opencast/pull/762)] -
  Elasticsearch 5.6.15
- [[MH-13423](https://opencast.jira.com/browse/MH-13423)][[#761](https://github.com/opencast/opencast/pull/761)] -
  Possible NPE if debugging is enabled
- [[MH-13422](https://opencast.jira.com/browse/MH-13422)][[#760](https://github.com/opencast/opencast/pull/760)] -
  Switch to markdownlint-cli
- [[MH-13420](https://opencast.jira.com/browse/MH-13420)][[#759](https://github.com/opencast/opencast/pull/759)] -
  ngRepeat does not allow duplicates
- [[MH-13417](https://opencast.jira.com/browse/MH-13417)][[#758](https://github.com/opencast/opencast/pull/758)] -
  UI Configuration Service Tests
- [[MH-13414](https://opencast.jira.com/browse/MH-13414)][[#756](https://github.com/opencast/opencast/pull/756)] -
  extended metadata multivalue fields are not handled properly
- [[MH-13413](https://opencast.jira.com/browse/MH-13413)][[#755](https://github.com/opencast/opencast/pull/755)] -
  UI Configuration Service Improvements
- [[MH-13412](https://opencast.jira.com/browse/MH-13412)][[#754](https://github.com/opencast/opencast/pull/754)] -
  Deprecate PathSupport.concat(…)
- [[MH-13411](https://opencast.jira.com/browse/MH-13411)][[#753](https://github.com/opencast/opencast/pull/753)] -
  Fix UI Config Service Dependencies
- [[MH-13410](https://opencast.jira.com/browse/MH-13410)][[#752](https://github.com/opencast/opencast/pull/752)] -
  Fix Broken Build Number
- [[MH-13397](https://opencast.jira.com/browse/MH-13397)][[#751](https://github.com/opencast/opencast/pull/751)] -
  Remove unfinished feature "Participation Management"
- [[MH-13396](https://opencast.jira.com/browse/MH-13396)][[#750](https://github.com/opencast/opencast/pull/750)] -
  Remove unfinished feature "Location Blacklisting"
- [[MH-13400](https://opencast.jira.com/browse/MH-13400)][[#745](https://github.com/opencast/opencast/pull/745)] -
  Admin Index Test Cleanup
- [[MH-13399](https://opencast.jira.com/browse/MH-13399)][[#744](https://github.com/opencast/opencast/pull/744)] -
  Update Elasticsearch Configuration
- [[MH-13395](https://opencast.jira.com/browse/MH-13395)][[#742](https://github.com/opencast/opencast/pull/742)] -
  Remove unfinished feature "Dashboard"
- [[MH-13394](https://opencast.jira.com/browse/MH-13394)][[#741](https://github.com/opencast/opencast/pull/741)] -
  Remove unfinished feature "User Blacklisting"
- [[MH-13393](https://opencast.jira.com/browse/MH-13393)][[#738](https://github.com/opencast/opencast/pull/738)] -
  Remove leftover index resources
- [[MH-13392](https://opencast.jira.com/browse/MH-13392)][[#737](https://github.com/opencast/opencast/pull/737)] -
  Added allowConflict parameter to methods and implemented
- [[#736](https://github.com/opencast/opencast/pull/736)] -
  Revert #523: Special handling of asset manager event removal
- [[MH-13390](https://opencast.jira.com/browse/MH-13390)][[#735](https://github.com/opencast/opencast/pull/735)] -
  Quick-Filter by Presenter
- [[MH-13221](https://opencast.jira.com/browse/MH-13221)][[#732](https://github.com/opencast/opencast/pull/732)] -
  Improve behaviour of single-select metadata fields
- [[MH-13385](https://opencast.jira.com/browse/MH-13385)][[#731](https://github.com/opencast/opencast/pull/731)] -
  Simplify the configuration of the URL signing components
- [[MH-13384](https://opencast.jira.com/browse/MH-13384)][[#730](https://github.com/opencast/opencast/pull/730)] -
  Remove duplicate `joda-time` dependency declaration
- [[MH-13277](https://opencast.jira.com/browse/MH-13277)][[#729](https://github.com/opencast/opencast/pull/729)] -
  fix concurrent Map updates in scheduler
- [[MH-13382](https://opencast.jira.com/browse/MH-13382)][[#727](https://github.com/opencast/opencast/pull/727)] -
  Minor Waveform Service Fixes
- [[MH-13379](https://opencast.jira.com/browse/MH-13379)][[#726](https://github.com/opencast/opencast/pull/726)] -
  Simplify Mime Type Handling
- [[MH-13368](https://opencast.jira.com/browse/MH-13368)][[#724](https://github.com/opencast/opencast/pull/724)] -
  Added color property to waveform operation handler
- [[MH-13376](https://opencast.jira.com/browse/MH-13376)][[#722](https://github.com/opencast/opencast/pull/722)] -
  Fix OSGI Bindings
- [[MH-13374](https://opencast.jira.com/browse/MH-13374)][[#720](https://github.com/opencast/opencast/pull/720)] -
  Update Node.js
- [[MH-13373](https://opencast.jira.com/browse/MH-13373)][[#719](https://github.com/opencast/opencast/pull/719)] -
  Upgrade Admin Interface Libraries
- [[MH-13372](https://opencast.jira.com/browse/MH-13372)][[#718](https://github.com/opencast/opencast/pull/718)] -
  Clean up orphaned asset manager properties
- [[MH-13371](https://opencast.jira.com/browse/MH-13371)][[#717](https://github.com/opencast/opencast/pull/717)] -
  Drop unused angular-md5
- [[MH-13370](https://opencast.jira.com/browse/MH-13370)][[#716](https://github.com/opencast/opencast/pull/716)] -
  Don't configure unnecessary default credentials
- [[MH-13294](https://opencast.jira.com/browse/MH-13294)][[#713](https://github.com/opencast/opencast/pull/713)] -
  Workflow for track replacement and cleanup Snapshots
- [[MH-13367](https://opencast.jira.com/browse/MH-13367)][[#711](https://github.com/opencast/opencast/pull/711)] -
  External API series acl returns null pointer with missing acl
- [[#710](https://github.com/opencast/opencast/pull/710)] -
  adds an WOH, which can add catalogs to the MediaPackage of an workflow instance
- [[MH-13365](https://opencast.jira.com/browse/MH-13365)][[#709](https://github.com/opencast/opencast/pull/709)] -
  inbox ingest into series and inbox retry
- [[MH-13364](https://opencast.jira.com/browse/MH-13364)][[#707](https://github.com/opencast/opencast/pull/707)] -
  Fix hidden OSGI wiring errors
- [[#704](https://github.com/opencast/opencast/pull/704)] -
  Fixed a typo in the analyze-tracks description
- [[MH-13362](https://opencast.jira.com/browse/MH-13362)][[#703](https://github.com/opencast/opencast/pull/703)] -
  Harmonize Admin Interface Menu Tooltips
- [[MH-13361](https://opencast.jira.com/browse/MH-13361)][[#702](https://github.com/opencast/opencast/pull/702)] -
  Fix Scheduler Item Serialization
- [[MH-13360](https://opencast.jira.com/browse/MH-13360)][[#701](https://github.com/opencast/opencast/pull/701)] -
  MH-13316: Watson transcripts improvements
- [[MH-13358](https://opencast.jira.com/browse/MH-13358)][[#698](https://github.com/opencast/opencast/pull/698)] -
  Update JavaScript Dependencies
- [[#691](https://github.com/opencast/opencast/pull/691)] -
  Documentation: Developer Console: How to shutdown
- [[MH-13275](https://opencast.jira.com/browse/MH-13275)][[#689](https://github.com/opencast/opencast/pull/689)] -
  Allows the workflow to select the audio track for composite videos
- [[MH-13350](https://opencast.jira.com/browse/MH-13350)][[#688](https://github.com/opencast/opencast/pull/688)] -
  Theodul core HTML validation
- [[#687](https://github.com/opencast/opencast/pull/687)] -
  Documentation: Publish Engage Workflow OH
- [[MH-13344](https://opencast.jira.com/browse/MH-13344)][[#685](https://github.com/opencast/opencast/pull/685)] -
  Enable AssetManager to reply NOT_MODIFIED
- [[#682](https://github.com/opencast/opencast/pull/682)] -
  add docs.opencast.org anchors for somewhat deep linking
- [[MH-13345](https://opencast.jira.com/browse/MH-13345)][[#681](https://github.com/opencast/opencast/pull/681)] -
  Switch to Gson for Languages Endpoint
- [[MH-13342](https://opencast.jira.com/browse/MH-13342)][[#678](https://github.com/opencast/opencast/pull/678)] -
  Don't try to create events with empty metadata
- [[#677](https://github.com/opencast/opencast/pull/677)] -
  Documentation: Dictionary service
- [[MH-13341](https://opencast.jira.com/browse/MH-13341)][[#676](https://github.com/opencast/opencast/pull/676)] -
  Deleting Capture Agents Should Not Modify Users
- [[MH-13340](https://opencast.jira.com/browse/MH-13340)][[#675](https://github.com/opencast/opencast/pull/675)] -
  Handle Empty Passwords
- [[MH-13339](https://opencast.jira.com/browse/MH-13339)][[#674](https://github.com/opencast/opencast/pull/674)] -
  Handle Bad User Update Requests
- [[MH-13336](https://opencast.jira.com/browse/MH-13336)][[#671](https://github.com/opencast/opencast/pull/671)] -
  Upgrade c3p0
- [[#670](https://github.com/opencast/opencast/pull/670)] -
  Documentation: Analyze Audio WOH: Unbreak table
- [[MH-13331](https://opencast.jira.com/browse/MH-13331)][[#667](https://github.com/opencast/opencast/pull/667)] -
  Fix ActiveMQ Defaults
- [[MH-13328](https://opencast.jira.com/browse/MH-13328)][[#666](https://github.com/opencast/opencast/pull/666)] -
  Remove save button at top of videoeditor
- [[MH-13147](https://opencast.jira.com/browse/MH-13147)][[#664](https://github.com/opencast/opencast/pull/664)] -
  OptimisticLockException in ServiceRegistry dispatchJob
- [[MH-13324](https://opencast.jira.com/browse/MH-13324)][[#662](https://github.com/opencast/opencast/pull/662)] -
  Simplify Data Loader
- [[MH-13323](https://opencast.jira.com/browse/MH-13323)][[#661](https://github.com/opencast/opencast/pull/661)] -
  Add documentation for list providers
- [[MH-13322](https://opencast.jira.com/browse/MH-13322)][[#660](https://github.com/opencast/opencast/pull/660)] -
  Avoid . in Elasticsearch Field Names
- [[MH-13321](https://opencast.jira.com/browse/MH-13321)][[#659](https://github.com/opencast/opencast/pull/659)] -
  Fix Series Item Serialization
- [[MH-13320](https://opencast.jira.com/browse/MH-13320)][[#658](https://github.com/opencast/opencast/pull/658)] -
  Asset Manager Performance
- [[MH-13319](https://opencast.jira.com/browse/MH-13319)][[#657](https://github.com/opencast/opencast/pull/657)] -
  Update Paella Binding Dependencies
- [[MH-13318](https://opencast.jira.com/browse/MH-13318)][[#656](https://github.com/opencast/opencast/pull/656)] -
  Update to Apache Karaf 4.2.2
- [[MH-13313](https://opencast.jira.com/browse/MH-13313)][[#653](https://github.com/opencast/opencast/pull/653)] -
  Properly Use ACL Merge-Mode Configuration
- [[MH-13307](https://opencast.jira.com/browse/MH-13307)][[#648](https://github.com/opencast/opencast/pull/648)] -
  Update Release Manager Documentation
- [[MH-13306](https://opencast.jira.com/browse/MH-13306)][[#647](https://github.com/opencast/opencast/pull/647)] -
  Clean up MetadataUtils
- [[MH-13244](https://opencast.jira.com/browse/MH-13244)][[#642](https://github.com/opencast/opencast/pull/642)] -
  Add override support to external api
- [[MH-13221](https://opencast.jira.com/browse/MH-13221)][[#641](https://github.com/opencast/opencast/pull/641)] -
  Add placeholder to multi-select fields
- [[MH-13290](https://opencast.jira.com/browse/MH-13290)][[#632](https://github.com/opencast/opencast/pull/632)] -
  Asset Manager Query Performance
- [[MH-13289](https://opencast.jira.com/browse/MH-13289)][[#631](https://github.com/opencast/opencast/pull/631)] -
  Introduce Metadatafield Copy Constructor
- [[MH-13288](https://opencast.jira.com/browse/MH-13288)][[#630](https://github.com/opencast/opencast/pull/630)] -
  Don't create incomplete metadata fields
- [[MH-13287](https://opencast.jira.com/browse/MH-13287)][[#629](https://github.com/opencast/opencast/pull/629)] -
  Fix incorrect text metadatafield types
- [[MH-13286](https://opencast.jira.com/browse/MH-13286)][[#628](https://github.com/opencast/opencast/pull/628)] -
  Remove unused functionality from MetadataField
- [[MH-13285](https://opencast.jira.com/browse/MH-13285)][[#627](https://github.com/opencast/opencast/pull/627)] -
  Display workflow description
- [[#626](https://github.com/opencast/opencast/pull/626)] -
  Provide location of `org.ops4j.pax.web.cfg`
- [[MH-13284](https://opencast.jira.com/browse/MH-13284)][[#625](https://github.com/opencast/opencast/pull/625)] -
  Update Elasticsearch to 2.x
- [[MH-12091](https://opencast.jira.com/browse/MH-12091)][[#622](https://github.com/opencast/opencast/pull/622)] -
  Per-Tenant Capture Agent Users
- [[MH-13281](https://opencast.jira.com/browse/MH-13281)][[#621](https://github.com/opencast/opencast/pull/621)] -
  Added property keep-last-snapshot for asset-delete WOH
- [[MH-13278](https://opencast.jira.com/browse/MH-13278)][[#617](https://github.com/opencast/opencast/pull/617)] -
  Drop Unused Exception
- [[MH-13238](https://opencast.jira.com/browse/MH-13238)][[#615](https://github.com/opencast/opencast/pull/615)] -
  don't throw related services straight into ERROR state just because job succeeded on current service
- [[MH-13277](https://opencast.jira.com/browse/MH-13277)][[#614](https://github.com/opencast/opencast/pull/614)] -
  improve scheduler performance
- [[MH-13276](https://opencast.jira.com/browse/MH-13276)][[#613](https://github.com/opencast/opencast/pull/613)] -
  Drop org.opencastproject.fun
- [[MH-13271](https://opencast.jira.com/browse/MH-13271)][[#610](https://github.com/opencast/opencast/pull/610)] -
  Remove Useless ACL Check
- [[MH-13270](https://opencast.jira.com/browse/MH-13270)][[#609](https://github.com/opencast/opencast/pull/609)] -
  Fix Message Item Serialization
- [[MH-13267](https://opencast.jira.com/browse/MH-13267)][[#607](https://github.com/opencast/opencast/pull/607)] -
  Update Deprecated Code In UIRolesRoleProvider
- [[#605](https://github.com/opencast/opencast/pull/605)] -
  NOJIRA: Fix misspelled digest
- [[MH-13157](https://opencast.jira.com/browse/MH-13157)][[#600](https://github.com/opencast/opencast/pull/600)] -
  Add multi-tenant support for all list providers
- [[MH-13262](https://opencast.jira.com/browse/MH-13262)][[#596](https://github.com/opencast/opencast/pull/596)] -
  Changed for partial-error comment description to better description.
- [[MH-13261](https://opencast.jira.com/browse/MH-13261)][[#595](https://github.com/opencast/opencast/pull/595)] -
  User Directory OSGI Service Definitions
- [[MH-13260](https://opencast.jira.com/browse/MH-13260)][[#594](https://github.com/opencast/opencast/pull/594)] -
  Simplify Runtime Info UI
- [[MH-13259](https://opencast.jira.com/browse/MH-13259)][[#593](https://github.com/opencast/opencast/pull/593)] -
  User/Role Directory Cleanup
- [[MH-13255](https://opencast.jira.com/browse/MH-13255)][[#590](https://github.com/opencast/opencast/pull/590)] -
  Updated Deprecated Methods in Workspace Tests
- [[MH-13254](https://opencast.jira.com/browse/MH-13254)][[#589](https://github.com/opencast/opencast/pull/589)] -
  Automate Dependency Checking
- [[MH-13253](https://opencast.jira.com/browse/MH-13253)][[#588](https://github.com/opencast/opencast/pull/588)] -
  External Elasticsearch
- [[MH-13251](https://opencast.jira.com/browse/MH-13251)][[#586](https://github.com/opencast/opencast/pull/586)] -
  Remove duplicate dependency
- [[MH-13247](https://opencast.jira.com/browse/MH-13247)][[#582](https://github.com/opencast/opencast/pull/582)] -
  Deprecated Methods In Elasticsearch
- [[MH-12816](https://opencast.jira.com/browse/MH-12816)][[#579](https://github.com/opencast/opencast/pull/579)] -
  Make waveform size configurable in WOH
- [[MH-13242](https://opencast.jira.com/browse/MH-13242)][[#578](https://github.com/opencast/opencast/pull/578)] -
  Set disable_search_threshold for chosen globally
- [[MH-13241](https://opencast.jira.com/browse/MH-13241)][[#577](https://github.com/opencast/opencast/pull/577)] -
  Filter Fileinstall Artifacts
- [[MH-13129](https://opencast.jira.com/browse/MH-13129)][[#575](https://github.com/opencast/opencast/pull/575)] -
  More configuration options for thumbnails
- [[MH-13239](https://opencast.jira.com/browse/MH-13239)][[#574](https://github.com/opencast/opencast/pull/574)] -
  Docs: Fix 'Edit on GitHub' link
- [[#573](https://github.com/opencast/opencast/pull/573)] -
  Documentation: Inbox
- [[MH-13234](https://opencast.jira.com/browse/MH-13234)][[#565](https://github.com/opencast/opencast/pull/565)] -
  Workspace Deprecation Fixes
- [[MH-13231](https://opencast.jira.com/browse/MH-13231)][[#564](https://github.com/opencast/opencast/pull/564)] -
  Allow entering multiple metadata values at once
- [[MH-13233](https://opencast.jira.com/browse/MH-13233)][[#563](https://github.com/opencast/opencast/pull/563)] -
  add note about the jdk version use for build
- [[MH-13229](https://opencast.jira.com/browse/MH-13229)][[#561](https://github.com/opencast/opencast/pull/561)] -
  External Library Updates
- [[MH-13227](https://opencast.jira.com/browse/MH-13227)][[#559](https://github.com/opencast/opencast/pull/559)] -
  Update to Apache Karaf 4.2
- [[MH-13226](https://opencast.jira.com/browse/MH-13226)][[#558](https://github.com/opencast/opencast/pull/558)] -
  Update Docuemnation Landing Page
- [[MH-13224](https://opencast.jira.com/browse/MH-13224)][[#556](https://github.com/opencast/opencast/pull/556)] -
  Drop commons-beanutils
- [[MH-13217](https://opencast.jira.com/browse/MH-13217)][[#551](https://github.com/opencast/opencast/pull/551)] -
  pom.xml housekeeping
- [[MH-13213](https://opencast.jira.com/browse/MH-13213)][[#548](https://github.com/opencast/opencast/pull/548)] -
  Separate External API Index
- [[MH-13212](https://opencast.jira.com/browse/MH-13212)][[#546](https://github.com/opencast/opencast/pull/546)] -
  Fix external-api dependencies
- [[MH-13210](https://opencast.jira.com/browse/MH-13210)][[#545](https://github.com/opencast/opencast/pull/545)] -
  Fix Deprecated IOUtils Usage
- [[#542](https://github.com/opencast/opencast/pull/542)] -
  Developer Installation Guide
- [[MH-13208](https://opencast.jira.com/browse/MH-13208)][[#540](https://github.com/opencast/opencast/pull/540)] -
  Create a short contributor guide
- [[MH-13200](https://opencast.jira.com/browse/MH-13200)][[#535](https://github.com/opencast/opencast/pull/535)] -
  Remove unused file acl-modal.html
- [[MH-13127](https://opencast.jira.com/browse/MH-13127)][[#534](https://github.com/opencast/opencast/pull/534)] -
  Make table headers non-interactive by default
- [[MH-13198](https://opencast.jira.com/browse/MH-13198)][[#529](https://github.com/opencast/opencast/pull/529)] -
  Properly Display Multiple Presenters
- [[MH-13197](https://opencast.jira.com/browse/MH-13197)][[#528](https://github.com/opencast/opencast/pull/528)] -
  Separate Admin Interface Index
- [[MH-13195](https://opencast.jira.com/browse/MH-13195)][[#526](https://github.com/opencast/opencast/pull/526)] -
  Fix Admin Interface Dependencies
- [[MH-13193](https://opencast.jira.com/browse/MH-13193)][[#524](https://github.com/opencast/opencast/pull/524)] -
  Improve performance of event deletion (2)
- [[MH-13193](https://opencast.jira.com/browse/MH-13193)][[#523](https://github.com/opencast/opencast/pull/523)] -
  Improve performance of event deletion (1)
- [[MH-13084](https://opencast.jira.com/browse/MH-13084)][[#519](https://github.com/opencast/opencast/pull/519)] -
  Create a generic user interface configuration service
- [[MH-13054](https://opencast.jira.com/browse/MH-13054)][[#518](https://github.com/opencast/opencast/pull/518)] -
  Update angular-ui-sortable, adapting build pipeline
- [[#515](https://github.com/opencast/opencast/pull/515)] -
  NOJIRA: Documentation: wait_timeout should be bigger than max.idle.time
- [[MH-13187](https://opencast.jira.com/browse/MH-13187)][[#514](https://github.com/opencast/opencast/pull/514)] -
  Improve Track Stream Handling
- [[MH-13186](https://opencast.jira.com/browse/MH-13186)][[#513](https://github.com/opencast/opencast/pull/513)] -
  Episode and Series ACL Handling
- [[MH-13185](https://opencast.jira.com/browse/MH-13185)][[#511](https://github.com/opencast/opencast/pull/511)] -
  Don't include test web server
- [[MH-13183](https://opencast.jira.com/browse/MH-13183)][[#505](https://github.com/opencast/opencast/pull/505)] -
  Add link to series details, out of the eventstable-view
- [[MH-13178](https://opencast.jira.com/browse/MH-13178)][[#502](https://github.com/opencast/opencast/pull/502)] -
  Clean-up Series Dialog Code
- [[MH-13177](https://opencast.jira.com/browse/MH-13177)][[#501](https://github.com/opencast/opencast/pull/501)] -
  Further Simplify MediaPackageElementFlavor
- [[MH-13175](https://opencast.jira.com/browse/MH-13175)][[#499](https://github.com/opencast/opencast/pull/499)] -
  Remove Apache Tika for Generating Mimetypes
- [[MH-13174](https://opencast.jira.com/browse/MH-13174)][[#498](https://github.com/opencast/opencast/pull/498)] -
  Simplify class MediaPackageElementFlavor
- [[MH-13155](https://opencast.jira.com/browse/MH-13155)][[#497](https://github.com/opencast/opencast/pull/497)] -
  Make weekday preselection optional
- [[MH-13168](https://opencast.jira.com/browse/MH-13168)][[#491](https://github.com/opencast/opencast/pull/491)] -
  Testcases to test a captureagent with Opencast integration.
- [[MH-13160](https://opencast.jira.com/browse/MH-13160)][[#488](https://github.com/opencast/opencast/pull/488)] -
  Send actually required data in workflow messages
- [[MH-13161](https://opencast.jira.com/browse/MH-13161)][[#483](https://github.com/opencast/opencast/pull/483)] -
  Simplify log statements
- [[MH-13158](https://opencast.jira.com/browse/MH-13158)][[#480](https://github.com/opencast/opencast/pull/480)] -
  Use default functional interface for SecurityUtil#runAs
- [[MH-13153](https://opencast.jira.com/browse/MH-13153)][[#477](https://github.com/opencast/opencast/pull/477)] -
  Workflow Service Code Cleanup
- [[MH-13151](https://opencast.jira.com/browse/MH-13151)][[#475](https://github.com/opencast/opencast/pull/475)] -
  Update to Apache Karaf 4.1.6
- [[MH-13148](https://opencast.jira.com/browse/MH-13148)][[#472](https://github.com/opencast/opencast/pull/472)] -
  Internationalization support for series LTI tools
- [[MH-13140](https://opencast.jira.com/browse/MH-13140)][[#466](https://github.com/opencast/opencast/pull/466)] -
  Clean-up REST Documentation Code
- [[MH-13061](https://opencast.jira.com/browse/MH-13061)][[#450](https://github.com/opencast/opencast/pull/450)] -
  Display responsible person for workflows
- [[MH-13121](https://opencast.jira.com/browse/MH-13121)][[#447](https://github.com/opencast/opencast/pull/447)] -
  Fix usertracking plugin in paella player
- [[MH-13124](https://opencast.jira.com/browse/MH-13124)][[#446](https://github.com/opencast/opencast/pull/446)] -
  Unify linting for JavaScript and HTML
- [[MH-13082](https://opencast.jira.com/browse/MH-13082)][[#440](https://github.com/opencast/opencast/pull/440)] -
  Fix LTI security vulnerability and refactor LTI and OAuth classes
- [[MH-13098](https://opencast.jira.com/browse/MH-13098)][[#430](https://github.com/opencast/opencast/pull/430)] -
  Add start-workflow WOH
- [[MH-13062](https://opencast.jira.com/browse/MH-13062)][[#401](https://github.com/opencast/opencast/pull/401)] -
  Added credentials for the Ingest Service.
- [[MH-13000](https://opencast.jira.com/browse/MH-13000)][[#398](https://github.com/opencast/opencast/pull/398)] -
  Group “Edit scheduled” events by weekday
- [[MH-12782](https://opencast.jira.com/browse/MH-12782)][[#209](https://github.com/opencast/opencast/pull/209)] -
  As an unprivileged user, I only want to see series and events that I have write access to.


Opencast 6
----------

### Opencast 6.7

*Released on December 8, 2019*

- [[#1200](https://github.com/opencast/opencast/pull/1200)] -
  Fix Crowdin Deployment
- [[#1143](https://github.com/opencast/opencast/pull/1143)] -
  Upgrade jackson to 2.9.10 (6.x)
- [[#1142](https://github.com/opencast/opencast/pull/1142)] -
  Update apache commons-compress to 1.19
- [[#1132](https://github.com/opencast/opencast/pull/1132)] -
  Fixed the "hide" button in the Documentation.
- [[#1080](https://github.com/opencast/opencast/pull/1080)] -
  Documentation reworked
- [[#1035](https://github.com/opencast/opencast/pull/1035)] -
  Pushing to Maven Central
- [[#1026](https://github.com/opencast/opencast/pull/1026)] -
  Adding Ansible script documentation
- [[#1019](https://github.com/opencast/opencast/pull/1019)] -
  SMIL tests fail when doctype url can't be resolved

### Opencast 6.6

*Released on August 2, 2019*

- [[MH-13674](https://opencast.jira.com/browse/MH-13674)][[#1013](https://github.com/opencast/opencast/pull/1013)] -
  Fix Cutting
- [[MH-13673](https://opencast.jira.com/browse/MH-13673)][[#1012](https://github.com/opencast/opencast/pull/1012)] -
  Workflow options not visually aligned
- [[MH-13672](https://opencast.jira.com/browse/MH-13672)][[#1011](https://github.com/opencast/opencast/pull/1011)] -
  Editor Maximum Height
- [[MH-13671](https://opencast.jira.com/browse/MH-13671)][[#1010](https://github.com/opencast/opencast/pull/1010)] -
  OAI-PMH autorepublish fails due to invalid urls
- [[MH-13648](https://opencast.jira.com/browse/MH-13648)][[#984](https://github.com/opencast/opencast/pull/984)] -
  Asset Manager Concurrecy Issue
- [[MH-13644](https://opencast.jira.com/browse/MH-13644)][[#980](https://github.com/opencast/opencast/pull/980)] -
  Sometimes paella does not play audio
- [[MH-13643](https://opencast.jira.com/browse/MH-13643)][[#979](https://github.com/opencast/opencast/pull/979)] -
  Update to Paella 6.1.4
- [[MH-13637](https://opencast.jira.com/browse/MH-13637)][[#974](https://github.com/opencast/opencast/pull/974)] -
  Asset manager endpoint fix
- [[MH-13633](https://opencast.jira.com/browse/MH-13633)][[#969](https://github.com/opencast/opencast/pull/969)] -
  Update spring-security-oauth
- [[MH-13611](https://opencast.jira.com/browse/MH-13611)][[#955](https://github.com/opencast/opencast/pull/955)] -
  Duplicate events fix

### Opencast 6.5

*Released on June 14, 2019*

- [[MH-13607](https://opencast.jira.com/browse/MH-13607)][[#946](https://github.com/opencast/opencast/pull/946)] -
  Show composite duration in video editor
- [[MH-13606](https://opencast.jira.com/browse/MH-13606)][[#944](https://github.com/opencast/opencast/pull/944)] -
  Don't archive smil on publication
- [[MH-13601](https://opencast.jira.com/browse/MH-13601)][[#939](https://github.com/opencast/opencast/pull/939)] -
  OAI-PMH database access syncronization
- [[MH-13575](https://opencast.jira.com/browse/MH-13575)][[#916](https://github.com/opencast/opencast/pull/916)] -
  Update paella player to 6.1.3
- [[MH-13573](https://opencast.jira.com/browse/MH-13573)][[#914](https://github.com/opencast/opencast/pull/914)] -
  Add .factorypath to .gitignore
- [[MH-13560](https://opencast.jira.com/browse/MH-13560)][[#902](https://github.com/opencast/opencast/pull/902)] -
  Admin Role in Moodle User Provider
- [[MH-13546](https://opencast.jira.com/browse/MH-13546)][[#888](https://github.com/opencast/opencast/pull/888)] -
  textextraction performance improvement
- [[MH-13544](https://opencast.jira.com/browse/MH-13544)][[#886](https://github.com/opencast/opencast/pull/886)] -
  Video editor shows incorrect notification
- [[MH-13536](https://opencast.jira.com/browse/MH-13536)][[#877](https://github.com/opencast/opencast/pull/877)] -
  OAI-PMH Remote Broken
- [[MH-13533](https://opencast.jira.com/browse/MH-13533)][[#875](https://github.com/opencast/opencast/pull/875)] -
  Document parameter "sign" of `GET /api/events/{id}/publications/*`
- [[MH-13526](https://opencast.jira.com/browse/MH-13526)][[#868](https://github.com/opencast/opencast/pull/868)] -
  Show unequal tracks correctly in editor
- [[MH-13521](https://opencast.jira.com/browse/MH-13521)][[#859](https://github.com/opencast/opencast/pull/859)] -
  Switch to openJDK 8 on Travis
- [[MH-13503](https://opencast.jira.com/browse/MH-13503)][[#856](https://github.com/opencast/opencast/pull/856)] -
  Job Dispatch Fairness
- [[MH-13330](https://opencast.jira.com/browse/MH-13330)][[#853](https://github.com/opencast/opencast/pull/853)] -
  The video editor does not always close after the user presses "Publish"
- [[MH-13511](https://opencast.jira.com/browse/MH-13511)][[#852](https://github.com/opencast/opencast/pull/852)] -
  Adding events in parallel does not work correctly
- [[MH-13501](https://opencast.jira.com/browse/MH-13501)][[#840](https://github.com/opencast/opencast/pull/840)] -
  Match against user pattern for loadUser() lookups
- [[MH-13495](https://opencast.jira.com/browse/MH-13495)][[#839](https://github.com/opencast/opencast/pull/839)] -
  Ignore old requests instead of cancelling
- [[#837](https://github.com/opencast/opencast/pull/837)] -
  Fix adaptive streaming configuration guide
- [[MH-13492](https://opencast.jira.com/browse/MH-13492)][[#833](https://github.com/opencast/opencast/pull/833)] -
  Add language support for Italian
- [[MH-13486](https://opencast.jira.com/browse/MH-13486)][[#829](https://github.com/opencast/opencast/pull/829)] -
  Cleanup NOTICES 6.x
- [[MH-13485](https://opencast.jira.com/browse/MH-13485)][[#828](https://github.com/opencast/opencast/pull/828)] -
  Update paella player to 6.1.2
- [[#827](https://github.com/opencast/opencast/pull/827)] -
  Change url query syntax to ?
- [[MH-13476](https://opencast.jira.com/browse/MH-13476)][[#818](https://github.com/opencast/opencast/pull/818)] -
  Filter capture agent roles for ACLs

### Opencast 6.4

*Released on April 01, 2019*

- [[MH-13449](https://opencast.jira.com/browse/MH-13449)][[cc11441
  ](https://github.com/opencast/opencast/commit/cc1144125ecda32cbb29afba51ab0ac7efb7ca7e)] -
  MH-13449, upgrade spring-security-oauth libs
- [[MH-13464](https://opencast.jira.com/browse/MH-13464)][[#805](https://github.com/opencast/opencast/pull/805)] -
  Update paella player to 6.1.0
- [[MH-13463](https://opencast.jira.com/browse/MH-13463)][[#803](https://github.com/opencast/opencast/pull/803)] -
  WOH select-streams does not hide audio track as expected
- [[MH-13444](https://opencast.jira.com/browse/MH-13444)][[#786](https://github.com/opencast/opencast/pull/786)] -
  Insecure Series Creation
- [[MH-13387](https://opencast.jira.com/browse/MH-13387)][[#777](https://github.com/opencast/opencast/pull/777)] -
  Get ACLs of finished workflows from AssetManager
- [Document encoding-profiles parameter in ComposeWorkflowHandler
  ](https://github.com/opencast/opencast/pull/772)
- [[MH-13429](https://opencast.jira.com/browse/MH-13429)][[#767](https://github.com/opencast/opencast/pull/767)] -
  Make sure series LTI tool respects provided series custom param

### Opencast 6.3

*Released on March 05, 2019*

- [[MH-13402](https://opencast.jira.com/browse/MH-13402)][[#749](https://github.com/opencast/opencast/pull/749)] -
  WOH select-tracks does not work with audio-only input
- [[MH-13404](https://opencast.jira.com/browse/MH-13404)][[#748](https://github.com/opencast/opencast/pull/748)] -
  Improve Workspace Logging
- [[MH-13401](https://opencast.jira.com/browse/MH-13401)][[#747](https://github.com/opencast/opencast/pull/747)] -
  Fix icon in Paella Player
- [[MH-13388](https://opencast.jira.com/browse/MH-13388)][[#734](https://github.com/opencast/opencast/pull/734)] -
  Updating job load values for composer service on worker nodes …
- [[MH-13378](https://opencast.jira.com/browse/MH-13378)][[#725](https://github.com/opencast/opencast/pull/725)] -
  Add mimetype audio/m4a
- [[MH-13377](https://opencast.jira.com/browse/MH-13377)][[#723](https://github.com/opencast/opencast/pull/723)] -
  Fix scheduler rrule TimeZone issue
- [[MH-12631](https://opencast.jira.com/browse/MH-12631)][[#721](https://github.com/opencast/opencast/pull/721)] -
  Drop the ORGANIZER field from the ical feed
- [[MH-13369](https://opencast.jira.com/browse/MH-13369)][[#715](https://github.com/opencast/opencast/pull/715)] -
  Delete Capture Agents
- [[MH-12177](https://opencast.jira.com/browse/MH-12177)][[#712](https://github.com/opencast/opencast/pull/712)] -
  TimeZone threadsafe and bulk schedule across DST (NEW)
- [[MH-13355](https://opencast.jira.com/browse/MH-13355)][[#700](https://github.com/opencast/opencast/pull/700)] -
  Increase the default timeout for TrustedHttpClientImpl
- [[MH-13359](https://opencast.jira.com/browse/MH-13359)][[#699](https://github.com/opencast/opencast/pull/699)] -
  Adding UTF-8 encoding for all remote services
- [[MH-13357](https://opencast.jira.com/browse/MH-13357)][[#697](https://github.com/opencast/opencast/pull/697)] -
  Enable being able to disable 2 confusing Admin UI metadata: "duration" & "created"
- [[MH-13356](https://opencast.jira.com/browse/MH-13356)][[#696](https://github.com/opencast/opencast/pull/696)] -
  Unnecessary Snapshots
- [[MH-13347](https://opencast.jira.com/browse/MH-13347)][[#695](https://github.com/opencast/opencast/pull/695)] -
  Don't always look for orphaned properties
- [[MH-13354](https://opencast.jira.com/browse/MH-13354)][[#694](https://github.com/opencast/opencast/pull/694)] -
  Asset Manager Property Performance
- [[MH-13352](https://opencast.jira.com/browse/MH-13352)][[#693](https://github.com/opencast/opencast/pull/693)] -
  Unnecessary Format
- [[MH-13310](https://opencast.jira.com/browse/MH-13310)][[#692](https://github.com/opencast/opencast/pull/692)] -
  Simplify `AQueryBuilderImpl#always`
- [[#686](https://github.com/opencast/opencast/pull/686)] -
  Document workaround steps for authentication with IBM Watson STT
- [[MH-13147](https://opencast.jira.com/browse/MH-13147)][[#683](https://github.com/opencast/opencast/pull/683)] -
  6.x): OptimisticLockException in ServiceRegistry dispatchJob
- [[MH-13343](https://opencast.jira.com/browse/MH-13343)][[#679](https://github.com/opencast/opencast/pull/679)] -
  Load track into workspace with unique ID
- [[MH-13338](https://opencast.jira.com/browse/MH-13338)][[#673](https://github.com/opencast/opencast/pull/673)] -
  Elasticsearch Upgrade Documentation
- [[MH-13337](https://opencast.jira.com/browse/MH-13337)][[#672](https://github.com/opencast/opencast/pull/672)] -
  Admin UI workflow status translation keys added
- [[MH-13329](https://opencast.jira.com/browse/MH-13329)][[#668](https://github.com/opencast/opencast/pull/668)] -
  Removing a capture agent resets the password of all Opencast users
- [[MH-13326](https://opencast.jira.com/browse/MH-13326)][[#663](https://github.com/opencast/opencast/pull/663)] -
  No file/directory found when taking snapshot
- [[MH-13315](https://opencast.jira.com/browse/MH-13315)][[#655](https://github.com/opencast/opencast/pull/655)] -
  Don't destroy Notifications service on destruction of the Notifications directive
- [[MH-13312](https://opencast.jira.com/browse/MH-13312)][[#654](https://github.com/opencast/opencast/pull/654)] -
  Do not show outdated conflict information

### Opencast 6.2

*Released on January 24, 2019*

- [[MH-13309](https://opencast.jira.com/browse/MH-13309)][[#649](https://github.com/opencast/opencast/pull/649)] -
  return empty list when finding findUsersByUserName when the name param is empty.

### Opencast 6.1

*Released on January 12, 2019*

- [[MH-13305](https://opencast.jira.com/browse/MH-13305)][[#646](https://github.com/opencast/opencast/pull/646)] -
  MacOS installation update
- [[MH-13304](https://opencast.jira.com/browse/MH-13304)][[#645](https://github.com/opencast/opencast/pull/645)] -
  Multi-value consistent with multi-select
- [[MH-13302](https://opencast.jira.com/browse/MH-13302)][[#644](https://github.com/opencast/opencast/pull/644)] -
  Don't save unnecessarily in Multi-Select
- [[MH-13301](https://opencast.jira.com/browse/MH-13301)][[#643](https://github.com/opencast/opencast/pull/643)] -
  Don't require event.publisher since it is a readonly field
- [[MH-13300](https://opencast.jira.com/browse/MH-13300)][[#640](https://github.com/opencast/opencast/pull/640)] -
  Display multi-value fields correctly on summary pages
- [[MH-13299](https://opencast.jira.com/browse/MH-13299)][[#639](https://github.com/opencast/opencast/pull/639)] -
  Make multi-select fields consistent again
- [[MH-13295](https://opencast.jira.com/browse/MH-13295)][[#635](https://github.com/opencast/opencast/pull/635)] -
  Handle null for presentable value extraction
- [[MH-13283](https://opencast.jira.com/browse/MH-13283)][[#624](https://github.com/opencast/opencast/pull/624)] -
  Fix Custom CXF Error Handler
- [[MH-13248](https://opencast.jira.com/browse/MH-13248)][[#623](https://github.com/opencast/opencast/pull/623)] -
  Allow hidden workflow parameters

### Opencast 6.0

*Released on December 10, 2018*

- [[#620](https://github.com/opencast/opencast/pull/620)] -
  Remove dropped translations
- [[MH-13230](https://opencast.jira.com/browse/MH-13230)][[#616](https://github.com/opencast/opencast/pull/616)] -
  remove the need for passing an Accept header with external api requests
- [[MH-13272](https://opencast.jira.com/browse/MH-13272)][[#611](https://github.com/opencast/opencast/pull/611)] -
  fix missing roles
- [[MH-13266](https://opencast.jira.com/browse/MH-13266)][[#606](https://github.com/opencast/opencast/pull/606)] -
  Start date cross link does not work correctly
- [[MH-13215](https://opencast.jira.com/browse/MH-13215)][[#602](https://github.com/opencast/opencast/pull/602)] -
  WorkflowOperationTagUtil throws a null pointer
- [[MH-13245](https://opencast.jira.com/browse/MH-13245)][[#601](https://github.com/opencast/opencast/pull/601)] -
  Paella player does not show a single presentation video
- [[MH-13252](https://opencast.jira.com/browse/MH-13252)][[#587](https://github.com/opencast/opencast/pull/587)] -
  Ineffective Synchronization of Elasticsearch Startup
- [[MH-13221](https://opencast.jira.com/browse/MH-13221)][[#585](https://github.com/opencast/opencast/pull/585)] -
  Improve multi-select metadata fields
- [[MH-13250](https://opencast.jira.com/browse/MH-13250)][[#584](https://github.com/opencast/opencast/pull/584)] -
  Thumbnail feature does not work for unprivileged users
- [[MH-13249](https://opencast.jira.com/browse/MH-13249)][[#583](https://github.com/opencast/opencast/pull/583)] -
  Invalid Group Endpoint Registration
- [[MH-13237](https://opencast.jira.com/browse/MH-13237)][[#576](https://github.com/opencast/opencast/pull/576)] -
  Track previews do not work with stream security
- [[MH-13214](https://opencast.jira.com/browse/MH-13214)][[#570](https://github.com/opencast/opencast/pull/570)] -
  Fix HTTP Digest Authentication
- [[MH-13232](https://opencast.jira.com/browse/MH-13232)][[#562](https://github.com/opencast/opencast/pull/562)] -
  Fix potentially negative fade-out start
- [[MH-13228](https://opencast.jira.com/browse/MH-13228)][[#560](https://github.com/opencast/opencast/pull/560)] -
  Homogeneous Width of Shortcut Icons
- [[MH-13225](https://opencast.jira.com/browse/MH-13225)][[#557](https://github.com/opencast/opencast/pull/557)] -
  Fix for exception in live scheduler service when rebuilding the admin ui index
- [[MH-13222](https://opencast.jira.com/browse/MH-13222)][[#554](https://github.com/opencast/opencast/pull/554)] -
  Some fixes to tiered storage asset manager
- [[MH-13209](https://opencast.jira.com/browse/MH-13209)][[#544](https://github.com/opencast/opencast/pull/544)] -
  Put CAS Feature In Distributions
- [[MH-13150](https://opencast.jira.com/browse/MH-13150)][[#541](https://github.com/opencast/opencast/pull/541)] -
  Add note about CAAM to release notes
- [[MH-13201](https://opencast.jira.com/browse/MH-13201)][[#538](https://github.com/opencast/opencast/pull/538)] -
  Convert uploaded images to appropriate size and format
- [[MH-13206](https://opencast.jira.com/browse/MH-13206)][[#537](https://github.com/opencast/opencast/pull/537)] -
  Use correct mouse cursor in filters
- [[MH-13205](https://opencast.jira.com/browse/MH-13205)][[#536](https://github.com/opencast/opencast/pull/536)] -
  Document, fix and improve thumbnail support
- [[MH-13196](https://opencast.jira.com/browse/MH-13196)][[#527](https://github.com/opencast/opencast/pull/527)] -
  Unregister Resource Servlets of Bundles to be Removed
- [[MH-13192](https://opencast.jira.com/browse/MH-13192)][[#522](https://github.com/opencast/opencast/pull/522)] -
  Improve performance of list requests
- [[MH-13191](https://opencast.jira.com/browse/MH-13191)][[#521](https://github.com/opencast/opencast/pull/521)] -
  Improve performance of retrieving groups
- [[MH-13188](https://opencast.jira.com/browse/MH-13188)][[#516](https://github.com/opencast/opencast/pull/516)] -
  Update paella player 6.0.3
- [[MH-13154](https://opencast.jira.com/browse/MH-13154)][[#512](https://github.com/opencast/opencast/pull/512)] -
  Unify vertical spacing in wizards
- [[MH-13184](https://opencast.jira.com/browse/MH-13184)][[#508](https://github.com/opencast/opencast/pull/508)] -
  Update request-digest
- [[#507](https://github.com/opencast/opencast/pull/507)] -
  Remove documentation about unused workflow pause role
- [[MH-13162](https://opencast.jira.com/browse/MH-13162)][[#506](https://github.com/opencast/opencast/pull/506)] -
  Show all series in edit-scheduled-events
- [[MH-13179](https://opencast.jira.com/browse/MH-13179)][[#503](https://github.com/opencast/opencast/pull/503)] -
  Fix Video Editor Preview Mode Default
- [[MH-13176](https://opencast.jira.com/browse/MH-13176)][[#500](https://github.com/opencast/opencast/pull/500)] -
  Bug fix update of Jackson
- [[MH-13170](https://opencast.jira.com/browse/MH-13170)][[#496](https://github.com/opencast/opencast/pull/496)] -
  Fix workflow not selected in event details
- [[MH-13171](https://opencast.jira.com/browse/MH-13171)][[#495](https://github.com/opencast/opencast/pull/495)] -
  Fix workflow configuration settings being displayed incorrectly
- [[MH-13173](https://opencast.jira.com/browse/MH-13173)][[#494](https://github.com/opencast/opencast/pull/494)] -
  Do not hardcode value of ACL override
- [[MH-13169](https://opencast.jira.com/browse/MH-13169)][[#492](https://github.com/opencast/opencast/pull/492)] -
  Update bibliographic metadata when technical metadata changes
- [[MH-13166](https://opencast.jira.com/browse/MH-13166)][[#489](https://github.com/opencast/opencast/pull/489)] -
  OAI-PMH Message Handler Performance
- [[MH-13164](https://opencast.jira.com/browse/MH-13164)][[#487](https://github.com/opencast/opencast/pull/487)] -
  Load catalog for snapshot message effeciently
- [[MH-13130](https://opencast.jira.com/browse/MH-13130)][[#486](https://github.com/opencast/opencast/pull/486)] -
  java.lang.ClassCastException in AdminUserAndGroupLoader when starting up
- [[MH-13163](https://opencast.jira.com/browse/MH-13163)][[#484](https://github.com/opencast/opencast/pull/484)] -
  Fix empty REST documentation notes
- [[MH-13159](https://opencast.jira.com/browse/MH-13159)][[#481](https://github.com/opencast/opencast/pull/481)] -
  Fix mattermost notification operation issues
- [[MH-13111](https://opencast.jira.com/browse/MH-13111)][[#479](https://github.com/opencast/opencast/pull/479)] -
  Fix display of metadata in series creation summary
- [[MH-13110](https://opencast.jira.com/browse/MH-13110)][[#478](https://github.com/opencast/opencast/pull/478)] -
  Fix display of metadata in event creation summary
- [[MH-13150](https://opencast.jira.com/browse/MH-13150)][[#474](https://github.com/opencast/opencast/pull/474)] -
  Opencast 6.0 release notes
- [[MH-13149](https://opencast.jira.com/browse/MH-13149)][[#473](https://github.com/opencast/opencast/pull/473)] -
  Timed tiered storage test fails on fast systems
- [[MH-13051](https://opencast.jira.com/browse/MH-13051)][[#471](https://github.com/opencast/opencast/pull/471)] -
  Fix dropdown placeholders
- [[#470](https://github.com/opencast/opencast/pull/470)] -
  Fix rest docs of GroupsEndpoint
- [[MH-13141](https://opencast.jira.com/browse/MH-13141)][[#469](https://github.com/opencast/opencast/pull/469)] -
  Correctly initialize stats service
- [[MH-13142](https://opencast.jira.com/browse/MH-13142)][[#468](https://github.com/opencast/opencast/pull/468)] -
  Error parsing non-existent schedule
- [[MH-13135](https://opencast.jira.com/browse/MH-13135)][[#467](https://github.com/opencast/opencast/pull/467)] -
  Pending requests are not cancelled as expected
- [[MH-13139](https://opencast.jira.com/browse/MH-13139)][[#465](https://github.com/opencast/opencast/pull/465)] -
  Documentation for the event publisher metadata
- [[MH-12819](https://opencast.jira.com/browse/MH-12819)][[#464](https://github.com/opencast/opencast/pull/464)] -
  change extract-text encoding profile for better OCR results…
- [[MH-13137](https://opencast.jira.com/browse/MH-13137)][[#462](https://github.com/opencast/opencast/pull/462)] -
  Less extensive statistics configuration
- [[MH-13136](https://opencast.jira.com/browse/MH-13136)][[#461](https://github.com/opencast/opencast/pull/461)] -
  Add Danish Translation
- [[MH-13133](https://opencast.jira.com/browse/MH-13133)][[#459](https://github.com/opencast/opencast/pull/459)] -
  TypeError: Cannot read property 'results' of null
- [[MH-13092](https://opencast.jira.com/browse/MH-13092)][[#458](https://github.com/opencast/opencast/pull/458)] -
  Fix failing scheduling for non-english browsers
- [[MH-13132](https://opencast.jira.com/browse/MH-13132)][[#457](https://github.com/opencast/opencast/pull/457)] -
  Fix REST Docs Overview Rendering
- [[MH-13131](https://opencast.jira.com/browse/MH-13131)][[#456](https://github.com/opencast/opencast/pull/456)] -
  Fix Feed Service REST Docs
- [[#455](https://github.com/opencast/opencast/pull/455)] -
  Remove misleading - sign in tag woh docs
- [[MH-13125](https://opencast.jira.com/browse/MH-13125)][[#451](https://github.com/opencast/opencast/pull/451)] -
  Remove unused configuration keys
- [[MH-13123](https://opencast.jira.com/browse/MH-13123)][[#448](https://github.com/opencast/opencast/pull/448)] -
  Update paella player 6.0.2
- [[MH-13117](https://opencast.jira.com/browse/MH-13117)][[#445](https://github.com/opencast/opencast/pull/445)] -
  Mark NPM managed modules as private packages
- [[MH-13116](https://opencast.jira.com/browse/MH-13116)][[#444](https://github.com/opencast/opencast/pull/444)] -
  Fix typo in paella error message
- [[MH-13115](https://opencast.jira.com/browse/MH-13115)][[#443](https://github.com/opencast/opencast/pull/443)] -
  Update Node, NPM and Libs
- [[MH-13114](https://opencast.jira.com/browse/MH-13114)][[#442](https://github.com/opencast/opencast/pull/442)] -
  Fix broken REST docs
- [[MH-13113](https://opencast.jira.com/browse/MH-13113)][[#441](https://github.com/opencast/opencast/pull/441)] -
  Drop unused HTML page
- [[MH-13025](https://opencast.jira.com/browse/MH-13025)][[#439](https://github.com/opencast/opencast/pull/439)] -
  Fix workflow-definitions URL
- [[MH-13109](https://opencast.jira.com/browse/MH-13109)][[#438](https://github.com/opencast/opencast/pull/438)] -
  Update Paella Player to 6.0.x
- [[MH-13107](https://opencast.jira.com/browse/MH-13107)][[#436](https://github.com/opencast/opencast/pull/436)] -
  Update admin interface build dependencies
- [[MH-13106](https://opencast.jira.com/browse/MH-13106)][[#435](https://github.com/opencast/opencast/pull/435)] -
  Add Moodle groups to Moodle role provider
- [[MH-13105](https://opencast.jira.com/browse/MH-13105)][[#434](https://github.com/opencast/opencast/pull/434)] -
  Fix minor mattermost notification operation issues
- [[MH-13104](https://opencast.jira.com/browse/MH-13104)][[#433](https://github.com/opencast/opencast/pull/433)] -
  Add linter for LTI tools
- [[MH-13103](https://opencast.jira.com/browse/MH-13103)][[#432](https://github.com/opencast/opencast/pull/432)] -
  Runtime UI NG JavaScript Dependencies
- [[MH-13102](https://opencast.jira.com/browse/MH-13102)][[#431](https://github.com/opencast/opencast/pull/431)] -
  Add linter (checkstyle) for JavaScript to engage-paella-player module
- [[MH-13097](https://opencast.jira.com/browse/MH-13097)][[#429](https://github.com/opencast/opencast/pull/429)] -
  Added a configuration parameter to be able to send HTML emails
- [[MH-13101](https://opencast.jira.com/browse/MH-13101)][[#428](https://github.com/opencast/opencast/pull/428)] -
  Update paella dependencies
- [[MH-13100](https://opencast.jira.com/browse/MH-13100)][[#427](https://github.com/opencast/opencast/pull/427)] -
  fix series view in Paella
- [[MH-13099](https://opencast.jira.com/browse/MH-13099)][[#426](https://github.com/opencast/opencast/pull/426)] -
  Warn when default credentials are being used
- [[MH-13096](https://opencast.jira.com/browse/MH-13096)][[#425](https://github.com/opencast/opencast/pull/425)] -
  Set workflow variables with duplicated media package IDs
- [[MH-13095](https://opencast.jira.com/browse/MH-13095)][[#424](https://github.com/opencast/opencast/pull/424)] -
  Add linter (checkstyle) for JavaScript
- [[MH-13083](https://opencast.jira.com/browse/MH-13083)][[#423](https://github.com/opencast/opencast/pull/423)] -
  Unify modal navigation
- [[MH-13094](https://opencast.jira.com/browse/MH-13094)][[#422](https://github.com/opencast/opencast/pull/422)] -
  Use global NPM repository
- [[MH-13090](https://opencast.jira.com/browse/MH-13090)][[#420](https://github.com/opencast/opencast/pull/420)] -
  Added support for blacklisting languages from the admin UI
- [[MH-12699](https://opencast.jira.com/browse/MH-12699)][[#419](https://github.com/opencast/opencast/pull/419)] -
  Remove opencast-paella binding dependency on Admin server
- [[MH-13088](https://opencast.jira.com/browse/MH-13088)][[#417](https://github.com/opencast/opencast/pull/417)] -
  Update Several Dependencies
- [[MH-13087](https://opencast.jira.com/browse/MH-13087)][[#416](https://github.com/opencast/opencast/pull/416)] -
  Update Runtime UI Libraries
- [[MH-13086](https://opencast.jira.com/browse/MH-13086)][[#415](https://github.com/opencast/opencast/pull/415)] -
  Update LTI Series Tool
- [[MH-13079](https://opencast.jira.com/browse/MH-13079)][[#413](https://github.com/opencast/opencast/pull/413)] -
  Introduce REST Interface for AssetManager Properties
- [[MH-13060](https://opencast.jira.com/browse/MH-13060)][[#412](https://github.com/opencast/opencast/pull/412)] -
  Add i18n support for workflow, operations, job and services status
- [[MH-13073](https://opencast.jira.com/browse/MH-13073)][[#411](https://github.com/opencast/opencast/pull/411)] -
  Don't split series metadata fields by ,
- [[MH-13074](https://opencast.jira.com/browse/MH-13074)][[#410](https://github.com/opencast/opencast/pull/410)] -
  Clean up asset manager REST endpoints
- [[MH-13072](https://opencast.jira.com/browse/MH-13072)][[#409](https://github.com/opencast/opencast/pull/409)] -
  Remove broken ltitool player
- [[MH-13071](https://opencast.jira.com/browse/MH-13071)][[#408](https://github.com/opencast/opencast/pull/408)] -
  Update markdown linter
- [[MH-13070](https://opencast.jira.com/browse/MH-13070)][[#407](https://github.com/opencast/opencast/pull/407)] -
  Update JS build and test libraries
- [[MH-13064](https://opencast.jira.com/browse/MH-13064)][[#399](https://github.com/opencast/opencast/pull/399)] -
  Encoding profile mimetypes are mostly ignored
- [[MH-13058](https://opencast.jira.com/browse/MH-13058)][[#395](https://github.com/opencast/opencast/pull/395)] -
  Remove unused font libraries
- [[MH-12688](https://opencast.jira.com/browse/MH-12688)][[#392](https://github.com/opencast/opencast/pull/392)] -
  Add translations for comment filter values
- [[MH-13045](https://opencast.jira.com/browse/MH-13045)][[#391](https://github.com/opencast/opencast/pull/391)] -
  Add missing i18n translations
- [[MH-13040](https://opencast.jira.com/browse/MH-13040)][[#388](https://github.com/opencast/opencast/pull/388)] -
  Make options fit “Actions” drop-down
- [[MH-12810](https://opencast.jira.com/browse/MH-12810)][[#387](https://github.com/opencast/opencast/pull/387)] -
  External API 1.1.0 - Add filters for new fields
- [[MH-13037](https://opencast.jira.com/browse/MH-13037)][[#386](https://github.com/opencast/opencast/pull/386)] -
  Remove unused External API roles
- [[MH-12690](https://opencast.jira.com/browse/MH-12690)][[#384](https://github.com/opencast/opencast/pull/384)] -
  Add i18n support for capture agent statuses
- [[MH-12761](https://opencast.jira.com/browse/MH-12761)][[#382](https://github.com/opencast/opencast/pull/382)] -
  Fixed event to listen to "plugin.events.captionsFound".
- [[MH-13028](https://opencast.jira.com/browse/MH-13028)][[#381](https://github.com/opencast/opencast/pull/381)] -
  Clean up mockup
- [[MH-13022](https://opencast.jira.com/browse/MH-13022)][[#378](https://github.com/opencast/opencast/pull/378)] -
  fixed LTI highly trusted keys being discarded
- [[#376](https://github.com/opencast/opencast/pull/376)] -
  Update and improve documentation for reviews
- [[MH-13027](https://opencast.jira.com/browse/MH-13027)][[#374](https://github.com/opencast/opencast/pull/374)] -
  Update angular-translate to 2.18.1
- [[MH-13026](https://opencast.jira.com/browse/MH-13026)][[#373](https://github.com/opencast/opencast/pull/373)] -
  Update Mac OS X 'Install from source' documentation
- [[MH-13025](https://opencast.jira.com/browse/MH-13025)][[#372](https://github.com/opencast/opencast/pull/372)] -
  Add workflow API to external API
- [[MH-13024](https://opencast.jira.com/browse/MH-13024)][[#371](https://github.com/opencast/opencast/pull/371)] -
  Video editor does not display information when being opened while an event is being processed
- [[#369](https://github.com/opencast/opencast/pull/369)] -
  Documentation: message-broker: binding localhost
- [[#368](https://github.com/opencast/opencast/pull/368)] -
  Documentation: Update security.https.md
- [[MH-13016](https://opencast.jira.com/browse/MH-13016)][[#362](https://github.com/opencast/opencast/pull/362)] -
  Workflow display order not working in editor screen
- [[MH-13013](https://opencast.jira.com/browse/MH-13013)][[#359](https://github.com/opencast/opencast/pull/359)] -
  Unused code in scheduler
- [[MH-13008](https://opencast.jira.com/browse/MH-13008)][[#358](https://github.com/opencast/opencast/pull/358)] -
  Prefill other input of startdate filter
- [[MH-13012](https://opencast.jira.com/browse/MH-13012)][[#357](https://github.com/opencast/opencast/pull/357)] -
  The iterable metadata values should not be splitted by `,`
- [[MH-13010](https://opencast.jira.com/browse/MH-13010)][[#356](https://github.com/opencast/opencast/pull/356)] -
  Series-Service-Remote incorrect character encoding
- [[MH-13009](https://opencast.jira.com/browse/MH-13009)][[#355](https://github.com/opencast/opencast/pull/355)] -
  Update translations
- [[MH-13007](https://opencast.jira.com/browse/MH-13007)][[#354](https://github.com/opencast/opencast/pull/354)] -
  Clarify Scheduler Calendar cutoff units in REST docs
- [[MH-12829](https://opencast.jira.com/browse/MH-12829)][[#348](https://github.com/opencast/opencast/pull/348)] -
  Make admin-ui statistics configurable
- [[MH-12998](https://opencast.jira.com/browse/MH-12998)][[#346](https://github.com/opencast/opencast/pull/346)] -
  Clear conflicts when closing “Edit Scheduled Events” modal
- [[MH-12996](https://opencast.jira.com/browse/MH-12996)][[#345](https://github.com/opencast/opencast/pull/345)] -
  Add header row to conflict table in “Edit scheduled”
- [[MH-12995](https://opencast.jira.com/browse/MH-12995)][[#344](https://github.com/opencast/opencast/pull/344)] -
  Fix conflict check not detecting some conflicts
- [[MH-12990](https://opencast.jira.com/browse/MH-12990)][[#343](https://github.com/opencast/opencast/pull/343)] -
  User switching: Privilege escalation too restrictive
- [[MH-12993](https://opencast.jira.com/browse/MH-12993)][[#342](https://github.com/opencast/opencast/pull/342)] -
  REST docs for Admin UI Event endpoint broken
- [[MH-12994](https://opencast.jira.com/browse/MH-12994)][[#341](https://github.com/opencast/opencast/pull/341)] -
  Make “Title” in “Edit scheduled” non-mandatory
- [[MH-12992](https://opencast.jira.com/browse/MH-12992)][[#340](https://github.com/opencast/opencast/pull/340)] -
  Trigger conflict check in “Edit scheduled” on “Next”
- [[MH-12989](https://opencast.jira.com/browse/MH-12989)][[#338](https://github.com/opencast/opencast/pull/338)] -
  Add missing roles for actions->edit scheduled
- [[#336](https://github.com/opencast/opencast/pull/336)] -
  Update version info
- [[MH-12987](https://opencast.jira.com/browse/MH-12987)][[#335](https://github.com/opencast/opencast/pull/335)] -
  Prohibit changing a scheduled event to be in the past
- [[MH-12985](https://opencast.jira.com/browse/MH-12985)][[#332](https://github.com/opencast/opencast/pull/332)] -
  Fix incorrect warnings in event modals
- [[MH-12803](https://opencast.jira.com/browse/MH-12803)][[#329](https://github.com/opencast/opencast/pull/329)] -
  Fix for mp 'start' when event is created (affects live scheduler service)
- [[MH-12980](https://opencast.jira.com/browse/MH-12980)][[#328](https://github.com/opencast/opencast/pull/328)] -
  Update documentation landign page
- [[MH-12930](https://github.com/opencast/opencast/pull/327)][[#327](https://github.com/opencast/opencast/pull/327)] -
  Fill creator metadata field with actual user when new event
- [[MH-12977](https://opencast.jira.com/browse/MH-12977)][[#322](https://github.com/opencast/opencast/pull/322)] -
  Fix data placeholders in edit scheduled events
- [[MH-11918](https://opencast.jira.com/browse/MH-11918)][[#321](https://github.com/opencast/opencast/pull/321)] -
  AWS S3 Asset Storage
- [[MH-12975](https://opencast.jira.com/browse/MH-12975)][[#320](https://github.com/opencast/opencast/pull/320)] -
  Inconsistent access control handling
- [[MH-12738](https://opencast.jira.com/browse/MH-12738)][[#319](https://github.com/opencast/opencast/pull/319)] -
  Tiered Storage for the Asset Manager
- [[MH-12969](https://opencast.jira.com/browse/MH-12969)][[#317](https://github.com/opencast/opencast/pull/317)] -
  Eclipse IDE import Opencast XML style preferences
- [[MH-12972](https://opencast.jira.com/browse/MH-12972)][[#316](https://github.com/opencast/opencast/pull/316)] -
  Drop unused getAclAttachments
- [[MH-12969](https://opencast.jira.com/browse/MH-12969)][[#314](https://github.com/opencast/opencast/pull/314)] -
  Ensure formatting of OSGI configuration
- [[#313](https://github.com/opencast/opencast/pull/313)] -
  NOJIRA-live-schedule-fix-issue-in-documatation
- [[MH-12965](https://opencast.jira.com/browse/MH-12965)][[#311](https://github.com/opencast/opencast/pull/311)] -
  Add more logging data to metadata parse WARN
- [[MH-12961](https://opencast.jira.com/browse/MH-12961)][[#308](https://github.com/opencast/opencast/pull/308)] -
  Remove unused JavaScript library bootstrap from Admin UI
- [[MH-12960](https://opencast.jira.com/browse/MH-12960)][[#307](https://github.com/opencast/opencast/pull/307)] -
  Remove unused JavaScript library backbone.js from Admin UI
- [[MH-12959](https://opencast.jira.com/browse/MH-12959)][[#306](https://github.com/opencast/opencast/pull/306)] -
  Remove unused JavaScript library visualsearch.js
- [[MH-12956](https://opencast.jira.com/browse/MH-12956)][[#305](https://github.com/opencast/opencast/pull/305)] -
  Incorrect permission check when requesting indexed workflows
- [[MH-12958](https://opencast.jira.com/browse/MH-12958)][[#301](https://github.com/opencast/opencast/pull/301)] -
  image-convert WOH
- [[MH-12607](https://opencast.jira.com/browse/MH-12607)][[#299](https://github.com/opencast/opencast/pull/299)] -
  Multiencode
- [[MH-12955](https://opencast.jira.com/browse/MH-12955)][[#298](https://github.com/opencast/opencast/pull/298)] -
  ffmpeg expect floating timestamp values separated by '.'
- [[MH-12949](https://opencast.jira.com/browse/MH-12949)][[#294](https://github.com/opencast/opencast/pull/294)] -
  Fix spacing between action items
- [[MH-12946](https://opencast.jira.com/browse/MH-12946)][[#292](https://github.com/opencast/opencast/pull/292)] -
  add event summary input translation
- [[MH-12948](https://opencast.jira.com/browse/MH-12948)][[#291](https://github.com/opencast/opencast/pull/291)] -
  Directly read XACML files
- [[MH-12905](https://opencast.jira.com/browse/MH-12905)][[#289](https://github.com/opencast/opencast/pull/289)] -
  Opencast does not startup anymore
- [[MH-12911](https://opencast.jira.com/browse/MH-12911)][[#266](https://github.com/opencast/opencast/pull/266)] -
  Hotkey cheat sheet
- [[MH-12813](https://opencast.jira.com/browse/MH-12813)][[#265](https://github.com/opencast/opencast/pull/265)] -
  Add audio and video track selection to video editor
- [[MH-12607](https://opencast.jira.com/browse/MH-12607)][[#264](https://github.com/opencast/opencast/pull/264)] -
  Process-Smil - edit and encode to multiple delivery formats
- [[MH-12918](https://opencast.jira.com/browse/MH-12918)][[#261](https://github.com/opencast/opencast/pull/261)] -
  Use Karaf generated jre.properties
- [[MH-12904](https://opencast.jira.com/browse/MH-12904)][[#252](https://github.com/opencast/opencast/pull/252)] -
  Paella player 5.3 update
- [[MH-12829](https://opencast.jira.com/browse/MH-12829)][[#237](https://github.com/opencast/opencast/pull/237)] -
  Fix broken sub tabs of Event Details-\>Assets
- [[MH-12889](https://opencast.jira.com/browse/MH-12889)][[#236](https://github.com/opencast/opencast/pull/236)] -
  Intuitive Merging of Video Segments
- [[MH-12828](https://opencast.jira.com/browse/MH-12828)][[#233](https://github.com/opencast/opencast/pull/233)] -
  re-enable Scheduler service conflicts json REST endpoint
- [[MH-12885](https://opencast.jira.com/browse/MH-12885)][[#232](https://github.com/opencast/opencast/pull/232)] -
  Capture Agent Access Management
- [[MH-12877](https://opencast.jira.com/browse/MH-12877)][[#231](https://github.com/opencast/opencast/pull/231)] -
  Add new modal to edit multiple scheduled events at once
- [[MH-12871](https://opencast.jira.com/browse/MH-12871)][[#220](https://github.com/opencast/opencast/pull/220)] -
  Ability to use user names in to/cc/bcc fields in send-email woh
- [[MH-12869](https://opencast.jira.com/browse/MH-12869)][[#219](https://github.com/opencast/opencast/pull/219)] -
  Remove superfluous playback tool
- [[MH-12829](https://opencast.jira.com/browse/MH-12829)][[#218](https://github.com/opencast/opencast/pull/218)] -
  Switch and rename event details tabs
- [[MH-12814](https://opencast.jira.com/browse/MH-12814)][[#208](https://github.com/opencast/opencast/pull/208)] -
  Manually Select And Upload Thumbnails
- [[MH-12815](https://opencast.jira.com/browse/MH-12815)][[#197](https://github.com/opencast/opencast/pull/197)] -
  delete series with events option
- [[MH-12826](https://opencast.jira.com/browse/MH-12826)][[#193](https://github.com/opencast/opencast/pull/193)] -
  Make workflow processing settings persistent
- [[MH-12823](https://opencast.jira.com/browse/MH-12823)][[#182](https://github.com/opencast/opencast/pull/182)] -
  Log Configuration and GELF Log4J with graylog
- [[#181](https://github.com/opencast/opencast/pull/181)] -
  adapt tracking default options to respect the EU GDPR
- [[MH-12822](https://opencast.jira.com/browse/MH-12822)][[#179](https://github.com/opencast/opencast/pull/179)] -
  Remove old OCv2x security context fix artifacts
- [[MH-12607](https://opencast.jira.com/browse/MH-12607)][[#172](https://github.com/opencast/opencast/pull/172)] -
  Harvard DCE), Demux Operation
- [[MH-12607](https://opencast.jira.com/browse/MH-12607)][[#171](https://github.com/opencast/opencast/pull/171)] -
  Harvard DCE), Lossless Concat Operation
- [[MH-12804](https://opencast.jira.com/browse/MH-12804)][[#170](https://github.com/opencast/opencast/pull/170)] -
  Introduce displayOrder for workflow definitions
- [[MH-12797](https://opencast.jira.com/browse/MH-12797)][[#168](https://github.com/opencast/opencast/pull/168)] -
  Explain UI actions (added missing tooltips)
- [[MH-12820](https://opencast.jira.com/browse/MH-12820)][[#167](https://github.com/opencast/opencast/pull/167)] -
  Mattermost-notification-workflowoperationhandler
- [[#165](https://github.com/opencast/opencast/pull/165)] -
  Be less quiet about errors on Travis
- [[MH-12797](https://opencast.jira.com/browse/MH-12797)][[#164](https://github.com/opencast/opencast/pull/164)] -
  Explain UI Actions
- [[MH-12794](https://opencast.jira.com/browse/MH-12794)][[#162](https://github.com/opencast/opencast/pull/162)] -
  turn off matomo notification
- [[MH-12793](https://opencast.jira.com/browse/MH-12793)][[#161](https://github.com/opencast/opencast/pull/161)] -
  Collapse multiple, redundant composer process methods
- [[MH-12647](https://opencast.jira.com/browse/MH-12647)][[#155](https://github.com/opencast/opencast/pull/155)] -
  MH-12756 extend external api
- [[MH-12786](https://opencast.jira.com/browse/MH-12786)][[#154](https://github.com/opencast/opencast/pull/154)] -
  Undistinguishable Entries in Groups Editor User List
- [[MH-12784](https://opencast.jira.com/browse/MH-12784)][[#153](https://github.com/opencast/opencast/pull/153)] -
  External API: Accept header not specified correctly
- [[MH-12091](https://opencast.jira.com/browse/MH-12091)][[#150](https://github.com/opencast/opencast/pull/150)] -
  Implement per-tenant digest user for capture agents
- [[MH-12703](https://opencast.jira.com/browse/MH-12703)][[#89](https://github.com/opencast/opencast/pull/89)] -
  Add userdirectory for Moodle
- [[MH-11621](https://opencast.jira.com/browse/MH-11621)][[#56](https://github.com/opencast/opencast/pull/56)] -
  Option to marshal empty values in DublinCore XML catalog.


Opencast 5
----------

### Opencast 5.5

*Released on April 1, 2019*

- [[MH-12603](https://opencast.jira.com/browse/MH-12603)][[#746](https://github.com/opencast/opencast/pull/746)] -
  Take 'ng' out of the youtube composite operation
- [[MH-13386](https://opencast.jira.com/browse/MH-13386)][[#733](https://github.com/opencast/opencast/pull/733)] -
  Event status calculation wrong assumption fixed
- [[MH-13383](https://opencast.jira.com/browse/MH-13383)][[#728](https://github.com/opencast/opencast/pull/728)] -
  don't smooth the waveform in the editor
- [[MH-13366](https://opencast.jira.com/browse/MH-13366)][[#708](https://github.com/opencast/opencast/pull/708)] -
  Add `REFERENCES` permission to standard Opencast `GRANT` statement
- [[MH-13363](https://opencast.jira.com/browse/MH-13363)][[#706](https://github.com/opencast/opencast/pull/706)] -
  Publish to OAI-PMH an allready published mediapackage …
- [[MH-13333](https://opencast.jira.com/browse/MH-13333)][[#669](https://github.com/opencast/opencast/pull/669)] -
  Do not import properties in publish WF

### Opencast 5.4

*Released on January 24, 2019*

- [[MH-13311](https://opencast.jira.com/browse/MH-13311)][[#652](https://github.com/opencast/opencast/pull/652)] -
  WOH cover-image is broken
- [SUREFIRE-1588: Resolving compilation issue on Debian and related distros
  ](https://github.com/opencast/opencast/pull/651)
- [[MH-13244](https://opencast.jira.com/browse/MH-13244)][[#581](https://github.com/opencast/opencast/pull/581)] -
  Improve concurrency of OAIPMH republication


### Opencast 5.3

*Released on January 11, 2019*

- [[MH-13297](https://opencast.jira.com/browse/MH-13297)][[#638](https://github.com/opencast/opencast/pull/638)] -
  FasterXML Jackson Bugfix Update
- [[MH-13296](https://opencast.jira.com/browse/MH-13296)][[#637](https://github.com/opencast/opencast/pull/637)] -
  Disable buttons of start task wizard while the tasks are being submitted
- [[MH-12290](https://opencast.jira.com/browse/MH-12290)][[#636](https://github.com/opencast/opencast/pull/636)] -
  prevent SAXParserFactory and SAXParser class load lag in series listprovider
- [[MH-13269](https://opencast.jira.com/browse/MH-13269)][[#608](https://github.com/opencast/opencast/pull/608)] -
  Handle Authorization Errors
- [[MH-13263](https://opencast.jira.com/browse/MH-13263)][[#598](https://github.com/opencast/opencast/pull/598)] -
  Invalid Ingest Encoding
- [[MH-13257](https://opencast.jira.com/browse/MH-13257)][[#597](https://github.com/opencast/opencast/pull/597)] -
  Fix outdated command line argument for tesseract >= 4.0.0
- [[MH-13258](https://opencast.jira.com/browse/MH-13258)][[#592](https://github.com/opencast/opencast/pull/592)] -
  Broken User Provider Removal
- [[MH-13256](https://opencast.jira.com/browse/MH-13256)][[#591](https://github.com/opencast/opencast/pull/591)] -
  Waveform operation fails
- [[MH-13243](https://opencast.jira.com/browse/MH-13243)][[#580](https://github.com/opencast/opencast/pull/580)] -
  Asset Manager ACL Cache Updates
- [[#572](https://github.com/opencast/opencast/pull/572)] -
  Documentation: Opencast 5.2 was released in Nov
- [[#571](https://github.com/opencast/opencast/pull/571)] -
  Documentation: Linkfixes in OC5.x upgrade guide
- [[MH-12332](https://opencast.jira.com/browse/MH-12332)][[#567](https://github.com/opencast/opencast/pull/567)] -
  disable workflows whose tags don't explicitly match the source type, UPLOAD|SCHEDULE 5.x

### Opencast 5.2

*Released on November 13, 2018*

- [[MH-13144](https://opencast.jira.com/browse/MH-13144)][[#553](https://github.com/opencast/opencast/pull/553)] -
  only set Job startDate if no set before
- [[MH-13216](https://opencast.jira.com/browse/MH-13216)][[#550](https://github.com/opencast/opencast/pull/550)] -
  Fix Documentation Pages
- [[MH-13211](https://opencast.jira.com/browse/MH-13211)][[#547](https://github.com/opencast/opencast/pull/547)] -
  engage-ui: Fix live schedule bug: event available before schedule
- [[MH-13190](https://opencast.jira.com/browse/MH-13190)][[#520](https://github.com/opencast/opencast/pull/520)] -
  Factor out JpaGroupRoleProvider JaxRs REST to mitigate load cycle race
- [[MH-13189](https://opencast.jira.com/browse/MH-13189)][[#517](https://github.com/opencast/opencast/pull/517)] -
  Fix paella xss security isues in opencast 5.x
- [[MH-13167](https://opencast.jira.com/browse/MH-13167)][[#490](https://github.com/opencast/opencast/pull/490)] -
  Republishing metadata does not update all metadata
- [[MH-13152](https://opencast.jira.com/browse/MH-13152)][[#476](https://github.com/opencast/opencast/pull/476)] -
  Reduce Workflow Messages
- [[MH-13138](https://opencast.jira.com/browse/MH-13138)][[#463](https://github.com/opencast/opencast/pull/463)] -
  Fix media module language configuration
- [[MH-13108](https://opencast.jira.com/browse/MH-13108)][[#437](https://github.com/opencast/opencast/pull/437)] -
  Prevent permission problem in Travis cache
- [[MH-13091](https://opencast.jira.com/browse/MH-13091)][[#421](https://github.com/opencast/opencast/pull/421)] -
  Concat operation problem with FFMPEG 4.x
- [[MH-13069](https://opencast.jira.com/browse/MH-13069)][[#406](https://github.com/opencast/opencast/pull/406)] -
  Update problematic admin interface libraries
- [[MH-12976](https://opencast.jira.com/browse/MH-12976)][[#389](https://github.com/opencast/opencast/pull/389)] -
  custom role patterns not working
- [[MH-12387](https://opencast.jira.com/browse/MH-12387)][[#350](https://github.com/opencast/opencast/pull/350)] -
  Fix CAS


### Opencast 5.1

*Released on September 3, 2018*

- [[MH-13067](https://opencast.jira.com/browse/MH-13067)][[#404](https://github.com/opencast/opencast/pull/404)] -
  Configuration panel does not work for default workflow
- [[MH-13049](https://opencast.jira.com/browse/MH-13049)][[#400](https://github.com/opencast/opencast/pull/400)] -
  Fix video editor zoom dropdown showing wrong value
- [[MH-13055](https://opencast.jira.com/browse/MH-13055)][[#396](https://github.com/opencast/opencast/pull/396)] -
  Stop making events with no ACL public on ingest
- [[MH-13048](https://opencast.jira.com/browse/MH-13048)][[#394](https://github.com/opencast/opencast/pull/394)] -
  Improve stability of the series index rebuild
- [[MH-13047](https://opencast.jira.com/browse/MH-13047)][[#393](https://github.com/opencast/opencast/pull/393)] -
  Document using Nginx for HTTPS
- [[MH-13044](https://opencast.jira.com/browse/MH-13044)][[#390](https://github.com/opencast/opencast/pull/390)] -
  Organization server configuration documentation
- [[MH-12016](https://opencast.jira.com/browse/MH-12016)][[#379](https://github.com/opencast/opencast/pull/379)] -
  Scrolling role fetch
- [[MH-13031](https://opencast.jira.com/browse/MH-13031)][[#377](https://github.com/opencast/opencast/pull/377)] -
  Active transaction notification on top
- [[MH-13029](https://opencast.jira.com/browse/MH-13029)][[#375](https://github.com/opencast/opencast/pull/375)] -
  Don't show old notifications
- [[MH-13023](https://opencast.jira.com/browse/MH-13023)][[#370](https://github.com/opencast/opencast/pull/370)] -
  Let default value fulfill requirement
- [[MH-13018](https://opencast.jira.com/browse/MH-13018)][[#367](https://github.com/opencast/opencast/pull/367)] -
  re-add recordings json to 5x (includes MH-12828 re-add conflicts.json)
- [[MH-13020](https://opencast.jira.com/browse/MH-13020)][[#366](https://github.com/opencast/opencast/pull/366)] -
  Read listproviders as UTF-8
- [[MH-13017](https://opencast.jira.com/browse/MH-13017)][[#363](https://github.com/opencast/opencast/pull/363)] -
  JS syntax error in publish workflow
- [[MH-13015](https://opencast.jira.com/browse/MH-13015)][[#361](https://github.com/opencast/opencast/pull/361)] -
  5.x database upgrade scripts
- [[MH-13014](https://opencast.jira.com/browse/MH-13014)][[#360](https://github.com/opencast/opencast/pull/360)] -
  Don't show stale search results
- [[MH-13006](https://opencast.jira.com/browse/MH-13006)][[#353](https://github.com/opencast/opencast/pull/353)] -
  Waveform operation cleanup creates problem with asynchronous NFS
- [[MH-13003](https://opencast.jira.com/browse/MH-13003)][[#352](https://github.com/opencast/opencast/pull/352)] -
  Implement detection of already recorded (as opposed to yet to be recorded, scheduled) events by the index service
- [[MH-13005](https://opencast.jira.com/browse/MH-13005)][[#351](https://github.com/opencast/opencast/pull/351)] -
  Skip waveform operation when no tracks
- [[MH-13001](https://opencast.jira.com/browse/MH-13001)][[#347](https://github.com/opencast/opencast/pull/347)] -
  Fixed live scheduler service pom
- [[MH-12988](https://opencast.jira.com/browse/MH-12988)][[#337](https://github.com/opencast/opencast/pull/337)] -
  delete-scheduled-live Fix for scheduled live event not deleted
- [[MH-12986](https://opencast.jira.com/browse/MH-12986)][[#333](https://github.com/opencast/opencast/pull/333)] -
  Admin UI deployed debugging: include source in SourceMap files
- [[MH-12981](https://opencast.jira.com/browse/MH-12981)][[#331](https://github.com/opencast/opencast/pull/331)] -
  fix for local admin-ui develop finding main.css
- [[MH-12979](https://opencast.jira.com/browse/MH-12979)][[#325](https://github.com/opencast/opencast/pull/325)] -
  Automatically test ddl scripts
- [[MH-12978](https://opencast.jira.com/browse/MH-12978)][[#324](https://github.com/opencast/opencast/pull/324)] -
  Fix data-placeholder in add event wizard
- [[MH-12974](https://opencast.jira.com/browse/MH-12974)][[#318](https://github.com/opencast/opencast/pull/318)] -
  Access denial to event for unprivileged user
- [[MH-12970](https://opencast.jira.com/browse/MH-12970)][[#315](https://github.com/opencast/opencast/pull/315)] -
  Senseless XACML parsing
- [[MH-12966](https://opencast.jira.com/browse/MH-12966)][[#312](https://github.com/opencast/opencast/pull/312)] -
  Do not pre-select-from option in metadata property sheets
- [[MH-12963](https://opencast.jira.com/browse/MH-12963)][[#310](https://github.com/opencast/opencast/pull/310)] -
  Localize dates/times in add-event summary
- [[MH-12950](https://opencast.jira.com/browse/MH-12950)][[#309](https://github.com/opencast/opencast/pull/309)] -
  Fix for workflow with no acl in solr index
- [NOJIRA: Skip install of Crowdin if it is already installed
  ](https://github.com/opencast/opencast/pull/304)
- [[MH-12957](https://opencast.jira.com/browse/MH-12957)][[#300](https://github.com/opencast/opencast/pull/300)] -
  Defaults on tab Source in Add Event wizards are broken
- [[MH-12954](https://opencast.jira.com/browse/MH-12954)][[#297](https://github.com/opencast/opencast/pull/297)] -
  wrong date format in coverimage file


### Opencast 5.0

*Released on June 12, 2018*

- [[MH-12952](https://opencast.jira.com/browse/MH-12952)][[#295](https://github.com/opencast/opencast/pull/295)] -
  animate WOH dependency version fixed
- [[MH-12946](https://opencast.jira.com/browse/MH-12946)][[#290](https://github.com/opencast/opencast/pull/290)] -
  Fix summary of add-event-dialog
- [[MH-12944](https://opencast.jira.com/browse/MH-12944)][[#288](https://github.com/opencast/opencast/pull/288)] -
  Remove bashism from start script
- [[MH-12905](https://opencast.jira.com/browse/MH-12905)][[#287](https://github.com/opencast/opencast/pull/287)] -
  TEMPORARY Karaf config assembly workaround (KARAF-5693)
- [[MH-12943](https://opencast.jira.com/browse/MH-12943)][[#286](https://github.com/opencast/opencast/pull/286)] -
  Minor Paella config REST endpoint improvements
- [[MH-12942](https://opencast.jira.com/browse/MH-12942)][[#285](https://github.com/opencast/opencast/pull/285)] -
  Paella player config REST endpoint should be accessible by anonymous user
- [[MH-12941](https://opencast.jira.com/browse/MH-12941)][[#284](https://github.com/opencast/opencast/pull/284)] -
  Gracefully handle empty flavors
- [[MH-12940](https://opencast.jira.com/browse/MH-12940)][[#283](https://github.com/opencast/opencast/pull/283)] -
  Ensure admin configuration is applied
- [[MH-12864](https://opencast.jira.com/browse/MH-12864)][[#282](https://github.com/opencast/opencast/pull/282)] -
  Don't attempt to parse 'undefined'
- [[MH-12938](https://opencast.jira.com/browse/MH-12938)][[#281](https://github.com/opencast/opencast/pull/281)] -
  Fix NullPointerException if no flavor is set
- [[MH-12937](https://opencast.jira.com/browse/MH-12937)][[#280](https://github.com/opencast/opencast/pull/280)] -
  Correctly place admin UI test helper
- [[MH-12936](https://opencast.jira.com/browse/MH-12936)][[#279](https://github.com/opencast/opencast/pull/279)] -
  Handle invalid flavors
- [[MH-12935](https://opencast.jira.com/browse/MH-12935)][[#278](https://github.com/opencast/opencast/pull/278)] -
  Update Docker image repository documentation
- [[MH-12934](https://opencast.jira.com/browse/MH-12934)][[#277](https://github.com/opencast/opencast/pull/277)] -
  Update translations
- [[MH-12933](https://opencast.jira.com/browse/MH-12933)][[#276](https://github.com/opencast/opencast/pull/276)] -
  Link documentation from Systemd unit
- [[MH-12932](https://opencast.jira.com/browse/MH-12932)][[#275](https://github.com/opencast/opencast/pull/275)] -
  Kernel Build Failure
- [[MH-12922](https://opencast.jira.com/browse/MH-12922)][[#272](https://github.com/opencast/opencast/pull/272)] -
  Job load fixes
- [[MH-12929](https://opencast.jira.com/browse/MH-12929)][[#271](https://github.com/opencast/opencast/pull/271)] -
  Change paella URL to /paella/ui
- [[MH-12928](https://opencast.jira.com/browse/MH-12928)][[#270](https://github.com/opencast/opencast/pull/270)] -
  Mitigation for KARAF-5526
- [[MH-12926](https://opencast.jira.com/browse/MH-12926)][[#269](https://github.com/opencast/opencast/pull/269)] -
  Prevent cluttering of logs by invalid access
- [[MH-12924](https://opencast.jira.com/browse/MH-12924)][[#268](https://github.com/opencast/opencast/pull/268)] -
  fix missing dropdown arrow
- [[MH-12919](https://opencast.jira.com/browse/MH-12919)][[#262](https://github.com/opencast/opencast/pull/262)] -
  REST Docs Dependencies
- [[MH-12917](https://opencast.jira.com/browse/MH-12917)][[#260](https://github.com/opencast/opencast/pull/260)] -
  Remove debug logging
- [[MH-12916](https://opencast.jira.com/browse/MH-12916)][[#259](https://github.com/opencast/opencast/pull/259)] -
  Admin Interface Configuration Defaults
- [[MH-12914](https://opencast.jira.com/browse/MH-12914)][[#258](https://github.com/opencast/opencast/pull/258)] -
  Remove deprecated IOUtils.closeQuietly
- [[MH-12913](https://opencast.jira.com/browse/MH-12913)][[#257](https://github.com/opencast/opencast/pull/257)] -
  Fix Admin Interface Deprecation Warnings
- [[MH-12868](https://opencast.jira.com/browse/MH-12868)][[#255](https://github.com/opencast/opencast/pull/255)] -
  Make frame-by-frame skipping function in the editor use the "actual" framerate
- [[MH-12908](https://opencast.jira.com/browse/MH-12908)][[#251](https://github.com/opencast/opencast/pull/251)] -
  Fix escaping of spaces
- [[MH-12907](https://opencast.jira.com/browse/MH-12907)][[#250](https://github.com/opencast/opencast/pull/250)] -
  Fix segmentation default job load
- [[MH-12906](https://opencast.jira.com/browse/MH-12906)][[#249](https://github.com/opencast/opencast/pull/249)] -
  Composoer should ignore system specific output pathes like /dev/null
- [[MH-12902](https://opencast.jira.com/browse/MH-12902)][[#248](https://github.com/opencast/opencast/pull/248)] -
  closing videoeditor should continue in events list
- [[MH-12901](https://opencast.jira.com/browse/MH-12901)][[#247](https://github.com/opencast/opencast/pull/247)] -
  Fix YouTube publication job loads
- [[MH-12900](https://opencast.jira.com/browse/MH-12900)][[#246](https://github.com/opencast/opencast/pull/246)] -
  Fix search service job loads
- [[MH-12899](https://opencast.jira.com/browse/MH-12899)][[#245](https://github.com/opencast/opencast/pull/245)] -
  Fix streaming distribution job load defaults
- [[MH-12898](https://opencast.jira.com/browse/MH-12898)][[#244](https://github.com/opencast/opencast/pull/244)] -
  Fix download distribution job load defaults
- [[MH-12897](https://opencast.jira.com/browse/MH-12897)][[#243](https://github.com/opencast/opencast/pull/243)] -
  Improve visibility of selected segments in the videoeditor
- [[MH-12896](https://opencast.jira.com/browse/MH-12896)][[#242](https://github.com/opencast/opencast/pull/242)] -
  Clarify default player configuration
- [[MH-12894](https://opencast.jira.com/browse/MH-12894)][[#240](https://github.com/opencast/opencast/pull/240)] -
  Update markdownlint
- [[MH-12893](https://opencast.jira.com/browse/MH-12893)][[#239](https://github.com/opencast/opencast/pull/239)] -
  Added ability to configure the job load for the aws s3 distribution service.
- [[MH-12892](https://opencast.jira.com/browse/MH-12892)][[#238](https://github.com/opencast/opencast/pull/238)] -
  Added ability to configure the job load for the transcription service.
- [[MH-12888](https://opencast.jira.com/browse/MH-12888)][[#235](https://github.com/opencast/opencast/pull/235)] -
  Missing FFmpeg on Travis CI
- [[MH-12887](https://opencast.jira.com/browse/MH-12887)][[#234](https://github.com/opencast/opencast/pull/234)] -
  Only set job date completed and runtime once.
- [[MH-12883](https://opencast.jira.com/browse/MH-12883)][[#230](https://github.com/opencast/opencast/pull/230)] -
  Maven build of admin-ui module without frontend profile
- [[MH-12882](https://opencast.jira.com/browse/MH-12882)][[#229](https://github.com/opencast/opencast/pull/229)] -
  Fix org.w3c.dom.smil version
- [[MH-12881](https://opencast.jira.com/browse/MH-12881)][[#228](https://github.com/opencast/opencast/pull/228)] -
  Remove deprecated method
- [[MH-12880](https://opencast.jira.com/browse/MH-12880)][[#227](https://github.com/opencast/opencast/pull/227)] -
  Remove redundant OSGI declarations
- [[MH-12879](https://opencast.jira.com/browse/MH-12879)][[#226](https://github.com/opencast/opencast/pull/226)] -
  Default location of paella configuration
- [[MH-12878](https://opencast.jira.com/browse/MH-12878)][[#224](https://github.com/opencast/opencast/pull/224)] -
  Don't verify NPM cache to speed up build process
- [[MH-12874](https://opencast.jira.com/browse/MH-12874)][[#223](https://github.com/opencast/opencast/pull/223)] -
  NotFoundException handling for OAI-PMH retract operation with non published event
- [[MH-12872](https://opencast.jira.com/browse/MH-12872)][[#222](https://github.com/opencast/opencast/pull/222)] -
  event can not be deleted
- [[MH-12873](https://opencast.jira.com/browse/MH-12873)][[#221](https://github.com/opencast/opencast/pull/221)] -
  Speed up test builds
- [[MH-12864](https://opencast.jira.com/browse/MH-12864)][[#215](https://github.com/opencast/opencast/pull/215)] -
  Readonly mode of fields not working correctly in property sheets
- [[MH-12807](https://opencast.jira.com/browse/MH-12807)][[#213](https://github.com/opencast/opencast/pull/213)] -
  Do not overwrite owner
- [[MH-12863](https://opencast.jira.com/browse/MH-12863)][[#212](https://github.com/opencast/opencast/pull/212)] -
  Fix default owner in SMIL endpoint
- [[MH-12862](https://opencast.jira.com/browse/MH-12862)][[#211](https://github.com/opencast/opencast/pull/211)] -
  Line break after required marker in REST docs
- [[MH-12834](https://opencast.jira.com/browse/MH-12834)][[#207](https://github.com/opencast/opencast/pull/207)] -
  Central documentation for filtering, sorting and pagination
- [[MH-12833](https://opencast.jira.com/browse/MH-12833)][[#204](https://github.com/opencast/opencast/pull/204)] -
  Consistently use External API as name
- [[MH-12852](https://opencast.jira.com/browse/MH-12852)][[#203](https://github.com/opencast/opencast/pull/203)] -
  Required fields not indicated in the event details and series details modals
- [[MH-12843](https://opencast.jira.com/browse/MH-12843)][[#200](https://github.com/opencast/opencast/pull/200)] -
  Fix “Add Event” Tab Index
- [Update main readme
  ](https://github.com/opencast/opencast/pull/199)
- [Fix tabs and trailing spaces in docs
  ](https://github.com/opencast/opencast/pull/198)
- [[MH-12839](https://opencast.jira.com/browse/MH-12839)][[#196](https://github.com/opencast/opencast/pull/196)] -
  fix all pom.xml
- [[MH-12837](https://opencast.jira.com/browse/MH-12837)][[#194](https://github.com/opencast/opencast/pull/194)] -
  external series API ACL is required
- [[MH-12832](https://opencast.jira.com/browse/MH-12832)][[#192](https://github.com/opencast/opencast/pull/192)] -
  Update to commons-collection4
- [[MH-12836](https://opencast.jira.com/browse/MH-12836)][[#191](https://github.com/opencast/opencast/pull/191)] -
  Fix event-comment dependencies not correctly specified
- [[MH-12831](https://opencast.jira.com/browse/MH-12831)][[#190](https://github.com/opencast/opencast/pull/190)] -
  Fixing dependencies
- [NOJIRA fix engage paella url security rules
  ](https://github.com/opencast/opencast/pull/187)
- [NOJIRA Localization developer guide updated
  ](https://github.com/opencast/opencast/pull/186)
- [[MH-12780](https://opencast.jira.com/browse/MH-12780)][[#184](https://github.com/opencast/opencast/pull/184)] -
  Fix sorting jobs by identifier in Systems->Jobs
- [[MH-12824](https://opencast.jira.com/browse/MH-12824)][[#183](https://github.com/opencast/opencast/pull/183)] -
  Speed up mvn site
- [T/clarify wording of user tracking in documentation
  ](https://github.com/opencast/opencast/pull/180)
- [[MH-12818](https://opencast.jira.com/browse/MH-12818)][[#177](https://github.com/opencast/opencast/pull/177)] -
  Improve Sox service tests
- [NOJIRA Crowdin project configuration updated
  ](https://github.com/opencast/opencast/pull/175)
- [NOJIRA Crowdin documentation updated
  ](https://github.com/opencast/opencast/pull/174)
- [[MH-12771](https://opencast.jira.com/browse/MH-12771)][[#173](https://github.com/opencast/opencast/pull/173)] -
  Document fields of External API 1.0.0
- [[MH-12795](https://opencast.jira.com/browse/MH-12795)][[#163](https://github.com/opencast/opencast/pull/163)] -
  REST docs don't respect @Produces annotation on class level
- [[MH-12788](https://opencast.jira.com/browse/MH-12788)][[#157](https://github.com/opencast/opencast/pull/157)] -
  UTF-8 encoding settings in OAI-PMH publication service remote
- [[MH-12616](https://opencast.jira.com/browse/MH-12616)][[#152](https://github.com/opencast/opencast/pull/152)] -
  Admin UI Flexible Asset Upload override or fallback display text
- [[MH-12775](https://opencast.jira.com/browse/MH-12775)][[#146](https://github.com/opencast/opencast/pull/146)] -
  Add JavaScript source map generation
- [[MH-12768](https://opencast.jira.com/browse/MH-12768)][[#142](https://github.com/opencast/opencast/pull/142)] -
  Minor XACMLAuthorizationService fixes
- [[MH-12825](https://opencast.jira.com/browse/MH-12825)][[#139](https://github.com/opencast/opencast/pull/139)] -
  Add markdownlint to Travis CI

- [[MH-12760](https://opencast.jira.com/browse/MH-12760)][[#160](https://github.com/opencast/opencast/pull/160)] -
  Cross-link column date in events table to enable the start date filter
- [[MH-12789](https://opencast.jira.com/browse/MH-12789)][[#158](https://github.com/opencast/opencast/pull/158)] -
  Remove tabs and trailing spaces in LTI tools
- [[MH-12509](https://opencast.jira.com/browse/MH-12509)][[#151](https://github.com/opencast/opencast/pull/151)] -
  Enable HTTP basic auth in default config
- [[MH-12759](https://opencast.jira.com/browse/MH-12759)][[#149](https://github.com/opencast/opencast/pull/149)] -
  More Control Over Workflows
- [[MH-12779](https://opencast.jira.com/browse/MH-12779)][[#147](https://github.com/opencast/opencast/pull/147)] -
  Support X-Forwarded-Proto header
- [[MH-12649](https://opencast.jira.com/browse/MH-12649)][[#138](https://github.com/opencast/opencast/pull/138)] -
  clone workflow operation handler
- [[MH-12764](https://opencast.jira.com/browse/MH-12764)][[#137](https://github.com/opencast/opencast/pull/137)] -
  update license information for admin-ui
- [[MH-12763](https://opencast.jira.com/browse/MH-12763)][[#136](https://github.com/opencast/opencast/pull/136)] -
  Minor Composer Fixes
- [[MH-12762](https://opencast.jira.com/browse/MH-12762)][[#135](https://github.com/opencast/opencast/pull/135)] -
  Fix Spaces In Configuration
- [Fallback For Synfig Install
  ](https://github.com/opencast/opencast/pull/134)
- [clean up woh documentation
  ](https://github.com/opencast/opencast/pull/133)
- [Make Travis check for tabs in pom.xml files
  ](https://github.com/opencast/opencast/pull/132)
- [Add Mkdocs To Travis Builds
  ](https://github.com/opencast/opencast/pull/131)
- [[MH-12757](https://opencast.jira.com/browse/MH-12757)][[#128](https://github.com/opencast/opencast/pull/128)] -
  Fix ClassCastException
- [[MH-12755](https://opencast.jira.com/browse/MH-12755)][[#127](https://github.com/opencast/opencast/pull/127)] -
  Fix workflow-workflowoperation dependencies
- [[MH-12746](https://opencast.jira.com/browse/MH-12746)][[#126](https://github.com/opencast/opencast/pull/126)] -
  Update Checkstyle
- [[MH-12746](https://opencast.jira.com/browse/MH-12746)][[#125](https://github.com/opencast/opencast/pull/125)] -
  Update Apache HTTPComponents
- [[MH-12746](https://opencast.jira.com/browse/MH-12746)][[#124](https://github.com/opencast/opencast/pull/124)] -
  Update Mina
- [[MH-12746](https://opencast.jira.com/browse/MH-12746)][[#123](https://github.com/opencast/opencast/pull/123)] -
  Remove commons-logging
- [[MH-12746](https://opencast.jira.com/browse/MH-12746)][[#122](https://github.com/opencast/opencast/pull/122)] -
  Update Jackson
- [[MH-12752](https://opencast.jira.com/browse/MH-12752)][[#121](https://github.com/opencast/opencast/pull/121)] -
  Ignore VSCode project data
- [[MH-12751](https://opencast.jira.com/browse/MH-12751)][[#120](https://github.com/opencast/opencast/pull/120)] -
  Add Travis Badge
- [[MH-12735](https://opencast.jira.com/browse/MH-12735)][[#119](https://github.com/opencast/opencast/pull/119)] -
  Remove Undocumented Operations
- [[MH-12746](https://opencast.jira.com/browse/MH-12746)][[#115](https://github.com/opencast/opencast/pull/115)] -
  Library Update
- [[MH-12742](https://opencast.jira.com/browse/MH-12742)][[#113](https://github.com/opencast/opencast/pull/113)] -
  Update to Karaf 4.0.10
- [[MH-12744](https://opencast.jira.com/browse/MH-12744)][[#111](https://github.com/opencast/opencast/pull/111)] -
  Fix migration bundle dependencies
- [[MH-12739](https://opencast.jira.com/browse/MH-12739)][[#109](https://github.com/opencast/opencast/pull/109)] -
  Transcription Service updated to support Paella
- [[MH-12737](https://opencast.jira.com/browse/MH-12737)][[#108](https://github.com/opencast/opencast/pull/108)] -
  OAI-PMH publication service
- [[MH-12732](https://opencast.jira.com/browse/MH-12732)][[#106](https://github.com/opencast/opencast/pull/106)] -
  Remove Unused Remote Service Registry
- [[MH-12731](https://opencast.jira.com/browse/MH-12731)][[#105](https://github.com/opencast/opencast/pull/105)] -
  Improve Recreating Series Index
- [[MH-12730](https://opencast.jira.com/browse/MH-12730)][[#104](https://github.com/opencast/opencast/pull/104)] -
  Workflow Index Rebuild Performance
- [[MH-12711](https://opencast.jira.com/browse/MH-12711)][[#100](https://github.com/opencast/opencast/pull/100)] -
  improve xacml parser
- [[MH-12726](https://opencast.jira.com/browse/MH-12726)][[#99](https://github.com/opencast/opencast/pull/99)] -
  Add description to theme
- [[MH-12704](https://opencast.jira.com/browse/MH-12704)][[#98](https://github.com/opencast/opencast/pull/98)] -
  Captions support for paella
- [[MH-12718](https://opencast.jira.com/browse/MH-12718)][[#97](https://github.com/opencast/opencast/pull/97)] -
  Animate Service
- [[MH-12713](https://opencast.jira.com/browse/MH-12713)][[#95](https://github.com/opencast/opencast/pull/95)] -
  Series cannot be created
- [[MH-12705](https://opencast.jira.com/browse/MH-12705)][[#87](https://github.com/opencast/opencast/pull/87)] -
  Fix scheduler hot-deployment
- [[MH-12701](https://opencast.jira.com/browse/MH-12701)][[#84](https://github.com/opencast/opencast/pull/84)] -
  Paella: Localization files + crowdin config file
- [[MH-12692](https://opencast.jira.com/browse/MH-12692)][[#83](https://github.com/opencast/opencast/pull/83)] -
  update maven bundle plugin for java8
- [[MH-12663](https://opencast.jira.com/browse/MH-12663)][[#81](https://github.com/opencast/opencast/pull/81)] -
  Don't search for non-existing WFR files
- [[MH-12694](https://opencast.jira.com/browse/MH-12694)][[#80](https://github.com/opencast/opencast/pull/80)] -
  Save" button in the editor now stays on the same page.
- [[MH-12693](https://opencast.jira.com/browse/MH-12693)][[#77](https://github.com/opencast/opencast/pull/77)] -
  Notes on how to enable, upgrade to HTTPS
- [[MH-12675](https://opencast.jira.com/browse/MH-12675)][[#76](https://github.com/opencast/opencast/pull/76)] -
  Send default startdate to backend also if it hasn't been changed.
- [[MH-12656](https://opencast.jira.com/browse/MH-12656)][[#75](https://github.com/opencast/opencast/pull/75)] -
  Updates to Theodul Matomo (formerly Piwik) Plugin
- [[MH-12684](https://opencast.jira.com/browse/MH-12684)][[#69](https://github.com/opencast/opencast/pull/69)] -
  Make License List Provider More Flexible
- [[MH-12683](https://opencast.jira.com/browse/MH-12683)][[#68](https://github.com/opencast/opencast/pull/68)] -
  Improve Video Editor Tests
- [[MH-12681](https://opencast.jira.com/browse/MH-12681)][[#66](https://github.com/opencast/opencast/pull/66)] -
  update media package series catalogs on event metadata update
- [[MH-12677](https://opencast.jira.com/browse/MH-12677)][[#65](https://github.com/opencast/opencast/pull/65)] -
  Be less technical about displaying the version number
- [[MH-12674](https://opencast.jira.com/browse/MH-12674)][[#63](https://github.com/opencast/opencast/pull/63)] -
  Remove unused hard-coded list providers
- [[MH-12665](https://opencast.jira.com/browse/MH-12665)][[#62](https://github.com/opencast/opencast/pull/62)] -
  Sort table on startup
- [[MH-12649](https://opencast.jira.com/browse/MH-12649)][[#59](https://github.com/opencast/opencast/pull/59)] -
  clone workflow operation handler
- [[MH-12668](https://opencast.jira.com/browse/MH-12668)][[#58](https://github.com/opencast/opencast/pull/58)] -
  Update packages of admin ui build pipeline
- [Use $timeout instead of $interval to resolve MH-12667
  ](https://github.com/opencast/opencast/pull/57)
- [[MH-12661](https://opencast.jira.com/browse/MH-12661)][[#52](https://github.com/opencast/opencast/pull/52)] -
  Update angular-translate to 2.17.0
- [[MH-12660](https://opencast.jira.com/browse/MH-12660)][[#51](https://github.com/opencast/opencast/pull/51)] -
  Scheduling Events by Specifying End Time
- [[MH-12658](https://opencast.jira.com/browse/MH-12658)][[#50](https://github.com/opencast/opencast/pull/50)] -
  Disable Jasmine for Theodul
- [[MH-12653](https://opencast.jira.com/browse/MH-12653)][[#46](https://github.com/opencast/opencast/pull/46)] -
  Authorization service should use workspace#read() wherever possible
- [[MH-12600](https://opencast.jira.com/browse/MH-12600)][[#45](https://github.com/opencast/opencast/pull/45)] -
  Move userdirectory stuff from bundle `kernel` to `userdirectory`
- [[MH-12648](https://opencast.jira.com/browse/MH-12648)][[#42](https://github.com/opencast/opencast/pull/42)] -
  As a system administrator, I want to use different encoding …
- [[MH-12645](https://opencast.jira.com/browse/MH-12645)][[#39](https://github.com/opencast/opencast/pull/39)] -
  Created an option to rebuild index for an specific service
- [[MH-12644](https://opencast.jira.com/browse/MH-12644)][[#37](https://github.com/opencast/opencast/pull/37)] -
  External API index schema fixes
- [[MH-12538](https://opencast.jira.com/browse/MH-12538)][[#36](https://github.com/opencast/opencast/pull/36)] -
  Remove obsolete ACL distribution service and WOH distribute-acl
- [[MH-12639](https://opencast.jira.com/browse/MH-12639)][[#35](https://github.com/opencast/opencast/pull/35)] -
  update angular-chosen to 1.8.0
- [[MH-11984](https://opencast.jira.com/browse/MH-11984)][[#32](https://github.com/opencast/opencast/pull/32)] -
  Allow customization of the username-to-user-role mapping
- [[MH-12367](https://opencast.jira.com/browse/MH-12367)][[#30](https://github.com/opencast/opencast/pull/30)] -
  Renaming all database tables
- [[MH-12633](https://opencast.jira.com/browse/MH-12633)][[#29](https://github.com/opencast/opencast/pull/29)] -
  Fix version of maven-dependency-plugin
- [[MH-12544](https://opencast.jira.com/browse/MH-12544)][[#26](https://github.com/opencast/opencast/pull/26)] -
  Play Deleted Segments in Video Editor
- [[MH-12575](https://opencast.jira.com/browse/MH-12575)][[#25](https://github.com/opencast/opencast/pull/25)] -
  Upgrade to AngularJS 1.5.11
- [[MH-12595](https://opencast.jira.com/browse/MH-12595)][[#24](https://github.com/opencast/opencast/pull/24)] -
  Improve Publications Usability
- [[MH-12613](https://opencast.jira.com/browse/MH-12613)][[#23](https://github.com/opencast/opencast/pull/23)] -
  New WorkflowOperationHandler 'create-event'
- [[MH-12628](https://opencast.jira.com/browse/MH-12628)][[#20](https://github.com/opencast/opencast/pull/20)] -
  MH-12629, MH-12630, Minor database fixes
- [[MH-10560](https://opencast.jira.com/browse/MH-10560)][[#19](https://github.com/opencast/opencast/pull/19)] -
  Live Scheduler Service
- [[MH-12615](https://opencast.jira.com/browse/MH-12615)][[#17](https://github.com/opencast/opencast/pull/17)] -
  Improve the languages drop-down menu
- [[MH-12623](https://opencast.jira.com/browse/MH-12623)][[#16](https://github.com/opencast/opencast/pull/16)] -
  Improve workflow dropdown menu
- [[MH-12621](https://opencast.jira.com/browse/MH-12621)][[#15](https://github.com/opencast/opencast/pull/15)] -
  submit paella player
- [[MH-12624](https://opencast.jira.com/browse/MH-12624)][[#11](https://github.com/opencast/opencast/pull/11)] -
  Fix link to Karaf remote debugging documentation
- [Update debs.md
  ](https://github.com/opencast/opencast/pull/10)
- [[MH-12472](https://opencast.jira.com/browse/MH-12472)][[#8](https://github.com/opencast/opencast/pull/8)] -
  FFmpeg Composer Implementation
- [[MH-12502](https://opencast.jira.com/browse/MH-12502)][[#7](https://github.com/opencast/opencast/pull/7)] -
  Do Not Leave Files In Workspace
- [[MH-12477](https://opencast.jira.com/browse/MH-12477)][[#6](https://github.com/opencast/opencast/pull/6)] -
  Operation To Log Workflow State
- [[MH-12555](https://opencast.jira.com/browse/MH-12555)][[#5](https://github.com/opencast/opencast/pull/5)] -
  Add support for Piwik Media Analytics
- [[MH-10016](https://opencast.jira.com/browse/MH-10016)][[#4](https://github.com/opencast/opencast/pull/4)] -
  Default Workflow
- [[MH-12603](https://opencast.jira.com/browse/MH-12603)][[#2](https://github.com/opencast/opencast/pull/2)] -
  Consistent Workflow IDs
- [[MH-12622](https://opencast.jira.com/browse/MH-12622)][[#1](https://github.com/opencast/opencast/pull/1)] -
  Surefire Versions Should Not Diverge



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


Opencast 3.x
------------

### Opencast 3.7

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


### Opencast 3.6

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


### Opencast 3.5

*Released on February 6, 2018*

- [[MH-12620]](https://opencast.jira.com/browse/MH-12620) - Document ActiveMQ memory requirements
- [[MH-12606]](https://opencast.jira.com/browse/MH-12606) - Using "Start Task" with a workflow containing an embedded
  script in the configuration which somehow modifies the input parameters does not update those values properly
- [[MH-12582]](https://opencast.jira.com/browse/MH-12582) - Editor WOH should not encode videos unless it is strictly
  necessary (to save time and resources)
- [[MH-12495]](https://opencast.jira.com/browse/MH-12495) - Job dispatching with loads needs optimization
- [[MH-12487]](https://opencast.jira.com/browse/MH-12487) - Add job load settings to the default encoding profles
- [[MH-12399]](https://opencast.jira.com/browse/MH-12399) - Oaipmh Retract very slow


### Opencast 3.4

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


### Opencast 3.3

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


### Opencast 3.2

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


### Opencast 3.1

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


### Opencast 3.0

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

Opencast 2.3.x
--------------

### Opencast 2.3.5

*Released on December 04, 2017*

- [[MH-12588]](https://opencast.jira.com/browse/MH-12588) - Stream Security Leaks Secrets
- [[MH-12317]](https://opencast.jira.com/browse/MH-12317) - AdminUI create every 5 seconds stats request and may crash
  on heavy server load
- [[MH-12269]](https://opencast.jira.com/browse/MH-12269) - Clarify in the documentation the recommendation of setting
  `dispatchinterval` to 0 applies to non-admin nodes only
- [[MH-12190]](https://opencast.jira.com/browse/MH-12190) - Script injection in Media Module and Player
- [[MH-12000]](https://opencast.jira.com/browse/MH-12000) - Cross-tenant URL signing
- [[MH-11042]](https://opencast.jira.com/browse/MH-11042) - Admin UI NG tests fail in +5:30 timezone

### Opencast 2.3.4

*Released on August 03, 2017*

- [[MH-12183]](https://opencast.jira.com/browse/MH-12183) - Theodul does not load
- [[MH-12203]](https://opencast.jira.com/browse/MH-12203) - Unescaped event and series titles when editing event or
  series (XSS)
- [[MH-12242]](https://opencast.jira.com/browse/MH-12242) - Theodul: Quality selector does not display/load
- [[MH-12246]](https://opencast.jira.com/browse/MH-12246) - Series WOH does not apply series DublinCore catalogs
- [[MH-12249]](https://opencast.jira.com/browse/MH-12249) - Media Module: Paging forgets search parameters

### Opencast 2.3.3

*Released on May 02, 2017*

- [[MH-10558]](https://opencast.jira.com/browse/MH-10558) - Mime type not identified for matroska / mkv files
- [[MH-10595]](https://opencast.jira.com/browse/MH-10595) - Incident service returns internal server error if
  cascade=true requested for deleted workflow
- [[MH-10747]](https://opencast.jira.com/browse/MH-10747) - Inputs for capture device should be pre-selected
- [[MH-11736]](https://opencast.jira.com/browse/MH-11736) - Difference in start time displayed in overview and metadata
  details
- [[MH-11811]](https://opencast.jira.com/browse/MH-11811) - Opencast build fails when system timezone is set to PDT
  (Pacific Daylight Time)
- [[MH-12048]](https://opencast.jira.com/browse/MH-12048) - Series drop-down not sorted alphabetically in filter
- [[MH-12069]](https://opencast.jira.com/browse/MH-12069) - Deleting an event leaves behind orphaned comments
- [[MH-12095]](https://opencast.jira.com/browse/MH-12095) - Server default timezone can be incorrect
- [[MH-12106]](https://opencast.jira.com/browse/MH-12106) - Preserve user attributes from providers during
  authentication
- [[MH-12107]](https://opencast.jira.com/browse/MH-12107) - Improve performance of Servers table in Admin UI
- [[MH-12118]](https://opencast.jira.com/browse/MH-12118) - Paging in media module is broken
- [[MH-12129]](https://opencast.jira.com/browse/MH-12129) - Media module only works with english localized browsers
- [[MH-12130]](https://opencast.jira.com/browse/MH-12130) - Filters set by selecting a category in the dashboard are not
  shown
- [[MH-12148]](https://opencast.jira.com/browse/MH-12148) - Undocumented Archive WOH Requirements
- [[MH-12150]](https://opencast.jira.com/browse/MH-12150) - Matroska files are not recognized
- [[MH-12158]](https://opencast.jira.com/browse/MH-12158) - Workflow job dispatching failures
- [[MH-12162]](https://opencast.jira.com/browse/MH-12162) - JpaJob object toString override for better log messages
- [[MH-12163]](https://opencast.jira.com/browse/MH-12163) - Events with stopped workflows sometimes cannot be deleted
- [[MH-12164]](https://opencast.jira.com/browse/MH-12164) - Updating serviceregistry config while running leaves
  Opencast in a non-functional state
- [[MH-12190]](https://opencast.jira.com/browse/MH-12190) - Script injection in Media Module and Player

### Opencast 2.3.2

*Released on March 22, 2017*

- [[MH-11224]](https://opencast.jira.com/browse/MH-11224) - Attempting to view source metadata through the new admin UI
  generates a stack trace
- [[MH-11340]](https://opencast.jira.com/browse/MH-11340) - Uncaught NullPointer Exception in Karaf console from
  com.entwinemedia.fn.data.json.SimpleSerializer.toJson
- [[MH-11616]](https://opencast.jira.com/browse/MH-11616) - Search Service will not remove mp from index if it is not
  found in database
- [[MH-11743]](https://opencast.jira.com/browse/MH-11743) - event.hasPreview() broken
- [[MH-11760]](https://opencast.jira.com/browse/MH-11760) - Event edit warning cannot be removed
- [[MH-11790]](https://opencast.jira.com/browse/MH-11790) - Slide Previews and slide text are not shown in Theodul
  Engage player
- [[MH-11817]](https://opencast.jira.com/browse/MH-11817) - Unhide volume controls in video-editor
- [[MH-11819]](https://opencast.jira.com/browse/MH-11819) - Admin UI Video Editor - Improved Zoom Controls
- [[MH-12009]](https://opencast.jira.com/browse/MH-12009) - Admin UI Video Editor: Segmentation lost after publishing
- [[MH-12058]](https://opencast.jira.com/browse/MH-12058) - Ingests fail if specified workflow does not exist
- [[MH-12059]](https://opencast.jira.com/browse/MH-12059) - Catch invalid dates when indexing
- [[MH-12061]](https://opencast.jira.com/browse/MH-12061) - Reduce the number of activemq messages and log entries
  during index rebuild
- [[MH-12062]](https://opencast.jira.com/browse/MH-12062) - Improve robustness of scheduler re-indexing
- [[MH-12063]](https://opencast.jira.com/browse/MH-12063) - Catch incomplete archive entries when indexing
- [[MH-12072]](https://opencast.jira.com/browse/MH-12072) - Wrong destinationId for External API message receiver
- [[MH-12084]](https://opencast.jira.com/browse/MH-12084) - The class "AsyncTimeoutRedirectFilter" swallows almost all
  the exceptions
- [[MH-12087]](https://opencast.jira.com/browse/MH-12087) - Null bitrate can cause UI display of source media to fail
- [[MH-12092]](https://opencast.jira.com/browse/MH-12092) - Return event ID when event is created through Scheduler API
- [[MH-12097]](https://opencast.jira.com/browse/MH-12097) - SegmentVideoWorkflowOperation: Modules not included in Admin
  Presentation build.

### Opencast 2.3.1

*Released on Janurary 25, 2017*

- [[MH-11267]](https://opencast.jira.com/browse/MH-11267) - Wrong notification text when deleting series
- [[MH-11458]](https://opencast.jira.com/browse/MH-11458) - Update translations from crowdin
- [[MH-11687]](https://opencast.jira.com/browse/MH-11687) - UI date formats are wrong for most of the English-speaking
  world
- [[MH-11776]](https://opencast.jira.com/browse/MH-11776) - CaptureAgentStateServiceImplTest incorrectly passes a
  non-long recording id, misses finding the NullPointer in Impl
- [[MH-11960]](https://opencast.jira.com/browse/MH-11960) - matterhorn-adminui-ng fails on first build
- [[MH-11961]](https://opencast.jira.com/browse/MH-11961) - Cannot access slidetext.xml should not break re-indexing
- [[MH-11963]](https://opencast.jira.com/browse/MH-11963) - Fix ingest REST docs
- [[MH-11966]](https://opencast.jira.com/browse/MH-11966) - Confusing AdminUI Groups Endpoint Documentation
- [[MH-11967]](https://opencast.jira.com/browse/MH-11967) - Publish internal fails on Distrubuted System Admin/Engage
- [[MH-11983]](https://opencast.jira.com/browse/MH-11983) - Only administrators should be allowed to assign the admin
  roles to other users
- [[MH-11987]](https://opencast.jira.com/browse/MH-11987) - Declare Admin UI Facade as module internal interface
- [[MH-11988]](https://opencast.jira.com/browse/MH-11988) - Advise to change karaf shutdown command in the docs
- [[MH-11989]](https://opencast.jira.com/browse/MH-11989) - Allow unknown as well as offline CAs to be removed via UI
- [[MH-11992]](https://opencast.jira.com/browse/MH-11992) - Compatibility issue when using contrib Wowza adaptive
  streaming module
- [[MH-11998]](https://opencast.jira.com/browse/MH-11998) - /info/me.json sometimes doesn't provide full information
  about the user
- [[MH-12004]](https://opencast.jira.com/browse/MH-12004) - Removing an recording does not remove all correspronding
  jobs
- [[MH-12005]](https://opencast.jira.com/browse/MH-12005) - UI shows inconsistent version due to missing version in
  cover-image-remote
- [[MH-12006]](https://opencast.jira.com/browse/MH-12006) - Security Issue Allowing Arbitrary Code Execution


### Opencast 2.3.0

*Released on December 13, 2016*

- [[MH-10342]](https://opencast.jira.com/browse/MH-10342) - As an external device I want to immediate start and stop a
  capture
- [[MH-11327]](https://opencast.jira.com/browse/MH-11327) - De-couple smilImpl/wfrImpl from ingestImpl
- [[MH-11378]](https://opencast.jira.com/browse/MH-11378) - Conditionally synchronize Archive Service's add mediapackge
- [[MH-11380]](https://opencast.jira.com/browse/MH-11380) - As a customer, I want to integrate my third party
  application to Opencast, so that I can use Opencast content in my application
- [[MH-11381]](https://opencast.jira.com/browse/MH-11381) - Remove documentation of items that have never been
  implemented
- [[MH-11411]](https://opencast.jira.com/browse/MH-11411) - move dashboard to header
- [[MH-11675]](https://opencast.jira.com/browse/MH-11675) - Add documentation for External API to the Admin Guide
- [[MH-11688]](https://opencast.jira.com/browse/MH-11688) - Set java file encoding on startup
- [[MH-11718]](https://opencast.jira.com/browse/MH-11718) - As a producer, I want to be able to make workflow settings
  persistent so that I can reuse them later
- [[MH-11725]](https://opencast.jira.com/browse/MH-11725) - Give users a starting point how to report bugs
- [[MH-11726]](https://opencast.jira.com/browse/MH-11726) - Add AdminUI style guide to developer guide
- [[MH-11728]](https://opencast.jira.com/browse/MH-11728) - Use Apache Commons Lang 3
- [[MH-11729]](https://opencast.jira.com/browse/MH-11729) - External API: Add documentation for Groups Endpoint
- [[MH-11731]](https://opencast.jira.com/browse/MH-11731) - Typofix Documentation
- [[MH-11737]](https://opencast.jira.com/browse/MH-11737) - Comment (mh\_event\_comment and mh\_event\_comment\_reply)
  text field is VARCHAR(255) should be TEXT
- [[MH-11740]](https://opencast.jira.com/browse/MH-11740) - optimization of segmentation
- [[MH-11741]](https://opencast.jira.com/browse/MH-11741) - Admin UI has timezone issues
- [[MH-11749]](https://opencast.jira.com/browse/MH-11749) - External API: Add REST documentation for Endpoints
- [[MH-11750]](https://opencast.jira.com/browse/MH-11750) - Clean-Up Opencast Code Base
- [[MH-11752]](https://opencast.jira.com/browse/MH-11752) - Upgrade Karaf to 3.0.8
- [[MH-11756]](https://opencast.jira.com/browse/MH-11756) - Admin UI NG Update CSS+HTML (1): FontAwesome, improve HTML,
  remove redundant images
- [[MH-11763]](https://opencast.jira.com/browse/MH-11763) - Counters hide series tab
- [[MH-11772]](https://opencast.jira.com/browse/MH-11772) - Admin UI source dropdowns inappropriately advance
- [[MH-11774]](https://opencast.jira.com/browse/MH-11774) - Admin UI Needs better documentation for debugging
- [[MH-11775]](https://opencast.jira.com/browse/MH-11775) - Library Update
- [[MH-11783]](https://opencast.jira.com/browse/MH-11783) - Custom publications labels not displayed when doing a
  mouse-over on Events-\>Published
- [[MH-11784]](https://opencast.jira.com/browse/MH-11784) - Remove Participation Management Code Pieces
- [[MH-11786]](https://opencast.jira.com/browse/MH-11786) - HttpsRequestWrapper wrongly sets the new URL
- [[MH-11791]](https://opencast.jira.com/browse/MH-11791) - As service provider I want to configure which kind of users
  can see the event counters
- [[MH-11792]](https://opencast.jira.com/browse/MH-11792) - NPM Proxy via Nexus
- [[MH-11794]](https://opencast.jira.com/browse/MH-11794) - NPM fails on first build
- [[MH-11795]](https://opencast.jira.com/browse/MH-11795) - Add support for title slides
- [[MH-11799]](https://opencast.jira.com/browse/MH-11799) - Maven bundle names too long
- [[MH-11800]](https://opencast.jira.com/browse/MH-11800) - LTI between Opencast and Moodle does not work
- [[MH-11801]](https://opencast.jira.com/browse/MH-11801) - Wowza streaming server needs flv: prefix for flv files
- [[MH-11802]](https://opencast.jira.com/browse/MH-11802) - Opencast Logo is missing in Player
- [[MH-11803]](https://opencast.jira.com/browse/MH-11803) - Player redirect is missing
- [[MH-11804]](https://opencast.jira.com/browse/MH-11804) - No video controls in embed mode
- [[MH-11808]](https://opencast.jira.com/browse/MH-11808) - Pre-select workflow in case only one option is available
- [[MH-11809]](https://opencast.jira.com/browse/MH-11809) - Fix syntax error in encoding profile composite.http
- [[MH-11812]](https://opencast.jira.com/browse/MH-11812) - Fix security configuration for ROLE\_UI\_TASKS\_CREATE
- [[MH-11813]](https://opencast.jira.com/browse/MH-11813) - Agent state REST endpoint documentation
- [[MH-11815]](https://opencast.jira.com/browse/MH-11815) - As a user I expect changes to be reflected in the Admin UI
  immediately
- [[MH-11817]](https://opencast.jira.com/browse/MH-11817) - Admin UI Video Editor - Bug Fixes
- [[MH-11817]](https://opencast.jira.com/browse/MH-11817) - Display video details in preview player/ editor of the admin
  ui
- [[MH-11817]](https://opencast.jira.com/browse/MH-11817) - Improve Button Hover Indication
- [[MH-11817]](https://opencast.jira.com/browse/MH-11817) - Make Next/Last Frame controls in videoeditor better
  recognizeable
- [[MH-11827]](https://opencast.jira.com/browse/MH-11827) - Recordings-\>Events-\>"Event Details"-\>Metadata: Incorrect
  translation used
- [[MH-11828]](https://opencast.jira.com/browse/MH-11828) - exception-handler-workflow not set correctly
- [[MH-11829]](https://opencast.jira.com/browse/MH-11829) - High memory usage on the admin server by dispatching jobs
- [[MH-11831]](https://opencast.jira.com/browse/MH-11831) - As a service provider, I want to configure whether Opencast
  creates an admin user automatically
- [[MH-11834]](https://opencast.jira.com/browse/MH-11834) - Unable to set capture agent configuration as JSON
- [[MH-11836]](https://opencast.jira.com/browse/MH-11836) - Additional ACL actions of series are missing when creating a
  new event in that series
- [[MH-11837]](https://opencast.jira.com/browse/MH-11837) - Unprivileged users have no access to fonts
- [[MH-11839]](https://opencast.jira.com/browse/MH-11839) - typo in Event Details: Comments
- [[MH-11841]](https://opencast.jira.com/browse/MH-11841) - Wait for NFS shares before start Opencast service
- [[MH-11842]](https://opencast.jira.com/browse/MH-11842) - Revert accidental downgrade of grunt version
- [[MH-11851]](https://opencast.jira.com/browse/MH-11851) - org.opencastproject.security.admin/pass can't be changed
- [[MH-11857]](https://opencast.jira.com/browse/MH-11857) - Fix log output "Unable to delete non existing object %s/%s"
- [[MH-11862]](https://opencast.jira.com/browse/MH-11862) - Search API handles roles wrong
- [[MH-11863]](https://opencast.jira.com/browse/MH-11863) - WOH analyze-tracks & WOH failing cause exceptions when
  shutting down Opencast
- [[MH-11864]](https://opencast.jira.com/browse/MH-11864) - WOH tag shall implement AbstractWorkflowOperationHandler
- [[MH-11865]](https://opencast.jira.com/browse/MH-11865) - Videoeditor Preview mixes in 2 Audiofiles
- [[MH-11866]](https://opencast.jira.com/browse/MH-11866) - Search box in Organization \>\> Groups not working
- [[MH-11867]](https://opencast.jira.com/browse/MH-11867) - Filter box in Organization \>\> Groups not working
- [[MH-11869]](https://opencast.jira.com/browse/MH-11869) - Deleting Series with 'Actions' is not working
- [[MH-11870]](https://opencast.jira.com/browse/MH-11870) - Wordlength in other languages except english too long
- [[MH-11871]](https://opencast.jira.com/browse/MH-11871) - ElasticSearch shall bind to 127.0.0.1
- [[MH-11875]](https://opencast.jira.com/browse/MH-11875) - ActiveMQ should not listen to all hosts by default
- [[MH-11880]](https://opencast.jira.com/browse/MH-11880) - Multiple issues with LDAP in branch 2.3.x
- [[MH-11883]](https://opencast.jira.com/browse/MH-11883) - Larger files may remain in system temp directory
- [[MH-11886]](https://opencast.jira.com/browse/MH-11886) - login pages throw errors on loading unnecessary scripts
- [[MH-11888]](https://opencast.jira.com/browse/MH-11888) - Organization Filter uses Provider where table uses Type
- [[MH-11889]](https://opencast.jira.com/browse/MH-11889) - Row size too large
- [[MH-11890]](https://opencast.jira.com/browse/MH-11890) - MySQL Connector Version Should Be Consistent
- [[MH-11891]](https://opencast.jira.com/browse/MH-11891) - Event counters query large amounts of useless data
- [[MH-11895]](https://opencast.jira.com/browse/MH-11895) - “Add Event” Wizard Input Fields Broken
- [[MH-11896]](https://opencast.jira.com/browse/MH-11896) - Java Warnings in AbstractEventEndpoint
- [[MH-11897]](https://opencast.jira.com/browse/MH-11897) - Remove Deprecated StringHelper
- [[MH-11898]](https://opencast.jira.com/browse/MH-11898) - Fix Technical Duration Calculation
- [[MH-11899]](https://opencast.jira.com/browse/MH-11899) - Prevent Requesting Event Objects Multiple Times
- [[MH-11900]](https://opencast.jira.com/browse/MH-11900) - Minor Index Service Fixes
- [[MH-11905]](https://opencast.jira.com/browse/MH-11905) - Publish Configure WOH incorrectly retracts publications
- [[MH-11912]](https://opencast.jira.com/browse/MH-11912) - No slider in playback video player
- [[MH-11919]](https://opencast.jira.com/browse/MH-11919) - WOH image claims SUCCEEDED when actually skipping
- [[MH-11920]](https://opencast.jira.com/browse/MH-11920) - WOH prepare-av: Misleading log message
- [[MH-11921]](https://opencast.jira.com/browse/MH-11921) - WOH partial-import looses partial audio tracks in specific
  cases
- [[MH-11950]](https://opencast.jira.com/browse/MH-11950) - Javadocs build error
- [[MH-11955]](https://opencast.jira.com/browse/MH-11955) - Add en-GB to Languages


Opencast 2.2.x
--------------

### Opencast 2.2.5

*Released on June 7, 2017*

- [[MH-11983]](https://opencast.jira.com/browse/MH-11983) - Only admins should be able to modify other admins
- [[MH-12006]](https://opencast.jira.com/browse/MH-12006) - Security Issue Allowing Arbitrary Code Execution
- [[MH-11962]](https://opencast.jira.com/browse/MH-11962) - Missing slidetext.xml should not break re-indexing

### Opencast 2.2.4

*Released on October 13, 2016*

- [[MH-11831]](https://opencast.jira.com/browse/MH-11831) - As a service provider, I want to configure whether Opencast
  creates an admin user automatically
- [[MH-11851]](https://opencast.jira.com/browse/MH-11851) - org.opencastproject.security.admin/pass can't be changed
- [[MH-11862]](https://opencast.jira.com/browse/MH-11862) - Search API handles roles wrong
- [[MH-11875]](https://opencast.jira.com/browse/MH-11875) - ActiveMQ should not listen to all hosts by default

### Opencast 2.2.3

*Released on October 13, 2016*

- [[MH-11285]](https://opencast.jira.com/browse/MH-11285) - Improve developers documentation: remote debugger with karaf
- [[MH-11741]](https://opencast.jira.com/browse/MH-11741) - Admin UI has timezone issues
- [[MH-11771]](https://opencast.jira.com/browse/MH-11771) - Improve section localization in developer guide
- [[MH-11773]](https://opencast.jira.com/browse/MH-11773) - Embed player does not use space very well and has scaling
  problems
- [[MH-11774]](https://opencast.jira.com/browse/MH-11774) - Admin UI Needs better documentation for debugging
- [[MH-11777]](https://opencast.jira.com/browse/MH-11777) - Event Details-\>Comments and Event Details-\>Assets don't
  work for unprivileged users
- [[MH-11787]](https://opencast.jira.com/browse/MH-11787) - Add release dates to changelog
- [[MH-11800]](https://opencast.jira.com/browse/MH-11800) - LTI between Opencast and Moodle does not work
- [[MH-11801]](https://opencast.jira.com/browse/MH-11801) - Wowza streaming server needs flv: prefix for flv files

### Opencast 2.2.2

*Released on September 14, 2016*

- [[MH-11194]](https://opencast.jira.com/browse/MH-11194) - created themes not showing up in series branding tab
- [[MH-11572]](https://opencast.jira.com/browse/MH-11572) - FFmpeg Inspection Service Test - accurateFrameCount
- [[MH-11587]](https://opencast.jira.com/browse/MH-11587) - SQL Error
- [[MH-11714]](https://opencast.jira.com/browse/MH-11714) - Fix unit test: Event controller #accessSave saves the event
  access
- [[MH-11724]](https://opencast.jira.com/browse/MH-11724) - Additional actions not available in create event wizard
  anymore
- [[MH-11734]](https://opencast.jira.com/browse/MH-11734) - Fix el7 RPM docs
- [[MH-11735]](https://opencast.jira.com/browse/MH-11735) - Fix Stream Security Documentation
- [[MH-11744]](https://opencast.jira.com/browse/MH-11744) - Actions-\>Start Task: Various localization bugs
- [[MH-11748]](https://opencast.jira.com/browse/MH-11748) - Inconsistent and incorrect use of translate directive
- [[MH-11751]](https://opencast.jira.com/browse/MH-11751) - Player won't work if there are no segments
- [[MH-11755]](https://opencast.jira.com/browse/MH-11755) - No quality selection in Theodul Player
- [[MH-11759]](https://opencast.jira.com/browse/MH-11759) - Make Inspector Unit Tests More Robust

### Opencast 2.2.1
--------------
*Released on July 30, 2016*

- [[MH-11092]](https://opencast.jira.com/browse/MH-11092) - Every Browser has an other "Remember me" checkbox
- [[MH-11169]](https://opencast.jira.com/browse/MH-11169) - Trimming points not set correctly after workflow is finished
- [[MH-11538]](https://opencast.jira.com/browse/MH-11538) - "No compatible source was found for this video" videojs
  player error in iOS device
- [[MH-11561]](https://opencast.jira.com/browse/MH-11561) - Style (CSS): Setting a server in Maintenance (srv-det-01)
- [[MH-11598]](https://opencast.jira.com/browse/MH-11598) - Wizards should not re-use data that has entered before
- [[MH-11644]](https://opencast.jira.com/browse/MH-11644) - Missing Admin Interface Mock Data
- [[MH-11653]](https://opencast.jira.com/browse/MH-11653) - Jobs do not always proceed
- [[MH-11655]](https://opencast.jira.com/browse/MH-11655) - Jobs with high job load never get processed
- [[MH-11659]](https://opencast.jira.com/browse/MH-11659) - Warning is missing that metada and ACL cannot be edited
  while job is processing.
- [[MH-11661]](https://opencast.jira.com/browse/MH-11661) - Link on logo on the media module points to admin ui or
  welcome page, instead of something that is accessable for every user
- [[MH-11664]](https://opencast.jira.com/browse/MH-11664) - Incorrect Inconsistency status when built from tarball
- [[MH-11665]](https://opencast.jira.com/browse/MH-11665) - Systems-\>Servers & Systems-\>Services show wrong mean
  runtime and mean queue time
- [[MH-11667]](https://opencast.jira.com/browse/MH-11667) - Align main table content
- [[MH-11668]](https://opencast.jira.com/browse/MH-11668) - Missing segment previews let to an erro in the player
- [[MH-11669]](https://opencast.jira.com/browse/MH-11669) - Do not archive OCR texts
- [[MH-11673]](https://opencast.jira.com/browse/MH-11673) - Add documentation for additional ACL actions
- [[MH-11674]](https://opencast.jira.com/browse/MH-11674) - Add documentation for metadata configuration
- [[MH-11679]](https://opencast.jira.com/browse/MH-11679) - Page size cannot be changed in any table
- [[MH-11681]](https://opencast.jira.com/browse/MH-11681) - Add documentation for role-based visibility
- [[MH-11682]](https://opencast.jira.com/browse/MH-11682) - Remove useless roles from roles.txt
- [[MH-11686]](https://opencast.jira.com/browse/MH-11686) - Extended metadata tab not shown although user has the role
  ROLE\_UI\_EVENTS\_DETAILS\_METADATA\_VIEW
- [[MH-11690]](https://opencast.jira.com/browse/MH-11690) - Various Documentation Improvements
- [[MH-11692]](https://opencast.jira.com/browse/MH-11692) - Remove Superfluous Mh-Db-Version
- [[MH-11693]](https://opencast.jira.com/browse/MH-11693) - Remove Superfluous Dependency Versions
- [[MH-11694]](https://opencast.jira.com/browse/MH-11694) - JavaDoc Generation Broken
- [[MH-11702]](https://opencast.jira.com/browse/MH-11702) - After an upgrade to 2.2.0, series are not displayed in the
  UI because the series creation date is now mandatory
- [[MH-11720]](https://opencast.jira.com/browse/MH-11720) - Opencast 2.2 requires Git to be installed at build time
- [[MH-11727]](https://opencast.jira.com/browse/MH-11727) - Fix unit test: adminNg.services.language #toLocalTime
  converts a zulu time string back to local time FAILED
- [[MH-11730]](https://opencast.jira.com/browse/MH-11730) - Make the automatic role prefix in LDAPUserProvider
  configurable

### Opencast 2.2.0

*Released on June 15, 2016*

- [[MH-9511]](https://opencast.jira.com/browse/MH-9511) - Wrong log level in Tesseract
- [[MH-9831]](https://opencast.jira.com/browse/MH-9831) - ehcache and quartz phones home
- [[MH-9950]](https://opencast.jira.com/browse/MH-9950) - Update player dependencies
- [[MH-10029]](https://opencast.jira.com/browse/MH-10029) - Remove Unnecessary Image Conversion Step From
  TextAnalysisService
- [[MH-10173]](https://opencast.jira.com/browse/MH-10173) - Do not ignore exceptions when closing Closeable's
- [[MH-10748]](https://opencast.jira.com/browse/MH-10748) - Matterhorn has to be restarted to schedule an event on a new
  capture device
- [[MH-10794]](https://opencast.jira.com/browse/MH-10794) - Delete Action should be disabled if nothing is selected
- [[MH-10869]](https://opencast.jira.com/browse/MH-10869) - ActiveMQ Configuration and Connection Problems
- [[MH-10870]](https://opencast.jira.com/browse/MH-10870) - ActiveMQ Exceptions While Shutting Down Matterhorn
- [[MH-10887]](https://opencast.jira.com/browse/MH-10887) - Users can schedule events in the past
- [[MH-10898]](https://opencast.jira.com/browse/MH-10898) - Update Apache HttpComponents (3.1.7 → 4.4.1)
- [[MH-10923]](https://opencast.jira.com/browse/MH-10923) - Theodul player : Filtering "composite" tags results in error
  when the composite workflow is used
- [[MH-10942]](https://opencast.jira.com/browse/MH-10942) - Events are not deselected after applying a task
- [[MH-10965]](https://opencast.jira.com/browse/MH-10965) - Theodul player : Videos not playable on IE10
- [[MH-10971]](https://opencast.jira.com/browse/MH-10971) - Newly created Series don't show up in Series dropdown
  selection lists without page reload
- [[MH-10978]](https://opencast.jira.com/browse/MH-10978) - Unable to retract 'internal' publications
- [[MH-10979]](https://opencast.jira.com/browse/MH-10979) - Opencast needs to better distribute load across the
  available nodes
- [[MH-10984]](https://opencast.jira.com/browse/MH-10984) - Extend ingest service by partial upload
- [[MH-11010]](https://opencast.jira.com/browse/MH-11010) - Stream Security should be able to prevent cross-tenants
  access
- [[MH-11014]](https://opencast.jira.com/browse/MH-11014) - Add support for additional ACL actions
- [[MH-11077]](https://opencast.jira.com/browse/MH-11077) - The Publish Workflow will not retract already published
  material
- [[MH-11097]](https://opencast.jira.com/browse/MH-11097) - View modes not working correctly
- [[MH-11107]](https://opencast.jira.com/browse/MH-11107) - Group list pagination not working
- [[MH-11121]](https://opencast.jira.com/browse/MH-11121) - MacOS X Installation Guide Needs 2.1 Update
- [[MH-11124]](https://opencast.jira.com/browse/MH-11124) - Incorrect documentation on how to create users
- [[MH-11128]](https://opencast.jira.com/browse/MH-11128) - Docs about SilenceDetector threashold are incorrect
- [[MH-11139]](https://opencast.jira.com/browse/MH-11139) - Unable to find mimetype for mkv
- [[MH-11140]](https://opencast.jira.com/browse/MH-11140) - Forward and backward buttons are greyed out
- [[MH-11143]](https://opencast.jira.com/browse/MH-11143) - Link to Media Module in Admin UI
- [[MH-11148]](https://opencast.jira.com/browse/MH-11148) - Search box layout incorrect: Icon overlaps text
- [[MH-11156]](https://opencast.jira.com/browse/MH-11156) - Users: Search box not implemented
- [[MH-11157]](https://opencast.jira.com/browse/MH-11157) - Groups: Search box not implemented
- [[MH-11165]](https://opencast.jira.com/browse/MH-11165) - Sorting does not work on Systems-&gt;Jobs,
  Systems-&gt;Servers and Systems-&gt;Services
- [[MH-11167]](https://opencast.jira.com/browse/MH-11167) - Layout problem on Workflow Error Details view
- [[MH-11183]](https://opencast.jira.com/browse/MH-11183) - Capture-&gt;Locations: Search box not implemented
- [[MH-11190]](https://opencast.jira.com/browse/MH-11190) - Theodul Shortcuts: Description could be improved
- [[MH-11191]](https://opencast.jira.com/browse/MH-11191) - Event Details-&gt;Assets: Use human-readable units for
  duration, bitrates and sizes
- [[MH-11192]](https://opencast.jira.com/browse/MH-11192) - Audio level slider does not change audio level while
  dragging
- [[MH-11199]](https://opencast.jira.com/browse/MH-11199) - Playback & video editor don't work while workflow is running
- [[MH-11209]](https://opencast.jira.com/browse/MH-11209) - LTI Documentation needs to be incorporated into new docs
- [[MH-11222]](https://opencast.jira.com/browse/MH-11222) - Replace System.out.println with logger
- [[MH-11229]](https://opencast.jira.com/browse/MH-11229) - workflowoperation unit tests are incredible slow
- [[MH-11252]](https://opencast.jira.com/browse/MH-11252) - Some service configuration files are stored in the wrong
  directory
- [[MH-11265]](https://opencast.jira.com/browse/MH-11265) - Ensure configuration files end with newline characters
- [[MH-11266]](https://opencast.jira.com/browse/MH-11266) - Logger ConversionPattern stated twice
- [[MH-11276]](https://opencast.jira.com/browse/MH-11276) - HttpNotificationWorkflowOperationHandlerTest fails if a
  certain Domain Exists
- [[MH-11280]](https://opencast.jira.com/browse/MH-11280) - Opencast fails to compile due to missing dependencies in
  test-harness
- [[MH-11281]](https://opencast.jira.com/browse/MH-11281) - Enhance WOH image to support extraction of multiple images
  using multiple encoding profiles from multiple sources
- [[MH-11282]](https://opencast.jira.com/browse/MH-11282) - Enhance WOH composite to support single video streams
- [[MH-11287]](https://opencast.jira.com/browse/MH-11287) - Update Apereo/Apache License List
- [[MH-11289]](https://opencast.jira.com/browse/MH-11289) - Change text extraction documentation or file name
- [[MH-11294]](https://opencast.jira.com/browse/MH-11294) - Create admin-worker and ingest distribution
- [[MH-11296]](https://opencast.jira.com/browse/MH-11296) - HTTP method POST is not supported by this url in r/2.1.x
- [[MH-11298]](https://opencast.jira.com/browse/MH-11298) - Fix json-simple version specification
- [[MH-11300]](https://opencast.jira.com/browse/MH-11300) - WOH partial-import looses partial audio tracks beginning at
  position zero
- [[MH-11304]](https://opencast.jira.com/browse/MH-11304) - Documentation for WOH partial-import and load configuration
  not listed in pages configuration
- [[MH-11306]](https://opencast.jira.com/browse/MH-11306) - Change job dispatcher sort order to: restart jobs, non-wf
  jobs, creation date
- [[MH-11307]](https://opencast.jira.com/browse/MH-11307) - Distribution Service is not on Presentation Node
- [[MH-11310]](https://opencast.jira.com/browse/MH-11310) - Document encoding profiles used by WOH partial-import
- [[MH-11311]](https://opencast.jira.com/browse/MH-11311) - Use existing encoding profiles in WOH partial-import example
- [[MH-11312]](https://opencast.jira.com/browse/MH-11312) - Fix Encode WOH Documentation
- [[MH-11313]](https://opencast.jira.com/browse/MH-11313) - Update Parallel Encode Profiles
- [[MH-11319]](https://opencast.jira.com/browse/MH-11319) - Media Module Always Uses Second Attachment as Preview
- [[MH-11320]](https://opencast.jira.com/browse/MH-11320) - Missing Image Preparation for text Extraction
- [[MH-11321]](https://opencast.jira.com/browse/MH-11321) - Fix default workflow configuration panel
- [[MH-11322]](https://opencast.jira.com/browse/MH-11322) - Update WebM Profiles
- [[MH-11355]](https://opencast.jira.com/browse/MH-11355) - Slide texts are not shown correctly in theodul player,
  except the first segment there a now slide texts shown ("No slide text available"). In the XML file the texts are
  correct
- [[MH-11356]](https://opencast.jira.com/browse/MH-11356) - Update Documentation Index Page
- [[MH-11357]](https://opencast.jira.com/browse/MH-11357) - Notifications are not removed after a while
- [[MH-11358]](https://opencast.jira.com/browse/MH-11358) - Dismiss Button for comments has an inconsistent design
- [[MH-11363]](https://opencast.jira.com/browse/MH-11363) - Notification that server is not reachable is missing
- [[MH-11364]](https://opencast.jira.com/browse/MH-11364) - Reasons in Comments section are no longer translated
- [[MH-11368]](https://opencast.jira.com/browse/MH-11368) - Changing to Chinese translation doesn't work
- [[MH-11369]](https://opencast.jira.com/browse/MH-11369) - Series filter displays series id instead of series title
- [[MH-11374]](https://opencast.jira.com/browse/MH-11374) - Videoeditor: Times are wrong in zoomed waveform view
- [[MH-11385]](https://opencast.jira.com/browse/MH-11385) - Metadata summary not showing any metadata at event creation
- [[MH-11386]](https://opencast.jira.com/browse/MH-11386) - Silence Detection / Video Editor Waveform bug
- [[MH-11389]](https://opencast.jira.com/browse/MH-11389) - security 1
- [[MH-11391]](https://opencast.jira.com/browse/MH-11391) - Improve Flavor creation and parsing
- [[MH-11392]](https://opencast.jira.com/browse/MH-11392) - Sorting by series.created does not work correctly
- [[MH-11401]](https://opencast.jira.com/browse/MH-11401) - Hiding of columns is globally broken
- [[MH-11404]](https://opencast.jira.com/browse/MH-11404) - Group editor shows users and roles twice
- [[MH-11405]](https://opencast.jira.com/browse/MH-11405) - Pagination broken for groups table
- [[MH-11409]](https://opencast.jira.com/browse/MH-11409) - Translation key
  EVENTS.EVENTS.GENERAL.SELECT\_WORKFLOW\_EMPTY is missing
- [[MH-11413]](https://opencast.jira.com/browse/MH-11413) - AdminUI comment dialog translations missing
- [[MH-11414]](https://opencast.jira.com/browse/MH-11414) - Logger is missing from several modules
- [[MH-11415]](https://opencast.jira.com/browse/MH-11415) - Incorrect Urlsigning Module Name
- [[MH-11416]](https://opencast.jira.com/browse/MH-11416) - Specify Opencast's Requirements
- [[MH-11417]](https://opencast.jira.com/browse/MH-11417) - Tab names of modals not vertically centered
- [[MH-11419]](https://opencast.jira.com/browse/MH-11419) - Tables not drawn correctly
- [[MH-11422]](https://opencast.jira.com/browse/MH-11422) - add event tab titles not translated
- [[MH-11427]](https://opencast.jira.com/browse/MH-11427) - Can't get host details from Serviceregistry REST endpoint
- [[MH-11428]](https://opencast.jira.com/browse/MH-11428) - Default Workflow Option Does Not Work
- [[MH-11430]](https://opencast.jira.com/browse/MH-11430) - Prevent user from accidentally press "Save & process" in
  Video Editor multiple times
- [[MH-11431]](https://opencast.jira.com/browse/MH-11431) - Prevent users from accidentally pressing the Delete/Retract
  button multiple times
- [[MH-11432]](https://opencast.jira.com/browse/MH-11432) - JSHint settings are missing
- [[MH-11434]](https://opencast.jira.com/browse/MH-11434) - "The task could not be created" error notification always
  appear when starting a task on multiple events
- [[MH-11435]](https://opencast.jira.com/browse/MH-11435) - Fix code style errors in Gruntfile.js
- [[MH-11436]](https://opencast.jira.com/browse/MH-11436) - Matterhorn on Login/Welcome Page
- [[MH-11437]](https://opencast.jira.com/browse/MH-11437) - Resource Problems On Login Page
- [[MH-11438]](https://opencast.jira.com/browse/MH-11438) - Resource Problem on Welcome Page
- [[MH-11439]](https://opencast.jira.com/browse/MH-11439) - Event description not available in WOH cover-image
- [[MH-11441]](https://opencast.jira.com/browse/MH-11441) - Clicking on Logo in top left corner will nmot get you to the
  start page
- [[MH-11443]](https://opencast.jira.com/browse/MH-11443) - Seeking is not possible before pressing play button at least
  once?!?
- [[MH-11446]](https://opencast.jira.com/browse/MH-11446) - Remove eclipse-gemini repository from main pom.xml
- [[MH-11447]](https://opencast.jira.com/browse/MH-11447) - Scheduling conflicts reporting completely broken
- [[MH-11448]](https://opencast.jira.com/browse/MH-11448) - Tipps on developing on admin ui ng
- [[MH-11450]](https://opencast.jira.com/browse/MH-11450) - Fix Defaults For Documentation Links
- [[MH-11453]](https://opencast.jira.com/browse/MH-11453) - Correctly link the stream security documentation
- [[MH-11457]](https://opencast.jira.com/browse/MH-11457) - Remove duplicate keys from Admin UI english translation
- [[MH-11458]](https://opencast.jira.com/browse/MH-11458) - Update translations from crowdin
- [[MH-11459]](https://opencast.jira.com/browse/MH-11459) - Logger Logs Nullpointer on Error
- [[MH-11462]](https://opencast.jira.com/browse/MH-11462) - Cover WOH is not included in a useful way
- [[MH-11464]](https://opencast.jira.com/browse/MH-11464) - setting personal preferences in admin UI fails
- [[MH-11468]](https://opencast.jira.com/browse/MH-11468) - There are unused ressources
- [[MH-11475]](https://opencast.jira.com/browse/MH-11475) - Fix typos in English master translation
- [[MH-11476]](https://opencast.jira.com/browse/MH-11476) - Series-&gt;Actions-&gt;Delete displays wrong notifications
- [[MH-11477]](https://opencast.jira.com/browse/MH-11477) - Editing status of series displays wrong notification when
  saving fails for all series
- [[MH-11480]](https://opencast.jira.com/browse/MH-11480) - Replace horizontal ellipsis
- [[MH-11481]](https://opencast.jira.com/browse/MH-11481) - Workflows started by unprivileged users hang
- [[MH-11492]](https://opencast.jira.com/browse/MH-11492) - forward and backward section not working in safari
- [[MH-11509]](https://opencast.jira.com/browse/MH-11509) - Failed test: Sorting groups list (grp-lis-01)
- [[MH-11511]](https://opencast.jira.com/browse/MH-11511) - Failed test: Manual set time in textbook for IE11
- [[MH-11512]](https://opencast.jira.com/browse/MH-11512) - hello world does not follow import statements rules
- [[MH-11518]](https://opencast.jira.com/browse/MH-11518) - Language selector is always displayed in system language
- [[MH-11519]](https://opencast.jira.com/browse/MH-11519) - Languages are only distinguished by main language
- [[MH-11520]](https://opencast.jira.com/browse/MH-11520) - Remove company logos
- [[MH-11521]](https://opencast.jira.com/browse/MH-11521) - ActiveMQ Library Configuration
- [[MH-11522]](https://opencast.jira.com/browse/MH-11522) - DataLoader Default Value
- [[MH-11523]](https://opencast.jira.com/browse/MH-11523) - Working file repository default value
- [[MH-11524]](https://opencast.jira.com/browse/MH-11524) - Distribution Service Default Values
- [[MH-11532]](https://opencast.jira.com/browse/MH-11532) - Wider language support in player
- [[MH-11534]](https://opencast.jira.com/browse/MH-11534) - Add language support for Chinese Simplified
- [[MH-11535]](https://opencast.jira.com/browse/MH-11535) - Add documentation about Crowdin to Developer Guide
- [[MH-11536]](https://opencast.jira.com/browse/MH-11536) - Remove Commercial Code From Core
- [[MH-11537]](https://opencast.jira.com/browse/MH-11537) - Execute Service WOH Cannot be Built
- [[MH-11539]](https://opencast.jira.com/browse/MH-11539) - Remove Old MH Logos in Favor of Opencast SVG Logos
- [[MH-11544]](https://opencast.jira.com/browse/MH-11544) - Admin UI links used inconsistently
- [[MH-11546]](https://opencast.jira.com/browse/MH-11546) - Pagination buttons too small for large numbers
- [[MH-11548]](https://opencast.jira.com/browse/MH-11548) - The "Edit" button at the top-right corner of the tables
  doesn't support localization
- [[MH-11550]](https://opencast.jira.com/browse/MH-11550) - Update Migration documentation 2.1 to 2.2
- [[MH-11554]](https://opencast.jira.com/browse/MH-11554) - Filtering does not work on Systems-&gt;Jobs,
  Systems-&gt;Servers and Systems-&gt;Services
- [[MH-11555]](https://opencast.jira.com/browse/MH-11555) - Localisation of Recordings-&gt;Events and
  Recordings-&gt;Series buggy
- [[MH-11556]](https://opencast.jira.com/browse/MH-11556) - Failed test: Filter locations (T1733, Filter by status does
  not work)
- [[MH-11559]](https://opencast.jira.com/browse/MH-11559) - outdated shortcurts configuration prevents player from
  loading.
- [[MH-11571]](https://opencast.jira.com/browse/MH-11571) - Elasticsearch shutdown command handler crash opencast
- [[MH-11573]](https://opencast.jira.com/browse/MH-11573) - Do not hide warnings
- [[MH-11574]](https://opencast.jira.com/browse/MH-11574) - Jetty Error on Large Workflow Instances
- [[MH-11575]](https://opencast.jira.com/browse/MH-11575) - Inspection Service Tests Fail With Certain FFmpeg Versions
- [[MH-11576]](https://opencast.jira.com/browse/MH-11576) - Servlet Filter Improvements
- [[MH-11578]](https://opencast.jira.com/browse/MH-11578) - Improve default order of columns in Systems-&gt;Jobs
- [[MH-11579]](https://opencast.jira.com/browse/MH-11579) - Admin UI mockup data for Systems-&gt;Jobs incomplete
- [[MH-11580]](https://opencast.jira.com/browse/MH-11580) - Unit tests for Admin UI language selection broken
- [[MH-11581]](https://opencast.jira.com/browse/MH-11581) - Systems-&gt;Jobs table not working correctly
- [[MH-11583]](https://opencast.jira.com/browse/MH-11583) - Fix Code Style
- [[MH-11588]](https://opencast.jira.com/browse/MH-11588) - Create side-by-side preview for video editor
- [[MH-11589]](https://opencast.jira.com/browse/MH-11589) - Feedback button does not work
- [[MH-11590]](https://opencast.jira.com/browse/MH-11590) - The WorkflowServiceImpl constructor sets the
  "waitForResources" argument incorrectly
- [[MH-11594]](https://opencast.jira.com/browse/MH-11594) - Add language support for Galician
- [[MH-11595]](https://opencast.jira.com/browse/MH-11595) - Fix admin ui unit tests for tableService
- [[MH-11597]](https://opencast.jira.com/browse/MH-11597) - Building matterhorn-engage-theodul-plugin-video-videojs
  reports a lot of code style issues
- [[MH-11600]](https://opencast.jira.com/browse/MH-11600) - Failed test: i18n (gen-int-01)
- [[MH-11601]](https://opencast.jira.com/browse/MH-11601) - current language can have undefined state
- [[MH-11604]](https://opencast.jira.com/browse/MH-11604) - Date picker for setting up the schedule is always french
- [[MH-11605]](https://opencast.jira.com/browse/MH-11605) - Disabling link to mediaplayer creates a broken link and
  missing logo
- [[MH-11606]](https://opencast.jira.com/browse/MH-11606) - Add language support for Greek
- [[MH-11608]](https://opencast.jira.com/browse/MH-11608) - Add documentation for WOH cleanup
- [[MH-11613]](https://opencast.jira.com/browse/MH-11613) - WOH editor fails when input has uneven width or height
- [[MH-11614]](https://opencast.jira.com/browse/MH-11614) - Partial matches not working anymore
- [[MH-11617]](https://opencast.jira.com/browse/MH-11617) - Add language support for Dutch
- [[MH-11620]](https://opencast.jira.com/browse/MH-11620) - Non privileged user can not login on presentation node
- [[MH-11623]](https://opencast.jira.com/browse/MH-11623) - Server statistics: Slow Query
- [[MH-11624]](https://opencast.jira.com/browse/MH-11624) - Workflow owners do not necessarily have access to their
  workflows: user comparison fails
- [[MH-11627]](https://opencast.jira.com/browse/MH-11627) - NullPointerException when creating a new Solr index
- [[MH-11629]](https://opencast.jira.com/browse/MH-11629) - Hide Some Confusing Warnings
- [[MH-11630]](https://opencast.jira.com/browse/MH-11630) - Service registry lacks of getActiveJobs() function
- [[MH-11631]](https://opencast.jira.com/browse/MH-11631) - Remove columns "Blacklisted from" and "Blacklisted until"
  from Capture-&gt;Locations
- [[MH-11632]](https://opencast.jira.com/browse/MH-11632) - Library Bugfix Upgrade
- [[MH-11636]](https://opencast.jira.com/browse/MH-11636) - Adjust FFmpegComposer Logging for Newer FFmpeg Versions
- [[MH-11637]](https://opencast.jira.com/browse/MH-11637) - Add language support for Swedish
- [[MH-11638]](https://opencast.jira.com/browse/MH-11638) - Improve Encoding Profiles
- [[MH-11639]](https://opencast.jira.com/browse/MH-11639) - Media module login form has poor usability and bugs
- [[MH-11642]](https://opencast.jira.com/browse/MH-11642) - Remove binding to non-existing method in WOH analyze-tracks
- [[MH-11643]](https://opencast.jira.com/browse/MH-11643) - Add language support for Polish
- [[MH-11645]](https://opencast.jira.com/browse/MH-11645) - Open AdminUI menu links in new tab does not work
- [[MH-11646]](https://opencast.jira.com/browse/MH-11646) - Add documentation for WOH comment
- [[MH-11652]](https://opencast.jira.com/browse/MH-11652) - Unit tests for servicesController broken
- [[MH-11654]](https://opencast.jira.com/browse/MH-11654) - Failed ingest jobs block system from dispatching other jobs
- [[MH-11656]](https://opencast.jira.com/browse/MH-11656) - Add documentation for WOH copy
- [[MH-11657]](https://opencast.jira.com/browse/MH-11657) - Improve documentation for workflow execution conditions
- [[MH-11658]](https://opencast.jira.com/browse/MH-11658) - Better quality for video editor previews
- [[MH-11663]](https://opencast.jira.com/browse/MH-11663) - Hide Participation Management from UI since not yet working
- [[MH-11666]](https://opencast.jira.com/browse/MH-11666) - Not all WOH listed in WOH overview


Opencast 2.1.x
--------------

### Opencast 2.1.2

*Released on May 10, 2016*

- [[MH-9831]](https://opencast.jira.com/browse/MH-9831) - ehcache and quartz phones home
- [[MH-11121]](https://opencast.jira.com/browse/MH-11121) - MacOS X Installation Guide Needs 2.1 Update
- [[MH-11124]](https://opencast.jira.com/browse/MH-11124) - Incorrect documentation on how to create users
- [[MH-11128]](https://opencast.jira.com/browse/MH-11128) - Docs about SilenceDetector threashold are incorrect
- [[MH-11209]](https://opencast.jira.com/browse/MH-11209) - LTI Documentation needs to be incorporated into new docs
- [[MH-11229]](https://opencast.jira.com/browse/MH-11229) - workflowoperation unit tests are incredible slow
- [[MH-11283]](https://opencast.jira.com/browse/MH-11283) - post-mediapackage WOH breaks further processing
- [[MH-11287]](https://opencast.jira.com/browse/MH-11287) - Update Apereo/Apache License List
- [[MH-11296]](https://opencast.jira.com/browse/MH-11296) - HTTP method POST is not supported by this url in r/2.1.x
- [[MH-11298]](https://opencast.jira.com/browse/MH-11298) - Fix json-simple version specification
- [[MH-11307]](https://opencast.jira.com/browse/MH-11307) - Distribution Service is not on Presentation Node
- [[MH-11319]](https://opencast.jira.com/browse/MH-11319) - Media Module Always Uses Second Attachment as Preview
- [[MH-11320]](https://opencast.jira.com/browse/MH-11320) - Missing Image Preparation for text Extraction
- [[MH-11321]](https://opencast.jira.com/browse/MH-11321) - Fix default workflow configuration panel
- [[MH-11323]](https://opencast.jira.com/browse/MH-11323) - Workflow Docs are Incorrect
- [[MH-11332]](https://opencast.jira.com/browse/MH-11332) - Document acceptance criteria for proposals
- [[MH-11356]](https://opencast.jira.com/browse/MH-11356) - Update Documentation Index Page
- [[MH-11377]](https://opencast.jira.com/browse/MH-11377) - Opencast does not have an ingest assembly

### Opencast 2.1.1

*Released on January 22, 2016*

- [[MH-11107]](https://opencast.jira.com/browse/MH-11107) - Group list pagination not working
- [[MH-11265]](https://opencast.jira.com/browse/MH-11265) - Ensure configuration files end with newline characters
- [[MH-11266]](https://opencast.jira.com/browse/MH-11266) - Logger ConversionPattern stated twice
- [[MH-11276]](https://opencast.jira.com/browse/MH-11276) - HttpNotificationWorkflowOperationHandlerTest fails if a
  certain Domain Exists
- [[MH-11280]](https://opencast.jira.com/browse/MH-11280) - Opencast fails to compile due to missing dependencies in
  test-harness

### Opencast 2.1.0

*Released on December 22, 2015*

- [[MH-10637]](https://opencast.jira.com/browse/MH-10637) - Hello World service
- [[MH-10651]](https://opencast.jira.com/browse/MH-10651) - Workspace cleaner job param in wrong units (ms vs s) and
  wrong logic
- [[MH-10714]](https://opencast.jira.com/browse/MH-10714) - Two clock icons at the time stamp of a comment
- [[MH-10805]](https://opencast.jira.com/browse/MH-10805) - The confirmation dialog are not translated
- [[MH-10818]](https://opencast.jira.com/browse/MH-10818) - The creation date is presented as ISO string in the event
  metadata
- [[MH-10869]](https://opencast.jira.com/browse/MH-10869) - ActiveMQ Configuration and Connection Problems
- [[MH-10874]](https://opencast.jira.com/browse/MH-10874) - Plugin does not properly handle multiple keys
- [[MH-10875]](https://opencast.jira.com/browse/MH-10875) - Include search capabilities into mkdocs documentation build
- [[MH-10890]](https://opencast.jira.com/browse/MH-10890) - Update Apache Commons Lang (2.6 → 3.4)
- [[MH-10908]](https://opencast.jira.com/browse/MH-10908) - Assemblie Module Names Too Long
- [[MH-10908]](https://opencast.jira.com/browse/MH-10908) - Consistency in Documentation: Presentation Server VS Engage
  Server
- [[MH-10908]](https://opencast.jira.com/browse/MH-10908) - Misconfigured Checkstyle Plug-in in Assemblies
- [[MH-10919]](https://opencast.jira.com/browse/MH-10919) - Top row for setting roles in the access policy for an event
  is not showing the right value
- [[MH-10953]](https://opencast.jira.com/browse/MH-10953) - Spanish layout is broken
- [[MH-10955]](https://opencast.jira.com/browse/MH-10955) - Make sure recent versions of mkdocs work
- [[MH-10956]](https://opencast.jira.com/browse/MH-10956) - Update Synchronize.js
- [[MH-10985]](https://opencast.jira.com/browse/MH-10985) - As an operator I want to check the health status of Opencast
- [[MH-10986]](https://opencast.jira.com/browse/MH-10986) - Scheduling around DST change fails
- [[MH-10987]](https://opencast.jira.com/browse/MH-10987) - Improve workflow query to accept paging by index
- [[MH-10988]](https://opencast.jira.com/browse/MH-10988) - Rewrite workspace to fix several small issues
- [[MH-10989]](https://opencast.jira.com/browse/MH-10989) - Improve working file repository stream response
- [[MH-11007]](https://opencast.jira.com/browse/MH-11007) - Remove 3rd party tool script
- [[MH-11026]](https://opencast.jira.com/browse/MH-11026) - Several invalid links in the Opencast User Guides
- [[MH-11031]](https://opencast.jira.com/browse/MH-11031) - Missing option to create new event using files ingested from
  the inbox
- [[MH-11036]](https://opencast.jira.com/browse/MH-11036) - Adapt Fast Testing Workflow for Admin NG
- [[MH-11051]](https://opencast.jira.com/browse/MH-11051) - Fix WOH Documentation
- [[MH-11069]](https://opencast.jira.com/browse/MH-11069) - When creating new series, warning about read/write
  requirements is shown twice.
- [[MH-11072]](https://opencast.jira.com/browse/MH-11072) - The ACL editor needs enhanced validation
- [[MH-11074]](https://opencast.jira.com/browse/MH-11074) - Admin UI Test: New Event API Resource assembles the metadata
  for SCHEDULE\_MULTIPLE with DST change is failing
- [[MH-11083]](https://opencast.jira.com/browse/MH-11083) - Clean-up Codebase after Karaf
- [[MH-11085]](https://opencast.jira.com/browse/MH-11085) - Make sure bundle cache is cleared when restarting
- [[MH-11086]](https://opencast.jira.com/browse/MH-11086) - Shorten File Names in Log Output
- [[MH-11088]](https://opencast.jira.com/browse/MH-11088) - translation error in theodul player
- [[MH-11089]](https://opencast.jira.com/browse/MH-11089) - Theodul player seems not to work with Internet Explorer at
  all
- [[MH-11093]](https://opencast.jira.com/browse/MH-11093) - single video screen size jump when clicked
- [[MH-11094]](https://opencast.jira.com/browse/MH-11094) - Problems in Theodul controls plugin due to wrong resolves of
  merge conflicts
- [[MH-11095]](https://opencast.jira.com/browse/MH-11095) - Make assemblies more user firedly
- [[MH-11096]](https://opencast.jira.com/browse/MH-11096) - Errors when loading admin-ng login page
- [[MH-11099]](https://opencast.jira.com/browse/MH-11099) - Removing one role from an Access Policy (acl-det-05)
- [[MH-11101]](https://opencast.jira.com/browse/MH-11101) - Creating a Theme with 2 bumper videos - In and Out
  (thm-new-01)
- [[MH-11109]](https://opencast.jira.com/browse/MH-11109) - Event details tab cannot handle long event titles well
- [[MH-11110]](https://opencast.jira.com/browse/MH-11110) - minor updates to ffmpeg video-editor and silence detection
  based on gregs review of the feature in 1.6.3
- [[MH-11111]](https://opencast.jira.com/browse/MH-11111) - Formatting issues in “Theodul Pass Player - URL Parameters”
- [[MH-11114]](https://opencast.jira.com/browse/MH-11114) - Remove System.out.println from FileReadDeleteTest
- [[MH-11120]](https://opencast.jira.com/browse/MH-11120) - Several Services Fail During Shutdown
- [[MH-11122]](https://opencast.jira.com/browse/MH-11122) - Create Service Files (Systemd/SysV-Init)
- [[MH-11126]](https://opencast.jira.com/browse/MH-11126) - Fix Translation for 2.1
- [[MH-11133]](https://opencast.jira.com/browse/MH-11133) - i18n: Theme Detail view layout broken in Spanish
- [[MH-11135]](https://opencast.jira.com/browse/MH-11135) - Create Release Manager Docs
- [[MH-11137]](https://opencast.jira.com/browse/MH-11137) - Comment reasons are not working correctly
- [[MH-11138]](https://opencast.jira.com/browse/MH-11138) - Clock icon displayed twice next to comment creation date
- [[MH-11141]](https://opencast.jira.com/browse/MH-11141) - Playback Speed in player needs more useful defaults
- [[MH-11142]](https://opencast.jira.com/browse/MH-11142) - fix translations for shortcuts
- [[MH-11144]](https://opencast.jira.com/browse/MH-11144) - update documentation regarding property for mediamodule logo
- [[MH-11147]](https://opencast.jira.com/browse/MH-11147) - Missing translations: FILTERS.USERS.PROVIDER.LABEL &
  FILTERS.USERS.ROLE.LABEL
- [[MH-11149]](https://opencast.jira.com/browse/MH-11149) - Filter locations: Translations FILTERS.AGENTS.NAME.LABEL &
  FILTERS.AGENTS.STATUS.LABEL missing
- [[MH-11151]](https://opencast.jira.com/browse/MH-11151) - Plaback speed from menu
- [[MH-11152]](https://opencast.jira.com/browse/MH-11152) - Editing ACL: Translation for
  USERS.ACLS.DETAILS.ACCESS.ACCESS\_POLICY.DESCRIPTION missing
- [[MH-11153]](https://opencast.jira.com/browse/MH-11153) - Access Policy Details: Cannot navigate to previous or next
  ACL
- [[MH-11154]](https://opencast.jira.com/browse/MH-11154) - New Access Policy: Translation for
  USERS.ACLS.NEW.ACCESS.ACCESS\_POLICY.DESCRIPTION missing
- [[MH-11155]](https://opencast.jira.com/browse/MH-11155) - ACL Editor: Role not displayed at all
- [[MH-11158]](https://opencast.jira.com/browse/MH-11158) - Playback Tool: Time can be edited, but editing has no effect
- [[MH-11159]](https://opencast.jira.com/browse/MH-11159) - Users sorting: Sort order for 'Name' not correct
- [[MH-11160]](https://opencast.jira.com/browse/MH-11160) - Create Group overwrites existing groups without warning
- [[MH-11162]](https://opencast.jira.com/browse/MH-11162) - security\_sample\_cas.xml in MH 2.0.1 Points to Wrong
  Welcome Page
- [[MH-11166]](https://opencast.jira.com/browse/MH-11166) - Number of rows not displayed on Systems-&gt;Servers
- [[MH-11176]](https://opencast.jira.com/browse/MH-11176) - Cannot playback a recording via LTI in 2.x
- [[MH-11177]](https://opencast.jira.com/browse/MH-11177) - Fix Player OSGI Dependencies
- [[MH-11178]](https://opencast.jira.com/browse/MH-11178) - Prevent FFmpeg Experimental AAC Encoder Bug to Affect
  Opencast
- [[MH-11180]](https://opencast.jira.com/browse/MH-11180) - Update video.js to latest 4.x version
- [[MH-11181]](https://opencast.jira.com/browse/MH-11181) - Flash streaming with multi-quality video does not work
- [[MH-11185]](https://opencast.jira.com/browse/MH-11185) - Event Details-&gt;Assets-&gt;: Asset size is always 0
- [[MH-11186]](https://opencast.jira.com/browse/MH-11186) - Event Details-&gt;Assets-&gt;Media-&gt;Media Details:
  Superfluous row 'Flavor'
- [[MH-11187]](https://opencast.jira.com/browse/MH-11187) - Configuration-&gt;Themes: Number of rows not displayed
  correctly
- [[MH-11189]](https://opencast.jira.com/browse/MH-11189) - Actions-&gt;Start Task: User can press create button
  multiple times
- [[MH-11193]](https://opencast.jira.com/browse/MH-11193) - Setting audio level slider to "zero" does not set the actual
  audio level to "zero"
- [[MH-11196]](https://opencast.jira.com/browse/MH-11196) - REST docs cannot be found in new admin ui
- [[MH-11198]](https://opencast.jira.com/browse/MH-11198) - Event dashboard seems not to support i18n
- [[MH-11201]](https://opencast.jira.com/browse/MH-11201) - Maven Assembly Plug-in Listed Twice
- [[MH-11202]](https://opencast.jira.com/browse/MH-11202) - FFmpeg video editor operation is synchronized
- [[MH-11212]](https://opencast.jira.com/browse/MH-11212) - Main Pom Clean-Up
- [[MH-11218]](https://opencast.jira.com/browse/MH-11218) - Karaf based Solr configuration
- [[MH-11221]](https://opencast.jira.com/browse/MH-11221) - ComposerServiceImpl creates incorrect incidents and error
  messages
- [[MH-11223]](https://opencast.jira.com/browse/MH-11223) - Remove unused files
- [[MH-11234]](https://opencast.jira.com/browse/MH-11234) - Admin-NG throws a couple of 404 errors
- [[MH-11236]](https://opencast.jira.com/browse/MH-11236) - Security ACL *see security list*
- [[MH-11237]](https://opencast.jira.com/browse/MH-11237) - Service files are missing
- [[MH-11238]](https://opencast.jira.com/browse/MH-11238) - Silence-detection does not read configuration value for
  ffmpeg binary path
- [[MH-11248]](https://opencast.jira.com/browse/MH-11248) - Publish-Engage Workflow Operation Documentation is Missing
  Configuration Keys
- [[MH-11249]](https://opencast.jira.com/browse/MH-11249) - Apply-ACL WOH not properly replaced by Seried-WOH in
  Documentation
- [[MH-11250]](https://opencast.jira.com/browse/MH-11250) - Put temporary files in karaf data not in opencast.storage
- [[MH-11251]](https://opencast.jira.com/browse/MH-11251) - Capture-Admin Tests May Fail When Executed Too Fast
- [[MH-11257]](https://opencast.jira.com/browse/MH-11257) - Deprecated Mkdocs Config
- [[MH-11258]](https://opencast.jira.com/browse/MH-11258) - Make host configuration easier

Opencast 2.0.x
--------------

### Opencast 2.0.2

*Released on December 22, 2015*

- [[MH-10235]](https://opencast.jira.com/browse/MH-10235) - Users are unable to determine the Version of Matterhorn
- [[MH-10484]](https://opencast.jira.com/browse/MH-10484) - Remove Mediainfo from 3rd-Party-Tools
- [[MH-10558]](https://opencast.jira.com/browse/MH-10558) - Mime type not identified for matroska / mkv files
- [[MH-10588]](https://opencast.jira.com/browse/MH-10588) - Improve MySQL DDL to make it consistent again
- [[MH-10759]](https://opencast.jira.com/browse/MH-10759) - Write QA documentation for Access Policies
- [[MH-10759]](https://opencast.jira.com/browse/MH-10759) - Write QA documentation for Series
- [[MH-10759]](https://opencast.jira.com/browse/MH-10759) - Write QA documentation for Themes
- [[MH-10818]](https://opencast.jira.com/browse/MH-10818) - The creation date is presented as ISO string in the event
  metadata
- [[MH-10918]](https://opencast.jira.com/browse/MH-10918) - Improve the representation of the
  attachments/catalogs/media/publications in the event details
- [[MH-10956]](https://opencast.jira.com/browse/MH-10956) - Update Synchronize.js
- [[MH-10964]](https://opencast.jira.com/browse/MH-10964) - The Opencast start script does not work on Mac OS X
- [[MH-10976]](https://opencast.jira.com/browse/MH-10976) - Eclipse (m2e) throws NullPointerException erros due to a
  missing property in the pom.xml file
- [[MH-11007]](https://opencast.jira.com/browse/MH-11007) - Remove 3rd party tool script
- [[MH-11007]](https://opencast.jira.com/browse/MH-11007) - Switch subtitle embedder to FFmpeg
- [[MH-11026]](https://opencast.jira.com/browse/MH-11026) - Several invalid links in the Opencast User Guides
- [[MH-11038]](https://opencast.jira.com/browse/MH-11038) - Make ListProviderScanner Scanner Less verbose
- [[MH-11048]](https://opencast.jira.com/browse/MH-11048) - admin ui tries to load missing library
- [[MH-11051]](https://opencast.jira.com/browse/MH-11051) - Fix WOH Documentation
- [[MH-11060]](https://opencast.jira.com/browse/MH-11060) - ActiveMQ settings filename fix (r/2.0.x)
- [[MH-11068]](https://opencast.jira.com/browse/MH-11068) - Table 'mh\_bundleinfo' doesn't exist
- [[MH-11110]](https://opencast.jira.com/browse/MH-11110) - minor updates to ffmpeg video-editor and silence detection
  based on gregs review of the feature in 1.6.3
- [[MH-11176]](https://opencast.jira.com/browse/MH-11176) - Cannot playback a recording via LTI in 2.x
- [[MH-11177]](https://opencast.jira.com/browse/MH-11177) - Fix Player OSGI Dependencies
- [[MH-11181]](https://opencast.jira.com/browse/MH-11181) - Flash streaming with multi-quality video does not work
- [[MH-11202]](https://opencast.jira.com/browse/MH-11202) - FFmpeg video editor operation is synchronized
- [[MH-11221]](https://opencast.jira.com/browse/MH-11221) - ComposerServiceImpl creates incorrect incidents and error
  messages
- [[MH-11236]](https://opencast.jira.com/browse/MH-11236) - Security ACL *see security list*
- [[MH-11238]](https://opencast.jira.com/browse/MH-11238) - Silence-detection does not read configuration value for
  ffmpeg binary path
- [[MH-11256]](https://opencast.jira.com/browse/MH-11256) - Opencast docs do not build anymore

### Opencast 2.0.1

*Released on September 3, 2015*

- [[MH-10822]](https://opencast.jira.com/browse/MH-10822) - Possible to create new access policy template without a role
  with read/write permissions
- [[MH-10938]](https://opencast.jira.com/browse/MH-10938) - Missing views counter in player
- [[MH-10941]](https://opencast.jira.com/browse/MH-10941) - Usertracking Service Missing Endpoint
- [[MH-10955]](https://opencast.jira.com/browse/MH-10955) - Make sure recent versions of mkdocs work
- [[MH-10962]](https://opencast.jira.com/browse/MH-10962) - Add missing licenses to NOTICES
- [[MH-10968]](https://opencast.jira.com/browse/MH-10968) - Add note about ffmpeg/libav on Ubuntu
- [[MH-10975]](https://opencast.jira.com/browse/MH-10975) - async loading of translations
- [[MH-10995]](https://opencast.jira.com/browse/MH-10995) - Gathering workflow statistics for JMX causes extreme
  performance issues

### Opencast 2.0.0

*Released on July 17, 2015*

- [[MH-9950]](https://opencast.jira.com/browse/MH-9950) - "Clean up"/Split up nested functions in the core routine
  (core.js)
- [[MH-9950]](https://opencast.jira.com/browse/MH-9950) - Load CSS files in the core HTML file, not the JavaScript
- [[MH-9950]](https://opencast.jira.com/browse/MH-9950) - Scrolling is required to see the controls if they are
  configured to be below the video.
- [[MH-9950]](https://opencast.jira.com/browse/MH-9950) - Some Keys don't work
- [[MH-9950]](https://opencast.jira.com/browse/MH-9950) - Theodul Core Jasmine Tests Sometimes Failing
- [[MH-10029]](https://opencast.jira.com/browse/MH-10029) - FFmpeg based Videosegmenter
- [[MH-10140]](https://opencast.jira.com/browse/MH-10140) - Capture agent with no configuration is always shown as
  "idle"
- [[MH-10202]](https://opencast.jira.com/browse/MH-10202) - No ACL in new series when ingested a new mediapackage with a
  new series.
- [[MH-10230]](https://opencast.jira.com/browse/MH-10230) - Typos on the welcome page
- [[MH-10332]](https://opencast.jira.com/browse/MH-10332) - Remove Mediainfo Inspection Service
- [[MH-10382]](https://opencast.jira.com/browse/MH-10382) - Add a UI Element to Easily Unregister Capture Agents
- [[MH-10419]](https://opencast.jira.com/browse/MH-10419) - Improve user tracking tables
- [[MH-10510]](https://opencast.jira.com/browse/MH-10510) - Move Workflow Operation Handler into their own Packages
- [[MH-10550]](https://opencast.jira.com/browse/MH-10550) - Non-Interactive Foreground Mode For Matterhorn
- [[MH-10572]](https://opencast.jira.com/browse/MH-10572) - ShibbolethLoginHandler: 500 Error when login the first time
- [[MH-10594]](https://opencast.jira.com/browse/MH-10594) - Re-configure Start Scripts for Different Deployment Types
- [[MH-10615]](https://opencast.jira.com/browse/MH-10615) - Enable Optional Compiler Arguments
- [[MH-10620]](https://opencast.jira.com/browse/MH-10620) - Port Silence Detector from GStreamer to FFmpeg
- [[MH-10622]](https://opencast.jira.com/browse/MH-10622) - Wave Generation Improvement
- [[MH-10623]](https://opencast.jira.com/browse/MH-10623) - Set Sensible Default for Workspace Cleanup Period
- [[MH-10624]](https://opencast.jira.com/browse/MH-10624) - Fixes for FFmpeg Videosegmenter (Set Binary)
- [[MH-10630]](https://opencast.jira.com/browse/MH-10630) - Extending common functionality
- [[MH-10631]](https://opencast.jira.com/browse/MH-10631) - Scheduler service authorization handling
- [[MH-10635]](https://opencast.jira.com/browse/MH-10635) - Text extractor dead lock
- [[MH-10640]](https://opencast.jira.com/browse/MH-10640) - several problems with the metadata form to create a new
  event
- [[MH-10656]](https://opencast.jira.com/browse/MH-10656) - Login Screen: Placeholder and Focus
- [[MH-10658]](https://opencast.jira.com/browse/MH-10658) - Email template: diverse problems
- [[MH-10664]](https://opencast.jira.com/browse/MH-10664) - What is a template in Access Policy and how do I create it?
- [[MH-10665]](https://opencast.jira.com/browse/MH-10665) - 404 for variables.json
- [[MH-10667]](https://opencast.jira.com/browse/MH-10667) - Previous Button does not always work
- [[MH-10681]](https://opencast.jira.com/browse/MH-10681) - Time is missing when a workflow operation has been started
  and stopped
- [[MH-10683]](https://opencast.jira.com/browse/MH-10683) - Remove Capture Agent
- [[MH-10683]](https://opencast.jira.com/browse/MH-10683) - Remove the Capture Agent integration tests
- [[MH-10684]](https://opencast.jira.com/browse/MH-10684) - Admin UI seems only unresponsive if server is down
- [[MH-10689]](https://opencast.jira.com/browse/MH-10689) - I should get a warning, if I leave the Admin UI while I
  still create an event (upload a file)
- [[MH-10698]](https://opencast.jira.com/browse/MH-10698) - workflow after videoeditor does not produce any `*/delivery`
  flavors
- [[MH-10700]](https://opencast.jira.com/browse/MH-10700) - Service Registry throws NPE exception on startup
- [[MH-10704]](https://opencast.jira.com/browse/MH-10704) - Workflows fail if adding themes
- [[MH-10705]](https://opencast.jira.com/browse/MH-10705) - Row counter in Jobs table is 1 too much
- [[MH-10707]](https://opencast.jira.com/browse/MH-10707) - Unit Test Failure
- [[MH-10710]](https://opencast.jira.com/browse/MH-10710) - NullPointerException in VideoSegmentationWOH
- [[MH-10711]](https://opencast.jira.com/browse/MH-10711) - OptimisticLockException after ingest
- [[MH-10712]](https://opencast.jira.com/browse/MH-10712) - Workflow cleanup out of memory error
- [[MH-10713]](https://opencast.jira.com/browse/MH-10713) - Cache util blocks forever
- [[MH-10726]](https://opencast.jira.com/browse/MH-10726) - Archive operation should use filesystem copy rather than
  http download
- [[MH-10736]](https://opencast.jira.com/browse/MH-10736) - Engage is currently broken and won't play videos but
  Theodule does
- [[MH-10740]](https://opencast.jira.com/browse/MH-10740) - NPE in ToolsEndpoint
- [[MH-10746]](https://opencast.jira.com/browse/MH-10746) - There is no event status column
- [[MH-10758]](https://opencast.jira.com/browse/MH-10758) - Issues found in production use of Theodul: changing icons,
  seeking in Chrome, using configured logos, wording, layout...
- [[MH-10759]](https://opencast.jira.com/browse/MH-10759) - Write QA documentation for Events
- [[MH-10759]](https://opencast.jira.com/browse/MH-10759) - Write QA documentation for Groups
- [[MH-10759]](https://opencast.jira.com/browse/MH-10759) - Write QA documentation for Servers
- [[MH-10759]](https://opencast.jira.com/browse/MH-10759) - Write QA documentation for Services
- [[MH-10763]](https://opencast.jira.com/browse/MH-10763) - Remove Old Confirations
- [[MH-10765]](https://opencast.jira.com/browse/MH-10765) - Operation details doesn't show operation attributes when
  state is instantiated
- [[MH-10768]](https://opencast.jira.com/browse/MH-10768) - Workflow operations table in the events details should
  refresh automatically
- [[MH-10769]](https://opencast.jira.com/browse/MH-10769) - Add (x) icon in the events and series tableview to allow
  deletion of single Events/Series
- [[MH-10770]](https://opencast.jira.com/browse/MH-10770) - Some captions of tabs are not yet translated
- [[MH-10772]](https://opencast.jira.com/browse/MH-10772) - Ensure that buttons order is consistent in the actions
  column
- [[MH-10773]](https://opencast.jira.com/browse/MH-10773) - Allow to have free-text value for presenters, contributors,
  organizers or publishers
- [[MH-10774]](https://opencast.jira.com/browse/MH-10774) - ACL editing should be locked on the Series level when events
  of the series are being processed
- [[MH-10775]](https://opencast.jira.com/browse/MH-10775) - All the roles with read/write rights can be deleted from the
  ACL editor in Events/Series details
- [[MH-10776]](https://opencast.jira.com/browse/MH-10776) - Include Spanish and French translation into Theodul.
- [[MH-10780]](https://opencast.jira.com/browse/MH-10780) - Specify Requirements
- [[MH-10781]](https://opencast.jira.com/browse/MH-10781) - Respect tags while filtering for suitable tracks in Theodul
  player
- [[MH-10792]](https://opencast.jira.com/browse/MH-10792) - Pom.xml Extra Modules
- [[MH-10798]](https://opencast.jira.com/browse/MH-10798) - Event Details tile shows hash identifier
- [[MH-10799]](https://opencast.jira.com/browse/MH-10799) - Videoeditor operation does not properly handle missing
  preview formats
- [[MH-10804]](https://opencast.jira.com/browse/MH-10804) - It is unclear in which timezone you schedule in the admin-ui
- [[MH-10807]](https://opencast.jira.com/browse/MH-10807) - New event POST request contains every series and user
- [[MH-10808]](https://opencast.jira.com/browse/MH-10808) - Disable Demo Users
- [[MH-10810]](https://opencast.jira.com/browse/MH-10810) - Rename upgrade script form 1.6 to 2.0
- [[MH-10812]](https://opencast.jira.com/browse/MH-10812) - Use bundles.configuration.location in admin ng settings.yml
- [[MH-10814]](https://opencast.jira.com/browse/MH-10814) - Pressing play while buffering breaks player
- [[MH-10816]](https://opencast.jira.com/browse/MH-10816) - Move Message Broker Configuration to Global Config
- [[MH-10821]](https://opencast.jira.com/browse/MH-10821) - Severe Issue with Scheduled Events
- [[MH-10829]](https://opencast.jira.com/browse/MH-10829) - Unchecking "Remember me" checkbox has no effect when logged
  out. Pressing the browsers back button you're still logged in an d can use all functions.
- [[MH-10836]](https://opencast.jira.com/browse/MH-10836) - Issues with matterhorn-engage-theodul-plugin-archetype
- [[MH-10837]](https://opencast.jira.com/browse/MH-10837) - Bulk deletion of events doesn't work correctly
- [[MH-10843]](https://opencast.jira.com/browse/MH-10843) - different video qualities are not filtered correctly.
- [[MH-10845]](https://opencast.jira.com/browse/MH-10845) - Summary of "Add Events" and "Add Series" shows irrelevant
  data
- [[MH-10847]](https://opencast.jira.com/browse/MH-10847) - Missing with-role directive in "Start Task" option in
  Actions dropdown
- [[MH-10848]](https://opencast.jira.com/browse/MH-10848) - Event conflict endpoint returns Server error 500
- [[MH-10849]](https://opencast.jira.com/browse/MH-10849) - Temporary videoeditor files get not deleted
- [[MH-10850]](https://opencast.jira.com/browse/MH-10850) - Interface MatterhornConstans has a typo
- [[MH-10853]](https://opencast.jira.com/browse/MH-10853) - Improve admin UI ng workflows
- [[MH-10855]](https://opencast.jira.com/browse/MH-10855) - Task Menu displays wrong UI
- [[MH-10864]](https://opencast.jira.com/browse/MH-10864) - Remove Trailing Spaces From Less Files
- [[MH-10866]](https://opencast.jira.com/browse/MH-10866) - Documentation: Incorrect Repository Links
- [[MH-10868]](https://opencast.jira.com/browse/MH-10868) - Linebreak before last segment in player
- [[MH-10873]](https://opencast.jira.com/browse/MH-10873) - capture-admin-service-impl tests randomly failing
- [[MH-10876]](https://opencast.jira.com/browse/MH-10876) - Admin UI NG makes calls to remote resources
- [[MH-10880]](https://opencast.jira.com/browse/MH-10880) - Remote base keeps try to call a service
- [[MH-10881]](https://opencast.jira.com/browse/MH-10881) - Wrong links to r/2.0.x on documentation page
- [[MH-10884]](https://opencast.jira.com/browse/MH-10884) - WokflowOperation getTimeInQueue should return 0 if value is
  NULL
- [[MH-10888]](https://opencast.jira.com/browse/MH-10888) - Theodul player: audio-only does not work - player checked
  for unavailable size.
- [[MH-10901]](https://opencast.jira.com/browse/MH-10901) - Execute Service is not in main pom.xml and will not be built
- [[MH-10902]](https://opencast.jira.com/browse/MH-10902) - ./modules/matterhorn-publication-service-youtube/ obsolete
- [[MH-10905]](https://opencast.jira.com/browse/MH-10905) - FFmpeg videoeditor only works with audio and video available
- [[MH-10911]](https://opencast.jira.com/browse/MH-10911) - Remove executable flag from non-executables
- [[MH-10912]](https://opencast.jira.com/browse/MH-10912) - Init scripts contain undefined references to DEBUG\_PORT and
  DEBUG\_SUSPEND
- [[MH-10913]](https://opencast.jira.com/browse/MH-10913) - Add Event: License Metadata Field Text
- [[MH-10924]](https://opencast.jira.com/browse/MH-10924) - Update to new Opencast logos
- [[MH-10926]](https://opencast.jira.com/browse/MH-10926) - Extensive PhantomJS warnings when building admin-ng
- [[MH-10928]](https://opencast.jira.com/browse/MH-10928) - Adjust loglevel in DictionaryService
- [[MH-10929]](https://opencast.jira.com/browse/MH-10929) - Cutting and Review are skipped when config is set to do so
- [[MH-10930]](https://opencast.jira.com/browse/MH-10930) - Fix missing German translation
- [[MH-10934]](https://opencast.jira.com/browse/MH-10934) - Once set, one cannot remove some metadata in the create
  event dialog
- [[MH-10938]](https://opencast.jira.com/browse/MH-10938) - Missing views counter in player
- [[MH-10939]](https://opencast.jira.com/browse/MH-10939) - Task Summary does not display configuration values
- [[MH-10946]](https://opencast.jira.com/browse/MH-10946) - Fix Opencast 2 Installation Guides
- [[MH-10950]](https://opencast.jira.com/browse/MH-10950) - Fix DDL Readme
- [[MH-10952]](https://opencast.jira.com/browse/MH-10952) - Fix matterhorn-execute-operations naming
- [[MH-10957]](https://opencast.jira.com/browse/MH-10957) - Add License Guide for Developers
