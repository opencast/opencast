Opencast 9 Changelog
--------------------


## Opencast 9.12

*Released on December 18th, 2021*

- [[#3280](https://github.com/opencast/opencast/pull/3280)]
  Security: Update to Pax Logging 1.11.12 (9.x)


## Opencast 9.11

*Released on December 17th, 2021*

- [[#3277](https://github.com/opencast/opencast/pull/3277)]
  Security: Fix Files Accessible to External Parties
- [[#3276](https://github.com/opencast/opencast/pull/3276)]
  Security: Update to Pax Logging 1.11.11 (9.x)


## Opencast 9.10

*Released on December 13th, 2021*

- [[GHSA-mf4f-j588-5xm8](https://github.com/opencast/opencast/security/advisories/GHSA-mf4f-j588-5xm8)] -
  Apache Log4j Remote Code Execution
- [[GHSA-j4mm-7pj3-jf7v](https://github.com/opencast/opencast/security/advisories/GHSA-j4mm-7pj3-jf7v)} -
  HTTP Method Spoofing
- [[#3080](https://github.com/opencast/opencast/pull/3080)] -
  Backport fixes: chrome tests and Safari fix


## Opencast 9.9

*Released on October 12th, 2021*

- [[#3041](https://github.com/opencast/opencast/pull/3041)] -
  Point out memory limits
- [[#2992](https://github.com/opencast/opencast/pull/2992)] -
  Fix create new event in admin UI when multiple extended catalogs are used
- [[#2951](https://github.com/opencast/opencast/pull/2951)] -
  Add Missing Translations Files

## Opencast 9.8

*Released on August 18th, 2021*

- [[#2926](https://github.com/opencast/opencast/pull/2926)] -
  Add exception for extron smp351 technical catalog
- [[#2918](https://github.com/opencast/opencast/pull/2918)] -
  Add CMAF mime types
- [[#2854](https://github.com/opencast/opencast/pull/2854)] -
  Fixed publication pop-overs overlaying each other
- [[#2846](https://github.com/opencast/opencast/pull/2846)] -
  Some bug fixes to IBM Watson transcription service
- [[#2739](https://github.com/opencast/opencast/pull/2739)] -
  Prevent search service endpoint from querying series service
- [[#2650](https://github.com/opencast/opencast/pull/2650)] -
  sort the options alphabeticaly, this makes the languages field etc more readable

## Opencast 9.7

*Released on July 15th, 2021*

- [[#2793](https://github.com/opencast/opencast/pull/2793)] -
  More HTML validation for AdminUI
- [[#2790](https://github.com/opencast/opencast/pull/2790)] -
  Fix file permissions of start-opencast
- [[#2788](https://github.com/opencast/opencast/pull/2788)] -
  Introduce Allinone Profile
- [[#2778](https://github.com/opencast/opencast/pull/2778)] -
  Cut Marks Attachments
- [[#2771](https://github.com/opencast/opencast/pull/2771)] -
  Documentation Deployment Conflicts
- [[#2770](https://github.com/opencast/opencast/pull/2770)] -
  Link Video in Installation Guide
- [[#2736](https://github.com/opencast/opencast/pull/2736)] -
  Fix processing of `fast` events
- [[#2723](https://github.com/opencast/opencast/pull/2723)] -
  Limit Ingest Filename Length
- [[#2722](https://github.com/opencast/opencast/pull/2722)] -
  Default for Access Control Entry Allow
- [[#2719](https://github.com/opencast/opencast/pull/2719)] -
  Closing tags for non-void elements in Admin UI
- [[#2717](https://github.com/opencast/opencast/pull/2717)] -
  Make Series Endpoint Accept Metadata Fields
- [[#2714](https://github.com/opencast/opencast/pull/2714)] -
  Fix pagination in engage-ui
- [[#2710](https://github.com/opencast/opencast/pull/2710)] -
  Recognize more input types in WF configuration
- [[#2678](https://github.com/opencast/opencast/pull/2678)] -
  OAI-PMH Sets
- [[#2543](https://github.com/opencast/opencast/pull/2543)] -
  Exclude user provider configuration for contributors list provider
- [[#2535](https://github.com/opencast/opencast/pull/2535)] -
  Prevent Ingests with Illegal Data

## Opencast 9.6

*Released on June 15th, 2021*

- [[#2734](https://github.com/opencast/opencast/pull/2734)] -
  Prepare AV fix for fast workflow: add textual warning to docs
- [[#2719](https://github.com/opencast/opencast/pull/2719)] -
  Closing tags for non-void elements in Admin UI
- [[#2718](https://github.com/opencast/opencast/pull/2718)] -
  Closing tags for consecutive select elements
- [[#2715](https://github.com/opencast/opencast/pull/2715)] -
  Update pull request template's URL
- [[#2714](https://github.com/opencast/opencast/pull/2714)] -
  Fix pagination in engage-ui
- [[#2702](https://github.com/opencast/opencast/pull/2702)] -
  Update Elasticsearch adopter documentation
- [[#2697](https://github.com/opencast/opencast/pull/2697)] -
  Admin UI theme wizard fixed (fixes #2460)
- [[#2696](https://github.com/opencast/opencast/pull/2696)] -
  Fix Media Package Series ACL Update
- [[#2695](https://github.com/opencast/opencast/pull/2695)] -
  Fixes removing a series from an event
- [[#2692](https://github.com/opencast/opencast/pull/2692)] -
  Admin UI editor segment list item delete button position fixed
- [[#2676](https://github.com/opencast/opencast/pull/2676)] -
  Event status will not change after removing the workflow
- [[#2675](https://github.com/opencast/opencast/pull/2675)] -
  Add workflow state for standalone editor
- [[#2665](https://github.com/opencast/opencast/pull/2665)] -
  Removing references to registering on pkg.opencast.org since this is no longer required
- [[#2656](https://github.com/opencast/opencast/pull/2656)] -
  Require EDIT role for editing metadata in Admin UI
- [[#2654](https://github.com/opencast/opencast/pull/2654)] -
  Update examples in publish-configure WOH's docs
- [[#2653](https://github.com/opencast/opencast/pull/2653)] -
  Meta publication handling by publish-configure WOH
- [[#2652](https://github.com/opencast/opencast/pull/2652)] -
  change translation for the video file upload from the lti tool
- [[#2651](https://github.com/opencast/opencast/pull/2651)] -
  fix language for the captions upload, als dfxp is supported now
- [[#2646](https://github.com/opencast/opencast/pull/2646)] -
  Wording error in release notes regarding Amberscript transcriptions?
- [[#2630](https://github.com/opencast/opencast/pull/2630)] -
  Update Adopter Registration
- [[#2629](https://github.com/opencast/opencast/pull/2629)] -
  Add missing new line in Elasticsearch admin docs
- [[#2626](https://github.com/opencast/opencast/pull/2626)] -
  Update new editor to release 2021-05-20
- [[#2620](https://github.com/opencast/opencast/pull/2620)] -
  Replacing remaining Freenode references with Matrix
- [[#2617](https://github.com/opencast/opencast/pull/2617)] -
  Estimate number of frames if not declared in file
- [[#2615](https://github.com/opencast/opencast/pull/2615)] -
  Ignore not found exception when automatically archiving to another storage
- [[#2614](https://github.com/opencast/opencast/pull/2614)] -
  Fix variable always resolving to the default value even when set
- [[#2604](https://github.com/opencast/opencast/pull/2604)] -
  Fix kernel test with running Opencast
- [[#2594](https://github.com/opencast/opencast/pull/2594)] -
  Series list provider should use admin UI index
- [[#2574](https://github.com/opencast/opencast/pull/2574)] -
  Silence detection should create media duration properties
- [[#2571](https://github.com/opencast/opencast/pull/2571)] -
  Fixed display error for the start date filter in the Admin UI
- [[#2568](https://github.com/opencast/opencast/pull/2568)] -
  Temporarily Ignore Failing Test
- [[#2566](https://github.com/opencast/opencast/pull/2566)] -
  Add support for basic authentication with Elasticsearch
- [[#2563](https://github.com/opencast/opencast/pull/2563)] -
  AmberScript WOH documentation updated
- [[#2562](https://github.com/opencast/opencast/pull/2562)] -
  Add "iFrame Resizer" library to LTI tools
- [[#2490](https://github.com/opencast/opencast/pull/2490)] -
  Multiple Creators in Series LTI Tool
- [[#2489](https://github.com/opencast/opencast/pull/2489)] -
  Attachment is not a function LTI error fixed

## Opencast 9.5

*Released on May 17th, 2021*

- [[#2602](https://github.com/opencast/opencast/pull/2602)] -
  Fix Graphs in Documentation
- [[#2575](https://github.com/opencast/opencast/pull/2575)] -
  Fixing unchecked directory list() call
- [[#2565](https://github.com/opencast/opencast/pull/2565)] -
  Organization Fallback for UI Configuration
- [[#2544](https://github.com/opencast/opencast/pull/2544)] -
  Remove unused `org.opencastproject.export.distribution.ExportUi.cfg`
- [[#2536](https://github.com/opencast/opencast/pull/2536)] -
  Tesseract Option Documentation
- [[#2530](https://github.com/opencast/opencast/pull/2530)] -
  macOS installation update
- [[#2526](https://github.com/opencast/opencast/pull/2526)] -
  LTI Service Docs
- [[#2525](https://github.com/opencast/opencast/pull/2525)] -
  Fix checkstyle violations in 11 modules
- [[#2516](https://github.com/opencast/opencast/pull/2516)] -
  Fix Default Password
- [[#2512](https://github.com/opencast/opencast/pull/2512)] -
  Add Build Date in User Interface
- [[#2502](https://github.com/opencast/opencast/pull/2502)] -
  Handle multiple creators in Paella player
- [[#2501](https://github.com/opencast/opencast/pull/2501)] -
  Handle multiple creators in Media Module
- [[#2493](https://github.com/opencast/opencast/pull/2493)] -
  Fixed "No response from service" for videogrid
- [[#2489](https://github.com/opencast/opencast/pull/2489)] -
  Attachment is not a function LTI error fixed
- [[#2435](https://github.com/opencast/opencast/pull/2435)] -
  Added Download Dropdown to Series LTI-Tools
- [[#2344](https://github.com/opencast/opencast/pull/2344)] -
  Auto-generate OAI-PMH database
- [[#2103](https://github.com/opencast/opencast/pull/2103)] -
  Only persist users with specific LTI role

## Opencast 9.4

*Released on April 19th, 2021*

- [[#2526](https://github.com/opencast/opencast/pull/2526)] -
  LTI Service Docs
- [[#2509](https://github.com/opencast/opencast/pull/2509)] -
  Fix checkstyle violations in 24 modules
- [[#2506](https://github.com/opencast/opencast/pull/2506)] -
  Fix checkstyle violation in 4 `search*` modules
- [[#2500](https://github.com/opencast/opencast/pull/2500)] -
  Make media package handle multi-byte Unicode characters
- [[#2497](https://github.com/opencast/opencast/pull/2497)] -
  Fixes getting the version information behind a proxy
- [[#2494](https://github.com/opencast/opencast/pull/2494)] -
  Fix Processing of Unicode Titles
- [[#2492](https://github.com/opencast/opencast/pull/2492)] -
  Prevent NPE if mediapackage duration is null
- [[#2479](https://github.com/opencast/opencast/pull/2479)] -
  Fix Memory Leak
- [[#2478](https://github.com/opencast/opencast/pull/2478)] -
  Fixed test for daylight saving time
- [[#2475](https://github.com/opencast/opencast/pull/2475)] -
  Enable Elasticsearch in docs
- [[#2473](https://github.com/opencast/opencast/pull/2473)] -
  Document Hardware Requirements
- [[#2472](https://github.com/opencast/opencast/pull/2472)] -
  Internal server error in workflow endpoint
- [[#2470](https://github.com/opencast/opencast/pull/2470)] -
  fixed admin UI - displaying roles correctly when adding a new event to a series
- [[#2467](https://github.com/opencast/opencast/pull/2467)] -
  Changed the content-type of the adopter POST request.
- [[#2464](https://github.com/opencast/opencast/pull/2464)] -
  Use a different ServiceType for the Standalone Video Editor on the presentation Node
- [[#2437](https://github.com/opencast/opencast/pull/2437)] -
  Updated new editor with new frontend-release 2021-03-24
- [[#2427](https://github.com/opencast/opencast/pull/2427)] -
  Revert "No Matrix Build on Opencast 8"
- [[#2424](https://github.com/opencast/opencast/pull/2424)] -
  Fix checkstyle violations in 22 modules
- [[#2423](https://github.com/opencast/opencast/pull/2423)] -
  Fix checkstyle violations for 3 `series-service*` modules
- [[#2420](https://github.com/opencast/opencast/pull/2420)] -
  Fix checkstyle violations in `lti` and `lti-service-impl`
- [[#2419](https://github.com/opencast/opencast/pull/2419)] -
  Fix checkstyle violations in 5 asset manager modules
- [[#2417](https://github.com/opencast/opencast/pull/2417)] -
  Correct Series ACLs when Recreating the Search Service Index
- [[#2414](https://github.com/opencast/opencast/pull/2414)] -
  Corrected configuration files in editor documentation
- [[#2413](https://github.com/opencast/opencast/pull/2413)] -
  Link new features
- [[#2411](https://github.com/opencast/opencast/pull/2411)] -
  Editor Documentation
- [[#2391](https://github.com/opencast/opencast/pull/2391)] -
  Perform `check-availibility` WF check with system user
- [[#2332](https://github.com/opencast/opencast/pull/2332)] -
  Fix resolution scaling by removing `force_original_aspect_ratio`
- [[#2318](https://github.com/opencast/opencast/pull/2318)] -
  Serverless HLS leaves files open
- [[#2298](https://github.com/opencast/opencast/pull/2298)] -
  Add infos about Wowza streaming configuration changes to upgrade guide
- [[#2112](https://github.com/opencast/opencast/pull/2112)] -
  Fix admin interface not displaying the correct role
- [[#2103](https://github.com/opencast/opencast/pull/2103)] -
  Only persist users with specific LTI role
- [[#1792](https://github.com/opencast/opencast/pull/1792)] -
  Standalone downloads Paella plugin
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/2443">2443</a>] -
  Bump guava from 24.1.1-jre to 30.1.1-jre</li>
</ul>
</details>

## Opencast 9.3

*Released on March 15th, 2021*

- [[#2395](https://github.com/opencast/opencast/pull/2395)] -
  Fix Hardcoded Dependency Version
- [[#2394](https://github.com/opencast/opencast/pull/2394)] -
  Editor Workflow Description
- [[#2373](https://github.com/opencast/opencast/pull/2373)] -
  Fix code formatting for aws s3 distribution
- [[#2368](https://github.com/opencast/opencast/pull/2368)] -
  Improve Email Workflow Operation
- [[#2361](https://github.com/opencast/opencast/pull/2361)] -
  Handle image extraction from very short videos
- [[#2355](https://github.com/opencast/opencast/pull/2355)] -
  Document and Test i18next in LTI Tools
- [[#2353](https://github.com/opencast/opencast/pull/2353)] -
  OAI-PMH Primary Key Length
- [[#2343](https://github.com/opencast/opencast/pull/2343)] -
  Fix LTI Tool Value Checks
- [[#2340](https://github.com/opencast/opencast/pull/2340)] -
  Fix checkstyle violations in 5 `distribution-*` modules
- [[#2338](https://github.com/opencast/opencast/pull/2338)] -
  Fix checkstyle violations in 5 `publications-service-*` modules
- [[#2335](https://github.com/opencast/opencast/pull/2335)] -
  Distinguish Between Documentation and Configuration Checks
- [[#2331](https://github.com/opencast/opencast/pull/2331)] -
  Simplify Conflict Check
- [[#2330](https://github.com/opencast/opencast/pull/2330)] -
  Fix Search Capability in Documentation
- [[#2329](https://github.com/opencast/opencast/pull/2329)] -
  Fixes Solr search failing when titles containing lots of upper case characters
- [[#2328](https://github.com/opencast/opencast/pull/2328)] -
  Document publish-configure changes from #1663 in upgrade guide
- [[#2316](https://github.com/opencast/opencast/pull/2316)] -
  Changed PartialImportWOH to ignore smil entries for tracks that don't exist
- [[#2301](https://github.com/opencast/opencast/pull/2301)] -
  Run Tests Only If Necessary
- [[#2296](https://github.com/opencast/opencast/pull/2296)] -
  Admin UI now shows a warning if it cannot reach Github
- [[#2277](https://github.com/opencast/opencast/pull/2277)] -
  Add Event Metrics
- [[#2263](https://github.com/opencast/opencast/pull/2263)] -
  Fix memory leak / performance in the LTI upload and job overview
- [[#2260](https://github.com/opencast/opencast/pull/2260)] -
  Stand-Alone Video Editor
- [[#2248](https://github.com/opencast/opencast/pull/2248)] -
  Selenium Tests for LTI Tools
- [[#2026](https://github.com/opencast/opencast/pull/2026)] -
  Fix Job Dispatching Test

## Opencast 9.2

*Released on February 15th, 2021*

- [[GHSA-vpc2-3wcv-qj4w#1](https://github.com/opencast/opencast-ghsa-vpc2-3wcv-qj4w/pull/1)] -
  Fix Engage Series Publication and Access
- [[#2309](https://github.com/opencast/opencast/pull/2309)] -
  HTTPS / Port
- [[#2300](https://github.com/opencast/opencast/pull/2300)] -
  Add Mermaid to Documentation
- [[#2299](https://github.com/opencast/opencast/pull/2299)] -
  Fix Total Search Results
- [[#2295](https://github.com/opencast/opencast/pull/2295)] -
  Limit Incident Text ID Text
- [[#2292](https://github.com/opencast/opencast/pull/2292)] -
  Fix Adopter Registration Configuration
- [[#2291](https://github.com/opencast/opencast/pull/2291)] -
  Shorten Adopter Registration Primary Key
- [[#2290](https://github.com/opencast/opencast/pull/2290)] -
  Fix Exception if GitHub is Unreachable
- [[#2281](https://github.com/opencast/opencast/pull/2281)] -
  Admin UI says current OC version unsupported when it can't reach GitHub
- [[#2275](https://github.com/opencast/opencast/pull/2275)] -
  Fixed possible typo in database grants statement
- [[#2274](https://github.com/opencast/opencast/pull/2274)] -
  Fix checkstyle violations for 7 modules
- [[#2273](https://github.com/opencast/opencast/pull/2273)] -
  Fix checkstyle violations for 9 modules
- [[#2270](https://github.com/opencast/opencast/pull/2270)] -
  Fix checkstyle violations for `authorization-manager`
- [[#2259](https://github.com/opencast/opencast/pull/2259)] -
  Enforce indentation checkstyle
- [[#2258](https://github.com/opencast/opencast/pull/2258)] -
  Fix Series Details
- [[#2257](https://github.com/opencast/opencast/pull/2257)] -
  Add syncronization to the access of the not thread safe xml marshaller.
- [[#2249](https://github.com/opencast/opencast/pull/2249)] -
  Test documentation only if necessary
- [[#2247](https://github.com/opencast/opencast/pull/2247)] -
  LTI Tools Mock Data and UI Server
- [[#2245](https://github.com/opencast/opencast/pull/2245)] -
  Default to server localhost also for multi tenancy
- [[#2244](https://github.com/opencast/opencast/pull/2244)] -
  Fix NullPointerException when accessing series details
- [[#2243](https://github.com/opencast/opencast/pull/2243)] -
  Fix Feeds REST Docs
- [[#2237](https://github.com/opencast/opencast/pull/2237)] -
  Add checkstyle-enforced rule about bracing style (K&R style, braces are mandatory)
- [[#2216](https://github.com/opencast/opencast/pull/2216)] -
  Add checkstyle-enforced line length limit (with most modules being excluded for now)
- [[#2203](https://github.com/opencast/opencast/pull/2203)] -
  Improved Development Runtime Dependency Containers
- [[#2198](https://github.com/opencast/opencast/pull/2198)] -
  List Upload Tool on LTI Landing Page
- [[#2188](https://github.com/opencast/opencast/pull/2188)] -
  Inspect Media Size
- [[#2186](https://github.com/opencast/opencast/pull/2186)] -
  No Decoration on Section Links
- [[#2185](https://github.com/opencast/opencast/pull/2185)] -
  Copy to Clipboard Tooltip
- [[#2181](https://github.com/opencast/opencast/pull/2181)] -
  Fix LTI Tool Documentation
- [[#2169](https://github.com/opencast/opencast/pull/2169)] -
  Better Port Randomization
- [[#2168](https://github.com/opencast/opencast/pull/2168)] -
  Fixes the lti captions upload together with the default ingest workflow #2167
- [[#2166](https://github.com/opencast/opencast/pull/2166)] -
  Add Firewall Documentation
- [[#2134](https://github.com/opencast/opencast/pull/2134)] -
  Addressing (most of) the missing ES config documentation
- [[#2106](https://github.com/opencast/opencast/pull/2106)] -
  Attempt to detect invalid DB credentials
- [[#2078](https://github.com/opencast/opencast/pull/2078)] -
  Setting appropriate defaults for AWS S3 dist config, matching the docs
- [[#2055](https://github.com/opencast/opencast/pull/2055)] -
  Metrics Exporter
- [[#2042](https://github.com/opencast/opencast/pull/2042)] -
  More efficient retrieval of active jobs
- [[#1686](https://github.com/opencast/opencast/pull/1686)] -
  Workflow Operation "CutMarksToSmil"
- [[#1017](https://github.com/opencast/opencast/pull/1017)] -
  Securing Static Files

## Opencast 9.1

*Released on December 16th, 2020*

- [[#2150](https://github.com/opencast/opencast/pull/2150)] -
  Add note about Studio config changes to the 8->9 update guide
- [[#2133](https://github.com/opencast/opencast/pull/2133)] -
  Update Debian install documentation
- [[#2160](https://github.com/opencast/opencast/pull/2160)] -
  Fix Ingest by Non-privileged User

## Opencast 9.0

*Released on December 15th, 2020*

- [[#2133](https://github.com/opencast/opencast/pull/2133)] -
  Update Debian install documentation
- [[#2110](https://github.com/opencast/opencast/pull/2110)] -
  Error Notification Style
- [[#2109](https://github.com/opencast/opencast/pull/2109)] -
  Fix apache-httpd doc
- [[#2108](https://github.com/opencast/opencast/pull/2108)] -
  Fix pagination for LTI series page
- [[#2107](https://github.com/opencast/opencast/pull/2107)] -
  Use series ACL as default ACL for events in LTI upload tool if available
- [[#2086](https://github.com/opencast/opencast/pull/2086)] -
  Move from Travis CI to GitHub Actions
- [[#2084](https://github.com/opencast/opencast/pull/2084)] -
  Add null checker when publishing to streaming service
- [[#2083](https://github.com/opencast/opencast/pull/2083)] -
  Reverting part of #1291 because this code is actually needed
- [[#2082](https://github.com/opencast/opencast/pull/2082)] -
  Fix Video Editor (Start Workflow)
- [[#2077](https://github.com/opencast/opencast/pull/2077)] -
  Fixing NPE exposed in PublishEngageWOH when publishing to AWS S3.
- [[#2074](https://github.com/opencast/opencast/pull/2074)] -
  #1907 Fix Start Task
- [[#2056](https://github.com/opencast/opencast/pull/2056)] -
  Add 'if-height-lt-' variable to resolution based encoding
- [[#2054](https://github.com/opencast/opencast/pull/2054)] -
  Add I18n translation sample file to Paella episodesFromSeries plugin
- [[#2048](https://github.com/opencast/opencast/pull/2048)] -
  Update lti landing page about series subtool
- [[#2044](https://github.com/opencast/opencast/pull/2044)] -
  Add S3 presigned URL support
- [[#2043](https://github.com/opencast/opencast/pull/2043)] -
  Add I18n support for text 'General' in 2 modals
- [[#2041](https://github.com/opencast/opencast/pull/2041)] -
  Update Node
- [[#2039](https://github.com/opencast/opencast/pull/2039)] -
  Show language of LTI tool depend on LTI param
- [[#2038](https://github.com/opencast/opencast/pull/2038)] -
  Load all supported languages in LTI tool
- [[#2023](https://github.com/opencast/opencast/pull/2023)] -
  Fix Broken Video Editor
- [[#2022](https://github.com/opencast/opencast/pull/2022)] -
  Long Labels in Segment List
- [[#2009](https://github.com/opencast/opencast/pull/2009)] -
  Fixing OpenJDK 11 builds by adding missing dependency.
- [[#2007](https://github.com/opencast/opencast/pull/2007)] -
  Fix Broken Admin Interface Sub-Tabs
- [[#2006](https://github.com/opencast/opencast/pull/2006)] -
  Update pull request to-do list
- [[#2005](https://github.com/opencast/opencast/pull/2005)] -
  Synchronize merge conflict check
- [[#2000](https://github.com/opencast/opencast/pull/2000)] -
  Better documentation for AAI DynamicLoginHandler
- [[#1982](https://github.com/opencast/opencast/pull/1982)] -
  Update Media Package POST Operation
- [[#1981](https://github.com/opencast/opencast/pull/1981)] -
  Remove Deprecated Process Executor
- [[#1970](https://github.com/opencast/opencast/pull/1970)] -
  Update cURL Commands
- [[#1963](https://github.com/opencast/opencast/pull/1963)] -
  Non-unique-files fix for Videogrid WOH
- [[#1950](https://github.com/opencast/opencast/pull/1950)] -
  Update documentation for the Docker images
- [[#1939](https://github.com/opencast/opencast/pull/1939)] -
  Update jakarta.xml.bind-api to 2.3.3
- [[#1937](https://github.com/opencast/opencast/pull/1937)] -
  Update adopter-registration-rest.xml to solve #1934 issue
- [[#1931](https://github.com/opencast/opencast/pull/1931)] -
  Make copy-event-to-series workflow id configurable
- [[#1836](https://github.com/opencast/opencast/pull/1836)] -
  Ensure User Roles
- [[#1910](https://github.com/opencast/opencast/pull/1910)] -
  Test admin frontend only once on CI
- [[#1904](https://github.com/opencast/opencast/pull/1904)] -
  Enforce Maven Dependency Checks on some more modules
- [[#1901](https://github.com/opencast/opencast/pull/1901)] -
  OSGi Annotations and Configuration
- [[#1900](https://github.com/opencast/opencast/pull/1900)] -
  Docs: admin-ui moved to admin-ui-frontend
- [[#1898](https://github.com/opencast/opencast/pull/1898)] -
  Add preencode option to partialImport WOH
- [[#1895](https://github.com/opencast/opencast/pull/1895)] -
  Development Runtime Dependency Containers
- [[#1890](https://github.com/opencast/opencast/pull/1890)] -
  Fix error while searching episode by browser
- [[#1886](https://github.com/opencast/opencast/pull/1886)] -
  Remove Spring's Patched JDOM
- [[#1876](https://github.com/opencast/opencast/pull/1876)] -
  OSGi Dependency Update
- [[#1875](https://github.com/opencast/opencast/pull/1875)] -
  Update NodeJS
- [[#1874](https://github.com/opencast/opencast/pull/1874)] -
  PostgreSQL and auto-generated databases
- [[#1872](https://github.com/opencast/opencast/pull/1872)] -
  Fix Version Check
- [[#1871](https://github.com/opencast/opencast/pull/1871)] -
  Workflow conditioner to handle floats correctly
- [[#1869](https://github.com/opencast/opencast/pull/1869)] -
  Extend the documentation concerning multiple audio tracks
- [[#1867](https://github.com/opencast/opencast/pull/1867)] -
  Update Mock Data
- [[#1866](https://github.com/opencast/opencast/pull/1866)] -
  Update to AngularJS 1.8
- [[#1858](https://github.com/opencast/opencast/pull/1858)] -
  User and role provider for Canvas LMS
- [[#1857](https://github.com/opencast/opencast/pull/1857)] -
  Refactor Metadata classes (Updated version)
- [[#1833](https://github.com/opencast/opencast/pull/1833)] -
  Add Merge Conflict Check
- [[#1831](https://github.com/opencast/opencast/pull/1831)] -
  Clarify the documentation on when new source tracks can be added
- [[#1823](https://github.com/opencast/opencast/pull/1823)] -
  Update Studio from 2020-06-25 to 2020-09-14
- [[#1814](https://github.com/opencast/opencast/pull/1814)] -
  Add a 'defaultValue' to getComponentContextProperty
- [[#1801](https://github.com/opencast/opencast/pull/1801)] -
  Remove Unused Servicewarnings Backend
- [[#1800](https://github.com/opencast/opencast/pull/1800)] -
  Adopter Registration
- [[#1796](https://github.com/opencast/opencast/pull/1796)] -
  Minimal message broker impl improvement
- [[#1795](https://github.com/opencast/opencast/pull/1795)] -
  Login Autocomplete Instructions
- [[#1794](https://github.com/opencast/opencast/pull/1794)] -
  Icon Cleanup
- [[#1791](https://github.com/opencast/opencast/pull/1791)] -
  Add with acl option to series api
- [[#1790](https://github.com/opencast/opencast/pull/1790)] -
  Request Lowercase Usernames in Moodle
- [[#1789](https://github.com/opencast/opencast/pull/1789)] -
  Remove JDOM From Ingest Service
- [[#1788](https://github.com/opencast/opencast/pull/1788)] -
  Properly parse boolean values
- [[#1773](https://github.com/opencast/opencast/pull/1773)] -
  Ingest Service Cleanup
- [[#1772](https://github.com/opencast/opencast/pull/1772)] -
  Role Prefix in Moodle User Provider
- [[#1771](https://github.com/opencast/opencast/pull/1771)] -
  OSGi Annotations for Engage UI
- [[#1764](https://github.com/opencast/opencast/pull/1764)] -
  LTI Context Role Prefix
- [[#1750](https://github.com/opencast/opencast/pull/1750)] -
  HTTPS with Apache httpd
- [[#1746](https://github.com/opencast/opencast/pull/1746)] -
  VideoGrid WOH
- [[#1719](https://github.com/opencast/opencast/pull/1719)] -
  Download button in theodul player
- [[#1684](https://github.com/opencast/opencast/pull/1684)] -
  Partial Retract WOH
- [[#1636](https://github.com/opencast/opencast/pull/1636)] -
  Support Serverless HLS
- [[#1615](https://github.com/opencast/opencast/pull/1615)] -
  Aditive Filter for Api/events endpoint
- [[#1607](https://github.com/opencast/opencast/pull/1607)] -
  Shibboleth dynamic login handler
- [[#1580](https://github.com/opencast/opencast/pull/1580)] -
  TagWorkflowOperationHandler now allows wildcards in target flavor
- [[#1768](https://github.com/opencast/opencast/pull/1768)] -
  Remove Empty Test Classes
- [[#1766](https://github.com/opencast/opencast/pull/1766)] -
  Fix minor typos
- [[#1763](https://github.com/opencast/opencast/pull/1763)] -
  Fix for issue 1280:  Notification of Newer Opencast Version in Admin UI
- [[#1762](https://github.com/opencast/opencast/pull/1762)] -
  Fixed a typo in es.upv.paella.opencast.loader.md
- [[#1760](https://github.com/opencast/opencast/pull/1760)] -
  User interface to sort by number of publications
- [[#1759](https://github.com/opencast/opencast/pull/1759)] -
  Create admin user cleanup
- [[#1758](https://github.com/opencast/opencast/pull/1758)] -
  Fix events sorted by publication
- [[#1713](https://github.com/opencast/opencast/pull/1713)] -
  Fixed double encoding of search-field in engage-ui
- [[#1710](https://github.com/opencast/opencast/pull/1710)] -
  Added Elasticsearch dependency to developer installation guide
- [[#1709](https://github.com/opencast/opencast/pull/1709)] -
  Use FontAwesome Icon
- [[#1701](https://github.com/opencast/opencast/pull/1701)] -
  Window Selection Style
- [[#1700](https://github.com/opencast/opencast/pull/1700)] -
  Document bundle:watch
- [[#1696](https://github.com/opencast/opencast/pull/1696)] -
  [Security] Bump lodash from 4.17.15 to 4.17.19 in /modules/runtime-info-ui
- [[#1695](https://github.com/opencast/opencast/pull/1695)] -
  [Security] Bump lodash from 4.17.15 to 4.17.19 in /modules/runtime-info-ui-ng
- [[#1694](https://github.com/opencast/opencast/pull/1694)] -
  [Security] Bump lodash from 4.17.15 to 4.17.19 in /modules/lti
- [[#1693](https://github.com/opencast/opencast/pull/1693)] -
  [Security] Bump lodash from 4.17.15 to 4.17.19 in /modules/engage-theodul-core
- [[#1692](https://github.com/opencast/opencast/pull/1692)] -
  [Security] Bump lodash from 4.17.15 to 4.17.19 in /modules/engage-ui
- [[#1671](https://github.com/opencast/opencast/pull/1671)] -
  Python < 3.0 requirement deleted
- [[#1670](https://github.com/opencast/opencast/pull/1670)] -
  Python < 3.0 requirement deleted
- [[#1668](https://github.com/opencast/opencast/pull/1668)] -
  Improved ffmpeg profile for extracting the last image of a video in P…
- [[#1663](https://github.com/opencast/opencast/pull/1663)] -
  Enable publish-configure to publish to streaming
- [[#1640](https://github.com/opencast/opencast/pull/1640)] -
  Fix Capture Agent API REST Docs
- [[#1637](https://github.com/opencast/opencast/pull/1637)] -
  Fix: Multiple identical workflow IDs prevent Opencast form starting properly
- [[#1635](https://github.com/opencast/opencast/pull/1635)] -
  Admin UI embedding code
- [[#1630](https://github.com/opencast/opencast/pull/1630)] -
  Fix Series in Media Module
- [[#1629](https://github.com/opencast/opencast/pull/1629)] -
  LTI User Data
- [[#1623](https://github.com/opencast/opencast/pull/1623)] -
  Update Node.js
- [[#1621](https://github.com/opencast/opencast/pull/1621)] -
  [Security] Bump websocket-extensions from 0.1.3 to 0.1.4 in /modules/admin-ui-frontend
- [[#1605](https://github.com/opencast/opencast/pull/1605)] -
  Update Several JavaScript Libraries
- [[#1567](https://github.com/opencast/opencast/pull/1567)] -
  Update Python on Travis CI
- [[#1566](https://github.com/opencast/opencast/pull/1566)] -
  Switching to Paella player by default
- [[#1553](https://github.com/opencast/opencast/pull/1553)] -
  Fix custom roles in admin ui
- [[#1549](https://github.com/opencast/opencast/pull/1549)] -
  Resolution based encoding extension: if-width-or-height-geq-
- [[#1548](https://github.com/opencast/opencast/pull/1548)] -
  #1541 adding write access parameter to events and series endpoint
- [[#1547](https://github.com/opencast/opencast/pull/1547)] -
  Download paella source code from github instead of using npm + paella update to 6.4.3
- [[#1536](https://github.com/opencast/opencast/pull/1536)] -
  Typo correction
- [[#1530](https://github.com/opencast/opencast/pull/1530)] -
  REST Docs: Ingest: WF parameters, WFIID deprecated
- [[#1523](https://github.com/opencast/opencast/pull/1523)] -
  Documentation: OsgiAclServiceRestEndpoint
- [[#1499](https://github.com/opencast/opencast/pull/1499)] -
  Add NUT container format
- [[#1497](https://github.com/opencast/opencast/pull/1497)] -
  Documentation: Update asset-delete-woh.md
- [[#1490](https://github.com/opencast/opencast/pull/1490)] -
  Make encoding profiles support odd widths and heights develop
- [[#1465](https://github.com/opencast/opencast/pull/1465)] -
  [Security] Bump minimist from 1.2.0 to 1.2.5 in /docs/guides
- [[#1464](https://github.com/opencast/opencast/pull/1464)] -
  [Security] Bump acorn from 7.1.0 to 7.1.1 in /modules/engage-paella-player
- [[#1463](https://github.com/opencast/opencast/pull/1463)] -
  [Security] Bump acorn from 7.1.0 to 7.1.1 in /modules/engage-theodul-core
- [[#1462](https://github.com/opencast/opencast/pull/1462)] -
  [Security] Bump acorn from 7.1.0 to 7.1.1 in /modules/engage-ui
- [[#1461](https://github.com/opencast/opencast/pull/1461)] -
  [Security] Bump acorn from 7.1.0 to 7.1.1 in /modules/lti
- [[#1460](https://github.com/opencast/opencast/pull/1460)] -
  [Security] Bump acorn from 7.1.0 to 7.1.1 in /modules/runtime-info-ui-ng
- [[#1459](https://github.com/opencast/opencast/pull/1459)] -
  [Security] Bump acorn from 7.1.0 to 7.1.1 in /modules/runtime-info-ui
- [[#1456](https://github.com/opencast/opencast/pull/1456)] -
  Adding support for 360 video playback to paella player
- [[#1455](https://github.com/opencast/opencast/pull/1455)] -
  Add bower_components/ to .gitignore file
- [[#1444](https://github.com/opencast/opencast/pull/1444)] -
  Make Admin Interface Use `npm ci`
- [[#1443](https://github.com/opencast/opencast/pull/1443)] -
  Allow Root In Bower (Again)
- [[#1440](https://github.com/opencast/opencast/pull/1440)] -
  Update android-mms
- [[#1439](https://github.com/opencast/opencast/pull/1439)] -
  Editor zooming improved
- [[#1431](https://github.com/opencast/opencast/pull/1431)] -
  Override all POSIX language variables in Gruntfile.js
- [[#1430](https://github.com/opencast/opencast/pull/1430)] -
  #1429 rewrite ServiceRegistryJpaImplTest to reduce TravisCI failures
- [[#1423](https://github.com/opencast/opencast/pull/1423)] -
  Fix REST Documentation
- [[#1421](https://github.com/opencast/opencast/pull/1421)] -
  Remove compose in favor of encode
- [[#1420](https://github.com/opencast/opencast/pull/1420)] -
  Override all LANG and LC_ environment variables for stable tests
- [[#1419](https://github.com/opencast/opencast/pull/1419)] -
  Set fixed LANG for stable tests. Fixes #1418
- [[#1413](https://github.com/opencast/opencast/pull/1413)] -
  Remove Unused Admin Interface Ressources
- [[#1407](https://github.com/opencast/opencast/pull/1407)] -
  Fix typo in LDAP documentation
- [[#1406](https://github.com/opencast/opencast/pull/1406)] -
  Add CAS authentication to default XML config
- [[#1403](https://github.com/opencast/opencast/pull/1403)] -
  Remove Outdated Shibboleth Configuration
- [[#1402](https://github.com/opencast/opencast/pull/1402)] -
  Quick-links in documentation
- [[#1401](https://github.com/opencast/opencast/pull/1401)] -
  Fix More Dependencies
- [[#1397](https://github.com/opencast/opencast/pull/1397)] -
  Silence Detector Cleanup
- [[#1396](https://github.com/opencast/opencast/pull/1396)] -
  Image Extraction Without Stream Duration
- [[#1395](https://github.com/opencast/opencast/pull/1395)] -
  Fix Image Extraction At Position Zero
- [[#1391](https://github.com/opencast/opencast/pull/1391)] -
  Documentation: Text Extraction Configuration
- [[#1388](https://github.com/opencast/opencast/pull/1388)] -
  Return bibliographic start date of event via API
- [[#1387](https://github.com/opencast/opencast/pull/1387)] -
  Speedup silence detection in case there is a video stream
- [[#1382](https://github.com/opencast/opencast/pull/1382)] -
  Show search results after changing chosen list
- [[#1381](https://github.com/opencast/opencast/pull/1381)] -
  Remove get acl scheduler endpoint as it's not used
- [[#1377](https://github.com/opencast/opencast/pull/1377)] -
  Update to MariaDB Client
- [[#1376](https://github.com/opencast/opencast/pull/1376)] -
  Create JpaUserReference for LTI user (update to 9.x)
- [[#1375](https://github.com/opencast/opencast/pull/1375)] -
  Log the proper index name when updating the asset manager index
- [[#1371](https://github.com/opencast/opencast/pull/1371)] -
  Split AdminUI in Java and JavaScript parts
- [[#1368](https://github.com/opencast/opencast/pull/1368)] -
  More OSGi Service Annotations
- [[#1365](https://github.com/opencast/opencast/pull/1365)] -
  Remove Drupal Based Pingback Service
- [[#1363](https://github.com/opencast/opencast/pull/1363)] -
  Added the adopter registration form for statistics.
- [[#1354](https://github.com/opencast/opencast/pull/1354)] -
  Addition of trim segment configuration and new documentation
- [[#1350](https://github.com/opencast/opencast/pull/1350)] -
  Dependency Tests
- [[#1349](https://github.com/opencast/opencast/pull/1349)] -
  Drop X-Opencast-Matterhorn-Authorization
- [[#1348](https://github.com/opencast/opencast/pull/1348)] -
  Add AmberScript Transcription Service
- [[#1347](https://github.com/opencast/opencast/pull/1347)] -
  LDAP Configuration
- [[#1346](https://github.com/opencast/opencast/pull/1346)] -
  Adjust documentation regarding Elasticsearch setup
- [[#1330](https://github.com/opencast/opencast/pull/1330)] -
  ESLint For Theodul Connection Plugin
- [[#1316](https://github.com/opencast/opencast/pull/1316)] -
  [Security] Bump handlebars from 4.2.0 to 4.5.3 in /modules/admin-ui
- [[#1295](https://github.com/opencast/opencast/pull/1295)] -
  Change npm install to npm ci
- [[#1293](https://github.com/opencast/opencast/pull/1293)] -
  Actually update event workflow via API
- [[#1291](https://github.com/opencast/opencast/pull/1291)] -
  Clean up unused code and ignored tests
- [[#1289](https://github.com/opencast/opencast/pull/1289)] -
  Improve LTI: add create event and edit event, improve series tool
- [[#1288](https://github.com/opencast/opencast/pull/1288)] -
  Java 11 Compatibility
- [[#1287](https://github.com/opencast/opencast/pull/1287)] -
  Add option to remove running workflows, fix restdocs for delete requests
- [[#1283](https://github.com/opencast/opencast/pull/1283)] -
  Fix hourly statistics export
- [[#1277](https://github.com/opencast/opencast/pull/1277)] -
  Fixed streaming distribution remote
- [[#1275](https://github.com/opencast/opencast/pull/1275)] -
  Fix small typo in External API docs
- [[#1272](https://github.com/opencast/opencast/pull/1272)] -
  Log progress of solr search reindex
- [[#1268](https://github.com/opencast/opencast/pull/1268)] -
  Additional logging for ACL parse errors
- [[#1267](https://github.com/opencast/opencast/pull/1267)] -
  Log the Ids of items being indexed
- [[#1255](https://github.com/opencast/opencast/pull/1255)] -
  Update paella player to 6.3.2
- [[#1254](https://github.com/opencast/opencast/pull/1254)] -
  Map internal service host URLs to tenant-specific URLs
- [[#1252](https://github.com/opencast/opencast/pull/1252)] -
  Make JPA Generated Database Match Script Generated Database
- [[#1250](https://github.com/opencast/opencast/pull/1250)] -
  Drop Unused Tables
- [[#1249](https://github.com/opencast/opencast/pull/1249)] -
  Documentation: Metadata fixes
- [[#1235](https://github.com/opencast/opencast/pull/1235)] -
  Update selected components to use OSGI annotations
- [[#1234](https://github.com/opencast/opencast/pull/1234)] -
  Add audio and video stream selectors for tracks to ExecuteMany WOH
- [[#1230](https://github.com/opencast/opencast/pull/1230)] -
  Single image video fix
- [[#1226](https://github.com/opencast/opencast/pull/1226)] -
  Implement StreamingDistributionService remotely
- [[#1205](https://github.com/opencast/opencast/pull/1205)] -
  Dropping SysV-Init
- [[#1198](https://github.com/opencast/opencast/pull/1198)] -
  Introduce ESlint for Theodul Controls Plugin
- [[#1179](https://github.com/opencast/opencast/pull/1179)] -
  Make wowza configuration tenant-specific
- [[#1171](https://github.com/opencast/opencast/pull/1171)] -
  Removed wrong comma in .json example
- [[#1163](https://github.com/opencast/opencast/pull/1163)] -
  Improve embed code generation of Theodul player to create a fully responsive embed code fragment
- [[#1161](https://github.com/opencast/opencast/pull/1161)] -
  fix #1158, add config properties to prevent XSS attacks on session co…
- [[#1159](https://github.com/opencast/opencast/pull/1159)] -
  Removing old references to org.opencastproject.db.ddl.generate
- [[#1154](https://github.com/opencast/opencast/pull/1154)] -
  Show users with same mail address and name
- [[#1150](https://github.com/opencast/opencast/pull/1150)] -
  Workflow: update-previews: Add description
- [[#1149](https://github.com/opencast/opencast/pull/1149)] -
  Workflow title: Update editor previews
- [[#1135](https://github.com/opencast/opencast/pull/1135)] -
  Allow to overwrite setenv variables
- [[#1133](https://github.com/opencast/opencast/pull/1133)] -
  Better JPA Annotation for Scheduler
- [[#1130](https://github.com/opencast/opencast/pull/1130)] -
  Updated com.fasterxml.jackson from version 2.9.9 to 2.10.0.
- [[#1128](https://github.com/opencast/opencast/pull/1128)] -
  Load series ACL-list step by step
- [[#1127](https://github.com/opencast/opencast/pull/1127)] -
  Update accesspolicies.md: fixed grammar issues
- [[#1121](https://github.com/opencast/opencast/pull/1121)] -
  Remove unnecessary ExceptionUtils.getStackTrace #1119
- [[#1120](https://github.com/opencast/opencast/pull/1120)] -
  Updates Service Registry dispatch interval property name and time unit
- [[#1118](https://github.com/opencast/opencast/pull/1118)] -
  Removes String.format calls in logs
- [[#1109](https://github.com/opencast/opencast/pull/1109)] -
  Extended statistics export
- [[#1107](https://github.com/opencast/opencast/pull/1107)] -
  ESLint for Theodul Core
- [[#1106](https://github.com/opencast/opencast/pull/1106)] -
  Update to ESLint 6.5.0
- [[#1105](https://github.com/opencast/opencast/pull/1105)] -
  Use JPA to auto-generate SQL schema
- [[#1104](https://github.com/opencast/opencast/pull/1104)] -
  Login Response for JavaScript
- [[#1081](https://github.com/opencast/opencast/pull/1081)] -
  Add modal to edit metadata of multiple events
- [[#1064](https://github.com/opencast/opencast/pull/1064)] -
  Update to paella player 6.2.2
- [[#1054](https://github.com/opencast/opencast/pull/1054)] -
  Fix a bug in paella loader plugin when a track has no tags
- [[#1046](https://github.com/opencast/opencast/pull/1046)] -
  Load all roles in Admin UI
- [[#1043](https://github.com/opencast/opencast/pull/1043)] -
  Multiple audio tracks support on paella
- [[#1032](https://github.com/opencast/opencast/pull/1032)] -
  Sort roles alphabetically in UI
- [[#1002](https://github.com/opencast/opencast/pull/1002)] -
  S3 S3 compatibility - Endpoint configuration for Amazon S3 alternatives added
- [[#884](https://github.com/opencast/opencast/pull/884)] -
  Display global notifications as overlay
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/1897">1897</a>] -
  Bump markdownlint-cli from 0.23.2 to 0.24.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1896">1896</a>] -
  Bump @types/react from 16.9.2 to 16.9.50 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1889">1889</a>] -
  Bump @types/react-select from 3.0.14 to 3.0.21 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1888">1888</a>] -
  Bump bootbox from 5.4.0 to 5.4.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1884">1884</a>] -
  Bump eslint from 7.9.0 to 7.10.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1883">1883</a>] -
  Bump eslint from 7.9.0 to 7.10.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1882">1882</a>] -
  Bump eslint from 7.9.0 to 7.10.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1881">1881</a>] -
  Bump eslint from 7.9.0 to 7.10.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1880">1880</a>] -
  Bump eslint from 7.9.0 to 7.10.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1879">1879</a>] -
  Bump eslint from 7.9.0 to 7.10.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1873">1873</a>] -
  Bump karma from 5.2.2 to 5.2.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1868">1868</a>] -
  Bump @types/node from 12.7.5 to 14.11.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1854">1854</a>] -
  Bump @types/jest from 24.0.18 to 26.0.14 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1844">1844</a>] -
  Bump eslint from 7.8.1 to 7.9.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1843">1843</a>] -
  Bump eslint from 7.8.1 to 7.9.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1841">1841</a>] -
  Bump eslint from 7.8.1 to 7.9.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1840">1840</a>] -
  Bump eslint from 7.8.1 to 7.9.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1839">1839</a>] -
  Bump eslint from 7.8.1 to 7.9.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1838">1838</a>] -
  Bump eslint from 7.8.1 to 7.9.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1830">1830</a>] -
  Bump karma from 5.2.1 to 5.2.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1829">1829</a>] -
  Bump chromedriver from 85.0.0 to 85.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1825">1825</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1824">1824</a>] -
  Bump karma from 5.1.1 to 5.2.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1822">1822</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1819">1819</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1817">1817</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1816">1816</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1808">1808</a>] -
  Bump underscore from 1.10.2 to 1.11.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1743">1743</a>] -
  Bump @types/react-helmet from 5.0.16 to 6.1.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1728">1728</a>] -
  Bump @types/react-dom from 16.9.0 to 16.9.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1727">1727</a>] -
  Bump react-helmet from 5.2.1 to 6.1.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1724">1724</a>] -
  Bump react-i18next from 10.13.2 to 11.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1825">1825</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1824">1824</a>] -
  Bump karma from 5.1.1 to 5.2.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1822">1822</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1819">1819</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1817">1817</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1816">1816</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1813">1813</a>] -
  Bump eslint from 7.7.0 to 7.8.1 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1811">1811</a>] -
  Bump chromedriver from 84.0.1 to 85.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1810">1810</a>] -
  Bump eslint-plugin-header from 3.0.0 to 3.1.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1809">1809</a>] -
  Bump eslint-plugin-header from 3.0.0 to 3.1.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1808">1808</a>] -
  Bump underscore from 1.10.2 to 1.11.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1807">1807</a>] -
  Bump eslint-plugin-header from 3.0.0 to 3.1.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1806">1806</a>] -
  Bump eslint-plugin-header from 3.0.0 to 3.1.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1803">1803</a>] -
  Bump eslint-plugin-header from 3.0.0 to 3.1.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1802">1802</a>] -
  Bump eslint-plugin-header from 3.0.0 to 3.1.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1785">1785</a>] -
  Bump grunt from 1.2.1 to 1.3.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1781">1781</a>] -
  Bump eslint from 7.6.0 to 7.7.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1780">1780</a>] -
  Bump eslint from 7.6.0 to 7.7.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1777">1777</a>] -
  Bump eslint from 7.6.0 to 7.7.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1776">1776</a>] -
  Bump eslint from 7.6.0 to 7.7.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1775">1775</a>] -
  Bump eslint from 7.6.0 to 7.7.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1774">1774</a>] -
  Bump eslint from 7.6.0 to 7.7.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1765">1765</a>] -
  Bump karma-jasmine from 4.0.0 to 4.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1761">1761</a>] -
  Bump karma-jasmine from 3.3.1 to 4.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1742">1742</a>] -
  Bump eslint from 7.5.0 to 7.6.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1741">1741</a>] -
  Bump eslint from 7.5.0 to 7.6.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1739">1739</a>] -
  Bump eslint from 7.5.0 to 7.6.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1738">1738</a>] -
  Bump eslint from 7.5.0 to 7.6.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1737">1737</a>] -
  Bump eslint from 7.5.0 to 7.6.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1736">1736</a>] -
  Bump eslint from 7.5.0 to 7.6.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1734">1734</a>] -
  Bump autoprefixer from 9.8.5 to 9.8.6 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1722">1722</a>] -
  Bump karma from 5.1.0 to 5.1.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1717">1717</a>] -
  Bump karma-coverage from 2.0.2 to 2.0.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1715">1715</a>] -
  Bump jasmine-core from 3.5.0 to 3.6.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1714">1714</a>] -
  Bump chromedriver from 84.0.0 to 84.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1708">1708</a>] -
  Bump eslint from 7.4.0 to 7.5.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1707">1707</a>] -
  Bump eslint from 7.4.0 to 7.5.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1706">1706</a>] -
  Bump eslint from 7.4.0 to 7.5.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1705">1705</a>] -
  Bump eslint from 7.4.0 to 7.5.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1704">1704</a>] -
  Bump eslint from 7.4.0 to 7.5.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1703">1703</a>] -
  Bump eslint from 7.4.0 to 7.5.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1702">1702</a>] -
  Bump eslint from 7.4.0 to 7.5.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1699">1699</a>] -
  Bump chromedriver from 83.0.1 to 84.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1698">1698</a>] -
  Bump grunt-contrib-connect from 2.1.0 to 3.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1697">1697</a>] -
  Bump grunt-contrib-uglify from 4.0.1 to 5.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1691">1691</a>] -
  Bump lodash from 4.17.15 to 4.17.19 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1689">1689</a>] -
  Bump autoprefixer from 9.8.4 to 9.8.5 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1687">1687</a>] -
  Bump grunt from 1.2.0 to 1.2.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1683">1683</a>] -
  Bump chromedriver from 83.0.0 to 83.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1681">1681</a>] -
  Bump eslint from 7.3.1 to 7.4.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1680">1680</a>] -
  Bump eslint from 7.3.1 to 7.4.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1679">1679</a>] -
  Bump grunt from 1.1.0 to 1.2.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1678">1678</a>] -
  Bump eslint from 7.3.1 to 7.4.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1677">1677</a>] -
  Bump eslint from 7.3.1 to 7.4.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1676">1676</a>] -
  Bump eslint from 7.3.1 to 7.4.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1675">1675</a>] -
  Bump eslint from 7.3.1 to 7.4.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1674">1674</a>] -
  Bump eslint from 7.3.1 to 7.4.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1666">1666</a>] -
  Bump markdownlint-cli from 0.23.1 to 0.23.2 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1665">1665</a>] -
  Bump http-errors from 1.7.3 to 1.8.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1660">1660</a>] -
  Bump autoprefixer from 9.8.2 to 9.8.4 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1658">1658</a>] -
  Bump eslint from 7.3.0 to 7.3.1 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1656">1656</a>] -
  Bump eslint from 7.3.0 to 7.3.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1655">1655</a>] -
  Bump eslint from 7.3.0 to 7.3.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1654">1654</a>] -
  Bump eslint from 7.3.0 to 7.3.1 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1653">1653</a>] -
  Bump eslint from 7.3.0 to 7.3.1 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1652">1652</a>] -
  Bump eslint from 7.3.0 to 7.3.1 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1651">1651</a>] -
  Bump eslint from 7.3.0 to 7.3.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1650">1650</a>] -
  Bump eslint from 7.2.0 to 7.3.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1649">1649</a>] -
  Bump eslint from 7.2.0 to 7.3.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1648">1648</a>] -
  Bump autoprefixer from 9.8.0 to 9.8.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1647">1647</a>] -
  Bump eslint from 7.2.0 to 7.3.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1646">1646</a>] -
  Bump eslint from 7.2.0 to 7.3.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1645">1645</a>] -
  Bump eslint from 7.2.0 to 7.3.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1644">1644</a>] -
  Bump eslint from 7.2.0 to 7.3.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1643">1643</a>] -
  Bump eslint from 7.2.0 to 7.3.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1638">1638</a>] -
  Bump karma from 5.0.9 to 5.1.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1632">1632</a>] -
  Bump eslint from 6.8.0 to 7.2.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1631">1631</a>] -
  Bump eslint from 7.1.0 to 7.2.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1628">1628</a>] -
  Bump eslint from 6.8.0 to 7.2.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1627">1627</a>] -
  Bump eslint from 6.8.0 to 7.2.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1626">1626</a>] -
  Bump eslint from 6.8.0 to 7.2.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1625">1625</a>] -
  Bump eslint from 6.8.0 to 7.2.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1624">1624</a>] -
  Bump eslint from 6.8.0 to 7.2.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1619">1619</a>] -
  Bump angular from 1.7.9 to 1.8.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1618">1618</a>] -
  Bump angular-route from 1.7.9 to 1.8.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1614">1614</a>] -
  Bump karma-jasmine from 3.2.0 to 3.3.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1606">1606</a>] -
  Bump karma-jasmine from 3.1.1 to 3.2.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1601">1601</a>] -
  Bump js-yaml from 3.13.1 to 3.14.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1599">1599</a>] -
  Bump eslint from 6.8.0 to 7.1.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1591">1591</a>] -
  Bump karma from 5.0.8 to 5.0.9 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1590">1590</a>] -
  Bump chromedriver from 81.0.0 to 83.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1587">1587</a>] -
  Bump karma from 5.0.5 to 5.0.8 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1585">1585</a>] -
  Bump http-proxy from 1.18.0 to 1.18.1 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1584">1584</a>] -
  Bump autoprefixer from 9.7.6 to 9.8.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1582">1582</a>] -
  Bump markdownlint-cli from 0.23.0 to 0.23.1 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1569">1569</a>] -
  Bump karma from 5.0.4 to 5.0.5 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1563">1563</a>] -
  Bump node-sass from 4.14.0 to 4.14.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1562">1562</a>] -
  Bump jquery from 3.5.0 to 3.5.1 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1561">1561</a>] -
  Bump markdownlint-cli from 0.22.0 to 0.23.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1560">1560</a>] -
  Bump jquery from 3.5.0 to 3.5.1 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1559">1559</a>] -
  Bump jquery from 3.5.0 to 3.5.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1558">1558</a>] -
  Bump jquery from 3.5.0 to 3.5.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1555">1555</a>] -
  Bump karma from 5.0.3 to 5.0.4 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1552">1552</a>] -
  Bump karma from 5.0.2 to 5.0.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1544">1544</a>] -
  Bump node-sass from 4.13.1 to 4.14.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1531">1531</a>] -
  Bump karma from 4.4.1 to 5.0.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1524">1524</a>] -
  Bump grunt-karma from 3.0.2 to 4.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1522">1522</a>] -
  Bump karma-coverage from 2.0.1 to 2.0.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1521">1521</a>] -
  Bump chromedriver from 80.0.1 to 81.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1520">1520</a>] -
  Bump jquery from 3.4.1 to 3.5.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1519">1519</a>] -
  Bump jquery from 3.4.1 to 3.5.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1518">1518</a>] -
  Bump jquery from 3.4.1 to 3.5.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1517">1517</a>] -
  Bump jquery from 3.4.1 to 3.5.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1511">1511</a>] -
  Bump autoprefixer from 9.7.5 to 9.7.6 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1510">1510</a>] -
  Bump grunt-ng-annotate from 3.0.0 to 4.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1498">1498</a>] -
  Bump underscore from 1.9.2 to 1.10.2 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1484">1484</a>] -
  Bump autoprefixer from 9.7.4 to 9.7.5 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1469">1469</a>] -
  Bump grunt from 1.0.4 to 1.1.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1466">1466</a>] -
  Bump mustache from 4.0.0 to 4.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1417">1417</a>] -
  Bump karma-jasmine from 3.1.0 to 3.1.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1416">1416</a>] -
  Bump chromedriver from 80.0.0 to 80.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1415">1415</a>] -
  Bump request from 2.88.0 to 2.88.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1398">1398</a>] -
  Bump markdownlint-cli from 0.21.0 to 0.22.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1389">1389</a>] -
  Bump chromedriver from 79.0.2 to 80.0.0 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1383">1383</a>] -
  Bump checkstyle from 8.21 to 8.29</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1378">1378</a>] -
  Bump chromedriver from 79.0.0 to 79.0.2 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1361">1361</a>] -
  Bump node-sass from 4.13.0 to 4.13.1 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1360">1360</a>] -
  Bump mustache from 3.2.1 to 4.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1355">1355</a>] -
  Bump autoprefixer from 9.7.3 to 9.7.4 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1344">1344</a>] -
  Bump karma-jasmine from 3.0.3 to 3.1.0 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1342">1342</a>] -
  Bump karma-jasmine from 3.0.1 to 3.0.3 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1334">1334</a>] -
  Bump karma-firefox-launcher from 1.2.0 to 1.3.0 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1332">1332</a>] -
  Bump karma-jasmine from 2.0.1 to 3.0.1 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1331">1331</a>] -
  Bump underscore from 1.9.1 to 1.9.2 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1322">1322</a>] -
  Bump markdownlint-cli from 0.20.0 to 0.21.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1317">1317</a>] -
  Bump mustache from 3.1.0 to 3.2.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1314">1314</a>] -
  Bump eslint from 6.7.2 to 6.8.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1313">1313</a>] -
  Bump bootbox from 5.3.4 to 5.4.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1312">1312</a>] -
  Bump eslint from 6.7.2 to 6.8.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1311">1311</a>] -
  Bump eslint from 6.7.2 to 6.8.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1310">1310</a>] -
  Bump eslint from 6.7.2 to 6.8.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1309">1309</a>] -
  Bump eslint from 6.7.2 to 6.8.0 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1308">1308</a>] -
  Bump eslint from 6.7.2 to 6.8.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1307">1307</a>] -
  Bump eslint from 6.7.2 to 6.8.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1298">1298</a>] -
  Bump markdownlint-cli from 0.19.0 to 0.20.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1282">1282</a>] -
  Bump chromedriver from 78.0.1 to 79.0.0 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1264">1264</a>] -
  Bump eslint from 6.7.1 to 6.7.2 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1263">1263</a>] -
  Bump eslint from 6.7.1 to 6.7.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1262">1262</a>] -
  Bump eslint from 6.7.1 to 6.7.2 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1261">1261</a>] -
  Bump autoprefixer from 9.7.2 to 9.7.3 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1260">1260</a>] -
  Bump eslint from 6.7.1 to 6.7.2 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1259">1259</a>] -
  Bump eslint from 6.7.1 to 6.7.2 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1258">1258</a>] -
  Bump eslint from 6.7.1 to 6.7.2 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1257">1257</a>] -
  Bump eslint from 6.7.1 to 6.7.2 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1242">1242</a>] -
  Bump eslint from 6.6.0 to 6.7.1 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1241">1241</a>] -
  Bump eslint from 6.6.0 to 6.7.1 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1240">1240</a>] -
  Bump eslint from 6.6.0 to 6.7.1 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1239">1239</a>] -
  Bump eslint from 6.6.0 to 6.7.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1238">1238</a>] -
  Bump eslint from 6.6.0 to 6.7.1 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1237">1237</a>] -
  Bump eslint from 6.6.0 to 6.7.1 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1236">1236</a>] -
  Bump eslint from 6.6.0 to 6.7.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1225">1225</a>] -
  Bump autoprefixer from 9.7.1 to 9.7.2 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1222">1222</a>] -
  Bump paginationjs from 2.1.4 to 2.1.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1211">1211</a>] -
  Bump eslint from 6.5.0 to 6.6.0 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1210">1210</a>] -
  Bump autoprefixer from 9.7.0 to 9.7.1 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1209">1209</a>] -
  Bump bootbox from 5.3.3 to 5.3.4 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1208">1208</a>] -
  Bump bootbox from 5.3.2 to 5.3.3 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1196">1196</a>] -
  Bump eslint from 6.5.0 to 6.6.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1195">1195</a>] -
  Bump http-proxy from 1.17.0 to 1.18.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1194">1194</a>] -
  Bump karma from 4.3.0 to 4.4.1 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1193">1193</a>] -
  Bump node-sass from 4.12.0 to 4.13.0 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1192">1192</a>] -
  Bump eslint from 6.5.0 to 6.6.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1191">1191</a>] -
  Bump autoprefixer from 9.6.1 to 9.7.0 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1190">1190</a>] -
  Bump chromedriver from 76.0.1 to 78.0.1 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1189">1189</a>] -
  Bump jasmine-core from 3.4.0 to 3.5.0 in /modules/admin-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1188">1188</a>] -
  Bump seedrandom from 3.0.3 to 3.0.5 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1187">1187</a>] -
  Bump eslint from 6.5.0 to 6.6.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1186">1186</a>] -
  Bump eslint from 6.5.0 to 6.6.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1184">1184</a>] -
  Bump eslint from 6.5.0 to 6.6.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1183">1183</a>] -
  Bump eslint from 6.5.0 to 6.6.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1182">1182</a>] -
  Bump markdownlint-cli from 0.18.0 to 0.19.0 in /docs/guides</li>
</ul>
</details>