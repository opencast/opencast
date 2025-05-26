Opencast 16
-----------

### Opencast 16.11

- [[#6681](https://github.com/opencast/opencast/pull/6681)] -
  Allow Opensearch to be in yellow state
- [[#6621](https://github.com/opencast/opencast/pull/6621)] -
  Properly handle Tobira harvest of events with streaming-only tracks
- [[#6581](https://github.com/opencast/opencast/pull/6581)] -
  Set sender value in sent emails if set
- [[#6431](https://github.com/opencast/opencast/pull/6431)] -
  Avoid NPEs during wowza retraction

### Opencast 16.10

- [[#6552](https://github.com/opencast/opencast/pull/6552)] -
  Opencast 16.10 Release Notes
- [[#6517](https://github.com/opencast/opencast/pull/6517)] -
  Fix uncleaned merge conflict
- [[#6499](https://github.com/opencast/opencast/pull/6499)] -
  Use system user for repopulate search index
- [[#6490](https://github.com/opencast/opencast/pull/6490)] -
  Fix search index rebuild
- [[#6484](https://github.com/opencast/opencast/pull/6484)] -
  Replace Ubuntu 20.04 with 22.04 for GHA work
- [[#6426](https://github.com/opencast/opencast/pull/6426)] -
  Fixing SFA pattern for search and livestream

### Opencast 16.9

- [[#6437](https://github.com/opencast/opencast/pull/6437)] -
  Migrate away from deprecated ::set-output in GitHub Actions
- [[#6434](https://github.com/opencast/opencast/pull/6434)] -
  Handle Opencast starting up before index is available

### Opencast 16.8

- [[#6429](https://github.com/opencast/opencast/pull/6429)] -
  Fix OCR transcriptions in Paella player
- [[#6385](https://github.com/opencast/opencast/pull/6385)] -
  Allow live events to be published by the CA user
- [[#6381](https://github.com/opencast/opencast/pull/6381)] -
  Search Service Index Rebuild fails when done on the presentation node
- [[#6367](https://github.com/opencast/opencast/pull/6367)] -
  Segment preview image URL fixed for Tobira
- [[#6363](https://github.com/opencast/opencast/pull/6363)] -
  Fix partial retract streaming elements
- [[#6329](https://github.com/opencast/opencast/pull/6329)] -
  Fixed divide by zero in search index rebuild
- [[#6328](https://github.com/opencast/opencast/pull/6328)] -
  Fix incorrect episode count in LTI series tool
- [[#6252](https://github.com/opencast/opencast/pull/6252)] -
  Disable hard linking between organizations
- [[#6233](https://github.com/opencast/opencast/pull/6233)] -
  Handle content id being null for playlist entries
- [[#6047](https://github.com/opencast/opencast/pull/6047)] -
  Release Notes for 6046 for OC 16

### Opencast 16.7

- [[#6318](https://github.com/opencast/opencast/pull/6318)] -
  Opencast 16.7
- [[GHSA-jh6x-7xfg-9cq2](https://github.com/opencast/opencast/security/advisories/GHSA-jh6x-7xfg-9cq2) -
  Searching Opencast may cause a denial of service (CVE-2024-52797)
- [[#6315](https://github.com/opencast/opencast/pull/6315)] -
  Update to admin interface release 2024-11-19
- [[#6314](https://github.com/opencast/opencast/pull/6314)] -
  Link webinar about upgrading Opencast via RPM
- [[#6313](https://github.com/opencast/opencast/pull/6313)] -
  Remove incorrect JDK support statement
- [[#6305](https://github.com/opencast/opencast/pull/6305)] -
  No opencast-plugin-paella-player-6 as install-feature
- [[#6302](https://github.com/opencast/opencast/pull/6302)] -
  Fix scrollbar overlaying code in docs
- [[#6299](https://github.com/opencast/opencast/pull/6299)] -
  Don't run attach transcription if AmberScript transcription failed
- [[#6292](https://github.com/opencast/opencast/pull/6292)] -
  Fix default session timeout
- [[#6262](https://github.com/opencast/opencast/pull/6262)] -
  Fix Amberscript Transcription Jobs Hanging
- [[#6255](https://github.com/opencast/opencast/pull/6255)] -
  Fix NPE when MP element can't be found in local asset manager store
- [[#6254](https://github.com/opencast/opencast/pull/6254)] -
  Lower log level for filter parsing errors
- [[#6251](https://github.com/opencast/opencast/pull/6251)] -
  Improve event catalog handling in External API
- [[#6250](https://github.com/opencast/opencast/pull/6250)] -
  Fix stale JWK provider cache when using kid
- [[#6244](https://github.com/opencast/opencast/pull/6244)] -
  Update Editor to Release 2024-10-16
- [[#6111](https://github.com/opencast/opencast/pull/6111)] -
  Treat `filter` parameter elements in admin UI APIs as URL encoded

### Opencast 16.6

- [[#6232](https://github.com/opencast/opencast/pull/6232)] -
  Update docs on how to access old admin ui
- [[#6186](https://github.com/opencast/opencast/pull/6186)] -
  Ignore uppercase in search sort parameters
- [[#6185](https://github.com/opencast/opencast/pull/6185)] -
  Fix sorting in engage interface
- [[#6184](https://github.com/opencast/opencast/pull/6184)] -
  Restructure Firewall Graph
- [[#6183](https://github.com/opencast/opencast/pull/6183)] -
  Update firewall documentation regarding OpenSearch
- [[#6181](https://github.com/opencast/opencast/pull/6181)] -
  Fix bug: user not always removed from LTI cache
- [[#6175](https://github.com/opencast/opencast/pull/6175)] -
  Deleting events which do not exist in the search index

### Opencast 16.5

- [[#6172](https://github.com/opencast/opencast/pull/6172)] -
  Prevent broken XML from failing index rebuild completely
- [[#6170](https://github.com/opencast/opencast/pull/6170)] -
  Skip elements during search-index rebuild that throw a NotFoundException
- [[#6169](https://github.com/opencast/opencast/pull/6169)] -
  Fix sort parameter in search series rest endpoint
- [[#6168](https://github.com/opencast/opencast/pull/6168)] -
  Document Java version requirements
- [[#6143](https://github.com/opencast/opencast/pull/6143)] -
  Fix Amberscript example workflows
- [[#6142](https://github.com/opencast/opencast/pull/6142)] -
  Allow Amberscript transcriptions to be attached as tracks
- [[#6141](https://github.com/opencast/opencast/pull/6141)] -
  Add missing target-tags to amberscript attach transcription docs

### Opencast 16.4

*Just fixed the broken 16.3 release*

- [[#6105](https://github.com/opencast/opencast/pull/6105)] -
  Opencast 16.4 release notes

### Opencast 16.3

- [[#6101](https://github.com/opencast/opencast/pull/6101)] -
  Release notes for Opencast 16.3
- [[#6100](https://github.com/opencast/opencast/pull/6100)] -
  Update admin interface to 2024-08-14
- [[#6094](https://github.com/opencast/opencast/pull/6094)] -
  Sign urls in search rest service again
- [[#6086](https://github.com/opencast/opencast/pull/6086)] -
  Update feed service security settings
- [[#6057](https://github.com/opencast/opencast/pull/6057)] -
  Fix version of subtitle timeshift operation

### Opencast 16.2

- [[#6056](https://github.com/opencast/opencast/pull/6056)] -
  Opencast 16.1 Release Notes
- [[#6055](ttps://github.com/opencast/opencast/pull/6055)] -
  Update admin interface to 2024-07-30
- [[#6042](https://github.com/opencast/opencast/pull/6042)] -
  Properly handle ACL publication for non-admins
- [[#6034](https://github.com/opencast/opencast/pull/6034)] -
  Fix Updating Engage for Non-Admins
- [[#6029](https://github.com/opencast/opencast/pull/6029)] -
  Modernize cluster install docs
- [[#6025](https://github.com/opencast/opencast/pull/6025)] -
  Add config changes to upgrade.md
- [[#6020](https://github.com/opencast/opencast/pull/6020)] -
  Request user data only if necessary
- [[#6015](https://github.com/opencast/opencast/pull/6015)] -
  Remove Solr Configuration
- [[#6011](https://github.com/opencast/opencast/pull/6011)] -
  Fix link to supported language codes
- [[#6006](https://github.com/opencast/opencast/pull/6006)] -
  Update karaf version to 4.4.6
- [[#6002](https://github.com/opencast/opencast/pull/6002)] -
  Remove Solr dependency
- [[#6001](https://github.com/opencast/opencast/pull/6001)] -
  Remove mentions of Solr from "migrating domain in media packages" guide

### Opencast 16.1

- [[#6000](https://github.com/opencast/opencast/pull/6000)] -
  Opencast 16.1 Release Notes
- [[#5999](https://github.com/opencast/opencast/pull/5999)] -
  Update admin interface to release 2024-07-02
- [[#5998](https://github.com/opencast/opencast/pull/5998)] -
  Fix Tobira Harvest API (includesItemsUntil & hasMore)
- [[#5997](https://github.com/opencast/opencast/pull/5997)] -
  Add a connection from presentation to OpenSearch in firewall diagram
- [[#5976](https://github.com/opencast/opencast/pull/5976)] -
  Rescue admin interface settings from deprecated section
- [[#5972](https://github.com/opencast/opencast/pull/5972)] -
  Fix distributed OC 16
- [[#5953](https://github.com/opencast/opencast/pull/5953)] -
  Remove unused method `getDistributedMediaPackage`
- [[#5947](https://github.com/opencast/opencast/pull/5947)] -
  Make admin-ng a binary
- [[#5939](https://github.com/opencast/opencast/pull/5939)] -
  Add `SearchResult::getCreatedDate` and use it to fix Tobira harvest API
- [[#5937](https://github.com/opencast/opencast/pull/5937)] -
  Fix Non-Admin Access to New Admin UI
- [[#5936](https://github.com/opencast/opencast/pull/5936)] -
  Switch order of register and install
- [[#5935](https://github.com/opencast/opencast/pull/5935)] -
  Update RPM docs for Opencast 16

### Opencast 16.0

- [[#5927](https://github.com/opencast/opencast/pull/5927)] -
  Update release notes for Opencast 16.0
- [[#5923](https://github.com/opencast/opencast/pull/5923)] -
  Update admin interface to release 2024-06-12
- [[#5918](https://github.com/opencast/opencast/pull/5918)] -
  Defaults should be commented out
- [[#5915](https://github.com/opencast/opencast/pull/5915)] -
  Mark old admin interface configuration as deprecated
- [[#5914](https://github.com/opencast/opencast/pull/5914)] -
  Admin interface theme configuration
- [[#5911](https://github.com/opencast/opencast/pull/5911)] -
  Upgraded to admin interface release 2024-06-06
- [[#5909](https://github.com/opencast/opencast/pull/5909)] -
  Add option to en-/disable statistics view in admin interface
- [[#5908](https://github.com/opencast/opencast/pull/5908)] -
  Tags parameter for External API event track update endpoint
- [[#5907](https://github.com/opencast/opencast/pull/5907)] -
  Fixed NullpointerException 330
- [[#5877](https://github.com/opencast/opencast/pull/5877)] -
  Add changelog note about Tobira harvest API
- [[#5874](https://github.com/opencast/opencast/pull/5874)] -
  Fix OCR text extraction handler issue
- [[#5859](https://github.com/opencast/opencast/pull/5859)] -
  Skip editor processing on empty cutting list
- [[#5852](https://github.com/opencast/opencast/pull/5852)] -
  Fix search REST bugs
- [[#5810](https://github.com/opencast/opencast/pull/5810)] -
  Make internal publication optionally visible in external api
- [[#5811](https://github.com/opencast/opencast/pull/5811)] -
  Turn old admin interface into plugin
- [[#5809](https://github.com/opencast/opencast/pull/5809)] -
  Fix Fixed Feature Version
- [[#5807](https://github.com/opencast/opencast/pull/5807)] -
  Developer Environment Setup Documentation change
- [[#5806](https://github.com/opencast/opencast/pull/5806)] -
  Bump ejs from 3.1.8 to 3.1.10 in /modules/lti
- [[#5805](https://github.com/opencast/opencast/pull/5805)] -
  Bump Legal-and-General/dependabot-batcher from 1.0.3 to 1.0.4
- [[#5803](https://github.com/opencast/opencast/pull/5803)] -
  Bump selenium-server-standalone-jar from 4.18.1 to 4.19.1 in /modules/admin-ui-frontend
- [[#5802](https://github.com/opencast/opencast/pull/5802)] -
  Bump html-validate from 8.18.1 to 8.18.2 in /modules/admin-ui-frontend
- [[#5801](https://github.com/opencast/opencast/pull/5801)] -
  Bump sass from 1.72.0 to 1.76.0 in /modules/admin-ui-frontend
- [[#5800](https://github.com/opencast/opencast/pull/5800)] -
  Bump chromedriver from 123.0.1 to 124.0.1 in /modules/admin-ui-frontend
- [[#5794](https://github.com/opencast/opencast/pull/5794)] -
  Bump react-i18next from 14.1.0 to 14.1.1 in /modules/lti
- [[#5793](https://github.com/opencast/opencast/pull/5793)] -
  Bump i18next from 23.10.1 to 23.11.3 in /modules/lti
- [[#5791](https://github.com/opencast/opencast/pull/5791)] -
  Bump iframe-resizer from 4.3.9 to 4.3.11 in /modules/lti
- [[#5790](https://github.com/opencast/opencast/pull/5790)] -
  Bump @types/node from 20.12.2 to 20.12.7 in /modules/lti
- [[#5789](https://github.com/opencast/opencast/pull/5789)] -
  Bump @fortawesome/free-solid-svg-icons from 6.5.1 to 6.5.2 in /modules/lti
- [[#5788](https://github.com/opencast/opencast/pull/5788)] -
  Bump @fortawesome/fontawesome-svg-core from 6.5.1 to 6.5.2 in /modules/lti
- [[#5787](https://github.com/opencast/opencast/pull/5787)] -
  Bump react and @types/react in /modules/lti
- [[#5786](https://github.com/opencast/opencast/pull/5786)] -
  Bump react-dom and @types/react-dom in /modules/lti
- [[#5783](https://github.com/opencast/opencast/pull/5783)] -
  Bump markdownlint-cli from 0.39.0 to 0.40.0 in /docs/guides
- [[#5779](https://github.com/opencast/opencast/pull/5779)] -
  Bump html-validate from 8.11.1 to 8.18.2 in /modules/engage-paella-player-7
- [[#5778](https://github.com/opencast/opencast/pull/5778)] -
  Bump paella-skins from 1.32.4 to 1.48.0 in /modules/engage-paella-player-7
- [[#5777](https://github.com/opencast/opencast/pull/5777)] -
  Bump webpack from 5.90.3 to 5.91.0 in /modules/engage-paella-player-7
- [[#5776](https://github.com/opencast/opencast/pull/5776)] -
  Bump paella-zoom-plugin from 1.41.1 to 1.41.3 in /modules/engage-paella-player-7
- [[#5773](https://github.com/opencast/opencast/pull/5773)] -
  Bump paella-user-tracking from 1.42.0 to 1.42.2 in /modules/engage-paella-player-7
- [[#5772](https://github.com/opencast/opencast/pull/5772)] -
  Bump paella-core from 1.46.6 to 1.48.2 in /modules/engage-paella-player-7
- [[#5771](https://github.com/opencast/opencast/pull/5771)] -
  Bump @babel/core from 7.24.3 to 7.24.5 in /modules/engage-paella-player-7
- [[#5770](https://github.com/opencast/opencast/pull/5770)] -
  Bump @babel/preset-env from 7.24.3 to 7.24.5 in /modules/engage-paella-player-7
- [[#5767](https://github.com/opencast/opencast/pull/5767)] -
  Fix paella tests
- [[#5765](https://github.com/opencast/opencast/pull/5765)] -
  Handle missing tags in Paella Player 7
- [[#5764](https://github.com/opencast/opencast/pull/5764)] -
  Speed up starting and stopping the OpenSearch dev container
- [[#5762](https://github.com/opencast/opencast/pull/5762)] -
  Use current LTS version of Node.js
- [[#5749](https://github.com/opencast/opencast/pull/5749)] -
  Documenting new RM selection rules
- [[#5743](https://github.com/opencast/opencast/pull/5743)] -
  Change Playlist Update behaviour
- [[#5740](https://github.com/opencast/opencast/pull/5740)] -
  Allow karma to use Chromium browser
- [[#5734](https://github.com/opencast/opencast/pull/5734)] -
  Add Playlists to Tobira Harvest API
- [[#5732](https://github.com/opencast/opencast/pull/5732)] -
  Opencast 16 Releasenotes
- [[#5728](https://github.com/opencast/opencast/pull/5728)] -
  Bump gulp from 4.0.2 to 5.0.0 in /modules/engage-paella-player
- [[#5727](https://github.com/opencast/opencast/pull/5727)] -
  Bump softprops/action-gh-release from 1 to 2
- [[#5726](https://github.com/opencast/opencast/pull/5726)] -
  Bump chromedriver from 122.0.4 to 123.0.1 in /modules/admin-ui-frontend
- [[#5725](https://github.com/opencast/opencast/pull/5725)] -
  Bump karma-firefox-launcher from 2.1.2 to 2.1.3 in /modules/admin-ui-frontend
- [[#5724](https://github.com/opencast/opencast/pull/5724)] -
  Bump selenium-server-standalone-jar from 4.17.0 to 4.18.1 in /modules/admin-ui-frontend
- [[#5722](https://github.com/opencast/opencast/pull/5722)] -
  Bump sass from 1.71.1 to 1.72.0 in /modules/admin-ui-frontend
- [[#5721](https://github.com/opencast/opencast/pull/5721)] -
  Bump html-validate from 8.11.1 to 8.18.1 in /modules/admin-ui-frontend
- [[#5717](https://github.com/opencast/opencast/pull/5717)] -
  Bump @babel/eslint-parser from 7.23.3 to 7.24.1 in /modules/engage-paella-player-7
- [[#5716](https://github.com/opencast/opencast/pull/5716)] -
  Bump @babel/preset-env from 7.23.9 to 7.24.3 in /modules/engage-paella-player-7
- [[#5715](https://github.com/opencast/opencast/pull/5715)] -
  Bump paella-basic-plugins from 1.44.2 to 1.44.7 in /modules/engage-paella-player-7
- [[#5714](https://github.com/opencast/opencast/pull/5714)] -
  Bump webpack-dev-server from 4.15.1 to 5.0.4 in /modules/engage-paella-player-7
- [[#5713](https://github.com/opencast/opencast/pull/5713)] -
  Bump @babel/core from 7.23.9 to 7.24.3 in /modules/engage-paella-player-7
- [[#5709](https://github.com/opencast/opencast/pull/5709)] -
  Bump @types/node from 20.11.24 to 20.12.2 in /modules/lti
- [[#5708](https://github.com/opencast/opencast/pull/5708)] -
  Bump i18next from 23.10.0 to 23.10.1 in /modules/lti
- [[#5707](https://github.com/opencast/opencast/pull/5707)] -
  Bump react-bootstrap from 2.10.1 to 2.10.2 in /modules/lti
- [[#5706](https://github.com/opencast/opencast/pull/5706)] -
  Bump axios from 1.6.7 to 1.6.8 in /modules/lti
- [[#5702](https://github.com/opencast/opencast/pull/5702)] -
  Bump i18next-browser-languagedetector from 7.2.0 to 7.2.1 in /modules/lti
- [[#5701](https://github.com/opencast/opencast/pull/5701)] -
  Bump react-i18next from 14.0.5 to 14.1.0 in /modules/lti
- [[#5695](https://github.com/opencast/opencast/pull/5695)] -
  Bump express from 4.18.1 to 4.19.2 in /modules/lti
- [[#5694](https://github.com/opencast/opencast/pull/5694)] -
  Bump express from 4.18.2 to 4.19.2 in /modules/engage-paella-player-7
- [[#5693](https://github.com/opencast/opencast/pull/5693)] -
  Change broken links in config files
- [[#5691](https://github.com/opencast/opencast/pull/5691)] -
  Bump webpack-dev-middleware from 5.3.3 to 5.3.4 in /modules/engage-paella-player-7
- [[#5690](https://github.com/opencast/opencast/pull/5690)] -
  Bump webpack-dev-middleware from 5.3.3 to 5.3.4 in /modules/lti
- [[#5684](https://github.com/opencast/opencast/pull/5684)] -
  Make the paella 7 tests locally without depending on develop.opencast…
- [[#5681](https://github.com/opencast/opencast/pull/5681)] -
  Bump follow-redirects from 1.15.5 to 1.15.6 in /modules/lti
- [[#5680](https://github.com/opencast/opencast/pull/5680)] -
  Bump follow-redirects from 1.15.5 to 1.15.6 in /modules/admin-ui-frontend
- [[#5679](https://github.com/opencast/opencast/pull/5679)] -
  Bump follow-redirects from 1.15.4 to 1.15.6 in /modules/engage-paella-player-7
- [[#5674](https://github.com/opencast/opencast/pull/5674)] -
  Feature request: privacy statement and imprint
- [[#5671](https://github.com/opencast/opencast/pull/5671)] -
  Bump @types/react from 17.0.39 to 18.2.65 in /modules/lti
- [[#5664](https://github.com/opencast/opencast/pull/5664)] -
  Fix a JavaDoc link
- [[#5661](https://github.com/opencast/opencast/pull/5661)] -
  Bump html-validate from 8.8.0 to 8.11.1 in /modules/engage-paella-player-7
- [[#5660](https://github.com/opencast/opencast/pull/5660)] -
  Bump style-loader from 3.3.3 to 3.3.4 in /modules/engage-paella-player-7
- [[#5659](https://github.com/opencast/opencast/pull/5659)] -
  Bump @playwright/test from 1.41.1 to 1.42.0 in /modules/engage-paella-player-7
- [[#5658](https://github.com/opencast/opencast/pull/5658)] -
  Bump express from 4.18.2 to 4.18.3 in /modules/engage-paella-player-7
- [[#5652](https://github.com/opencast/opencast/pull/5652)] -
  Bump eslint from 8.56.0 to 8.57.0 in /modules/engage-paella-player-7
- [[#5651](https://github.com/opencast/opencast/pull/5651)] -
  Bump junit5.version from 5.10.0 to 5.10.2 in /modules/db
- [[#5647](https://github.com/opencast/opencast/pull/5647)] -
  Bump org.owasp.esapi:esapi from 2.5.2.0 to 2.5.3.1 in /modules/db
- [[#5646](https://github.com/opencast/opencast/pull/5646)] -
  Bump eclipselink.version from 2.7.11 to 2.7.14 in /modules/db
- [[#5644](https://github.com/opencast/opencast/pull/5644)] -
  Bump org.osgi:org.osgi.service.http from 1.2.1 to 1.2.2 in /modules/metrics-exporter
- [[#5640](https://github.com/opencast/opencast/pull/5640)] -
  Bump @types/node from 20.11.20 to 20.11.24 in /modules/lti
- [[#5639](https://github.com/opencast/opencast/pull/5639)] -
  Bump bootstrap from 5.3.2 to 5.3.3 in /modules/lti
- [[#5638](https://github.com/opencast/opencast/pull/5638)] -
  Bump @types/react-dom from 18.2.18 to 18.2.19 in /modules/lti
- [[#5637](https://github.com/opencast/opencast/pull/5637)] -
  Bump @types/jest from 29.5.11 to 29.5.12 in /modules/lti
- [[#5636](https://github.com/opencast/opencast/pull/5636)] -
  Bump sass from 1.70.0 to 1.71.1 in /modules/admin-ui-frontend
- [[#5635](https://github.com/opencast/opencast/pull/5635)] -
  Bump html-validate from 8.9.1 to 8.11.1 in /modules/admin-ui-frontend
- [[#5634](https://github.com/opencast/opencast/pull/5634)] -
  Bump karma from 6.4.2 to 6.4.3 in /modules/admin-ui-frontend
- [[#5633](https://github.com/opencast/opencast/pull/5633)] -
  Bump eslint from 8.56.0 to 8.57.0 in /modules/admin-ui-frontend
- [[#5632](https://github.com/opencast/opencast/pull/5632)] -
  Bump jasmine-core from 5.1.1 to 5.1.2 in /modules/admin-ui-frontend
- [[#5631](https://github.com/opencast/opencast/pull/5631)] -
  Bump chromedriver from 122.0.3 to 122.0.4 in /modules/admin-ui-frontend
- [[#5628](https://github.com/opencast/opencast/pull/5628)] -
  Bump webpack from 5.88.2 to 5.90.3 in /modules/engage-paella-player-7
- [[#5627](https://github.com/opencast/opencast/pull/5627)] -
  Bump paella-core from 1.46.1 to 1.46.6 in /modules/engage-paella-player-7
- [[#5625](https://github.com/opencast/opencast/pull/5625)] -
  Bump chromedriver from 121.0.2 to 122.0.3 in /modules/admin-ui-frontend
- [[#5624](https://github.com/opencast/opencast/pull/5624)] -
  Bump @types/node from 20.10.6 to 20.11.20 in /modules/lti
- [[#5623](https://github.com/opencast/opencast/pull/5623)] -
  Fix use of s3 distribution service in live scheduler
- [[#5622](https://github.com/opencast/opencast/pull/5622)] -
  Remove `MediaPackageObserver`
- [[#5621](https://github.com/opencast/opencast/pull/5621)] -
  Fix some broken JavaDoc links
- [[#5617](https://github.com/opencast/opencast/pull/5617)] -
  Bump org.postgresql:postgresql from 42.5.3 to 42.7.2 in /modules/db
- [[#5616](https://github.com/opencast/opencast/pull/5616)] -
  Bump org.apache.commons:commons-compress from 1.24.0 to 1.26.0
- [[#5610](https://github.com/opencast/opencast/pull/5610)] -
  Update config file and docs for correct shibboleth logout
- [[#5609](https://github.com/opencast/opencast/pull/5609)] -
  Remove Entwine from Asset Manager API module
- [[#5597](https://github.com/opencast/opencast/pull/5597)] -
  Replace Solr Search with OpenSearch
- [[#5593](https://github.com/opencast/opencast/pull/5593)] -
  Use MediaPackageElementSelector in every WOH
- [[#5592](https://github.com/opencast/opencast/pull/5592)] -
  Patch for 360° Video
- [[#5582](https://github.com/opencast/opencast/pull/5582)] -
  Bump net.java.dev.jna:jna from 5.13.0 to 5.14.0 in /modules/db
- [[#5581](https://github.com/opencast/opencast/pull/5581)] -
  Bump source-map-loader from 4.0.1 to 5.0.0 in /modules/engage-paella-player-7
- [[#5578](https://github.com/opencast/opencast/pull/5578)] -
  Bump @babel/preset-env from 7.22.14 to 7.23.9 in /modules/engage-paella-player-7
- [[#5577](https://github.com/opencast/opencast/pull/5577)] -
  Bump css-loader from 6.8.1 to 6.10.0 in /modules/engage-paella-player-7
- [[#5576](https://github.com/opencast/opencast/pull/5576)] -
  Bump @playwright/test from 1.40.1 to 1.41.1 in /modules/engage-paella-player-7
- [[#5575](https://github.com/opencast/opencast/pull/5575)] -
  Bump @babel/core from 7.23.2 to 7.23.9 in /modules/engage-paella-player-7
- [[#5574](https://github.com/opencast/opencast/pull/5574)] -
  Bump peter-evans/create-or-update-comment from 3 to 4
- [[#5573](https://github.com/opencast/opencast/pull/5573)] -
  Bump actions/cache from 3 to 4
- [[#5572](https://github.com/opencast/opencast/pull/5572)] -
  Bump peter-evans/find-comment from 2 to 3
- [[#5571](https://github.com/opencast/opencast/pull/5571)] -
  Bump sass from 1.69.6 to 1.70.0 in /modules/admin-ui-frontend
- [[#5569](https://github.com/opencast/opencast/pull/5569)] -
  Bump selenium-server-standalone-jar from 4.16.1 to 4.17.0 in /modules/admin-ui-frontend
- [[#5568](https://github.com/opencast/opencast/pull/5568)] -
  Bump html-validate from 8.8.0 to 8.9.1 in /modules/admin-ui-frontend
- [[#5566](https://github.com/opencast/opencast/pull/5566)] -
  Bump markdownlint-cli from 0.38.0 to 0.39.0 in /docs/guides
- [[#5565](https://github.com/opencast/opencast/pull/5565)] -
  Bump react-select from 5.7.7 to 5.8.0 in /modules/lti
- [[#5549](https://github.com/opencast/opencast/pull/5549)] -
  Add `skip_frame nokey` to timelinepreview operation for longer videos
- [[#5545](https://github.com/opencast/opencast/pull/5545)] -
  Improve encoding profiles by relaxing the GOP range requirements and using CRF 22
- [[#5542](https://github.com/opencast/opencast/pull/5542)] -
  Update issue templates
- [[#5533](https://github.com/opencast/opencast/pull/5533)] -
  Build(deps-dev): Bump follow-redirects from 1.15.2 to 1.15.4 in /modules/engage-paella-player-7
- [[#5525](https://github.com/opencast/opencast/pull/5525)] -
  Bump markdownlint-cli from 0.37.0 to 0.38.0 in /docs/guides
- [[#5520](https://github.com/opencast/opencast/pull/5520)] -
  Bump com.googlecode.maven-download-plugin:download-maven-plugin from 1.6.8 to 1.8.0 in /modules/db
- [[#5518](https://github.com/opencast/opencast/pull/5518)] -
  Bump sass from 1.69.5 to 1.69.6 in /modules/admin-ui-frontend
- [[#5517](https://github.com/opencast/opencast/pull/5517)] -
  Bump eslint from 8.54.0 to 8.56.0 in /modules/admin-ui-frontend
- [[#5516](https://github.com/opencast/opencast/pull/5516)] -
  Bump chromedriver from 119.0.1 to 120.0.1 in /modules/admin-ui-frontend
- [[#5515](https://github.com/opencast/opencast/pull/5515)] -
  Bump selenium-server-standalone-jar from 4.14.0 to 4.16.1 in /modules/admin-ui-frontend
- [[#5514](https://github.com/opencast/opencast/pull/5514)] -
  Bump html-validate from 8.7.3 to 8.8.0 in /modules/admin-ui-frontend
- [[#5513](https://github.com/opencast/opencast/pull/5513)] -
  Bump actions/upload-artifact from 3 to 4
- [[#5512](https://github.com/opencast/opencast/pull/5512)] -
  Bump actions/setup-python from 4 to 5
- [[#5511](https://github.com/opencast/opencast/pull/5511)] -
  Bump paella-skins from 1.32.3 to 1.32.4 in /modules/engage-paella-player-7
- [[#5510](https://github.com/opencast/opencast/pull/5510)] -
  Bump paella-slide-plugins from 1.41.1 to 1.41.4 in /modules/engage-paella-player-7
- [[#5508](https://github.com/opencast/opencast/pull/5508)] -
  Bump paella-basic-plugins from 1.44.0 to 1.44.2 in /modules/engage-paella-player-7
- [[#5505](https://github.com/opencast/opencast/pull/5505)] -
  Bump html-validate from 8.7.3 to 8.8.0 in /modules/engage-paella-player-7
- [[#5503](https://github.com/opencast/opencast/pull/5503)] -
  Bump eslint from 8.54.0 to 8.56.0 in /modules/engage-paella-player-7
- [[#5500](https://github.com/opencast/opencast/pull/5500)] -
  Bump react-bootstrap from 2.9.1 to 2.9.2 in /modules/lti
- [[#5499](https://github.com/opencast/opencast/pull/5499)] -
  Bump @types/jest from 29.5.10 to 29.5.11 in /modules/lti
- [[#5498](https://github.com/opencast/opencast/pull/5498)] -
  Bump iframe-resizer from 4.3.7 to 4.3.9 in /modules/lti
- [[#5496](https://github.com/opencast/opencast/pull/5496)] -
  Bump react-i18next from 13.3.1 to 14.0.0 in /modules/lti
- [[#5495](https://github.com/opencast/opencast/pull/5495)] -
  Bump @types/react-dom from 18.2.14 to 18.2.18 in /modules/lti
- [[#5494](https://github.com/opencast/opencast/pull/5494)] -
  Bump @types/node from 20.8.10 to 20.10.6 in /modules/lti
- [[#5490](https://github.com/opencast/opencast/pull/5490)] -
  Docs: Mention other frontends in the developer docs
- [[#5489](https://github.com/opencast/opencast/pull/5489)] -
  Docs: Sort pages in Opencast Architecture
- [[#5486](https://github.com/opencast/opencast/pull/5486)] -
  Docs: Make Developer Overview page more verbose
- [[#5485](https://github.com/opencast/opencast/pull/5485)] -
  Docs: Remove outdated step from release branch cut
- [[#5484](https://github.com/opencast/opencast/pull/5484)] -
  Docs: Add beginner guide for submitting issues
- [[#5483](https://github.com/opencast/opencast/pull/5483)] -
  Docs: Move "Localization" to "Participate"
- [[#5480](https://github.com/opencast/opencast/pull/5480)] -
  Dev Debugging docs
- [[#5478](https://github.com/opencast/opencast/pull/5478)] -
  Add Playlists
- [[#5477](https://github.com/opencast/opencast/pull/5477)] -
  Don't set bogus default email for admin
- [[#5473](https://github.com/opencast/opencast/pull/5473)] -
  Make Whisper the default STT engine
- [[#5466](https://github.com/opencast/opencast/pull/5466)] -
  Remove unnecessary dependencies
- [[#5462](https://github.com/opencast/opencast/pull/5462)] -
  Bump @babel/eslint-parser from 7.22.15 to 7.23.3 in /modules/engage-paella-player-7
- [[#5460](https://github.com/opencast/opencast/pull/5460)] -
  Bump eslint from 8.52.0 to 8.54.0 in /modules/engage-paella-player-7
- [[#5459](https://github.com/opencast/opencast/pull/5459)] -
  Bump @playwright/test from 1.39.0 to 1.40.1 in /modules/engage-paella-player-7
- [[#5457](https://github.com/opencast/opencast/pull/5457)] -
  Bump html-validate from 8.7.0 to 8.7.3 in /modules/engage-paella-player-7
- [[#5456](https://github.com/opencast/opencast/pull/5456)] -
  Bump html-validate from 8.7.0 to 8.7.3 in /modules/admin-ui-frontend
- [[#5455](https://github.com/opencast/opencast/pull/5455)] -
  Bump eslint from 8.53.0 to 8.54.0 in /modules/admin-ui-frontend
- [[#5453](https://github.com/opencast/opencast/pull/5453)] -
  Bump actions/setup-java from 3 to 4
- [[#5449](https://github.com/opencast/opencast/pull/5449)] -
  Bump commons-io:commons-io from 2.8.0 to 2.15.1 in /modules/metrics-exporter
- [[#5446](https://github.com/opencast/opencast/pull/5446)] -
  Bump i18next-browser-languagedetector from 7.1.0 to 7.2.0 in /modules/lti
- [[#5445](https://github.com/opencast/opencast/pull/5445)] -
  Bump @types/jest from 29.5.7 to 29.5.10 in /modules/lti
- [[#5443](https://github.com/opencast/opencast/pull/5443)] -
  Bump @types/react-helmet from 6.1.8 to 6.1.9 in /modules/lti
- [[#5442](https://github.com/opencast/opencast/pull/5442)] -
  Bump @fortawesome/fontawesome-svg-core from 6.4.2 to 6.5.1 in /modules/lti
- [[#5441](https://github.com/opencast/opencast/pull/5441)] -
  Bump @types/react-js-pagination from 3.0.6 to 3.0.7 in /modules/lti
- [[#5440](https://github.com/opencast/opencast/pull/5440)] -
  Bump @fortawesome/free-solid-svg-icons from 6.4.2 to 6.5.1 in /modules/lti
- [[#5439](https://github.com/opencast/opencast/pull/5439)] -
  Bump i18next from 23.6.0 to 23.7.7 in /modules/lti
- [[#5435](https://github.com/opencast/opencast/pull/5435)] -
  Fix links in documentation
- [[#5312](https://github.com/opencast/opencast/pull/5312)] -
  Make LTI "Custom role" configuration more configurable
- [[#5257](https://github.com/opencast/opencast/pull/5257)] -
  Hint at how to properly close issues with pull requests
- [[#4677](https://github.com/opencast/opencast/pull/4677)] -
  Bump xml-apis from 1.4.01 to 2.0.2 in /modules/db