Opencast 11
----------

### Opencast 11.12

*Released on November 16th, 2022*

- [[#4382](https://github.com/opencast/opencast/pull/4382)] -
  Fix Encoding Profile Type Handling in Encode WOH
- [[#4332](https://github.com/opencast/opencast/pull/4332)] -
  CVE-2022-42889 library upgrade
- [[#4316](https://github.com/opencast/opencast/pull/4316)] -
  Update Live Publication in Archive on Capture Agent Change
- [[#4250](https://github.com/opencast/opencast/pull/4250)] -
  Actually Retract Live Publication
- [[#4249](https://github.com/opencast/opencast/pull/4249)] -
  Make initialization of new event ACL with series ACL in the Admin UI configurable
- [[#4230](https://github.com/opencast/opencast/pull/4230)] -
  Opencast 11.11 release notes
- [[#4208](https://github.com/opencast/opencast/pull/4208)] -
  Fix composer incident codes and messages for process-smil and multiencode.
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4242">4242</a>] -
  Bump eslint from 8.23.1 to 8.24.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4169">4169</a>] -
  Bump express from 4.17.3 to 4.18.1 in /modules/engage-paella-player</li>
</ul>
</details>

### Opencast 11.11

*Released on September 21th, 2022*

- [[#4200](https://github.com/opencast/opencast/pull/4200)] -
  Add size to term aggregation in order to return more than 10 terms
- [[#4185](https://github.com/opencast/opencast/pull/4185)] -
  Fix for S3 asset storage not releasing http connections
- [[#4182](https://github.com/opencast/opencast/pull/4182)] -
  Fix race condition when loading workflow definitions
- [[#4172](https://github.com/opencast/opencast/pull/4172)] -
  Allow upload of multiple assets with the same flavor (based on config)
- [[#4164](https://github.com/opencast/opencast/pull/4164)] -
  Use getObjectMetadata to get object version when uploading file to S3 archive (fixes #4161)
- [[#4159](https://github.com/opencast/opencast/pull/4159)] -
  Fix search service limit - update more than 10 events for a series change
- [[#4155](https://github.com/opencast/opencast/pull/4155)] -
  Fix ingest of series catalog via external URL
- [[#4155](https://github.com/opencast/opencast/pull/4155)] -
  Fix workflow config handover from ingest to scheduler service
- [[#4119](https://github.com/opencast/opencast/pull/4119)] -
  Respect system settings when sending adopter data
- [[#3681](https://github.com/opencast/opencast/pull/3681)] -
  Make deletion of live publication in case of capture errors configurable

### Opencast 11.10

*Released on August 17th, 2022*

- [[#4068](https://github.com/opencast/opencast/pull/4068)] -
  Fix CreatorDate Filter in GET api/series/
- [[#3990](https://github.com/opencast/opencast/pull/3990)] -
  Remove Graphviz from Documentation
- [[#3952](https://github.com/opencast/opencast/pull/3952)] -
  Add changelog and release notes for OC 11.9
- [[#3923](https://github.com/opencast/opencast/pull/3923)] -
  Handle tracks with multiple videos in engage player

### Opencast 11.9

*Released on July 6th, 2022*

- [[#3915](https://github.com/opencast/opencast/pull/3915)] -
  Fix Ingest Service Download Feature
- [[#3907](https://github.com/opencast/opencast/pull/3907)] -
  Split url parameter on first equal sign
- [[#3902](https://github.com/opencast/opencast/pull/3902)] -
  Fix seriesname solver when ID is shorter than 7 characters
- [[#3882](https://github.com/opencast/opencast/pull/3882)] -
  Adopter wizard botton text repair
- [[#3881](https://github.com/opencast/opencast/pull/3881)] -
  Fix admin ui proxy
- [[#3871](https://github.com/opencast/opencast/pull/3871)] -
  Add changelog and release notes for OC 11.8
- [[#3805](https://github.com/opencast/opencast/pull/3805)] -
  Set Title and start date and time for duplicate events using duplicate WFH

### Opencast 11.8

*Released on June 15th, 2022*

- [[#3808](https://github.com/opencast/opencast/pull/3808)] -
  Add missing PRs to OC11 changelog
- [[#3785](https://github.com/opencast/opencast/pull/3785)] -
  Clarify that #3715 was a fix in the release notes
- [[#3777](https://github.com/opencast/opencast/pull/3777)] -
  Add changelog and release notes for OC 11.7
- [[#3672](https://github.com/opencast/opencast/pull/3672)] -
  Disable group removement upon missing group roles

### Opencast 11.7

*Released on May 18th, 2022*

- [[#3716](https://github.com/opencast/opencast/pull/3716)] -
  Add missing tags in encode WOH
- [[#3715](https://github.com/opencast/opencast/pull/3715)] -
  Unbreak the metadata editor included with the video editor and save events as
  well as the video editor state when save is pressed
- [[#3708](https://github.com/opencast/opencast/pull/3708)] -
  partial-publish workflow fix
- [[#3707](https://github.com/opencast/opencast/pull/3707)] -
  Out of video image extraction should not fail
- [[#3688](https://github.com/opencast/opencast/pull/3688)] -
  Add release notes and changelog for OC 11.6
- [[#3682](https://github.com/opencast/opencast/pull/3682)] -
  Make creation of default External API group configurable
- [[#3680](https://github.com/opencast/opencast/pull/3680)] -
  Acl null-checks for `/api/series/{seriesId}/acl` endpoint
- [[#3679](https://github.com/opencast/opencast/pull/3679)] -
  Don't store invalid ACLs submitted to api-endpoint
- [[#3661](https://github.com/opencast/opencast/pull/3661)] -
  Check if user can be loaded before starting the workflow
- [[#3635](https://github.com/opencast/opencast/pull/3635)] -
  Fix bug with adding a series when a deleted one still lingers

### Opencast 11.6

*Released on April 20th, 2022*

- [[#3638](https://github.com/opencast/opencast/pull/3638)] -
  Fix add punctuation and models support to google transcription
- [[#3633](https://github.com/opencast/opencast/pull/3633)] -
  allow admin user for node to node communication #3556
- [[#3631](https://github.com/opencast/opencast/pull/3631)] -
  Error handling in Vosk module
- [[#3630](https://github.com/opencast/opencast/pull/3630)] -
  Fix Vosk Concurrency Problem
- [[#3605](https://github.com/opencast/opencast/pull/3605)] -
  fix #3602, Terminate state service, check that host is not in maintenance
- [[#3586](https://github.com/opencast/opencast/pull/3586)] -
  Auto-generated series for Capture Agents
- [[#3567](https://github.com/opencast/opencast/pull/3567)] -
  Fix workflow definition installation
- [[#3555](https://github.com/opencast/opencast/pull/3555)] -
  Rework the userdirectory-brightspace module
- [[#3550](https://github.com/opencast/opencast/pull/3550)] -
  Add release notes and changelog for OC 11.5
- [[#3330](https://github.com/opencast/opencast/pull/3330)] -
  Removed ManagedService from MediaInspectionService

### Opencast 11.5

*Released on March 23rd, 2022*

- [[#3544](https://github.com/opencast/opencast/pull/3544)] -
  Fix bug: Editor sometimes doesn't skip deactivated sections on Safari Browser
- [[#3547](https://github.com/opencast/opencast/pull/3547)] -
  Redowngrade Paella to 6.4.4
- [[#3546](https://github.com/opencast/opencast/pull/3546)] -
  Update Editor to 2022-03-22
- [[#3545](https://github.com/opencast/opencast/pull/3545)] -
  Update 11.x translations, restore zh_TW translations
- [[#3541](https://github.com/opencast/opencast/pull/3541)] -
  Downgrade commons-io
- [[#3540](https://github.com/opencast/opencast/pull/3540)] -
  Fix NPE when signing publication URLs
- [[#3534](https://github.com/opencast/opencast/pull/3534)] -
  Fix Admin UI builds due to missing SASS
- [[#3532](https://github.com/opencast/opencast/pull/3532)] -
  Fix wrong name of admin UI configuration file
- [[#3520](https://github.com/opencast/opencast/pull/3520)] -
  Docs: Force same sample rate for audio files in PartialImport WOH encoding
- [[#3474](https://github.com/opencast/opencast/pull/3474)] -
  Fix OC 11.4 release notes layout issues
- [[#3466](https://github.com/opencast/opencast/pull/3466)] -
  Check if encoding profile exist for imageToVideo Operation
- [[#3460](https://github.com/opencast/opencast/pull/3460)] -
  fix admin-ui endpoint crash, when a series without title exists
- [[#3459](https://github.com/opencast/opencast/pull/3459)] -
  Add release notes and changelog for OC 11.4
- [[#3343](https://github.com/opencast/opencast/pull/3343)] -
  Password strength indicator
- [[#3197](https://github.com/opencast/opencast/pull/3197)] -
  Elasticsearch Retry Config

### Opencast 11.4

*Released on February 16th, 2022*

- [[#3457](https://github.com/opencast/opencast/pull/3457)] -
  Update Opencast Studio to 2022-02-16
- [[#3444](https://github.com/opencast/opencast/pull/3444)] -
  Fix frontend-no-prebuilt profile
- [[#3437](https://github.com/opencast/opencast/pull/3437)] -
  Fix merge conflict
- [[#3432](https://github.com/opencast/opencast/pull/3432)] -
  Fix `pom.xml` Version
- [[#3423](https://github.com/opencast/opencast/pull/3423)] -
  Fixes OSGi error when shutting down Opencast
- [[#3416](https://github.com/opencast/opencast/pull/3416)] -
  Fixed Typo in Logging Statement
- [[#3384](https://github.com/opencast/opencast/pull/3384)] -
  Add DublinCore Catalog to Scheduler JSON
- [[#3369](https://github.com/opencast/opencast/pull/3369)] -
  Update slf4j
- [[#3368](https://github.com/opencast/opencast/pull/3368)] -
  Capture agent calendar as JSON
- [[#3367](https://github.com/opencast/opencast/pull/3367)] -
  Add release notes and changelog for OC 11.3
- [[#3365](https://github.com/opencast/opencast/pull/3365)] -
  Remove `mvn site` Test
- [[#3344](https://github.com/opencast/opencast/pull/3344)] -
  merge the ldap userdirectory behaviour from 9.x into the current module
- [[#3342](https://github.com/opencast/opencast/pull/3342)] -
  Allow metadata editting
- [[#3340](https://github.com/opencast/opencast/pull/3340)] -
  Match Inbox Events Against Schedule
- [[#3327](https://github.com/opencast/opencast/pull/3327)] -
  Extend Metadata Extraction in Inbox
- [[#3203](https://github.com/opencast/opencast/pull/3203)] -
  start-workflow-wfh allows to pass mediapackage id from previous WFH
- [[#3155](https://github.com/opencast/opencast/pull/3155)] -
  Allow the use of organization properties in workflows


### Opencast 11.3

*Released on January 19th, 2022*

- [[#3337](https://github.com/opencast/opencast/pull/3337)] -
  Remove non-optional optional
- [[#3336](https://github.com/opencast/opencast/pull/3336)] -
  Fix REST Test Network Bindings
- [[#3329](https://github.com/opencast/opencast/pull/3329)] -
  Use 1970-01-02 as dummy value in MySQL migration
- [[#3328](https://github.com/opencast/opencast/pull/3328)] -
  Java Dependency Update
- [[#3301](https://github.com/opencast/opencast/pull/3301)] -
  Add missing 11.2 changelog
- [[#3297](https://github.com/opencast/opencast/pull/3297)] -
  Mark Opencast 11 as Available
- [[#3273](https://github.com/opencast/opencast/pull/3273)] -
  Changed embed code selection design
- [[#3254](https://github.com/opencast/opencast/pull/3254)] -
  Prevent API failing on empty files
- [[#3188](https://github.com/opencast/opencast/pull/3188)] -
  Cover image surrogate problem
- [[#3154](https://github.com/opencast/opencast/pull/3154)] -
  Allow selection by tags in execute-once
- [[#3152](https://github.com/opencast/opencast/pull/3152)] -
  Add id to execute-many and org_id to execute-many and execute-once
- [[#2855](https://github.com/opencast/opencast/pull/2855)] -
  Added speech to text (Vosk)


### Opencast 11.2

*Released on December 20th, 2021*

- [[#3282](https://github.com/opencast/opencast/pull/3282)]
  Security: Update to Pax Logging 1.11.12


### Opencast 11.1

*Released on December 17th, 2021*

- [[#3270](https://github.com/opencast/opencast/pull/3270)] -
  Remove old release notes snippets
- [[#3268](https://github.com/opencast/opencast/pull/3268)] -
  Fix Typos and Syntax in Opencast 11 Release Notes
- [[#3266](https://github.com/opencast/opencast/pull/3266)] -
  Fix formatting of OC 11 release notes
- [[#3265](https://github.com/opencast/opencast/pull/3265)] -
  Prepare docs for OC 11 release
- [[#3264](https://github.com/opencast/opencast/pull/3264)] -
  Update Logging Documentation
- [[#3263](https://github.com/opencast/opencast/pull/3263)] -
  Remove chinese traditional
- [[#3221](https://github.com/opencast/opencast/pull/3221)] -
  Link Configuration Files in GitHub
- [[#3220](https://github.com/opencast/opencast/pull/3220)] -
  Improve GitHub Actions Concurrency Configuration
- [[#3128](https://github.com/opencast/opencast/pull/3128)] -
  Let Encode Handle HLS


### Opencast 11.0

*Released on December 15th, 2021*

- [[#2949](https://github.com/opencast/opencast/pull/2949)] -
  Encode username before useing it in CanvasUserRoleProvider
- [[#2591](https://github.com/opencast/opencast/pull/2591)] -
  Remove automatic handling of HLS bitrate ladder
- [[#2559](https://github.com/opencast/opencast/pull/2559)] -
  Allow proper mapping of tenant hostnames to URLs
- [[#3263](https://github.com/opencast/opencast/pull/3263)] -
  Remove chinese traditional
- [[#3264](https://github.com/opencast/opencast/pull/3264)] -
  Update Logging Documentation
- [[#3231](https://github.com/opencast/opencast/pull/3231)] -
  Fix possible type change in external API
- [[#3221](https://github.com/opencast/opencast/pull/3221)] -
  Link Configuration Files in GitHub
- [[#3220](https://github.com/opencast/opencast/pull/3220)] -
  Improve GitHub Actions Concurrency Configuration
- [[#3201](https://github.com/opencast/opencast/pull/3201)] -
  Revert 3161 remove solr from series service
- [[#3198](https://github.com/opencast/opencast/pull/3198)] -
  Fixed Admin UI Endpoint Configuration not loading on Opencast startup
- [[#3185](https://github.com/opencast/opencast/pull/3185)] -
  Update Node to Latest LTS
- [[#3184](https://github.com/opencast/opencast/pull/3184)] -
  Update osgi compendium in 10.x merge
- [[#3177](https://github.com/opencast/opencast/pull/3177)] -
  Replace native javascript with angularJS code
- [[#3127](https://github.com/opencast/opencast/pull/3127)] -
  Fix minor bugs in AdaptivePlaylist
- [[#3176](https://github.com/opencast/opencast/pull/3176)] -
  Save Buttons for Metadata
- [[#3169](https://github.com/opencast/opencast/pull/3169)] -
  Add spa style redirect if resource does not exist
- [[#3168](https://github.com/opencast/opencast/pull/3168)] -
  Fix content-type header of ingest endpoint
- [[#3166](https://github.com/opencast/opencast/pull/3166)] -
  fix remote component typo name
- [[#3158](https://github.com/opencast/opencast/pull/3158)] -
  Fix mime type parsing for publish-oaipmh operation
- [[#3157](https://github.com/opencast/opencast/pull/3157)] -
  Always update groups
- [[#3153](https://github.com/opencast/opencast/pull/3153)] -
  Always ouput execute-\* process logs
- [[#3151](https://github.com/opencast/opencast/pull/3151)] -
  Add more mimetypes
- [[#3150](https://github.com/opencast/opencast/pull/3150)] -
  Allow asset upload of tracks
- [[#3149](https://github.com/opencast/opencast/pull/3149)] -
  Use original file extension in the asset manger
- [[#3148](https://github.com/opencast/opencast/pull/3148)] -
  Allow empty track duration
- [[#3145](https://github.com/opencast/opencast/pull/3145)] -
  Typos & Style
- [[#3144](https://github.com/opencast/opencast/pull/3144)] -
  Remove com.springsource.org.cyberneko.html
- [[#3134](https://github.com/opencast/opencast/pull/3134)] -
  Remove Apache Mina
- [[#3132](https://github.com/opencast/opencast/pull/3132)] -
  Downgrade to latest xml-apis version from 2.0.2 to 1.4.01
- [[#3131](https://github.com/opencast/opencast/pull/3131)] -
  Update osgi compendium and osgi core from 5.0.0 to 6.0.0
- [[#3129](https://github.com/opencast/opencast/pull/3129)] -
  Extract image from source
- [[#3126](https://github.com/opencast/opencast/pull/3126)] -
  Add config for service error states to service registry
- [[#3125](https://github.com/opencast/opencast/pull/3125)] -
  Document impact of ROLE_CAPTURE_AGENT
- [[#3119](https://github.com/opencast/opencast/pull/3119)] -
  Exclude administrators from artificial limit in `SolrRequester`
- [[#3106](https://github.com/opencast/opencast/pull/3106)] -
  Added back encoding profiles removed by previous commit
- [[#3104](https://github.com/opencast/opencast/pull/3104)] -
  Remove Deprecated Methods
- [[#3103](https://github.com/opencast/opencast/pull/3103)] -
  Release Note Updates
- [[#3092](https://github.com/opencast/opencast/pull/3092)] -
  Workflow Configuration Margin
- [[#3091](https://github.com/opencast/opencast/pull/3091)] -
  Drop Unused Logger Configuration
- [[#3090](https://github.com/opencast/opencast/pull/3090)] -
  Wowza stream security  "Prefix:Secret" configuration
- [[#3089](https://github.com/opencast/opencast/pull/3089)] -
  Drop default dispatch interval to 2 seconds
- [[#3088](https://github.com/opencast/opencast/pull/3088)] -
  Remove more of the Entwine FN Library
- [[#3087](https://github.com/opencast/opencast/pull/3087)] -
  Fix “Loading” Message in Engage UI
- [[#3086](https://github.com/opencast/opencast/pull/3086)] -
  Fix Episode Display in Engage UI
- [[#3076](https://github.com/opencast/opencast/pull/3076)] -
  Store modification & deletion dates for series and add range-lookup method to `SeriesService`
- [[#3075](https://github.com/opencast/opencast/pull/3075)] -
  Expose ACLs in search service (via `SearchResultItem`)
- [[#3066](https://github.com/opencast/opencast/pull/3066)] -
  Fix LTI Tool Build
- [[#3061](https://github.com/opencast/opencast/pull/3061)] -
  Use engage plugin name in URL to prevent random ID changes
- [[#3050](https://github.com/opencast/opencast/pull/3050)] -
  Fixed limit value returned by search service endpoint
- [[#3049](https://github.com/opencast/opencast/pull/3049)] -
  Removed ManagedService from Admin UI backend
- [[#3031](https://github.com/opencast/opencast/pull/3031)] -
  Remove staticweave plugin
- [[#3025](https://github.com/opencast/opencast/pull/3025)] -
  Replace “Click Here” Links on Documentation Landing Page
- [[#3014](https://github.com/opencast/opencast/pull/3014)] -
  Spellcheck webinar section in docs landing page
- [[#3012](https://github.com/opencast/opencast/pull/3012)] -
  Add passed proposal from 24 feb 2021
- [[#3011](https://github.com/opencast/opencast/pull/3011)] -
  Add Learn section and webinars list
- [[#3010](https://github.com/opencast/opencast/pull/3010)] -
  Don't update events in index twice when changing series metadata
- [[#3005](https://github.com/opencast/opencast/pull/3005)] -
  Set external API version v1.7.0 as default
- [[#3002](https://github.com/opencast/opencast/pull/3002)] -
  Sign publication URL of events in External API
- [[#2979](https://github.com/opencast/opencast/pull/2979)] -
  Update OC 11 release schedule
- [[#2976](https://github.com/opencast/opencast/pull/2976)] -
  Prepare release notes
- [[#2962](https://github.com/opencast/opencast/pull/2962)] -
  Update Editor Profile
- [[#2954](https://github.com/opencast/opencast/pull/2954)] -
  Fix episodeFromSeries plugin show "&nbsp;" while presenter is empty
- [[#2953](https://github.com/opencast/opencast/pull/2953)] -
  Add I18n support for presenter name label in episodeFromSeries plugin
- [[#2952](https://github.com/opencast/opencast/pull/2952)] -
  Link Crowdin Project List
- [[#2950](https://github.com/opencast/opencast/pull/2950)] -
  Check whether streamingDistributionService is set before invoking it
- [[#2942](https://github.com/opencast/opencast/pull/2942)] -
  Update Nginx example regarding proxy_cookie_path
- [[#2940](https://github.com/opencast/opencast/pull/2940)] -
  Document Opencast 11 RPM Installation
- [[#2938](https://github.com/opencast/opencast/pull/2938)] -
  Update Debian support in OC 11
- [[#2936](https://github.com/opencast/opencast/pull/2936)] -
  Cleanup S3 code
- [[#2934](https://github.com/opencast/opencast/pull/2934)] -
  Update LTI Dependencies Monthly
- [[#2925](https://github.com/opencast/opencast/pull/2925)] -
  Test only with Firefox or Chrome
- [[#2924](https://github.com/opencast/opencast/pull/2924)] -
  Simplify getUserIdRole
- [[#2910](https://github.com/opencast/opencast/pull/2910)] -
  Document Committers
- [[#2900](https://github.com/opencast/opencast/pull/2900)] -
  docs/developer: corrected java version 8>11 as mentioned here: https:…
- [[#2899](https://github.com/opencast/opencast/pull/2899)] -
  Hello World Workflow Operation
- [[#2878](https://github.com/opencast/opencast/pull/2878)] -
  Add endpoint to resume Index Rebuild for specified service
- [[#2877](https://github.com/opencast/opencast/pull/2877)] -
  Only run Github database test for sql scripts
- [[#2875](https://github.com/opencast/opencast/pull/2875)] -
  Adding proposal from June
- [[#2872](https://github.com/opencast/opencast/pull/2872)] -
  Minor Improvements to Cleanup Operation
- [[#2871](https://github.com/opencast/opencast/pull/2871)] -
  No Manual Job Sorting
- [[#2862](https://github.com/opencast/opencast/pull/2862)] -
  Updating Release Manager responsibilities
- [[#2861](https://github.com/opencast/opencast/pull/2861)] -
  Adding developer tips gathered in the technical meeting
- [[#2860](https://github.com/opencast/opencast/pull/2860)] -
  UI Configuration Service Configuration
- [[#2857](https://github.com/opencast/opencast/pull/2857)] -
  One Elasticsearch index to rule them all
- [[#2856](https://github.com/opencast/opencast/pull/2856)] -
  Server Job Statistics
- [[#2834](https://github.com/opencast/opencast/pull/2834)] -
  Remove Security-related Workaround in AssetManager
- [[#2826](https://github.com/opencast/opencast/pull/2826)] -
  Update Deprecated Code
- [[#2814](https://github.com/opencast/opencast/pull/2814)] -
  Add track fields `is_master_playlist` and `is_live` to external API
- [[#2806](https://github.com/opencast/opencast/pull/2806)] -
  Fix Landing Page
- [[#2803](https://github.com/opencast/opencast/pull/2803)] -
  Fix Landing Page
- [[#2785](https://github.com/opencast/opencast/pull/2785)] -
  Added Arne Wilken as a developer to the main pom file
- [[#2775](https://github.com/opencast/opencast/pull/2775)] -
  Fix random error with Maven dependency download in GitHub Actions
- [[#2774](https://github.com/opencast/opencast/pull/2774)] -
  Fix removal of remote assets
- [[#2773](https://github.com/opencast/opencast/pull/2773)] -
  Don't force inclusion of referred elements in snapshot
- [[#2767](https://github.com/opencast/opencast/pull/2767)] -
  Fix Upgrade Documentation
- [[#2750](https://github.com/opencast/opencast/pull/2750)] -
  JavaScript deployment on docs.opencast.org
- [[#2749](https://github.com/opencast/opencast/pull/2749)] -
  Use HTTPS in Documentation Landing Page
- [[#2744](https://github.com/opencast/opencast/pull/2744)] -
  Changed asset mime type  length from 64 to 255
- [[#2735](https://github.com/opencast/opencast/pull/2735)] -
  Clarifies debug options in setenv file
- [[#2716](https://github.com/opencast/opencast/pull/2716)] -
  Fix URL to the Security Issue Process
- [[#2712](https://github.com/opencast/opencast/pull/2712)] -
  fix URL to the development process documentation
- [[#2705](https://github.com/opencast/opencast/pull/2705)] -
  Remove OAI-PMH Harvester
- [[#2679](https://github.com/opencast/opencast/pull/2679)] -
  Update some documentation URLs from latest to develop
- [[#2674](https://github.com/opencast/opencast/pull/2674)] -
  Fix `metrics-exporter` Prometheus dependency
- [[#2670](https://github.com/opencast/opencast/pull/2670)] -
  Update Responsibilities of a Committer
- [[#2658](https://github.com/opencast/opencast/pull/2658)] -
  Document that hashes are worked with
- [[#2655](https://github.com/opencast/opencast/pull/2655)] -
  Document encode WOH's source-flavors config key
- [[#2616](https://github.com/opencast/opencast/pull/2616)] -
  Document new ActiveMQ connection requirements
- [[#2603](https://github.com/opencast/opencast/pull/2603)] -
  Update Node.js
- [[#2596](https://github.com/opencast/opencast/pull/2596)] -
  New  woh: select-version
- [[#2573](https://github.com/opencast/opencast/pull/2573)] -
  Refactoring workflows and encoding profiles
- [[#2560](https://github.com/opencast/opencast/pull/2560)] -
  Add additional s3 operations
- [[#2553](https://github.com/opencast/opencast/pull/2553)] -
  Automatic cleaning of working file repository
- [[#2546](https://github.com/opencast/opencast/pull/2546)] -
  Default Visibility in Admin Interface
- [[#2534](https://github.com/opencast/opencast/pull/2534)] -
  Sanitize xml input in admin frontend
- [[#2513](https://github.com/opencast/opencast/pull/2513)] -
  LTI Tool Updates
- [[#1227](https://github.com/opencast/opencast/pull/1227)] -
  Delete option of series for the SearchService
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/3180">3180</a>] -
  Bump karma from 6.3.8 to 6.3.9 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3175">3175</a>] -
  Bump bower from 1.8.12 to 1.8.13 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3171">3171</a>] -
  Bump html-validate from 6.1.1 to 6.1.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3170">3170</a>] -
  Bump http-errors from 1.8.0 to 1.8.1 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3163">3163</a>] -
  Bump html-validate from 6.1.0 to 6.1.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3143">3143</a>] -
  Bump rest-assured to 4.4.0</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3141">3141</a>] -
  Bump eslint from 8.1.0 to 8.2.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3140">3140</a>] -
  Bump eslint from 8.1.0 to 8.2.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3139">3139</a>] -
  Bump eslint from 8.1.0 to 8.2.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3138">3138</a>] -
  Bump eslint from 8.1.0 to 8.2.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3137">3137</a>] -
  Bump eslint from 8.1.0 to 8.2.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3136">3136</a>] -
  Bump karma from 6.3.7 to 6.3.8 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3135">3135</a>] -
  Bump eslint from 8.1.0 to 8.2.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3124">3124</a>] -
  Bump karma-firefox-launcher from 2.1.1 to 2.1.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3123">3123</a>] -
  Bump karma from 6.3.6 to 6.3.7 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3121">3121</a>] -
  Bump i18next from 21.2.0 to 21.3.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3120">3120</a>] -
  Bump @types/react-dom from 17.0.9 to 17.0.10 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3117">3117</a>] -
  Bump @types/node from 16.10.2 to 16.11.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3116">3116</a>] -
  Bump react-i18next from 11.12.0 to 11.13.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3115">3115</a>] -
  Bump react-bootstrap from 1.6.4 to 2.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3114">3114</a>] -
  Bump @fortawesome/react-fontawesome from 0.1.15 to 0.1.16 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3113">3113</a>] -
  Bump typescript from 4.4.3 to 4.4.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3112">3112</a>] -
  Bump @types/react from 17.0.26 to 17.0.33 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3110">3110</a>] -
  Bump bootstrap from 5.1.1 to 5.1.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3109">3109</a>] -
  Bump @types/react-helmet from 6.1.2 to 6.1.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3108">3108</a>] -
  Bump axios from 0.22.0 to 0.24.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3102">3102</a>] -
  Bump karma from 6.3.5 to 6.3.6 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3101">3101</a>] -
  Bump chromedriver from 94.0.0 to 95.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3099">3099</a>] -
  Bump eslint from 8.0.1 to 8.1.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3098">3098</a>] -
  Bump eslint from 8.0.1 to 8.1.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3097">3097</a>] -
  Bump eslint from 8.0.1 to 8.1.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3096">3096</a>] -
  Bump eslint from 8.0.1 to 8.1.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3095">3095</a>] -
  Bump jasmine-core from 3.10.0 to 3.10.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3094">3094</a>] -
  Bump eslint from 8.0.1 to 8.1.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3093">3093</a>] -
  Bump eslint from 8.0.1 to 8.1.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3085">3085</a>] -
  Bump karma from 6.3.4 to 6.3.5 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3073">3073</a>] -
  Bump eslint from 8.0.0 to 8.0.1 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3072">3072</a>] -
  Bump eslint from 8.0.0 to 8.0.1 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3071">3071</a>] -
  Bump eslint from 8.0.0 to 8.0.1 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3070">3070</a>] -
  Bump eslint from 8.0.0 to 8.0.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3069">3069</a>] -
  Bump jasmine-core from 3.9.0 to 3.10.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3068">3068</a>] -
  Bump eslint from 8.0.0 to 8.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3067">3067</a>] -
  Bump eslint from 8.0.0 to 8.0.1 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3057">3057</a>] -
  Bump eslint from 7.32.0 to 8.0.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3056">3056</a>] -
  Bump eslint from 7.32.0 to 8.0.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3055">3055</a>] -
  Bump eslint from 7.32.0 to 8.0.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3054">3054</a>] -
  Bump eslint from 7.32.0 to 8.0.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3053">3053</a>] -
  Bump eslint from 7.32.0 to 8.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3052">3052</a>] -
  Bump eslint from 7.32.0 to 8.0.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3048">3048</a>] -
  Bump grunt-contrib-concat from 1.0.1 to 2.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3047">3047</a>] -
  Bump autoprefixer from 9.8.7 to 9.8.8 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3046">3046</a>] -
  Bump coffeescript from 2.6.0 to 2.6.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3045">3045</a>] -
  Bump markdownlint-cli from 0.28.1 to 0.29.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3043">3043</a>] -
  Bump grunt-contrib-jshint from 3.0.0 to 3.1.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3042">3042</a>] -
  Bump html-validate from 6.0.2 to 6.1.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3039">3039</a>] -
  Bump @types/jest from 27.0.1 to 27.0.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3038">3038</a>] -
  Bump axios from 0.21.2 to 0.22.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3037">3037</a>] -
  Bump i18next from 21.1.1 to 21.2.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3036">3036</a>] -
  Bump react-bootstrap from 1.6.3 to 1.6.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3033">3033</a>] -
  Bump @types/node from 16.10.1 to 16.10.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3032">3032</a>] -
  Bump @types/react from 17.0.20 to 17.0.26 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3030">3030</a>] -
  Bump @types/node from 16.7.6 to 16.10.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3029">3029</a>] -
  Bump i18next from 20.4.0 to 21.1.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3027">3027</a>] -
  Bump html-validate from 5.4.1 to 6.0.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3026">3026</a>] -
  Bump axios from 0.21.1 to 0.21.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3020">3020</a>] -
  Bump chromedriver from 92.0.2 to 94.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3019">3019</a>] -
  Bump autoprefixer from 9.8.6 to 9.8.7 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3018">3018</a>] -
  Bump grunt-html-validate from 1.0.1 to 1.0.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3009">3009</a>] -
  Bump xmlsec from 2.1.4 to 2.1.7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3008">3008</a>] -
  Bump coffeescript from 2.5.1 to 2.6.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3001">3001</a>] -
  Bump bootstrap from 5.1.0 to 5.1.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3000">3000</a>] -
  Bump react-bootstrap from 1.6.1 to 1.6.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2999">2999</a>] -
  Bump react-i18next from 11.11.4 to 11.12.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2996">2996</a>] -
  Bump typescript from 4.4.2 to 4.4.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2988">2988</a>] -
  Bump @types/react from 17.0.19 to 17.0.20 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2978">2978</a>] -
  Bump tar from 6.1.4 to 6.1.11 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2977">2977</a>] -
  Bump chromedriver from 92.0.1 to 92.0.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2975">2975</a>] -
  Bump prometheus.version from 0.11.0 to 0.12.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2974">2974</a>] -
  Bump typescript from 4.3.5 to 4.4.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2973">2973</a>] -
  Bump @types/node from 16.7.1 to 16.7.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2972">2972</a>] -
  Bump html-validate from 5.4.0 to 5.4.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2965">2965</a>] -
  Bump html-validate from 5.3.0 to 5.4.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2961">2961</a>] -
  Bump html-validate from 5.2.1 to 5.3.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2959">2959</a>] -
  Bump @types/react from 17.0.18 to 17.0.19 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2958">2958</a>] -
  Bump @types/node from 16.6.1 to 16.7.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2957">2957</a>] -
  Bump jasmine-core from 3.8.0 to 3.9.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2933">2933</a>] -
  Bump @types/react from 17.0.16 to 17.0.18 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2932">2932</a>] -
  Bump i18next from 20.3.5 to 20.4.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2931">2931</a>] -
  Bump @types/jest from 26.0.24 to 27.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2930">2930</a>] -
  Bump @types/node from 16.4.13 to 16.6.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2921">2921</a>] -
  Bump path-parse from 1.0.6 to 1.0.7 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2916">2916</a>] -
  Bump html-validate from 5.2.0 to 5.2.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2909">2909</a>] -
  Bump @types/react from 17.0.15 to 17.0.16 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2908">2908</a>] -
  Bump @types/node from 16.4.12 to 16.4.13 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2905">2905</a>] -
  Bump @fortawesome/react-fontawesome from 0.1.14 to 0.1.15 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2904">2904</a>] -
  Bump @fortawesome/free-solid-svg-icons from 5.15.3 to 5.15.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2903">2903</a>] -
  Bump bootstrap from 5.0.2 to 5.1.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2902">2902</a>] -
  Bump @types/node from 16.4.7 to 16.4.12 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2901">2901</a>] -
  Bump @fortawesome/fontawesome-svg-core from 1.2.35 to 1.2.36 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2898">2898</a>] -
  Bump tar from 6.1.0 to 6.1.4 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2895">2895</a>] -
  Bump chromedriver from 92.0.0 to 92.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2890">2890</a>] -
  Bump eslint from 7.31.0 to 7.32.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2889">2889</a>] -
  Bump eslint from 7.31.0 to 7.32.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2888">2888</a>] -
  Bump eslint from 7.31.0 to 7.32.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2887">2887</a>] -
  Bump eslint from 7.31.0 to 7.32.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2885">2885</a>] -
  Bump eslint from 7.31.0 to 7.32.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2884">2884</a>] -
  Bump eslint from 7.31.0 to 7.32.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2869">2869</a>] -
  Bump @types/react from 17.0.14 to 17.0.15 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2868">2868</a>] -
  Bump @types/node from 16.3.3 to 16.4.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2867">2867</a>] -
  Bump i18next from 20.3.3 to 20.3.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2866">2866</a>] -
  Bump html-validate from 5.1.1 to 5.2.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2865">2865</a>] -
  Bump chromedriver from 91.0.1 to 92.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2864">2864</a>] -
  Bump url-parse from 1.5.1 to 1.5.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2859">2859</a>] -
  Bump markdownlint-cli from 0.27.1 to 0.28.1 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2845">2845</a>] -
  Bump react-i18next from 11.11.0 to 11.11.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2844">2844</a>] -
  Bump eslint from 7.30.0 to 7.31.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2843">2843</a>] -
  Bump eslint from 7.30.0 to 7.31.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2842">2842</a>] -
  Bump eslint from 7.30.0 to 7.31.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2841">2841</a>] -
  Bump eslint from 7.30.0 to 7.31.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2840">2840</a>] -
  Bump @types/node from 16.0.0 to 16.3.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2839">2839</a>] -
  Bump i18next from 20.3.2 to 20.3.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2838">2838</a>] -
  Bump eslint from 7.30.0 to 7.31.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2837">2837</a>] -
  Bump eslint from 7.30.0 to 7.31.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2825">2825</a>] -
  Bump @types/react-dom from 17.0.8 to 17.0.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2824">2824</a>] -
  Bump @types/jest from 26.0.23 to 26.0.24 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2823">2823</a>] -
  Bump @types/react-select from 4.0.16 to 4.0.17 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2821">2821</a>] -
  Bump @types/react-js-pagination from 3.0.3 to 3.0.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2820">2820</a>] -
  Bump @types/react-helmet from 6.1.1 to 6.1.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2818">2818</a>] -
  Bump @types/react from 17.0.13 to 17.0.14 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2817">2817</a>] -
  Bump html-validate from 5.0.2 to 5.1.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2808">2808</a>] -
  Bump html-validate from 4.14.0 to 5.0.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2802">2802</a>] -
  Bump @types/node from 15.12.5 to 16.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2801">2801</a>] -
  Bump eslint from 7.29.0 to 7.30.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2800">2800</a>] -
  Bump eslint from 7.29.0 to 7.30.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2799">2799</a>] -
  Bump eslint from 7.29.0 to 7.30.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2798">2798</a>] -
  Bump eslint from 7.29.0 to 7.30.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2797">2797</a>] -
  Bump @types/react from 17.0.11 to 17.0.13 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2796">2796</a>] -
  Bump eslint from 7.29.0 to 7.30.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2795">2795</a>] -
  Bump grunt-html-validate from 1.0.0 to 1.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2794">2794</a>] -
  Bump eslint from 7.29.0 to 7.30.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2789">2789</a>] -
  Bump jasmine-core from 3.7.1 to 3.8.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2783">2783</a>] -
  Bump @types/react-select from 4.0.15 to 4.0.16 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2782">2782</a>] -
  Bump @types/node from 15.12.4 to 15.12.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2781">2781</a>] -
  Bump query-string from 7.0.0 to 7.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2780">2780</a>] -
  Bump bootstrap from 5.0.1 to 5.0.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2779">2779</a>] -
  Bump grunt-html-validate from 0.5.0 to 1.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2772">2772</a>] -
  Bump node-sass from 6.0.0 to 6.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2760">2760</a>] -
  Bump i18next from 20.3.1 to 20.3.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2759">2759</a>] -
  Bump @types/react-dom from 17.0.7 to 17.0.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2758">2758</a>] -
  Bump @types/node from 15.12.2 to 15.12.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2757">2757</a>] -
  Bump eslint from 7.28.0 to 7.29.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2756">2756</a>] -
  Bump eslint from 7.28.0 to 7.29.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2755">2755</a>] -
  Bump eslint from 7.28.0 to 7.29.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2754">2754</a>] -
  Bump eslint from 7.28.0 to 7.29.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2753">2753</a>] -
  Bump i18next-browser-languagedetector from 6.1.1 to 6.1.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2752">2752</a>] -
  Bump eslint from 7.28.0 to 7.29.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2751">2751</a>] -
  Bump eslint from 7.28.0 to 7.29.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2737">2737</a>] -
  Bump chromedriver from 91.0.0 to 91.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2729">2729</a>] -
  Bump @types/react-dom from 17.0.6 to 17.0.7 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2728">2728</a>] -
  Bump @types/node from 15.12.1 to 15.12.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2727">2727</a>] -
  Bump react-i18next from 11.10.0 to 11.11.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2726">2726</a>] -
  Bump @types/react from 17.0.9 to 17.0.11 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2725">2725</a>] -
  Bump karma from 6.3.3 to 6.3.4 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2693">2693</a>] -
  Bump glob-parent from 5.1.0 to 5.1.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2689">2689</a>] -
  Bump eslint from 7.27.0 to 7.28.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2688">2688</a>] -
  Bump eslint from 7.27.0 to 7.28.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2687">2687</a>] -
  Bump eslint from 7.27.0 to 7.28.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2686">2686</a>] -
  Bump eslint from 7.27.0 to 7.28.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2685">2685</a>] -
  Bump @types/node from 15.6.1 to 15.12.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2684">2684</a>] -
  Bump @types/react-dom from 17.0.5 to 17.0.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2683">2683</a>] -
  Bump @types/react from 17.0.8 to 17.0.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2682">2682</a>] -
  Bump react-bootstrap from 1.6.0 to 1.6.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2681">2681</a>] -
  Bump eslint from 7.27.0 to 7.28.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2680">2680</a>] -
  Bump eslint from 7.27.0 to 7.28.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2672">2672</a>] -
  Bump karma-firefox-launcher from 2.1.0 to 2.1.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2671">2671</a>] -
  Bump chromedriver from 90.0.1 to 91.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2668">2668</a>] -
  Bump karma from 6.3.2 to 6.3.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2663">2663</a>] -
  Bump prometheus.version from 0.10.0 to 0.11.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2662">2662</a>] -
  Bump react-i18next from 11.8.15 to 11.10.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2661">2661</a>] -
  Bump @types/node from 15.6.0 to 15.6.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2660">2660</a>] -
  Bump @types/react from 17.0.6 to 17.0.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2659">2659</a>] -
  Bump i18next from 20.3.0 to 20.3.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2642">2642</a>] -
  Bump grunt-cli from 1.4.2 to 1.4.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2641">2641</a>] -
  Bump grunt from 1.4.0 to 1.4.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2640">2640</a>] -
  Bump browserslist from 4.13.0 to 4.16.6 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2639">2639</a>] -
  Bump eslint from 7.26.0 to 7.27.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2638">2638</a>] -
  Bump eslint from 7.26.0 to 7.27.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2637">2637</a>] -
  Bump eslint from 7.26.0 to 7.27.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2636">2636</a>] -
  Bump eslint from 7.26.0 to 7.27.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2635">2635</a>] -
  Bump @types/node from 15.3.0 to 15.6.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2634">2634</a>] -
  Bump @types/react from 17.0.5 to 17.0.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2633">2633</a>] -
  Bump i18next from 20.2.4 to 20.3.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2632">2632</a>] -
  Bump eslint from 7.26.0 to 7.27.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2631">2631</a>] -
  Bump eslint from 7.26.0 to 7.27.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2628">2628</a>] -
  Bump chromedriver from 90.0.0 to 90.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2576">2576</a>] -
  Bump hosted-git-info from 2.8.4 to 2.8.9 in /modules/engage-paella-player</li>
</ul>
</details>