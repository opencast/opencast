Opencast 15
-----------

### Opencast 15.13

- [[#6306](https://github.com/opencast/opencast/pull/6306)] -
  Fixes session timeout bug by upgrading pax-web
- [[#6242](https://github.com/opencast/opencast/pull/6242)] -
  Backport "Attach Amberscript transcriptions as tracks"

### Opencast 15.12

- [[#6296](https://github.com/opencast/opencast/pull/6296)] -
  Upgrade upload-artifacts from v3 to v4
- [[#6256](https://github.com/opencast/opencast/pull/6256)] -
  Refix the CLA check
- [[#6156](https://github.com/opencast/opencast/pull/6156)] -
  Fix non-themed subtitle publications

### Opencast 15.11

- [[#6238](https://github.com/opencast/opencast/pull/6238)] -
  Fix ICLA Check
- [[#6157](https://github.com/opencast/opencast/pull/6157)] -
  Fix tobira harvest acls

### Opencast 15.10

- [[#6106](https://github.com/opencast/opencast/pull/6106)] -
  Remove old database update test
- [[#6083](https://github.com/opencast/opencast/pull/6083)] -
  Always send adopter registration statistics in rest api
- [[#5587](https://github.com/opencast/opencast/pull/5587)] -
  Fix ltitools Upload crashing if extendedMetadata exists

### Opencast 15.9

- [[#6104](https://github.com/opencast/opencast/pull/6104)] -
  Fix broken ingest
- [[#6081](https://github.com/opencast/opencast/pull/6081)] -
  Fix indentation
- [[#6095](https://github.com/opencast/opencast/pull/6095)] -
  Remove references to JIRA in feedback URL docs

### Opencast 15.8

- [[#6061](https://github.com/opencast/opencast/pull/6061)] -
  Cleanup everything
- [[#6046](https://github.com/opencast/opencast/pull/6046)] -
  Don't duplicate captions during publication
- [[#6044](https://github.com/opencast/opencast/pull/6044)] -
  Check org of role provider when adding roles
- [[#6037](https://github.com/opencast/opencast/pull/6037)] -
  Make in-place text substitution in the docs more portable
- [[#6030](https://github.com/opencast/opencast/pull/6030)] -
  Downgrade Editor to 2024-06-11
- [[#6017](https://github.com/opencast/opencast/pull/6017)] -
  Fix docs of `publish-engage` WOHs
- [[#6012](https://github.com/opencast/opencast/pull/6012)] -
  Use current docker images for development
- [[#6008](https://github.com/opencast/opencast/pull/6008)] -
  Add basic date validation check to ingest/addMediaPackage/{wfId}
- [[#5913](https://github.com/opencast/opencast/pull/5913)] -
  Add new Workflow Operation: Subtitle Timeshift

### Opencast 15.7

- [[#6022](https://github.com/opencast/opencast/pull/6022)] -
  Update admin interface to 2024-07-12
- [[#6021](https://github.com/opencast/opencast/pull/6021)] -
  Update Editor to 2024-07-12
- [[#6004](https://github.com/opencast/opencast/pull/6004)] -
  First steps towards java 21
- [[#6003](https://github.com/opencast/opencast/pull/6003)] -
  Update maven and maven wrapper
- [[#5996](https://github.com/opencast/opencast/pull/5996)] -
  State mapping for fast workflow
- [[#5994](https://github.com/opencast/opencast/pull/5994)] -
  Fix sporadic port binding errors in tests
- [[#5970](https://github.com/opencast/opencast/pull/5970)] -
  Display state mappings in the status column again
- [[#5956](https://github.com/opencast/opencast/pull/5956)] -
  Fix publication icon paths for new admin ui
- [[#5950](https://github.com/opencast/opencast/pull/5950)] -
  Fix incorrect language parameter for Whisper
- [[#5938](https://github.com/opencast/opencast/pull/5938)] -
  Update documentation for enabling LMS user providers
- [[#5933](https://github.com/opencast/opencast/pull/5933)] -
  Update Studio to 2024-06-12
- [[#5932](https://github.com/opencast/opencast/pull/5932)] -
  Fix link in editor config file
- [[#5931](https://github.com/opencast/opencast/pull/5931)] -
  Add note about editor releases to release notes
- [[#5864](https://github.com/opencast/opencast/pull/5864)] -
  Update HTTPS migration guide to be more general & use standard SQL

### Opencast 15.6

- [[#5922](https://github.com/opencast/opencast/pull/5922)] -
  Update Editor to 2024-06-11
- [[#5880](https://github.com/opencast/opencast/pull/5880)] -
  Handle lang tag by asset upload correctly
- [[#5879](https://github.com/opencast/opencast/pull/5879)] -
  Skip subtitle generation from tracks without audio stream
- [[#5873](https://github.com/opencast/opencast/pull/5873)] -
  Return dates as UNIX timestamp
- [[#5860](https://github.com/opencast/opencast/pull/5860)] -
  Fix missing configuration panel for fast.yaml workflow
- [[#5858](https://github.com/opencast/opencast/pull/5858)] -
  Change segment `startTime` from mpeg7 to milliseconds in Tobira API
- [[#5857](https://github.com/opencast/opencast/pull/5857)] -
  Improve Assetmanager multi store behavior
- [[#5832](https://github.com/opencast/opencast/pull/5832)] -
  Log Excessively Large Jobs Status
- [[#5829](https://github.com/opencast/opencast/pull/5829)] -
  Ignore subtitle tracks in select-tracks WHO

### Opencast 15.5

- [[#5757](https://github.com/opencast/opencast/pull/5757)] -
  Add slide text and segments to Tobira harvest API
- [[#5685](https://github.com/opencast/opencast/pull/5685)] -
  Paella 7 mp4 quality selector

### Opencast 15.4

- [[#5738](https://github.com/opencast/opencast/pull/5738)] -
  Update (New) Admin UI to 2024-04-10
- [[#5696](https://github.com/opencast/opencast/pull/5696)] -
  Mark RPMs as available
- [[#5689](https://github.com/opencast/opencast/pull/5689)] -
  Process-smil WOH tag-with-profile configuration does not work depending on the encoding profile suffix configured
- [[#5686](https://github.com/opencast/opencast/pull/5686)] -
  Fix editor track selection for updating tags
- [[#5678](https://github.com/opencast/opencast/pull/5678)] -
  Remove Spurious Warnings During Build
- [[#5677](https://github.com/opencast/opencast/pull/5677)] -
  Fix more JavaDoc
- [[#5675](https://github.com/opencast/opencast/pull/5675)] -
  Add synchronous deletion to Search API
- [[#5673](https://github.com/opencast/opencast/pull/5673)] -
  Paella7: Avoid opening downloaded video
- [[#5670](https://github.com/opencast/opencast/pull/5670)] -
  Make a dynamic OSGi dependency static
- [[#5669](https://github.com/opencast/opencast/pull/5669)] -
  Update temporal if empty
- [[#5588](https://github.com/opencast/opencast/pull/5588)] -
  Update typescript type definitions in ltitools

### Opencast 15.3

- [[#5669](https://github.com/opencast/opencast/pull/5669)] -
  Update temporal if empty
- [[#5663](https://github.com/opencast/opencast/pull/5663)] -
  `publish-engage` nitpicks
- [[#5630](https://github.com/opencast/opencast/pull/5630)] -
  Add jersey to engage-ui build
- [[#5618](https://github.com/opencast/opencast/pull/5618)] -
  Update chromedriver
- [[#5611](https://github.com/opencast/opencast/pull/5611)] -
  Update maven bundle configuration
- [[#5596](https://github.com/opencast/opencast/pull/5596)] -
  Update build plugins
- [[#5588](https://github.com/opencast/opencast/pull/5588)] -
  Update typescript type definitions in ltitools
- [[#5586](https://github.com/opencast/opencast/pull/5586)] -
  Update axios from 0 to 1 in ltitools
- [[#5543](https://github.com/opencast/opencast/pull/5543)] -
  Use edited thumbnails in partial-publish.xml
- [[#5487](https://github.com/opencast/opencast/pull/5487)] -
  Add maven wrapper
- [[#5339](https://github.com/opencast/opencast/pull/5339)] -
  #5663 Paella7 patches frameList undefined exception
- [[#4973](https://github.com/opencast/opencast/pull/4973)] -
  Add WhisperC++ engine to speech-to-text-impl

### Opencast 15.2

- [[#5556](https://github.com/opencast/opencast/pull/5556)] -
  Don't create composites for new editor
- [[#5532](https://github.com/opencast/opencast/pull/5532)] -
  Paella 7: Download audio trancripts (without timestamps)
- [[#5530](https://github.com/opencast/opencast/pull/5530)] -
  Don't log internal AmberScript service state
- [[#5529](https://github.com/opencast/opencast/pull/5529)] -
  Avoid unnecessary FFmpeg logs
- [[#5528](https://github.com/opencast/opencast/pull/5528)] -
  Editor shouldn't just overwrite existing files

### Opencast 15.1

- [[#5546](https://github.com/opencast/opencast/pull/5546)] -
  Update (New) Admin UI to 2024-01-17
- [[#5540](https://github.com/opencast/opencast/pull/5540)] -
  Update xmlsec version (CAS fix)
- [[#5539](https://github.com/opencast/opencast/pull/5539)] -
  Fix Paella Player 7 for single stream videos
- [[#5493](https://github.com/opencast/opencast/pull/5493)] -
  Fix broken link in markdown documentation
- [[#5491](https://github.com/opencast/opencast/pull/5491)] -
  Paella7: Add support for text/vtt captions in DownloadsPlugin
- [[#5488](https://github.com/opencast/opencast/pull/5488)] -
  Fix Paella 7 with no segments

### Opencast 15.0

*Released on December 13th, 2023*

- [[#5429](https://github.com/opencast/opencast/pull/5429)] -
  Update Landing Page Libraries
- [[#5428](https://github.com/opencast/opencast/pull/5428)] -
  Fix Docs Landing Page
- [[#5427](https://github.com/opencast/opencast/pull/5427)] -
  Update Admin Interface to 2023-11-21
- [[#5426](https://github.com/opencast/opencast/pull/5426)] -
  Update integrated OC Studio to 2023-11-20
- [[#5424](https://github.com/opencast/opencast/pull/5424)] -
  Log cached FFmpeg files
- [[#5423](https://github.com/opencast/opencast/pull/5423)] -
  It's "YouTube," not "Youtube"
- [[#5422](https://github.com/opencast/opencast/pull/5422)] -
  (Re-)Enable the JWT feature by default
- [[#5421](https://github.com/opencast/opencast/pull/5421)] -
  Remove duplicate publication channel configuration
- [[#5419](https://github.com/opencast/opencast/pull/5419)] -
  Set publication variables in analyze-MP
- [[#5418](https://github.com/opencast/opencast/pull/5418)] -
  Update Studio to 2023-11-08
- [[#5417](https://github.com/opencast/opencast/pull/5417)] -
  Fix broken links in documentation
- [[#5415](https://github.com/opencast/opencast/pull/5415)] -
  Paella 7: Default skin loaded from paella-skins repository
- [[#5414](https://github.com/opencast/opencast/pull/5414)] -
  Update the default admin interface
- [[#5409](https://github.com/opencast/opencast/pull/5409)] -
  Don't Drop Tags in Editor WOH
- [[#5405](https://github.com/opencast/opencast/pull/5405)] -
  Fail on broken configuration
- [[#5369](https://github.com/opencast/opencast/pull/5369)] -
  Add calendar link, and sysadmin meeting to docs.opencast.org
- [[#5368](https://github.com/opencast/opencast/pull/5368)] -
  add chevron to `<details>` in docs
- [[#5367](https://github.com/opencast/opencast/pull/5367)] -
  Re-add details css for docs (reverts part of PR 5359)
- [[#5360](https://github.com/opencast/opencast/pull/5360)] -
  Documentation: Move "Modules" into "Configuration"
- [[#5359](https://github.com/opencast/opencast/pull/5359)] -
  Docs: Hide bot PRs in changelog
- [[#5353](https://github.com/opencast/opencast/pull/5353)] -
  Remove metadata layer from preview image (presenter)
- [[#5352](https://github.com/opencast/opencast/pull/5352)] -
  Set generator tags in speech-to-text WOH
- [[#5351](https://github.com/opencast/opencast/pull/5351)] -
  Return ISO 639 language code for language detected by Whisper
- [[#5340](https://github.com/opencast/opencast/pull/5340)] -
  Update Editor to 2023-10-17
- [[#5318](https://github.com/opencast/opencast/pull/5318)] -
  Update integrated Studio to 2023-10-10
- [[#5314](https://github.com/opencast/opencast/pull/5314)] -
  Do not nest GH Artifacts
- [[#5311](https://github.com/opencast/opencast/pull/5311)] -
  Remove Internal Logger
- [[#5307](https://github.com/opencast/opencast/pull/5307)] -
  Modified dispatch.interval in Job Dispatcher to allow float values
- [[#5306](https://github.com/opencast/opencast/pull/5306)] -
  Fix Crowdin Package Name
- [[#5305](https://github.com/opencast/opencast/pull/5305)] -
  Fix Headless Firefox in Selenium Tests
- [[#5302](https://github.com/opencast/opencast/pull/5302)] -
  Auto-Update GitHub Actions
- [[#5301](https://github.com/opencast/opencast/pull/5301)] -
  Replace internal logger where possible
- [[#5271](https://github.com/opencast/opencast/pull/5271)] -
  Fix Source Code headers
- [[#5256](https://github.com/opencast/opencast/pull/5256)] -
  Fix Translation Updates
- [[#5249](https://github.com/opencast/opencast/pull/5249)] -
  Prune Committer List
- [[#5248](https://github.com/opencast/opencast/pull/5248)] -
  Extend Asset Upload Workflow for Captions
- [[#5247](https://github.com/opencast/opencast/pull/5247)] -
  Don't take snapshot on asset upload before workflow
- [[#5245](https://github.com/opencast/opencast/pull/5245)] -
  Paella 7 download plugin
- [[#5233](https://github.com/opencast/opencast/pull/5233)] -
  Drop DeleteSnapshot Event Handler
- [[#5183](https://github.com/opencast/opencast/pull/5183)] -
  Change default karaf shutdown command
- [[#5152](https://github.com/opencast/opencast/pull/5152)] -
  Remove a quick-fix that's made obsolete by a Kraraf upgrade
- [[#5128](https://github.com/opencast/opencast/pull/5128)] -
  Fix bullet points in Developer Docs
- [[#5126](https://github.com/opencast/opencast/pull/5126)] -
  Improves Dev docs
- [[#5125](https://github.com/opencast/opencast/pull/5125)] -
  Logging nitpicks
- [[#5121](https://github.com/opencast/opencast/pull/5121)] -
  Paella 7: Add default layout for unknown track flavours and multiple streams
- [[#5120](https://github.com/opencast/opencast/pull/5120)] -
  Alter License Header
- [[#5110](https://github.com/opencast/opencast/pull/5110)] -
  Remove useless settings from `persistence.xml` files
- [[#5100](https://github.com/opencast/opencast/pull/5100)] -
  docs: Fix NGINX configuration template preventing alias traversals
- [[#5057](https://github.com/opencast/opencast/pull/5057)] -
  Cache FFmpeg Versions
- [[#5046](https://github.com/opencast/opencast/pull/5046)] -
  conditionally encode 1440p in profile.adaptive-parallel.http
- [[#5035](https://github.com/opencast/opencast/pull/5035)] -
  Publish and update maven cache
- [[#5023](https://github.com/opencast/opencast/pull/5023)] -
  Remove Track Selection Restrictions from the Old Editor
- [[#4966](https://github.com/opencast/opencast/pull/4966)] -
  Enable Java 17 support
- [[#4965](https://github.com/opencast/opencast/pull/4965)] -
  Turn Paella 6 into a plugin
- [[#4960](https://github.com/opencast/opencast/pull/4960)] -
  Remove unused method in composer service
- [[#4959](https://github.com/opencast/opencast/pull/4959)] -
  Add subtitle stream support
- [[#4958](https://github.com/opencast/opencast/pull/4958)] -
  Update to actions/checkout@v3
- [[#4957](https://github.com/opencast/opencast/pull/4957)] -
  Update to Node 18
- [[#4956](https://github.com/opencast/opencast/pull/4956)] -
  Prepare Opencast 14.x release notes the right way
- [[#4955](https://github.com/opencast/opencast/pull/4955)] -
  Asset manager multiple storage
- [[#4951](https://github.com/opencast/opencast/pull/4951)] -
  Allow opencast to share the oc-remember-me cookie between nodes
- [[#4844](https://github.com/opencast/opencast/pull/4844)] -
  Add migration notes for subtitles as tracks
- [[#4842](https://github.com/opencast/opencast/pull/4842)] -
  Subtitles as tracks in transcription services
- [[#4828](https://github.com/opencast/opencast/pull/4828)] -
  New developers docs (Part 1: Index)
- [[#4791](https://github.com/opencast/opencast/pull/4791)] -
  fixed faulty regex on analyze-mediapackage
- [[#4711](https://github.com/opencast/opencast/pull/4711)] -
  Fix caption creation in lti for subtitles as tracks
- [[#4670](https://github.com/opencast/opencast/pull/4670)] -
  Add a configuration file for the WebVTTCaptionConverter
- [[#4651](https://github.com/opencast/opencast/pull/4651)] -
  Add studio series dropdown selection
- [[#4627](https://github.com/opencast/opencast/pull/4627)] -
  Add documentation on subtitles
- [[#4626](https://github.com/opencast/opencast/pull/4626)] -
  Refit editor endpoint for subtitles as tracks
- [[#4624](https://github.com/opencast/opencast/pull/4624)] -
  Update tag support for subtitles as tracks in Paella Player
- [[#4623](https://github.com/opencast/opencast/pull/4623)] -
  Add captions as standard upload option in Admin UI
- [[#4622](https://github.com/opencast/opencast/pull/4622)] -
  Treat captions as tracks in standard workflows
- [[#4617](https://github.com/opencast/opencast/pull/4617)] -
  Adds the 'add-force-flavors' config to the publish-engage workflow
- [[#4560](https://github.com/opencast/opencast/pull/4560)] -
  chapter extraction as segmentation
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/5430">5430</a>] -
  Bump paella-core from 1.44.1 to 1.46.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5401">5401</a>] -
  Bump @types/jest from 29.5.6 to 29.5.7 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5400">5400</a>] -
  Bump iframe-resizer from 4.3.6 to 4.3.7 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5399">5399</a>] -
  Bump @types/node from 20.5.7 to 20.8.10 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5398">5398</a>] -
  Bump i18next from 23.4.6 to 23.6.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5397">5397</a>] -
  Bump @types/react-dom from 18.2.12 to 18.2.14 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5396">5396</a>] -
  Bump @types/react-helmet from 6.1.6 to 6.1.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5395">5395</a>] -
  Bump selenium-server-standalone-jar from 4.7.1 to 4.14.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5394">5394</a>] -
  Bump sass from 1.69.1 to 1.69.5 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5393">5393</a>] -
  Bump chromedriver from 117.0.3 to 119.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5392">5392</a>] -
  Bump html-validate from 8.5.0 to 8.7.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5385">5385</a>] -
  Bump html-validate from 8.6.0 to 8.7.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5380">5380</a>] -
  Bump org.osgi:org.osgi.service.component.annotations from 1.5.0 to 1.5.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5379">5379</a>] -
  Bump org.mariadb.jdbc:mariadb-java-client from 3.1.4 to 3.2.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5377">5377</a>] -
  Bump react-bootstrap from 2.8.0 to 2.9.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5375">5375</a>] -
  Bump @types/react-js-pagination from 3.0.4 to 3.0.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5374">5374</a>] -
  Bump @types/jest from 29.5.4 to 29.5.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5373">5373</a>] -
  Bump actions/setup-node from 1 to 4</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5372">5372</a>] -
  Bump react-i18next from 13.2.1 to 13.3.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5357">5357</a>] -
  Bump eslint from 8.50.0 to 8.52.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5356">5356</a>] -
  Bump eslint from 8.51.0 to 8.52.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5355">5355</a>] -
  Bump org.apache.santuario:xmlsec from 2.1.7 to 2.2.6</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5346">5346</a>] -
  Bump @babel/traverse from 7.22.5 to 7.23.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5341">5341</a>] -
  Bump @babel/traverse from 7.18.6 to 7.23.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5332">5332</a>] -
  Bump react-select from 5.7.4 to 5.7.7 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5330">5330</a>] -
  Bump sass from 1.68.0 to 1.69.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5326">5326</a>] -
  Bump actions/setup-python from 1 to 4</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5324">5324</a>] -
  Bump actions/setup-java from 1 to 3</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5323">5323</a>] -
  Bump actions/checkout from 3 to 4</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5322">5322</a>] -
  Bump @types/react-dom from 18.2.7 to 18.2.12 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5320">5320</a>] -
  Bump actions/cache from 2 to 3</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5309">5309</a>] -
  Bump postcss from 8.4.23 to 8.4.31 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5299">5299</a>] -
  Bump html-validate from 8.3.0 to 8.5.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5298">5298</a>] -
  Bump chromedriver from 116.0.0 to 117.0.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5297">5297</a>] -
  Bump eslint from 8.48.0 to 8.50.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5296">5296</a>] -
  Bump markdownlint-cli from 0.35.0 to 0.37.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5295">5295</a>] -
  Bump sass from 1.66.1 to 1.68.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5293">5293</a>] -
  Bump @babel/eslint-parser from 7.22.11 to 7.22.15 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5291">5291</a>] -
  Bump @playwright/test from 1.36.2 to 1.38.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5289">5289</a>] -
  Bump paella-user-tracking from 1.41.0 to 1.42.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5287">5287</a>] -
  Bump paella-zoom-plugin from 1.41.0 to 1.41.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5285">5285</a>] -
  Bump paella-basic-plugins from 1.38.0 to 1.44.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5284">5284</a>] -
  Bump bootstrap from 5.3.1 to 5.3.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5283">5283</a>] -
  Bump eslint from 8.48.0 to 8.50.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5281">5281</a>] -
  Bump @babel/core from 7.22.11 to 7.23.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5279">5279</a>] -
  Bump html-validate from 8.3.0 to 8.5.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5277">5277</a>] -
  Bump paella-slide-plugins from 1.8.1 to 1.41.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5255">5255</a>] -
  Bump org.apache.commons:commons-compress from 1.23.0 to 1.24.0</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5250">5250</a>] -
  Bump paella-core from 1.41.0 to 1.42.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5225">5225</a>] -
  Bump jasmine-core from 5.1.0 to 5.1.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5224">5224</a>] -
  Bump html-validate from 8.1.0 to 8.3.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5223">5223</a>] -
  Bump chromedriver from 115.0.0 to 116.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5222">5222</a>] -
  Bump eslint from 8.46.0 to 8.48.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5221">5221</a>] -
  Bump sass from 1.64.2 to 1.66.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5219">5219</a>] -
  Bump jquery from 3.7.0 to 3.7.1 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5215">5215</a>] -
  Bump paella-zoom-plugin from 1.29.0 to 1.41.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5214">5214</a>] -
  Bump @babel/preset-env from 7.22.9 to 7.22.14 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5213">5213</a>] -
  Bump babel-loader from 9.1.2 to 9.1.3 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5212">5212</a>] -
  Bump eslint from 8.42.0 to 8.48.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5211">5211</a>] -
  Bump paella-user-tracking from 1.8.0 to 1.41.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5210">5210</a>] -
  Bump html-validate from 8.1.0 to 8.3.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5209">5209</a>] -
  Bump @babel/eslint-parser from 7.21.8 to 7.22.11 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5208">5208</a>] -
  Bump i18next from 23.4.2 to 23.4.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5206">5206</a>] -
  Bump bootstrap from 5.3.0 to 5.3.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5204">5204</a>] -
  Bump @types/node from 20.4.8 to 20.5.7 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5203">5203</a>] -
  Bump @fortawesome/free-solid-svg-icons from 6.4.0 to 6.4.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5202">5202</a>] -
  Bump @fortawesome/fontawesome-svg-core from 6.4.0 to 6.4.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5201">5201</a>] -
  Bump @types/jest from 29.5.2 to 29.5.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5200">5200</a>] -
  Bump react-i18next from 12.3.1 to 13.2.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5197">5197</a>] -
  Bump org.apache.httpcomponents:httpclient-osgi from 4.5.13 to 4.5.14 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5196">5196</a>] -
  Bump org.apache.servicemix.bundles:org.apache.servicemix.bundles.xerces from 2.12.1_1 to 2.12.2_1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5195">5195</a>] -
  Bump jquery from 3.7.0 to 3.7.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5193">5193</a>] -
  Bump @babel/core from 7.21.8 to 7.22.11 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5182">5182</a>] -
  Bump org.apache.xmlgraphics:batik-bridge from 1.14 to 1.17 in /modules/cover-image-impl</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5181">5181</a>] -
  Bump org.apache.xmlgraphics:batik-transcoder from 1.14 to 1.17 in /modules/cover-image-impl</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5180">5180</a>] -
  Bump org.apache.xmlgraphics:batik-svgrasterizer from 1.14 to 1.17 in /modules/cover-image-impl</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5179">5179</a>] -
  Bump org.apache.xmlgraphics:batik-script from 1.14 to 1.17 in /modules/cover-image-impl</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5156">5156</a>] -
  Bump i18next from 22.5.0 to 23.4.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5155">5155</a>] -
  Bump @types/node from 20.4.5 to 20.4.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5149">5149</a>] -
  Bump grunt-contrib-connect from 3.0.0 to 4.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5148">5148</a>] -
  Bump jasmine-core from 5.0.1 to 5.1.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5147">5147</a>] -
  Bump html-validate from 7.15.1 to 8.1.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5146">5146</a>] -
  Bump eslint from 8.44.0 to 8.46.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5145">5145</a>] -
  Bump chromedriver from 114.0.2 to 115.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5144">5144</a>] -
  Bump sass from 1.62.1 to 1.64.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5139">5139</a>] -
  Bump org.freemarker:freemarker from 2.3.31 to 2.3.32 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5137">5137</a>] -
  Bump @types/node from 20.2.5 to 20.4.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5136">5136</a>] -
  Bump @types/react-dom from 18.2.4 to 18.2.7 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5133">5133</a>] -
  Bump react-select from 5.7.3 to 5.7.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5132">5132</a>] -
  Bump webpack from 5.82.0 to 5.88.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5131">5131</a>] -
  Bump html-validate from 8.0.0 to 8.1.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5130">5130</a>] -
  Bump @playwright/test from 1.33.0 to 1.36.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5119">5119</a>] -
  Bump word-wrap from 1.2.3 to 1.2.4 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5118">5118</a>] -
  Bump word-wrap from 1.2.3 to 1.2.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5111">5111</a>] -
  Bump @babel/preset-env from 7.21.5 to 7.22.9 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5106">5106</a>] -
  Bump semver from 6.3.0 to 6.3.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5104">5104</a>] -
  Bump tough-cookie from 4.0.0 to 4.1.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5093">5093</a>] -
  Bump maven-assembly-plugin from 3.5.0 to 3.6.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5091">5091</a>] -
  Bump grunt-html-validate from 2.0.0 to 3.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5090">5090</a>] -
  Bump eslint from 8.42.0 to 8.44.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5089">5089</a>] -
  Bump chromedriver from 114.0.1 to 114.0.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5086">5086</a>] -
  Bump org.eclipse.persistence.asm from 9.3.0 to 9.5.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5084">5084</a>] -
  Bump jasmine-core from 5.0.0 to 5.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5081">5081</a>] -
  Bump karma-coverage from 2.2.0 to 2.2.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5079">5079</a>] -
  Bump webpack-dev-server from 4.13.3 to 4.15.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5077">5077</a>] -
  Bump webpack-cli from 5.1.1 to 5.1.4 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5071">5071</a>] -
  Bump react-bootstrap from 2.7.4 to 2.8.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5068">5068</a>] -
  Bump markdownlint-cli from 0.34.0 to 0.35.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5066">5066</a>] -
  Bump i18next-browser-languagedetector from 7.0.1 to 7.1.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5039">5039</a>] -
  Bump jetty-server from 9.4.20.v20190813 to 9.4.51.v20230217 in /modules/rest-test-environment</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5028">5028</a>] -
  Bump html-validate from 7.16.0 to 8.0.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5026">5026</a>] -
  Bump chromedriver from 112.0.0 to 114.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5022">5022</a>] -
  Bump eslint from 8.39.0 to 8.42.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5021">5021</a>] -
  Bump eslint from 8.39.0 to 8.42.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5020">5020</a>] -
  Bump socket.io-parser from 4.2.2 to 4.2.4 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5013">5013</a>] -
  Bump style-loader from 3.3.2 to 3.3.3 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5009">5009</a>] -
  Bump i18next from 22.4.15 to 22.5.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5008">5008</a>] -
  Bump css-loader from 6.7.3 to 6.8.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5006">5006</a>] -
  Bump react-select from 5.7.2 to 5.7.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5005">5005</a>] -
  Bump jasmine-core from 4.6.0 to 5.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5003">5003</a>] -
  Bump @types/jest from 29.5.1 to 29.5.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/5001">5001</a>] -
  Bump @types/node from 18.16.3 to 20.2.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4996">4996</a>] -
  Bump maven-bundle-plugin from 5.1.2 to 5.1.9 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4993">4993</a>] -
  Bump maven-plugin-plugin from 3.6.0 to 3.9.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4987">4987</a>] -
  Bump jackson.version from 2.15.0 to 2.15.2 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4986">4986</a>] -
  Bump @types/react-dom from 18.2.1 to 18.2.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4985">4985</a>] -
  Bump org.osgi.service.component from 1.5.0 to 1.5.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4984">4984</a>] -
  Bump jackson.version from 2.15.0 to 2.15.2 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4982">4982</a>] -
  Bump commons-compress from 1.21 to 1.23.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4979">4979</a>] -
  Bump mariadb-java-client from 3.1.2 to 3.1.4 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4978">4978</a>] -
  Bump bootstrap from 5.2.3 to 5.3.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4976">4976</a>] -
  Bump react-i18next from 12.2.2 to 12.3.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4975">4975</a>] -
  Bump jquery from 3.6.4 to 3.7.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4974">4974</a>] -
  Bump jquery from 3.6.4 to 3.7.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4949">4949</a>] -
  Bump webpack from 5.80.0 to 5.82.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4948">4948</a>] -
  Bump webpack-cli from 5.0.1 to 5.1.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4916">4916</a>] -
  Bump jackson.version from 2.14.2 to 2.15.0 in /modules/db</li>
</ul>
</details>
