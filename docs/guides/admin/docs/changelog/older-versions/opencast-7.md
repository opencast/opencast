Opencast 7 Changelog
--------------------

## Opencast 7.9

*Released on December 8, 2020*

- [[GHSA-44cw-p2hm-gpf6](https://github.com/opencast/opencast/security/advisories/GHSA-44cw-p2hm-gpf6)]
  Security: Disabled Hostname Verification
- [[#1964](https://github.com/opencast/opencast/pull/1964)] -
  Docs: When the sidebar is hidden, the navigation links are disabled now
- [[#1922](https://github.com/opencast/opencast/pull/1922)] -
  Remove Spring snapshot repository from main pom

## Opencast 7.8

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

## Opencast 7.7

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

## Opencast 7.6

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
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/1315">1315</a>] -
  Bump spring-security-oauth from 2.3.6.RELEASE to 2.3.7.RELEASE</li>
</ul>
</details>

## Opencast 7.5

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

## Opencast 7.4

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

## Opencast 7.3

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

## Opencast 7.2

*Released on August 02, 2019*

- [[MH-13662](https://opencast.jira.com/browse/MH-13662)][[#1000](https://github.com/opencast/opencast/pull/1000)] -
  Update LTI Information

## Opencast 7.1

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

## Opencast 7.0

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