Opencast 17 Changelog
---------------------

Changes marked with \* were also included in 16.x.

## Opencast 17.6 (2025-07-24)

- [[#6932](https://github.com/opencast/opencast/pull/6932)] -
  Update r/17.x Admin UI to 17.x-2025-07-24
- [[#6933](https://github.com/opencast/opencast/pull/6933)] -
  Update r/17.x Editor to 17.x-2025-07-24
- [[#6890](https://github.com/opencast/opencast/pull/6890)] -
  Trim plugin configuration data
- [[#6892](https://github.com/opencast/opencast/pull/6892)] -
  Fix incorrect media types in Tobira endpoint
- [[#6898](https://github.com/opencast/opencast/pull/6898)] -
  Fix subtitles when used in series that contain watermarks
- [[#6921](https://github.com/opencast/opencast/pull/6921)] -
  Watermarks must be specific filetypes
- [[#6922](https://github.com/opencast/opencast/pull/6922)] -
  Series list provider performance
- [[#6884](https://github.com/opencast/opencast/pull/6884)] -
  Allow to escape colons in workflow variable syntax
- [[#6883](https://github.com/opencast/opencast/pull/6883)] -
  Limit OpenMP Threads only for Tesseract
- [[#6885](https://github.com/opencast/opencast/pull/6885)] -
  Fix snapshot deletion for remote only MediaPackages
- [[#6825](https://github.com/opencast/opencast/pull/6825)] -
  HttpClient Timeouts in Stud.IP User Directory
- [[#6831](https://github.com/opencast/opencast/pull/6831)] -
  Accept AAI attribute values as list
- [[#6837](https://github.com/opencast/opencast/pull/6837)] -
  Fix changelog link in Github Release
- [[#6839](https://github.com/opencast/opencast/pull/6839)] -
  Update Java Version Support Documentation

## Opencast 17.5 (2025-06-27)

- [[#6835](https://github.com/opencast/opencast/pull/6835)] -
  Update r/17.x Admin UI to 17.x-2025-06-27
- [[#6728](https://github.com/opencast/opencast/pull/6728)] -
  Allow to select three or more tracks
- [[#6827](https://github.com/opencast/opencast/pull/6827)] -
  Fix Matrix Room Topic Update\*
- [[#6822](https://github.com/opencast/opencast/pull/6822)] -
  Fix episode id roles in series of search service index
- [[#6818](https://github.com/opencast/opencast/pull/6818)] -
  Replace video.ethz.ch links with OC explore & adjust docs dashboard
- [[#6792](https://github.com/opencast/opencast/pull/6792)] -
  Deselecting a series may result in loss of access to the event
- [[#6813](https://github.com/opencast/opencast/pull/6813)] -
  Remove series load in paella player
- [[#6742](https://github.com/opencast/opencast/pull/6742)] -
  Splitting the Changelog into Multiple Files and other Minor Docs Improvements
- [[#6789](https://github.com/opencast/opencast/pull/6789)] -
  Tobira URLs improvement
- [[#6811](https://github.com/opencast/opencast/pull/6811)] -
  Update r/17.x Editor to 17.x-2025-06-19
- [[#6809](https://github.com/opencast/opencast/pull/6809)] -
  Disable icla check for opencastproject user
- [[#6802](https://github.com/opencast/opencast/pull/6802)] -
  Add support for extra data in adopter registration
- [[#6791](https://github.com/opencast/opencast/pull/6791)] -
  Fix jwt validation failures when using ECDSA algorithms
- [[#6524](https://github.com/opencast/opencast/pull/6524)] -
  Add index migration script
- [[#6795](https://github.com/opencast/opencast/pull/6795)] -
  Explain that WhisperC++ autoencode is an Opencast feature
- [[#6793](https://github.com/opencast/opencast/pull/6793)] -
  Fix minor markdown doc warnings
- [[#6733](https://github.com/opencast/opencast/pull/6733)] -
  Add filters to series list query for admin ui
- [[#6702](https://github.com/opencast/opencast/pull/6702)] -
  Fix whispercpp operation when using the language from metadata
- [[#6750](https://github.com/opencast/opencast/pull/6750)] -
  Wait for Opensearch on Opencast boot\*
- [[#6783](https://github.com/opencast/opencast/pull/6783)] -
  Paella security fix: Paella Player can load configuration files from arbitrary servers\*

## Opencast 17.4 (2025-05-26)
- [[#6741](https://github.com/opencast/opencast/pull/6741)] -
  Update Admin UI to 17.x-2025-05-24
- [[#6738](https://github.com/opencast/opencast/pull/6738)] -
  Update 17.x Editor to 17.x-2025-05-23
- [[#6729](https://github.com/opencast/opencast/pull/6729)] -
  Fix documentation regarding dummy capture agent configuration
- [[#6561](https://github.com/opencast/opencast/pull/6561)] -
  Add graphql event filter option
- [[#6562](https://github.com/opencast/opencast/pull/6562)] -
  Remove mentions of SysV Init scripts
- [[#6633](https://github.com/opencast/opencast/pull/6633)] -
  Enable partial matching in SearchRestService
- [[#6637](https://github.com/opencast/opencast/pull/6637)] -
  Workaround for "laggy" servers table in admin ui
- [[#6688](https://github.com/opencast/opencast/pull/6688)] -
  Document Search index rebuild when enabling episode id roles
- [[#6681](https://github.com/opencast/opencast/pull/6681)] -
  Allow Opensearch to be in yellow state\*
- [[#6661](https://github.com/opencast/opencast/pull/6661)] -
  Update to Studio 2025-04-30 (important bug fix)
- [[#6653](https://github.com/opencast/opencast/pull/6653)] -
  Add search endpoint to trigger single event index update

## Opencast 17.3 (2025-04-17)

- [[#6641](https://github.com/opencast/opencast/pull/6641)] -
-  Update Admin UI release to 2025-04-17
- [[#6615](https://github.com/opencast/opencast/pull/6615)] -
  Update Studio to 2025-04-02
- [[#6616](https://github.com/opencast/opencast/pull/6616)] -
  Fix series table sorting for organizers column
- [[#6573](https://github.com/opencast/opencast/pull/6573)] -
  Include Job ID in Job Error
- [[#6628](https://github.com/opencast/opencast/pull/6628)] -
  Update Asset Manager Java API Documentation
- [[#6627](https://github.com/opencast/opencast/pull/6627)] -
  Improve Performance of Metrics Endpoint
- [[#6614](https://github.com/opencast/opencast/pull/6614)] -
  Improved Paella player Matomo plugin documentation and default config
- [[#6584](https://github.com/opencast/opencast/pull/6584)] -
  Fix 500 response for editor endpoints
- [[#6578](https://github.com/opencast/opencast/pull/6578)] -
  Fix adopter statistic breaking on non-set metadata
- [[#6577](https://github.com/opencast/opencast/pull/6577)] -
  Prevent errors when shutting down Opencast
- [[#6576](https://github.com/opencast/opencast/pull/6576)] -
  Handle workflows without creator
- [[#6575](https://github.com/opencast/opencast/pull/6575)] -
  Add asset manager endpoint to trigger event index update
- [[#6632](https://github.com/opencast/opencast/pull/6632)] -
  Add isOnline to admin ui service endpoint
- [[#6631](https://github.com/opencast/opencast/pull/6631)] -
  Remove additional mailing list references
- [[#6581](https://github.com/opencast/opencast/pull/6581)] -
  Set sender value in sent emails if set\*
- [[#6621](https://github.com/opencast/opencast/pull/6621)] -
  Properly handle Tobira harvest of events with streaming-only tracks\*
- [[#6431](https://github.com/opencast/opencast/pull/6431)] -
  Avoid NPEs during wowza retraction\*
- [[#6549](https://github.com/opencast/opencast/pull/6549)] -
  Don't fail removal on already deleted workflow
- [[#6548](https://github.com/opencast/opencast/pull/6548)] -
  Log Publications Blocking Event Deletion
- [[#6547](https://github.com/opencast/opencast/pull/6547)] -
  Log ID of event failing in scheduler index rebuild
- [[#6550](https://github.com/opencast/opencast/pull/6550)] -
  Fix use of bitwise instead of logical operator
- [[#6556](https://github.com/opencast/opencast/pull/6556)] -
  Update admin interface to 2025-03-20
- [[#6563](https://github.com/opencast/opencast/pull/6563)] -
  Document OpenSearch analysis-icu plugin install in Debs
- [[#6559](https://github.com/opencast/opencast/pull/6559)] -
  Document OpenSearch plugin upgrade when using RPMs
- [[#6558](https://github.com/opencast/opencast/pull/6558)] -
  OpenSearch, not Opensearch
- [[#6557](https://github.com/opencast/opencast/pull/6557)] -
  Improve upgrade documentation

## Opencast 17.2 (2025-03-20)

- [[#6528](https://github.com/opencast/opencast/pull/6528)] -
  Revert org.apache.santuario upgrade
- [[#6542](https://github.com/opencast/opencast/pull/6542)] -
  Don't fail theme index rebuild if user does not exist
- [[#6537](https://github.com/opencast/opencast/pull/6537)] -
  Fix stt attach operation
- [[#6530](https://github.com/opencast/opencast/pull/6530)] -
  Fix circular reference
- [[#6321](https://github.com/opencast/opencast/pull/6321)] -
  Adds sorting parameter to series ltitools
- [[#6501](https://github.com/opencast/opencast/pull/6501)] -
  Respect order of sort parameters for search queries
- [[#6486](https://github.com/opencast/opencast/pull/6486)] -
  Fix endpoint /ingest/addPartialTrack
- [[#6487](https://github.com/opencast/opencast/pull/6487)] -
  GraphQL bug fixes and chore
- [[#6499](https://github.com/opencast/opencast/pull/6499)] -
  Use system user for repopulate search index\*
- [[#6492](https://github.com/opencast/opencast/pull/6492)] -
  Check acl against search entity
- [[#6517](https://github.com/opencast/opencast/pull/6517)] -
  Fix uncleaned merge conflict\*
- [[#6440](https://github.com/opencast/opencast/pull/6440)] -
  Fix outright javadoc errors
- [[#6491](https://github.com/opencast/opencast/pull/6491)] -
  Wait for OpenSearch cluster state yellow or green
- [[#6473](https://github.com/opencast/opencast/pull/6473)] -
  Update maven version used by maven wrapper
- [[#6426](https://github.com/opencast/opencast/pull/6426)] -
  Fixing SFA pattern for search and livestream\*
- [[#6484](https://github.com/opencast/opencast/pull/6484)] -
  Replace Ubuntu 20.04 with 22.04 for GHA work\*
- [[#6490](https://github.com/opencast/opencast/pull/6490)] -
  Fix search index rebuild\*
- [[#6434](https://github.com/opencast/opencast/pull/6434)] -
  Handle Opencast starting up before index is available\*
- [[#6438](https://github.com/opencast/opencast/pull/6438)] -
  Fix manual trigger in create release workflow
- [[#6430](https://github.com/opencast/opencast/pull/6430)] -
  Set wf variables for silent tracks in silence detection WOH
- [[#6461](https://github.com/opencast/opencast/pull/6461)] -
  Allow sorting events by UID
- [[#6439](https://github.com/opencast/opencast/pull/6439)] -
  Update karaf 4.4.7

## Opencast 17.1 (2025-01-23)

- [[#6432](https://github.com/opencast/opencast/pull/6432)] -
-  Update to admin interface release 2025-01-21
- [[#6429](https://github.com/opencast/opencast/pull/6429)] -
  Fix OCR transcriptions in Paella player\*
- [[#6362](https://github.com/opencast/opencast/pull/6362)] -
  Add "Show for new or existing" attribute to asset upload options
- [[#6388](https://github.com/opencast/opencast/pull/6388)] -
  Update editor to version 2025-01-08
- [[#6385](https://github.com/opencast/opencast/pull/6385)] -
  Allow live events to be published by the CA user\*
- [[#6379](https://github.com/opencast/opencast/pull/6379)] -
  Fix security config to actually make `/tobira/version` public
- [[#6384](https://github.com/opencast/opencast/pull/6384)] -
  Drop Paella Dependabot Batcher
- [[#6419](https://github.com/opencast/opencast/pull/6419)] -
  Remove superfluous slash in endpoint URL when loading series from Engage UI
- [[#6363](https://github.com/opencast/opencast/pull/6363)] -
  Fix partial retract streaming elements\*
- [[#6367](https://github.com/opencast/opencast/pull/6367)] -
  Segment preview image URL fixed for Tobira\*
- [[#6233](https://github.com/opencast/opencast/pull/6233)] -
  Handle content id being null for playlist entries\*
- [[#6381](https://github.com/opencast/opencast/pull/6381)] -
  Search Service Index Rebuild fails when done on the presentation node\*
- [[#6415](https://github.com/opencast/opencast/pull/6415)] -
  Document OpenSearch plugin requirements
- [[#6364](https://github.com/opencast/opencast/pull/6364)] -
  Refactor isAuthorized utility function
- [[#6241](https://github.com/opencast/opencast/pull/6241)] -
  Editor: Unify endpoints for saving
- [[#6390](https://github.com/opencast/opencast/pull/6390)] -
  Fix jQuery mime type
- [[#6328](https://github.com/opencast/opencast/pull/6328)] -
  Fix incorrect episode count in LTI series tool\*
- [[#6361](https://github.com/opencast/opencast/pull/6361)] -
  Allow removing subtitles
- [[#6317](https://github.com/opencast/opencast/pull/6317)] -
  Add tobira series endpoint to remove paths

## Opencast 17.0 (2024-12-04)

- [[#6309](https://github.com/opencast/opencast/pull/6309)] -
  Remove duplicate dependency declaration
- [[#6307](https://github.com/opencast/opencast/pull/6307)] -
  Use legacy rest-docs per default
- [[#6261](https://github.com/opencast/opencast/pull/6261)] -
  Fix restdocs
- [[#6248](https://github.com/opencast/opencast/pull/6248)] -
  Auto-encode audio track for Whisper
- [[#6247](https://github.com/opencast/opencast/pull/6247)] -
  Allow generating subtitles in the background
- [[#6239](https://github.com/opencast/opencast/pull/6239)] -
  Make developers explain why they target legacy
- [[#6235](https://github.com/opencast/opencast/pull/6235)] -
  Remove mentions of the old admin interface
- [[#6234](https://github.com/opencast/opencast/pull/6234)] -
  Remove unnecessary login redirect
- [[#6188](https://github.com/opencast/opencast/pull/6188)] -
  Ask developers to provide test configuration for pull requests
- [[#6187](https://github.com/opencast/opencast/pull/6187)] -
  Fix integration tests, distribution upload and demo server update
- [[#6178](https://github.com/opencast/opencast/pull/6178)] -
  Document Matrix Space
- [[#6144](https://github.com/opencast/opencast/pull/6144)] -
  Add useful videos to documentation
- [[#6140](https://github.com/opencast/opencast/pull/6140)] -
  Add tips for pull request review to documentation
- [[#6098](https://github.com/opencast/opencast/pull/6098)] -
  Replace AngularJS based login page
- [[#6097](https://github.com/opencast/opencast/pull/6097)] -
  Code readability improvements
- [[#6091](https://github.com/opencast/opencast/pull/6091)] -
  Add more admin-UI Tobira endpoints
- [[#6087](https://github.com/opencast/opencast/pull/6087)] -
  Stop generating feed previews
- [[#6059](https://github.com/opencast/opencast/pull/6059)] -
  Auto-generate list of committers
- [[#6054](https://github.com/opencast/opencast/pull/6054)] -
  Add me (LukasKalbertodt) to committers list in docs
- [[#6052](https://github.com/opencast/opencast/pull/6052)] -
  paella: add preview portrait image. Fix #5917
- [[#6051](https://github.com/opencast/opencast/pull/6051)] -
  Release Schedule for OC 17
- [[#6049](https://github.com/opencast/opencast/pull/6049)] -
  Paella fix: Track captions have priority over attachments captions.
- [[#6048](https://github.com/opencast/opencast/pull/6048)] -
  Remove contributors from main pom.xml
- [[#6039](https://github.com/opencast/opencast/pull/6039)] -
  Update themes default
- [[#6038](https://github.com/opencast/opencast/pull/6038)] -
  Remove unnecessary tag operation from fast workflow
- [[#6028](https://github.com/opencast/opencast/pull/6028)] -
  Add waveform option filter mode
- [[#6023](https://github.com/opencast/opencast/pull/6023)] -
  Adding a Filter to filter Events according to their is_published state
- [[#6019](https://github.com/opencast/opencast/pull/6019)] -
  Prevent NullPointerException in ACL parser
- [[#6018](https://github.com/opencast/opencast/pull/6018)] -
  Use JSON instead of XACML for ACL templates
- [[#6014](https://github.com/opencast/opencast/pull/6014)] -
  Replace aai spring junit class runner
- [[#6010](https://github.com/opencast/opencast/pull/6010)] -
  Feature request: terms of use for new user
- [[#5995](https://github.com/opencast/opencast/pull/5995)] -
  Logging and other nitpicks
- [[#5965](https://github.com/opencast/opencast/pull/5965)] -
  Remove old admin interface
- [[#5959](https://github.com/opencast/opencast/pull/5959)] -
  Fix log format strings
- [[#5958](https://github.com/opencast/opencast/pull/5958)] -
  Remove redundant `groupId` declarations
- [[#5949](https://github.com/opencast/opencast/pull/5949)] -
  Fix sporadic port binding errors in tests
- [[#5946](https://github.com/opencast/opencast/pull/5946)] -
  LTI translation strings
- [[#5940](https://github.com/opencast/opencast/pull/5940)] -
  Fix paella dev server
- [[#5876](https://github.com/opencast/opencast/pull/5876)] -
  Microsoft Azure transcription service refactoring
- [[#5875](https://github.com/opencast/opencast/pull/5875)] -
  Add changelog note about Tobira harvest API
- [[#5872](https://github.com/opencast/opencast/pull/5872)] -
  Remove duplicates in .gitignore
- [[#5871](https://github.com/opencast/opencast/pull/5871)] -
  Fix 500 error in workflow service REST endpoint "update" (fixes #5870)
- [[#5869](https://github.com/opencast/opencast/pull/5869)] -
  Fix for event state when workflow is paused when rebuilding ES index (fixes #5868)
- [[#5865](https://github.com/opencast/opencast/pull/5865)] -
  Remove obsolete `version` field from `docker-compose` files
- [[#5862](https://github.com/opencast/opencast/pull/5862)] -
  Remove prepared flavor
- [[#5855](https://github.com/opencast/opencast/pull/5855)] -
  Fixed bug: tag-engage workflow failes when event is not published
- [[#5831](https://github.com/opencast/opencast/pull/5831)] -
  Update ESLint
- [[#5830](https://github.com/opencast/opencast/pull/5830)] -
  ESLint nitpicks in the `lti` module
- [[#5766](https://github.com/opencast/opencast/pull/5766)] -
  GraphQL API - Technology Preview
- [[#5763](https://github.com/opencast/opencast/pull/5763)] -
  Bump java version to 17
- [[#5759](https://github.com/opencast/opencast/pull/5759)] -
  Add SERIES.WRITE_ONLY list provider
- [[#5758](https://github.com/opencast/opencast/pull/5758)] -
  Move multiple series title check logic
- [[#5668](https://github.com/opencast/opencast/pull/5668)] -
  Refactor jaxrs and OpenAPI support
- [[#5413](https://github.com/opencast/opencast/pull/5413)] -
  Add search index sort multi field
- [[#5056](https://github.com/opencast/opencast/pull/5056)] -
  Allow episode ID based access control via roles

<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/6291">6291</a>] -
  Build(deps): bump cookie and express in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6290">6290</a>] -
  Build(deps-dev): bump webpack from 5.95.0 to 5.96.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6289">6289</a>] -
  Build(deps): bump globals from 15.10.0 to 15.11.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6288">6288</a>] -
  Build(deps): bump @eslint/js from 9.12.0 to 9.13.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6285">6285</a>] -
  Build(deps): bump org.apache.maven.plugins:maven-plugin-plugin from 3.11.0 to 3.15.1 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6282">6282</a>] -
  Build(deps): bump org.apache.maven.plugins:maven-source-plugin from 3.3.0 to 3.3.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6281">6281</a>] -
  Build(deps): bump org.owasp.esapi:esapi from 2.5.3.1 to 2.5.5.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6279">6279</a>] -
  Build(deps-dev): bump @babel/preset-env from 7.25.7 to 7.26.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6277">6277</a>] -
  Build(deps-dev): bump eslint from 9.12.0 to 9.13.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6276">6276</a>] -
  Build(deps-dev): bump html-validate from 8.24.1 to 8.24.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6275">6275</a>] -
  Build(deps-dev): bump @babel/eslint-parser from 7.25.7 to 7.25.9 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6274">6274</a>] -
  Build(deps-dev): bump @playwright/test from 1.47.2 to 1.48.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6273">6273</a>] -
  Build(deps-dev): bump express from 4.21.0 to 4.21.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6272">6272</a>] -
  Build(deps-dev): bump @types/jest from 29.5.13 to 29.5.14 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6271">6271</a>] -
  Build(deps): bump i18next from 23.15.2 to 23.16.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6269">6269</a>] -
  Build(deps): bump react-select from 5.8.1 to 5.8.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6267">6267</a>] -
  Build(deps-dev): bump @types/react-dom from 18.3.0 to 18.3.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6266">6266</a>] -
  Build(deps): bump react-i18next from 15.0.2 to 15.1.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6265">6265</a>] -
  Build(deps-dev): bump @types/react from 18.3.11 to 18.3.12 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6264">6264</a>] -
  Build(deps): bump iframe-resizer from 5.3.1 to 5.3.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6263">6263</a>] -
  Build(deps-dev): bump @types/node from 22.7.4 to 22.8.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6258">6258</a>] -
  Build(deps): bump http-proxy-middleware from 2.0.6 to 2.0.7 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6240">6240</a>] -
  Build(deps): bump org.eclipse.jetty:jetty-server from 9.4.52.v20230823 to 9.4.55.v20240627 in /modules/rest-test-environment</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6230">6230</a>] -
  Build(deps): bump org.checkerframework:checker-qual from 3.33.0 to 3.48.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6229">6229</a>] -
  Build(deps-dev): bump @babel/eslint-parser from 7.25.1 to 7.25.7 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6228">6228</a>] -
  Build(deps-dev): bump @babel/core from 7.25.2 to 7.25.7 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6227">6227</a>] -
  Build(deps-dev): bump html-validate from 8.24.0 to 8.24.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6226">6226</a>] -
  Build(deps): bump paella-core from 1.49.5 to 1.49.7 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6225">6225</a>] -
  Build(deps): bump eclipselink.version from 2.7.14 to 2.7.15 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6224">6224</a>] -
  Build(deps-dev): bump eslint from 9.11.1 to 9.12.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6223">6223</a>] -
  Build(deps-dev): bump @types/react from 18.3.5 to 18.3.11 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6222">6222</a>] -
  Build(deps): bump eslint-plugin-headers from 1.1.2 to 1.2.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6221">6221</a>] -
  Build(deps-dev): bump @babel/preset-env from 7.24.5 to 7.25.7 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6220">6220</a>] -
  Build(deps): bump @eslint/js from 9.11.1 to 9.12.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6219">6219</a>] -
  Build(deps): bump i18next from 23.15.1 to 23.15.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6218">6218</a>] -
  Build(deps): bump globals from 15.9.0 to 15.10.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6212">6212</a>] -
  Bump eclipselink.version from 2.7.14 to 2.7.15 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6210">6210</a>] -
  Bump org.apache.servicemix.bundles:org.apache.servicemix.bundles.xalan from 2.7.2_3 to 2.7.3_3 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6206">6206</a>] -
  Bump webpack-dev-server from 5.0.4 to 5.1.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6205">6205</a>] -
  Bump babel-loader from 9.1.3 to 9.2.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6204">6204</a>] -
  Bump html-validate from 8.21.0 to 8.24.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6203">6203</a>] -
  Bump eslint from 9.10.0 to 9.11.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6202">6202</a>] -
  Bump @playwright/test from 1.46.1 to 1.47.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6200">6200</a>] -
  Bump i18next from 23.14.0 to 23.15.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6199">6199</a>] -
  Bump @eslint/js from 9.10.0 to 9.11.1 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6198">6198</a>] -
  Bump react-i18next from 15.0.1 to 15.0.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6197">6197</a>] -
  Bump iframe-resizer from 5.3.0 to 5.3.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6195">6195</a>] -
  Bump @types/jest from 29.5.12 to 29.5.13 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6194">6194</a>] -
  Bump @types/node from 22.5.4 to 22.7.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6193">6193</a>] -
  Bump react-bootstrap from 2.10.4 to 2.10.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6192">6192</a>] -
  Bump react-select from 5.8.0 to 5.8.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6190">6190</a>] -
  Bump markdownlint-cli from 0.41.0 to 0.42.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6182">6182</a>] -
  Bump webpack from 5.93.0 to 5.95.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6180">6180</a>] -
  Bump rollup from 2.75.7 to 2.79.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6167">6167</a>] -
  Bump @eslint/js from 9.8.0 to 9.10.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6166">6166</a>] -
  Bump @types/node from 22.0.2 to 22.5.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6164">6164</a>] -
  Bump path-to-regexp and express in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6165">6165</a>] -
  Bump serve-static and express in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6162">6162</a>] -
  Bump body-parser and express in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6163">6163</a>] -
  Bump send and express in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6159">6159</a>] -
  Bump serve-static and express in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6158">6158</a>] -
  Bump eslint from 9.4.0 to 9.10.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6154">6154</a>] -
  Bump iframe-resizer from 4.4.5 to 5.3.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6137">6137</a>] -
  Bump org.slf4j:slf4j-reload4j from 1.7.36 to 2.0.16 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6135">6135</a>] -
  Bump org.apache.maven.plugins:maven-project-info-reports-plugin from 3.4.3 to 3.7.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6134">6134</a>] -
  Bump org.apache.maven.plugins:maven-pmd-plugin from 3.21.2 to 3.25.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6133">6133</a>] -
  Bump axios from 1.7.2 to 1.7.7 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6132">6132</a>] -
  Bump i18next from 23.12.2 to 23.14.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6129">6129</a>] -
  Bump react-i18next from 15.0.0 to 15.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6128">6128</a>] -
  Bump @types/react from 18.3.3 to 18.3.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6126">6126</a>] -
  Bump @babel/eslint-parser from 7.24.6 to 7.25.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6125">6125</a>] -
  Bump html-validate from 8.18.2 to 8.21.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6124">6124</a>] -
  Bump paella-slide-plugins from 1.48.0 to 1.48.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6123">6123</a>] -
  Bump @playwright/test from 1.44.1 to 1.46.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6120">6120</a>] -
  Bump globals from 15.8.0 to 15.9.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6118">6118</a>] -
  Bump org.slf4j:slf4j-api from 1.7.36 to 2.0.16 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6117">6117</a>] -
  Bump com.google.guava:guava from 32.1.3-jre to 33.3.0-jre in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6115">6115</a>] -
  Bump org.codehaus.mojo:build-helper-maven-plugin from 3.5.0 to 3.6.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6113">6113</a>] -
  Bump webpack from 5.76.1 to 5.94.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6110">6110</a>] -
  Bump micromatch from 4.0.5 to 4.0.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6078">6078</a>] -
  Bump underscore from 1.13.6 to 1.13.7 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6077">6077</a>] -
  Bump i18next from 23.11.5 to 23.12.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6075">6075</a>] -
  Bump paella-core from 1.48.2 to 1.49.5 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6074">6074</a>] -
  Bump @fortawesome/free-solid-svg-icons from 6.5.2 to 6.6.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6072">6072</a>] -
  Bump @types/node from 20.14.9 to 22.0.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6071">6071</a>] -
  Bump iframe-resizer from 4.4.2 to 4.4.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6070">6070</a>] -
  Bump react-i18next from 14.1.2 to 15.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6069">6069</a>] -
  Bump @fortawesome/fontawesome-svg-core from 6.5.2 to 6.6.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6068">6068</a>] -
  Bump @babel/core from 7.24.5 to 7.25.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6067">6067</a>] -
  Bump paella-basic-plugins from 1.44.7 to 1.44.10 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6066">6066</a>] -
  Bump paella-user-tracking from 1.42.2 to 1.42.5 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6065">6065</a>] -
  Bump webpack from 5.91.0 to 5.93.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6063">6063</a>] -
  Bump @eslint/js from 9.6.0 to 9.8.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6062">6062</a>] -
  Bump globals from 15.7.0 to 15.8.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5987">5987</a>] -
  Bump react-bootstrap from 2.10.2 to 2.10.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5986">5986</a>] -
  Bump @types/node from 20.13.0 to 20.14.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5979">5979</a>] -
  Bump globals from 15.3.0 to 15.7.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5978">5978</a>] -
  Bump @eslint/js from 9.4.0 to 9.6.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5930">5930</a>] -
  Bump braces from 3.0.2 to 3.0.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5928">5928</a>] -
  Bump braces from 3.0.2 to 3.0.3 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5906">5906</a>] -
  Bump globals from 15.1.0 to 15.3.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5905">5905</a>] -
  Bump @eslint/js from 9.2.0 to 9.4.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5902">5902</a>] -
  Bump css-loader from 6.11.0 to 7.1.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5900">5900</a>] -
  Bump eslint from 9.2.0 to 9.4.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5899">5899</a>] -
  Bump @playwright/test from 1.43.1 to 1.44.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5896">5896</a>] -
  Bump @babel/eslint-parser from 7.24.5 to 7.24.6 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5894">5894</a>] -
  Bump markdownlint-cli from 0.40.0 to 0.41.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5893">5893</a>] -
  Bump sass from 1.76.0 to 1.77.4 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5892">5892</a>] -
  Bump chromedriver from 124.0.1 to 125.0.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5891">5891</a>] -
  Bump eslint from 9.2.0 to 9.4.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5890">5890</a>] -
  Bump html-validate from 8.18.2 to 8.19.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5889">5889</a>] -
  Bump iframe-resizer from 4.3.11 to 4.4.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5888">5888</a>] -
  Bump @fortawesome/react-fontawesome from 0.2.0 to 0.2.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5887">5887</a>] -
  Bump axios from 1.6.8 to 1.7.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5886">5886</a>] -
  Bump i18next from 23.11.3 to 23.11.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5885">5885</a>] -
  Bump react-i18next from 14.1.1 to 14.1.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5884">5884</a>] -
  Bump @types/node from 20.12.7 to 20.13.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5883">5883</a>] -
  Bump @types/react from 18.3.1 to 18.3.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5846">5846</a>] -
  Bump i18next-browser-languagedetector from 7.2.1 to 8.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5826">5826</a>] -
  Bump copy-webpack-plugin from 11.0.0 to 12.0.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5822">5822</a>] -
  Bump style-loader from 3.3.4 to 4.0.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5795">5795</a>] -
  Bump org.codehaus.plexus:plexus-utils from 3.3.0 to 4.0.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5704">5704</a>] -
  Bump joda-time:joda-time from 2.12.5 to 2.12.7 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5699">5699</a>] -
  Bump org.apache.maven.plugins:maven-assembly-plugin from 3.6.0 to 3.7.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5629">5629</a>] -
  Bump org.apache.santuario:xmlsec from 2.2.6 to 4.0.2 in /modules/db</li>
</ul>
</details>
