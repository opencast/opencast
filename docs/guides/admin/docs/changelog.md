Changelog
=========

Opencast 16
-----------

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

Opencast 15
-----------

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


Opencast 14
-----------


### Opencast 14.13

*Released on June 11th, 2024*

- [[#5844](https://github.com/opencast/opencast/pull/5844)] -
  Fixes handling of OAI-PMH setSpec
- [[#5843](https://github.com/opencast/opencast/pull/5843)] -
  Invalidate cached user on changes
- [[#5840](https://github.com/opencast/opencast/pull/5840)] -
  Remove series ACL file from workspaces
- [[#5839](https://github.com/opencast/opencast/pull/5839)] -
  More configuration options for database pool settings
- [[#5769](https://github.com/opencast/opencast/pull/5769)] -
  Document Translation Change Rules
- [[#5754](https://github.com/opencast/opencast/pull/5754)] -
  Series ACL and extended metadata handling on ingest


### Opencast 14.12

*Released on May 14th, 2024*

- [[#5769](https://github.com/opencast/opencast/pull/5769)] -
  Document Translation Change Rules
- [[#5761](https://github.com/opencast/opencast/pull/5761)] -
  Paella:  Prevent video download. Disable context menu.
- [[#5756](https://github.com/opencast/opencast/pull/5756)] -
  Fix deleting user references from the admin UI
- [[#5755](https://github.com/opencast/opencast/pull/5755)] -
  Skip the OBR
- [[#5752](https://github.com/opencast/opencast/pull/5752)] -
  Reformat new admin UI `pom.xml`
- [[#5751](https://github.com/opencast/opencast/pull/5751)] -
  `download-maven-plugin` nitpicks
- [[#5750](https://github.com/opencast/opencast/pull/5750)] -
  Paella: Fix error displayed before authenticating user.


### Opencast 14.11

*Released on April 16th, 2024*

- [[#5682](https://github.com/opencast/opencast/pull/5682)] -
Quickfix dangling hard links on cephfs volumes
- [[#5667](https://github.com/opencast/opencast/pull/5667)] -
Fixed NPE on filtering data

### Opencast 14.10

*Released on March 14th, 2024*

- [[#5619](https://github.com/opencast/opencast/pull/5619)] -
Update setenv with currently supported options for Karaf
- [[#5607](https://github.com/opencast/opencast/pull/5607)] -
Fixed created value on scheduling event series
- [[#5606](https://github.com/opencast/opencast/pull/5606)] -
Fix vosk output filenames
- [[#5604](https://github.com/opencast/opencast/pull/5604)] -
Encode player redirect id parameter
- [[#5600](https://github.com/opencast/opencast/pull/5600)] -
Fix removal of event ACL from search when updating series ACL
- [[#5594](https://github.com/opencast/opencast/pull/5594)] -
Include all metadata in Tobira harvest API for series


### Opencast 14.9

*Released on February 13th, 2024*

- [[#5603](https://github.com/opencast/opencast/pull/5603)] -
  Fixed encoding profile typo
- [[#5555](https://github.com/opencast/opencast/pull/5555)] -
  Debian installation documentation fixed
- [[#5554](https://github.com/opencast/opencast/pull/5554)] -
  Fix ACL Template Display for Series in old Admin UI
- [[#5553](https://github.com/opencast/opencast/pull/5553)] -
  Don't duplicate user roles when switching ACL templates in old Admin UI
- [[#5551](https://github.com/opencast/opencast/pull/5551)] -
  Disable running Firefox from Karma on Macs
- [[#5548](https://github.com/opencast/opencast/pull/5548)] -
  Encoding profile fix for portrait videos
- [[#5526](https://github.com/opencast/opencast/pull/5526)] -
  Document how to turn static file auth off for Tobira usage.
- [[#5472](https://github.com/opencast/opencast/pull/5472)] -
  Fix concurrency problem in Whisper integration
- [[#5231](https://github.com/opencast/opencast/pull/5231)] -
  Update Stud.IP User Provider

### Opencast 14.8

*Released on January 16th, 2024*

- [[#5538](https://github.com/opencast/opencast/pull/5538)] -
  Don't warn about using the default tool
- [[#5537](https://github.com/opencast/opencast/pull/5537)] -
  Fix ACL template selection breaking after first selection
- [[#5492](https://github.com/opencast/opencast/pull/5492)] -
  Add support for custom actions in ACL to Tobira harvest API
- [[#5481](https://github.com/opencast/opencast/pull/5481)] -
  Fix Paella Player 7 login redirect

### Opencast 14.7

*Released on December 12th, 2023*

- [[#5425](https://github.com/opencast/opencast/pull/5425)] -
Fix Unsaved ACL Changes popup if user can't request ACL
- [[#5416](https://github.com/opencast/opencast/pull/5416)] -
Silence detection: Skip attaching smil file if empty
- [[#5412](https://github.com/opencast/opencast/pull/5412)] -
Update Karaf from 4.4.3 to 4.4.4
- [[#5270](https://github.com/opencast/opencast/pull/5270)] -
Fix logging for WhisperEngine

### Opencast 14.6

*Released on November 16th, 2023*

- [[#5365](https://github.com/opencast/opencast/pull/5365)] -
  Stream media packages to avoid memory issues
- [[#5361](https://github.com/opencast/opencast/pull/5361)] -
  Avoid a potential nullpointer exception in thumbnail generation
- [[#5345](https://github.com/opencast/opencast/pull/5345)] -
  Fix/improve parts of the Tobira harvest API (subtitles & duration)
- [[#5315](https://github.com/opencast/opencast/pull/5315)] -
  Improve Performance of Permission Check in AssetManager

### Opencast 14.5

*Released on October 19th, 2023*

- [[#5259](https://github.com/opencast/opencast/pull/5259)] -
  Improve asset manager event handler logging
- [[#5254](https://github.com/opencast/opencast/pull/5254)] -
  Update Opencast Studio to `2023-09-14`
- [[#5252](https://github.com/opencast/opencast/pull/5252)] -
  Paella 7: Fix vertically stretched thumbnails in transcriptions plugin
- [[#5243](https://github.com/opencast/opencast/pull/5243)] -
  Extend analyze-mp for tag variables
- [[#5240](https://github.com/opencast/opencast/pull/5240)] -
  Use context aware logger for workflow service

### Opencast 14.4

*Released on September 14th, 2023*

- [[#5241](https://github.com/opencast/opencast/pull/5241)] -
  Fix incorrect Debian install documentation
- [[#5239](https://github.com/opencast/opencast/pull/5239)] -
  Skip publications when removing temporary files
- [[#5237](https://github.com/opencast/opencast/pull/5237)] -
  Prevent concurrent cleanups
- [[#5236](https://github.com/opencast/opencast/pull/5236)] -
  Fix NPE when removing a workflow without creator
- [[#5232](https://github.com/opencast/opencast/pull/5232)] -
  Logging of delete snapshot workflow operation
- [[#5229](https://github.com/opencast/opencast/pull/5229)] -
  Fixed Admin UI redirect after login
- [[#5228](https://github.com/opencast/opencast/pull/5228)] -
  Fix AmberScript transcription failing if video contains no speech
- [[#5194](https://github.com/opencast/opencast/pull/5194)] -
  Fix Crowdin Sources
- [[#5178](https://github.com/opencast/opencast/pull/5178)] -
  Incorrect crowdin paths
- [[#5176](https://github.com/opencast/opencast/pull/5176)] -
  Opencast 14.3 release notes


### Opencast 14.3

*Released on August 23rd, 2023*

- [[#5167](https://github.com/opencast/opencast/pull/5167)] -
  Fix admin interface permissions
- [[#5166](https://github.com/opencast/opencast/pull/5166)] -
  Fix Admin Interface Redirect
- [[#5165](https://github.com/opencast/opencast/pull/5165)] -
  Fix crowdin package name
- [[#5163](https://github.com/opencast/opencast/pull/5163)] -
  Add Opencast 14.2 release notes


### Opencast 14.2

*Released on August 9th, 2023*

- [[#5159](https://github.com/opencast/opencast/pull/5159)] -
  Remove obsolete maven-bundle-plugin config
- [[#5124](https://github.com/opencast/opencast/pull/5124)] -
  Fix changed pax web config keys
- [[#5114](https://github.com/opencast/opencast/pull/5114)] -
  Upgrade Crowdin Integration


### Opencast 14.1

*Released on July 13th, 2023*

- [[#5109](https://github.com/opencast/opencast/pull/5109)] -
  Paella Player 7 URL parameters documentation fixed
- [[#5065](https://github.com/opencast/opencast/pull/5065)] -
  Update Opencast 14 RPM Docs
- [[#5053](https://github.com/opencast/opencast/pull/5053)] -
  Paella7 fix trimming url params
- [[#5037](https://github.com/opencast/opencast/pull/5037)] -
  Switch to Opensearch by default


### Opencast 14.0

*Released on June 22th, 2023*

- [[#5051](https://github.com/opencast/opencast/pull/5051)] -
  Paella7 backwards support for old captions/dfxp flavored xml files
- [[#5050](https://github.com/opencast/opencast/pull/5050)] -
  Fixes Session IllegalStateException
- [[#5049](https://github.com/opencast/opencast/pull/5049)] -
  Paella 7: Enable dfxp captions support
- [[#5048](https://github.com/opencast/opencast/pull/5048)] -
  Paella 7: Add missing metadata
- [[#4946](https://github.com/opencast/opencast/pull/4946)] -
  Truncate Bundle Info
- [[#4945](https://github.com/opencast/opencast/pull/4945)] -
  Drop orphan statistics database index
- [[#4943](https://github.com/opencast/opencast/pull/4943)] -
  Update Paella 7 default theme
- [[#4930](https://github.com/opencast/opencast/pull/4930)] -
  Karaf upgrade 4.4.3
- [[#4928](https://github.com/opencast/opencast/pull/4928)] -
  Fix rest docs forms
- [[#4924](https://github.com/opencast/opencast/pull/4924)] -
  Update deprecated ACL code
- [[#4921](https://github.com/opencast/opencast/pull/4921)] -
  Fix REST docs login problem
- [[#4881](https://github.com/opencast/opencast/pull/4881)] -
  Run auto-update on main repo only
- [[#4878](https://github.com/opencast/opencast/pull/4878)] -
  Make Composite Ffmpeg Command Configurable
- [[#4876](https://github.com/opencast/opencast/pull/4876)] -
  Set new default editor
- [[#4875](https://github.com/opencast/opencast/pull/4875)] -
  Make Paella 7 Default
- [[#4849](https://github.com/opencast/opencast/pull/4849)] -
  Set Dependabot Interval to Monthly
- [[#4798](https://github.com/opencast/opencast/pull/4798)] -
  Paella7: Add paella7 i18n strings and fix localization path in crowdin config file
- [[#4763](https://github.com/opencast/opencast/pull/4763)] -
  Fix `esline` vs. `eslint` typo in some POMs
- [[#4745](https://github.com/opencast/opencast/pull/4745)] -
  Include Amberscript-Transcription Documentation in Module Overview
- [[#4695](https://github.com/opencast/opencast/pull/4695)] -
  Add new admin UI as beta
- [[#4609](https://github.com/opencast/opencast/pull/4609)] -
  Fix documentation syntax error
- [[#4595](https://github.com/opencast/opencast/pull/4595)] -
  Document feature pull request targetting rules
- [[#4556](https://github.com/opencast/opencast/pull/4556)] -
  image preview added
- [[#4530](https://github.com/opencast/opencast/pull/4530)] -
  Fix parent POM version of redirect module in develop
- [[#4520](https://github.com/opencast/opencast/pull/4520)] -
  Remove Twitter and Facebook links
- [[#4511](https://github.com/opencast/opencast/pull/4511)] -
  Make debug output in the JWT filters simpler and more idiomatic
- [[#4488](https://github.com/opencast/opencast/pull/4488)] -
  Update board list in documentation
- [[#4456](https://github.com/opencast/opencast/pull/4456)] -
  New workflow implementation and migration fixes (OC 13)
- [[#4450](https://github.com/opencast/opencast/pull/4450)] -
  Add index for oc_job_argument table (OC 13)
- [[#4440](https://github.com/opencast/opencast/pull/4440)] -
  Enrich LDAP users with name and mail address
- [[#4423](https://github.com/opencast/opencast/pull/4423)] -
  Update Issue Template
- [[#4415](https://github.com/opencast/opencast/pull/4415)] -
  Publish Captions by Default
- [[#4408](https://github.com/opencast/opencast/pull/4408)] -
  Add support for multipart mails using text and HTML
- [[#4383](https://github.com/opencast/opencast/pull/4383)] -
  Simplify ldap user directory implementation
- [[#4380](https://github.com/opencast/opencast/pull/4380)] -
  Add organization properties to mail template data
- [[#4376](https://github.com/opencast/opencast/pull/4376)] -
  Allow use of extended metadata in send-email WOH
- [[#4315](https://github.com/opencast/opencast/pull/4315)] -
  Remove Theodul
- [[#4206](https://github.com/opencast/opencast/pull/4206)] -
  Changed rebuild order and added rebuild indicator

<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4942">4942</a>] -
  Bump engine.io from 6.4.1 to 6.4.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4936">4936</a>] -
  Bump grunt-contrib-cssmin from 4.0.0 to 5.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4935">4935</a>] -
  Bump @types/react-dom from 18.2.0 to 18.2.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4934">4934</a>] -
  Bump sass from 1.57.1 to 1.62.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4933">4933</a>] -
  Bump @types/node from 18.16.0 to 18.16.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4931">4931</a>] -
  Bump markdownlint-cli from 0.33.0 to 0.34.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4919">4919</a>] -
  Bump @types/react-dom from 18.0.11 to 18.2.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4917">4917</a>] -
  Bump maven-project-info-reports-plugin from 3.4.2 to 3.4.3 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4912">4912</a>] -
  Bump checker-qual from 3.29.0 to 3.33.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4908">4908</a>] -
  Bump chromedriver from 111.0.0 to 112.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4907">4907</a>] -
  Bump eslint from 8.37.0 to 8.39.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4905">4905</a>] -
  Bump karma from 6.4.1 to 6.4.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4902">4902</a>] -
  Bump react-bootstrap from 2.7.2 to 2.7.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4898">4898</a>] -
  Bump react-i18next from 12.2.0 to 12.2.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4897">4897</a>] -
  Bump html-validate from 7.14.0 to 7.15.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4895">4895</a>] -
  Bump @types/jest from 29.5.0 to 29.5.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4893">4893</a>] -
  Bump karma-chrome-launcher from 3.1.1 to 3.2.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4892">4892</a>] -
  Bump i18next from 22.4.14 to 22.4.15 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4889">4889</a>] -
  Bump @types/node from 18.15.11 to 18.16.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4888">4888</a>] -
  Bump esapi from 2.3.0.0 to 2.5.2.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4884">4884</a>] -
  Bump exec-maven-plugin from 1.6.0 to 3.1.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4882">4882</a>] -
  Bump download-maven-plugin from 1.6.6 to 1.6.8 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4859">4859</a>] -
  Bump maven-scr-plugin from 1.26.2 to 1.26.4 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4855">4855</a>] -
  Bump iframe-resizer from 4.3.4 to 4.3.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4854">4854</a>] -
  Bump maven-site-plugin from 3.10.0 to 3.12.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4852">4852</a>] -
  Bump jackson.version from 2.14.1 to 2.14.2 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4847">4847</a>] -
  Bump maven-enforcer-plugin from 3.0.0-M2 to 3.3.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4845">4845</a>] -
  Bump maven-gpg-plugin from 1.5 to 3.0.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4840">4840</a>] -
  Bump i18next from 22.0.4 to 22.4.14 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4838">4838</a>] -
  Bump maven-compiler-plugin from 3.8.1 to 3.11.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4837">4837</a>] -
  Bump @types/jest from 29.4.0 to 29.5.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4836">4836</a>] -
  Bump @fortawesome/fontawesome-svg-core from 6.3.0 to 6.4.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4835">4835</a>] -
  Bump react-i18next from 12.1.4 to 12.2.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4834">4834</a>] -
  Bump react-select from 5.7.0 to 5.7.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4833">4833</a>] -
  Bump @types/node from 18.14.2 to 18.15.11 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4832">4832</a>] -
  Bump @fortawesome/free-solid-svg-icons from 6.3.0 to 6.4.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4827">4827</a>] -
  Bump joda-time from 2.10.10 to 2.12.5 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4818">4818</a>] -
  Bump eslint from 8.36.0 to 8.37.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4811">4811</a>] -
  Bump html-validate from 7.13.3 to 7.14.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4810">4810</a>] -
  Bump maven-install-plugin from 2.5.2 to 3.1.1 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4807">4807</a>] -
  Bump maven-resources-plugin from 3.2.0 to 3.3.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4803">4803</a>] -
  Bump jettison from 1.5.2 to 1.5.4</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4784">4784</a>] -
  Bump jasmine-core from 4.5.0 to 4.6.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4775">4775</a>] -
  Bump webpack from 5.73.0 to 5.76.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4766">4766</a>] -
  Bump eslint from 8.35.0 to 8.36.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4760">4760</a>] -
  Bump chromedriver from 110.0.0 to 111.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4759">4759</a>] -
  Bump html-validate from 7.13.2 to 7.13.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4752">4752</a>] -
  Bump jquery from 3.6.3 to 3.6.4 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4751">4751</a>] -
  Bump jquery from 3.6.3 to 3.6.4 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4750">4750</a>] -
  Bump jquery from 3.6.3 to 3.6.4 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4731">4731</a>] -
  Bump @fortawesome/free-solid-svg-icons from 6.2.1 to 6.3.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4730">4730</a>] -
  Bump @types/node from 18.11.18 to 18.14.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4728">4728</a>] -
  Bump @fortawesome/fontawesome-svg-core from 6.2.1 to 6.3.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4727">4727</a>] -
  Bump react-bootstrap from 2.7.0 to 2.7.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4725">4725</a>] -
  Bump @types/react-dom from 18.0.10 to 18.0.11 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4724">4724</a>] -
  Bump iframe-resizer from 4.3.3 to 4.3.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4718">4718</a>] -
  Bump eslint from 8.34.0 to 8.35.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4712">4712</a>] -
  Bump maven-assembly-plugin from 3.3.0 to 3.5.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4710">4710</a>] -
  Bump commons-fileupload from 1.4 to 1.5</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4686">4686</a>] -
  Bump eslint from 8.33.0 to 8.34.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4678">4678</a>] -
  Bump httpcore-osgi from 4.4.15 to 4.4.16 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4675">4675</a>] -
  Bump chromedriver from 109.0.0 to 110.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4674">4674</a>] -
  Bump html-validate from 7.13.1 to 7.13.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4671">4671</a>] -
  Bump postgresql from 42.5.1 to 42.5.3 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4667">4667</a>] -
  Bump typescript from 4.9.4 to 4.9.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4666">4666</a>] -
  Bump grunt from 1.5.3 to 1.6.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4665">4665</a>] -
  Bump @types/jest from 29.2.5 to 29.4.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4663">4663</a>] -
  Bump @types/react-helmet from 6.1.5 to 6.1.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4660">4660</a>] -
  Bump iframe-resizer from 4.3.2 to 4.3.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4656">4656</a>] -
  Bump eslint from 8.32.0 to 8.33.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4652">4652</a>] -
  Bump ua-parser-js from 0.7.31 to 0.7.33 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4647">4647</a>] -
  Bump maven-surefire-report-plugin from 2.20 to 2.22.2 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4645">4645</a>] -
  Bump nexus-staging-maven-plugin from 1.6.8 to 1.6.13 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4639">4639</a>] -
  Bump org.apache.felix.fileinstall from 3.6.4 to 3.7.4 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4637">4637</a>] -
  Bump maven-source-plugin from 2.2.1 to 3.2.1 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4632">4632</a>] -
  Bump gson from 2.8.9 to 2.10.1 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4629">4629</a>] -
  Bump maven-project-info-reports-plugin from 2.8.1 to 3.4.2 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4621">4621</a>] -
  Bump mariadb-java-client from 3.1.1 to 3.1.2 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4606">4606</a>] -
  Bump jna from 5.12.1 to 5.13.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4602">4602</a>] -
  Bump eslint from 8.31.0 to 8.32.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4601">4601</a>] -
  Bump html-validate from 7.12.2 to 7.13.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4598">4598</a>] -
  Bump mariadb-java-client from 3.1.0 to 3.1.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4597">4597</a>] -
  Bump chromedriver from 108.0.0 to 109.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4593">4593</a>] -
  Bump html-validate from 7.12.1 to 7.12.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4592">4592</a>] -
  Bump react-i18next from 12.0.0 to 12.1.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4589">4589</a>] -
  Bump markdownlint-cli from 0.32.2 to 0.33.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4587">4587</a>] -
  Bump checker-qual from 3.26.0 to 3.29.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4586">4586</a>] -
  Bump json5 from 1.0.1 to 1.0.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4585">4585</a>] -
  Bump json5 from 1.0.1 to 1.0.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4581">4581</a>] -
  Bump html-validate from 7.12.0 to 7.12.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4580">4580</a>] -
  Bump jettison from 1.5.1 to 1.5.2</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4577">4577</a>] -
  Bump eslint from 8.30.0 to 8.31.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4576">4576</a>] -
  Bump bootstrap from 5.2.2 to 5.2.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4575">4575</a>] -
  Bump @types/jest from 29.2.3 to 29.2.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4574">4574</a>] -
  Bump @fortawesome/free-solid-svg-icons from 6.2.0 to 6.2.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4573">4573</a>] -
  Bump react-i18next from 12.0.0 to 12.1.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4572">4572</a>] -
  Bump @types/react-dom from 18.0.9 to 18.0.10 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4571">4571</a>] -
  Bump @types/node from 18.11.8 to 18.11.18 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4568">4568</a>] -
  Bump react-bootstrap from 2.6.0 to 2.7.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4567">4567</a>] -
  Bump json5 from 2.2.0 to 2.2.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4564">4564</a>] -
  Bump html-validate from 7.11.1 to 7.12.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4557">4557</a>] -
  Bump html-validate from 7.11.0 to 7.11.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4555">4555</a>] -
  Bump jquery from 3.6.2 to 3.6.3 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4554">4554</a>] -
  Bump jquery from 3.6.2 to 3.6.3 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4553">4553</a>] -
  Bump jquery from 3.6.2 to 3.6.3 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4551">4551</a>] -
  Bump sass from 1.55.0 to 1.57.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4545">4545</a>] -
  Bump html-validate from 7.10.1 to 7.11.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4544">4544</a>] -
  Bump eslint from 8.29.0 to 8.30.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4534">4534</a>] -
  Bump jquery from 3.6.1 to 3.6.2 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4533">4533</a>] -
  Bump jquery from 3.6.1 to 3.6.2 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4532">4532</a>] -
  Bump jquery from 3.6.1 to 3.6.2 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4531">4531</a>] -
  Bump selenium-server-standalone-jar from 3.141.59 to 4.7.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4526">4526</a>] -
  Bump typescript from 4.8.4 to 4.9.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4509">4509</a>] -
  Bump html-validate from 7.9.0 to 7.10.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4507">4507</a>] -
  Bump chromedriver from 107.0.3 to 108.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4506">4506</a>] -
  Bump decode-uri-component from 0.2.0 to 0.2.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4504">4504</a>] -
  Bump eslint from 8.27.0 to 8.29.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4497">4497</a>] -
  Bump react-select from 5.5.9 to 5.7.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4491">4491</a>] -
  Bump bootbox from 5.5.3 to 6.0.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4489">4489</a>] -
  Bump postgresql from 42.5.0 to 42.5.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4483">4483</a>] -
  Bump engine.io from 6.2.0 to 6.2.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4467">4467</a>] -
  Bump i18next-browser-languagedetector from 7.0.0 to 7.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4466">4466</a>] -
  Bump @types/react-dom from 18.0.8 to 18.0.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4464">4464</a>] -
  Bump @fortawesome/fontawesome-svg-core from 6.2.0 to 6.2.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4463">4463</a>] -
  Bump @types/jest from 29.2.1 to 29.2.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4462">4462</a>] -
  Bump react-bootstrap from 2.5.0 to 2.6.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4452">4452</a>] -
  Bump mariadb-java-client from 3.0.8 to 3.1.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4451">4451</a>] -
  Bump html-validate from 7.8.0 to 7.9.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4449">4449</a>] -
  Bump loader-utils from 2.0.2 to 2.0.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4447">4447</a>] -
  Bump loader-utils from 1.4.0 to 1.4.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4443">4443</a>] -
  Bump minimatch and recursive-readdir in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4336">4336</a>] -
  Bump jackson from 2.13.2.x to 2.14.1</li>
</ul>
</details>

Opencast 13
-----------

### Opencast 13.12

*Released on November 15th, 2023*

- [[#5411](https://github.com/opencast/opencast/pull/5411)] -
  Update lockfile version in the old admin UI
- [[#5407](https://github.com/opencast/opencast/pull/5407)] -
  Fix manually entering time in `datetimepicker`-s in Safari
- [[#5406](https://github.com/opencast/opencast/pull/5406)] -
  Fix access policy tab always showing unsaved changes
- [[#5404](https://github.com/opencast/opencast/pull/5404)] -
  Add role for listprovider external api
- [[#5354](https://github.com/opencast/opencast/pull/5354)] -
  Get thumbnail for editor frontend from archive
- [[#5350](https://github.com/opencast/opencast/pull/5350)] -
  YT-Publish WOH: Fail more aggressively
- [[#5349](https://github.com/opencast/opencast/pull/5349)] -
  Youtube-Publication: Throw explicit error if client_secret is malformed
- [[#5348](https://github.com/opencast/opencast/pull/5348)] -
  Update Youtube Publication documentation
- [[#5344](https://github.com/opencast/opencast/pull/5344)] -
  Fix external api listprovider header
- [[#5342](https://github.com/opencast/opencast/pull/5342)] -
  Add option to pass additional args to Whisper
- [[#5338](https://github.com/opencast/opencast/pull/5338)] -
  Fixes connection test on NCast Hydra with CA user
- [[#5337](https://github.com/opencast/opencast/pull/5337)] -
  Document Registration
- [[#5317](https://github.com/opencast/opencast/pull/5317)] -
  Add config-keys to set minimum rules for cuts and segments duration
- [[#5269](https://github.com/opencast/opencast/pull/5269)] -
  Fix custom roles pattern now working in access policy tab

### Opencast 13.12

*Released on October 18th, 2023*

- [[#5313](https://github.com/opencast/opencast/pull/5313)] -
  Update events in archive in order on series change
- [[#5260](https://github.com/opencast/opencast/pull/5260)] -
  Update ES index before triggering event handlers from AssetManager
- [[#5253](https://github.com/opencast/opencast/pull/5253)] -
  Paella 7: Use the localstorage to store the user preferences and upgrades Paella core to 1.43
- [[#5246](https://github.com/opencast/opencast/pull/5246)] -
  Paella 7 plugins documentation

### Opencast 13.11

*Released on September 14th, 2023*

- [[#5251](https://github.com/opencast/opencast/pull/5251)] -
  Add Opencast 13.11 release notes
- [[#5230](https://github.com/opencast/opencast/pull/5230)] -
  Fix static file service exception on non-existing file
- [[#5226](https://github.com/opencast/opencast/pull/5226)] -
  Fix Workflow Index Rebuild
- [[#5191](https://github.com/opencast/opencast/pull/5191)] -
  Fix NPE when workflow user no longer exists
- [[#5177](https://github.com/opencast/opencast/pull/5177)] -
  Fix wrong failedOperation characterers in send email docs WoH and improves Freemaker documentation.
- [[#5175](https://github.com/opencast/opencast/pull/5175)] -
  Add Opencast 13.10 release notes
- [[#5171](https://github.com/opencast/opencast/pull/5171)] -
  Skip deleting non existent file
- [[#5169](https://github.com/opencast/opencast/pull/5169)] -
  Copy active inputs between CAs if they have the same set of inputs
- [[#5164](https://github.com/opencast/opencast/pull/5164)] -
  Fix editing custom actions in the ACL editor
- [[#5101](https://github.com/opencast/opencast/pull/5101)] -
  Add whisper-ctranslate2 flags to WhisperEngine.java
- [[#5062](https://github.com/opencast/opencast/pull/5062)] -
  Dont allow to delete user with active workflow in Admin UI
- [[#4684](https://github.com/opencast/opencast/pull/4684)] -
  Ensure workflows have an associated org after upgrade

### Opencast 13.10

*Released on August 23th, 2023*

- [[#5162](https://github.com/opencast/opencast/pull/5162)] -
  Add Opencast 13.9 release notes
- [[#5157](https://github.com/opencast/opencast/pull/5157)] -
  Test for broken JAR (zip) files
- [[#5150](https://github.com/opencast/opencast/pull/5150)] -
  Fix Endless Loop on Elasticsearch Exception
- [[#5033](https://github.com/opencast/opencast/pull/5033)] -
  Escape ES query string in external API endpoints

### Opencast 13.9

*Released on August 8th, 2023*

- [[#5158](https://github.com/opencast/opencast/pull/5158)] -
  Update maven-bundle-plugin to latest version to fix invalid
  zip headers in jar files distributed by Opencast; otherwise
  Opencast would not start up with OpenJDK 11.0.20.
- [[#5161](https://github.com/opencast/opencast/pull/5161)] -
  Revert "Enable Tobira adopter stats tracking"
- [[#5153](https://github.com/opencast/opencast/pull/5153)] -
  Disable auto refresh on feature installation
- [[#5123](https://github.com/opencast/opencast/pull/5123)] -
  Add Opencast 13.8 release notes
- [[#5117](https://github.com/opencast/opencast/pull/5117)] -
  Only show Asset Upload for options of correct type
- [[#5115](https://github.com/opencast/opencast/pull/5115)] -
  Fix index rebuild
- [[#5108](https://github.com/opencast/opencast/pull/5108)] -
  Mark tag operation as skipped if nothing happened

### Opencast 13.8

*Released on July 26th, 2023*

- [[#5123](https://github.com/opencast/opencast/pull/5123)] -
  Add Opencast 13.8 release notes
- [[#5105](https://github.com/opencast/opencast/pull/5105)] -
  Increase index rebuild logging frequency for batches
- [[#5102](https://github.com/opencast/opencast/pull/5102)] -
  Add endpoint to get languages & licenses
- [[#5064](https://github.com/opencast/opencast/pull/5064)] -
  Log dispatch interval in seconds, not milliseconds
- [[#5063](https://github.com/opencast/opencast/pull/5063)] -
  Allow engage ui and ltitools to handle non-16/9 thumbnails
- [[#5058](https://github.com/opencast/opencast/pull/5058)] -
  Allow deletion of reference users from the Admin UI
- [[#5055](https://github.com/opencast/opencast/pull/5055)] -
  Dont copy media files to new scheduled event
- [[#5052](https://github.com/opencast/opencast/pull/5052)] -
  Fix tainted canvas in editor thumbnail extractor
- [[#5040](https://github.com/opencast/opencast/pull/5040)] -
  Enable Tobira adopter stats tracking
- [[#5032](https://github.com/opencast/opencast/pull/5032)] -
  Count user references
- [[#4970](https://github.com/opencast/opencast/pull/4970)] -
  Show user information in event workflow details
- [[#4969](https://github.com/opencast/opencast/pull/4969)] -
  Automate PR comments with built tarball links

### Opencast 13.7

*Released on June 26th, 2023*

- [[#5019](https://github.com/opencast/opencast/pull/5019)] -
  Fix Distribution of Elements for Live Events
- [[#4972](https://github.com/opencast/opencast/pull/4972)] -
  Fix directory cleanup for symlinks
- [[#4968](https://github.com/opencast/opencast/pull/4968)] -
  Fix STT Vosk test
- [[#4954](https://github.com/opencast/opencast/pull/4954)] -
  Add Opencast 13.6 release notes
- [[#4952](https://github.com/opencast/opencast/pull/4952)] -
  Fix bug in paella 7 usertracking plugin
- [[#4944](https://github.com/opencast/opencast/pull/4944)] -
  Fix possible deadlock spanning DB transaction and caching lock with user references
- [[#4927](https://github.com/opencast/opencast/pull/4927)] -
  Fix Check for Whether Live Publication has Changed

### Opencast 13.6

*Released on Mai 26th, 2023*

- [[#4961](https://github.com/opencast/opencast/pull/4961)] -
  Remove empty options in ACL template select (#4910)
- [[#4925](https://github.com/opencast/opencast/pull/4925)] -
  Ensure all plugin jars are present in tarballs.
- [[#4923](https://github.com/opencast/opencast/pull/4923)] -
  Mitigate not loading ACLs
- [[#4920](https://github.com/opencast/opencast/pull/4920)] -
  Batch Dependabot Updates for Paella 7
- [[#4879](https://github.com/opencast/opencast/pull/4879)] -
  Editor Release 2023-04-20
- [[#4870](https://github.com/opencast/opencast/pull/4870)] -
  TermsFeed Cookie Consent NOTICE
- [[#4869](https://github.com/opencast/opencast/pull/4869)] -
  [Whisper] Fixes automatic language detection
- [[#4868](https://github.com/opencast/opencast/pull/4868)] -
  Extend configuration options of Amberscript integration
- [[#4864](https://github.com/opencast/opencast/pull/4864)] -
  Remove dead `CONFIG_OPTIONS` from WOHs
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4890">4890</a>] -
  Bump eslint from 8.37.0 to 8.39.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4885">4885</a>] -
  Bump html-validate from 7.14.0 to 7.15.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4853">4853</a>] -
  Bump webpack from 5.77.0 to 5.78.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4851">4851</a>] -
  Bump paella-zoom-plugin from 1.2.1 to 1.27.0 in /modules/engage-paella-player-7</li>
</ul>
</details>

### Opencast 13.5

*Released on April 19th, 2023*

- [[#4866](https://github.com/opencast/opencast/pull/4866)] -
  Add Opencast 13.5 release notes
- [[#4843](https://github.com/opencast/opencast/pull/4843)] -
  Fixed pagination when reindexing asset manager
- [[#4823](https://github.com/opencast/opencast/pull/4823)] -
  Batch Dependabot Updates for Paella 7
- [[#4782](https://github.com/opencast/opencast/pull/4782)] -
  Paella 7: Allow to disable the cookie consent banner
- [[#4772](https://github.com/opencast/opencast/pull/4772)] -
  Batch Dependabot Updates for Paella 7
- [[#4770](https://github.com/opencast/opencast/pull/4770)] -
  Whisper language detection and tagging
- [[#4738](https://github.com/opencast/opencast/pull/4738)] -
  Enable Whisper English Translation
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4824">4824</a>] -
  Bump @babel/preset-env from 7.20.2 to 7.21.4 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4813">4813</a>] -
  Bump html-validate from 7.13.3 to 7.14.0 in /modules/engage-paella-player-7</li>
</ul>
</details>

### Opencast 13.4

*Released on March 21th, 2023*

<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4780">4780</a>] -
  Bump @babel/eslint-parser from 7.19.1 to 7.21.3 in /modules/engage-paella-player-7</li>
</ul>
</details>

### Opencast 13.3

*Released on March 16th, 2023*

- [[#4783](https://github.com/opencast/opencast/pull/4783)] -
  Add paging to asset manager index rebuild
- [[#4758](https://github.com/opencast/opencast/pull/4758)] -
  Add Opencast 13.3 release notes
- [[#4756](https://github.com/opencast/opencast/pull/4756)] -
  Batch Dependabot Updates for Paella 7
- [[#4740](https://github.com/opencast/opencast/pull/4740)] -
  Fix reindex of multi-tanant systems
- [[#4739](https://github.com/opencast/opencast/pull/4739)] -
  Fix exception when retrieving comments where the author is missing
- [[#4734](https://github.com/opencast/opencast/pull/4734)] -
  Batch Dependabot Updates for Paella 7
- [[#4722](https://github.com/opencast/opencast/pull/4722)] -
  Paella 7 matomo plugin
- [[#4717](https://github.com/opencast/opencast/pull/4717)] -
  Shows event Title on Paella 7 browser tab
- [[#4707](https://github.com/opencast/opencast/pull/4707)] -
  Dependabot-batcher update
- [[#4690](https://github.com/opencast/opencast/pull/4690)] -
  Add Opencast 13.2 release notes
- [[#4683](https://github.com/opencast/opencast/pull/4683)] -
  Fix typo and adds recomendations to whisper doc
- [[#4515](https://github.com/opencast/opencast/pull/4515)] -
  Allow hotkeys in create dialogs in input elements
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4753">4753</a>] -
  Bump paella-core from 1.20.2 to 1.22.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4719">4719</a>] -
  Bump eslint from 8.34.0 to 8.35.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4713">4713</a>] -
  Bump paella-core from 1.20.0 to 1.20.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4706">4706</a>] -
  Bump @babel/core from 7.20.12 to 7.21.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4705">4705</a>] -
  Bump paella-core from 1.16.0 to 1.20.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4688">4688</a>] -
  Bump paella-basic-plugins from 1.8.4 to 1.18.0 in /modules/engage-paella-player-7</li>
</ul>
</details>

### Opencast 13.2

*Released on February 15th, 2023*

- [[#4616](https://github.com/opencast/opencast/pull/4616)] -
  Fix adopter data gathering bugs
- [[#4654](https://github.com/opencast/opencast/pull/4654)] -
  Add webvtt-to-cutmarks to list of workflow operations
- [[#4628](https://github.com/opencast/opencast/pull/4628)] -
  Add missing expected response code
- [[#4619](https://github.com/opencast/opencast/pull/4619)] -
  Fix calendar.json endpoint
- [[#4607](https://github.com/opencast/opencast/pull/4607)] -
  Add Opencast 13.1 release notes
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4687">4687</a>] -
  Bump eslint from 8.33.0 to 8.34.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4680">4680</a>] -
  Bump html-validate from 7.13.1 to 7.13.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4657">4657</a>] -
  Bump eslint from 8.32.0 to 8.33.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4648">4648</a>] -
  Bump paella-core from 1.14.2 to 1.16.0 in /modules/engage-paella-player-7</li>
</ul>
</details>

### Opencast 13.1

*Released on January 18th, 2023*

- [[#4607](https://github.com/opencast/opencast/pull/4607)] -
  Add Opencast 13.1 release notes
- [[#4599](https://github.com/opencast/opencast/pull/4599)] -
  Fix Syntax Error
- [[#4548](https://github.com/opencast/opencast/pull/4548)] -
  Mark OC13 RPMs as Stable
- [[#4536](https://github.com/opencast/opencast/pull/4536)] -
  Avoid using jobs in SeriesUpdatedEventHandler
- [[#4516](https://github.com/opencast/opencast/pull/4516)] -
  Change default hotkeys for create dialogs in admin UI
- [[#4502](https://github.com/opencast/opencast/pull/4502)] -
  Fix: series deleted from search index cannot be re-added
- [[#4484](https://github.com/opencast/opencast/pull/4484)] -
  Prepare release notes for Opencast 13
- [[#4482](https://github.com/opencast/opencast/pull/4482)] -
  Add silient detection based on subtitles (webvtt-to-cutmarks woh)
- [[#4478](https://github.com/opencast/opencast/pull/4478)] -
  Bug fix: publish engage woh with merge SKIP the operation if media package not in search index
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4608">4608</a>] -
  Bump paella-core from 1.11.3 to 1.14.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4604">4604</a>] -
  Bump eslint from 8.30.0 to 8.32.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4603">4603</a>] -
  Bump html-validate from 7.11.1 to 7.13.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4584">4584</a>] -
  Bump @babel/core from 7.20.5 to 7.20.12 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4582">4582</a>] -
  Bump babel-loader from 9.1.0 to 9.1.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4578">4578</a>] -
  Bump paella-basic-plugins from 1.8.1 to 1.8.4 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4558">4558</a>] -
  Bump html-validate from 7.11.0 to 7.11.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4547">4547</a>] -
  Bump eslint from 8.29.0 to 8.30.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4546">4546</a>] -
  Bump html-validate from 7.10.1 to 7.11.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4541">4541</a>] -
  Bump paella-core from 1.11.1 to 1.11.3 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4540">4540</a>] -
  Bump css-loader from 6.7.2 to 6.7.3 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4512">4512</a>] -
  Bump webpack-cli from 5.0.0 to 5.0.1 in /modules/engage-paella-player-7</li>
</ul>
</details>

### Opencast 13.0

*Released on December 14th, 2022*

- [[#4529](https://github.com/opencast/opencast/pull/4529)] -
  Fix parent POM version of redirect module in r/13.x
- [[#4519](https://github.com/opencast/opencast/pull/4519)] -
  Reduce number of snapshots taken in the new editor backend
- [[#4513](https://github.com/opencast/opencast/pull/4513)] -
  Adds Whisper STT to SpeechToText WoH
- [[#4490](https://github.com/opencast/opencast/pull/4490)] -
  Add rules for merging pull requests to the docs
- [[#4456](https://github.com/opencast/opencast/pull/4456)] -
  New workflow implementation and migration fixes (OC 13)
- [[#4450](https://github.com/opencast/opencast/pull/4450)] -
  Add index for oc_job_argument table (OC 13)
- [[#4445](https://github.com/opencast/opencast/pull/4445)] -
  Update to eclipselink 2.7.8
- [[#4442](https://github.com/opencast/opencast/pull/4442)] -
  Update Karaf to 4.2.16
- [[#4440](https://github.com/opencast/opencast/pull/4440)] -
  Enrich LDAP users with name and mail address
- [[#4422](https://github.com/opencast/opencast/pull/4422)] -
  More ESLint Auto-Updates
- [[#4421](https://github.com/opencast/opencast/pull/4421)] -
  Fix Auto-update Tests
- [[#4418](https://github.com/opencast/opencast/pull/4418)] -
  Opencast 13 release schedule
- [[#4415](https://github.com/opencast/opencast/pull/4415)] -
  Publish Captions by Default
- [[#4408](https://github.com/opencast/opencast/pull/4408)] -
  Add support for multipart mails using text and HTML
- [[#4388](https://github.com/opencast/opencast/pull/4388)] -
  Use valid URL for admin interface mock data
- [[#4383](https://github.com/opencast/opencast/pull/4383)] -
  Simplify ldap user directory implementation
- [[#4380](https://github.com/opencast/opencast/pull/4380)] -
  Add organization properties to mail template data
- [[#4376](https://github.com/opencast/opencast/pull/4376)] -
  Allow use of extended metadata in send-email WOH
- [[#4359](https://github.com/opencast/opencast/pull/4359)] -
  Make PostgreSQL no longer experimental
- [[#4358](https://github.com/opencast/opencast/pull/4358)] -
  Add assert workflow operation
- [[#4357](https://github.com/opencast/opencast/pull/4357)] -
  Deprecated junit import
- [[#4351](https://github.com/opencast/opencast/pull/4351)] -
  Extract workflow filename filter
- [[#4349](https://github.com/opencast/opencast/pull/4349)] -
  Replace fallback element identifiers with UUIDs
- [[#4348](https://github.com/opencast/opencast/pull/4348)] -
  Use System cURL for Tests
- [[#4330](https://github.com/opencast/opencast/pull/4330)] -
  Enforce Workflow Operation Documentation Style
- [[#4319](https://github.com/opencast/opencast/pull/4319)] -
  Remove unused class "DispatchableComparator"
- [[#4280](https://github.com/opencast/opencast/pull/4280)] -
  ADD support for f4v file type
- [[#4236](https://github.com/opencast/opencast/pull/4236)] -
  Add equals() for AccessControlList
- [[#4232](https://github.com/opencast/opencast/pull/4232)] -
  Do not update default player components on legacy branches
- [[#4225](https://github.com/opencast/opencast/pull/4225)] -
  PoC: Karaf shell opencast:plugin* commands
- [[#4202](https://github.com/opencast/opencast/pull/4202)] -
  Add index on object_key field (needed when deleting assets).
- [[#4201](https://github.com/opencast/opencast/pull/4201)] -
  When archiving version 0 to store properties, include metadata from dc catalog
- [[#4186](https://github.com/opencast/opencast/pull/4186)] -
  Added correct type of keyword.
- [[#4181](https://github.com/opencast/opencast/pull/4181)] -
  Add support for multiple common metadata catalogs
- [[#4180](https://github.com/opencast/opencast/pull/4180)] -
  Add basic auth support ingest download
- [[#4157](https://github.com/opencast/opencast/pull/4157)] -
  Allow setting explicit id for themes
- [[#4156](https://github.com/opencast/opencast/pull/4156)] -
  Allow tags for ingesting attachments or catalogs via URL
- [[#4120](https://github.com/opencast/opencast/pull/4120)] -
  Use bulk inserts for all services during index rebuild
- [[#4116](https://github.com/opencast/opencast/pull/4116)] -
  Remove Entwine Functional Library from Execute Service
- [[#4109](https://github.com/opencast/opencast/pull/4109)] -
  Remove Entwine Functional Library from Userdirectory
- [[#4108](https://github.com/opencast/opencast/pull/4108)] -
  Remove code which is not loading any roles
- [[#4102](https://github.com/opencast/opencast/pull/4102)] -
  Move thumbnail preview generation to the client
- [[#4098](https://github.com/opencast/opencast/pull/4098)] -
  Change default resolution of live schedule impl to standard 16:9
- [[#4091](https://github.com/opencast/opencast/pull/4091)] -
  Generate more of the release notes
- [[#4082](https://github.com/opencast/opencast/pull/4082)] -
  named the correct directory - Update workflow.md
- [[#4061](https://github.com/opencast/opencast/pull/4061)] -
  Update List of Committers
- [[#4044](https://github.com/opencast/opencast/pull/4044)] -
  Target to released branches Paella 6 and 7 updates
- [[#4029](https://github.com/opencast/opencast/pull/4029)] -
  Add comments in the event index
- [[#4014](https://github.com/opencast/opencast/pull/4014)] -
  Fail on configuration error
- [[#4013](https://github.com/opencast/opencast/pull/4013)] -
  Faster Blend Effects on Cuts
- [[#4008](https://github.com/opencast/opencast/pull/4008)] -
  Fix Pom Version of Azure Transcription Service
- [[#3998](https://github.com/opencast/opencast/pull/3998)] -
  Add navigation shortcuts in admin ui modals
- [[#3940](https://github.com/opencast/opencast/pull/3940)] -
  Remove Helmet LTI Dependency
- [[#3933](https://github.com/opencast/opencast/pull/3933)] -
  Updates karma-jasmine to 5.1.0
- [[#3903](https://github.com/opencast/opencast/pull/3903)] -
  Common persistence util classes that also implement transaction retries
- [[#3893](https://github.com/opencast/opencast/pull/3893)] -
  Rerun Failed Tests
- [[#3892](https://github.com/opencast/opencast/pull/3892)] -
  Corrects return value documentation for ingest endpoint
- [[#3880](https://github.com/opencast/opencast/pull/3880)] -
  Update adopter deletion copy
- [[#3849](https://github.com/opencast/opencast/pull/3849)] -
  Amberscript configuration fix
- [[#3827](https://github.com/opencast/opencast/pull/3827)] -
  Missing setting track logical name when serverless HLS in process-smil
- [[#3799](https://github.com/opencast/opencast/pull/3799)] -
  Update maven documentation
- [[#3794](https://github.com/opencast/opencast/pull/3794)] -
  Replace Deprecated Code
- [[#3778](https://github.com/opencast/opencast/pull/3778)] -
  Update Branch Cut Announcement
- [[#3767](https://github.com/opencast/opencast/pull/3767)] -
  Patch for admin-ui-frontend Gruntfile.js when developing locally and using a remote proxy.
- [[#3741](https://github.com/opencast/opencast/pull/3741)] -
  ADD specialist worker nodes
- [[#3713](https://github.com/opencast/opencast/pull/3713)] -
  Add catalog ui adapter organization wildcard support
- [[#3670](https://github.com/opencast/opencast/pull/3670)] -
  Added track upload endpoint the External Events Api
- [[#3607](https://github.com/opencast/opencast/pull/3607)] -
  Extract JobDispatcher into its own class
- [[#3274](https://github.com/opencast/opencast/pull/3274)] -
  Index Extended Metadata
- [[#3218](https://github.com/opencast/opencast/pull/3218)] -
  Opencast Plugin Manager
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4510">4510</a>] -
  Bump html-validate from 7.10.0 to 7.10.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4505">4505</a>] -
  Bump eslint from 8.28.0 to 8.29.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4495">4495</a>] -
  Bump @babel/core from 7.20.2 to 7.20.5 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4492">4492</a>] -
  Bump paella-core from 1.8.9 to 1.11.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4480">4480</a>] -
  Bump eslint from 8.27.0 to 8.28.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4476">4476</a>] -
  Bump html-validate from 7.8.0 to 7.10.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4471">4471</a>] -
  Bump @babel/preset-env from 7.19.4 to 7.20.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4470">4470</a>] -
  Bump webpack-cli from 4.10.0 to 5.0.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4451">4451</a>] -
  Bump html-validate from 7.8.0 to 7.9.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4425">4425</a>] -
  Bump eslint from 8.26.0 to 8.27.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4424">4424</a>] -
  Bump eslint from 8.26.0 to 8.27.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4420">4420</a>] -
  Bump eslint from 8.26.0 to 8.27.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4419">4419</a>] -
  Bump eslint from 8.26.0 to 8.27.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4413">4413</a>] -
  Bump chromedriver from 107.0.2 to 107.0.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4403">4403</a>] -
  Bump @types/node from 18.8.3 to 18.11.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4401">4401</a>] -
  Bump react-select from 5.4.0 to 5.5.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4399">4399</a>] -
  Bump @types/jest from 29.1.2 to 29.2.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4398">4398</a>] -
  Bump i18next from 21.10.0 to 22.0.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4397">4397</a>] -
  Bump i18next-browser-languagedetector from 6.1.8 to 7.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4396">4396</a>] -
  Bump react-i18next from 11.18.6 to 12.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4395">4395</a>] -
  Bump @types/react-dom from 18.0.6 to 18.0.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4394">4394</a>] -
  Bump html-validate from 7.7.1 to 7.8.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4390">4390</a>] -
  Bump chromedriver from 107.0.1 to 107.0.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4389">4389</a>] -
  Bump jasmine-core from 4.4.0 to 4.5.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4384">4384</a>] -
  Bump chromedriver from 107.0.0 to 107.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4379">4379</a>] -
  Bump chromedriver from 106.0.1 to 107.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4372">4372</a>] -
  Bump html-validate from 7.7.0 to 7.7.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4364">4364</a>] -
  Bump eslint from 8.25.0 to 8.26.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4363">4363</a>] -
  Bump eslint from 8.25.0 to 8.26.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4362">4362</a>] -
  Bump eslint from 8.25.0 to 8.26.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4361">4361</a>] -
  Bump html-validate from 7.6.0 to 7.7.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4360">4360</a>] -
  Bump eslint from 8.25.0 to 8.26.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4335">4335</a>] -
  Bump jettison from 1.4.1 to 1.5.1</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4301">4301</a>] -
  Bump eslint from 8.24.0 to 8.25.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4300">4300</a>] -
  Bump eslint from 8.24.0 to 8.25.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4299">4299</a>] -
  Bump html-validate from 7.5.0 to 7.6.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4298">4298</a>] -
  Bump eslint from 8.24.0 to 8.25.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4297">4297</a>] -
  Bump eslint from 8.24.0 to 8.25.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4296">4296</a>] -
  Bump express from 4.18.1 to 4.18.2 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4292">4292</a>] -
  Bump @types/node from 18.7.23 to 18.8.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4290">4290</a>] -
  Bump i18next-browser-languagedetector from 6.1.5 to 6.1.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4286">4286</a>] -
  Bump bootstrap from 5.2.1 to 5.2.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4285">4285</a>] -
  Bump i18next from 21.9.2 to 21.10.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4284">4284</a>] -
  Bump @types/jest from 29.1.1 to 29.1.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4272">4272</a>] -
  Bump checker-qual from 3.25.0 to 3.26.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4264">4264</a>] -
  Bump @types/jest from 29.0.0 to 29.1.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4263">4263</a>] -
  Bump @types/node from 18.7.15 to 18.7.23 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4262">4262</a>] -
  Bump react-i18next from 11.18.5 to 11.18.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4260">4260</a>] -
  Bump i18next from 21.9.1 to 21.9.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4259">4259</a>] -
  Bump typescript from 4.8.2 to 4.8.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4258">4258</a>] -
  Bump bootstrap from 5.2.0 to 5.2.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4256">4256</a>] -
  Bump chromedriver from 105.0.1 to 106.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4246">4246</a>] -
  Bump underscore from 1.13.4 to 1.13.6 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4245">4245</a>] -
  Bump eslint from 8.23.1 to 8.24.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4244">4244</a>] -
  Bump eslint from 8.23.1 to 8.24.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4243">4243</a>] -
  Bump eslint from 8.23.1 to 8.24.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4234">4234</a>] -
  Bump sass from 1.54.9 to 1.55.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4223">4223</a>] -
  Bump chromedriver from 105.0.0 to 105.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4222">4222</a>] -
  Bump karma from 6.4.0 to 6.4.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4216">4216</a>] -
  Bump html-validate from 7.4.1 to 7.5.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4194">4194</a>] -
  Bump eslint from 8.23.0 to 8.23.1 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4193">4193</a>] -
  Bump eslint from 8.23.0 to 8.23.1 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4190">4190</a>] -
  Bump eslint from 8.23.0 to 8.23.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4189">4189</a>] -
  Bump html-validate from 7.3.3 to 7.4.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4176">4176</a>] -
  Bump sass from 1.54.4 to 1.54.9 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4166">4166</a>] -
  Bump @types/node from 18.7.14 to 18.7.15 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4160">4160</a>] -
  Bump jasmine-core from 4.3.0 to 4.4.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4152">4152</a>] -
  Bump checker-qual from 3.24.0 to 3.25.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4149">4149</a>] -
  Bump @types/jest from 28.1.6 to 29.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4148">4148</a>] -
  Bump @fortawesome/fontawesome-svg-core from 6.1.2 to 6.2.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4147">4147</a>] -
  Bump react-i18next from 11.18.3 to 11.18.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4146">4146</a>] -
  Bump typescript from 4.7.4 to 4.8.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4145">4145</a>] -
  Bump i18next-browser-languagedetector from 6.1.4 to 6.1.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4144">4144</a>] -
  Bump @types/node from 18.6.3 to 18.7.14 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4143">4143</a>] -
  Bump @fortawesome/free-solid-svg-icons from 6.1.2 to 6.2.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4141">4141</a>] -
  Bump i18next from 21.8.16 to 21.9.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4140">4140</a>] -
  Bump chromedriver from 104.0.0 to 105.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4139">4139</a>] -
  Bump react-bootstrap from 2.4.0 to 2.5.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4128">4128</a>] -
  Bump eslint from 8.22.0 to 8.23.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4127">4127</a>] -
  Bump jquery from 3.6.0 to 3.6.1 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4126">4126</a>] -
  Bump eslint from 8.22.0 to 8.23.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4125">4125</a>] -
  Bump eslint from 8.22.0 to 8.23.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4124">4124</a>] -
  Bump jquery from 3.6.0 to 3.6.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4123">4123</a>] -
  Bump jquery from 3.6.0 to 3.6.1 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4122">4122</a>] -
  Bump eslint from 8.22.0 to 8.23.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4121">4121</a>] -
  Bump eslint from 8.22.0 to 8.23.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4115">4115</a>] -
  Bump html-validate from 7.3.2 to 7.3.3 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4114">4114</a>] -
  Bump html-validate from 7.3.2 to 7.3.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4112">4112</a>] -
  Bump postgresql from 42.4.1 to 42.5.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4106">4106</a>] -
  Bump html-validate from 7.3.1 to 7.3.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4105">4105</a>] -
  Bump html-validate from 7.3.0 to 7.3.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4099">4099</a>] -
  Bump html-validate from 7.3.0 to 7.3.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4095">4095</a>] -
  Bump markdownlint-cli from 0.32.1 to 0.32.2 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4081">4081</a>] -
  Bump eslint from 8.21.0 to 8.22.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4080">4080</a>] -
  Bump eslint from 8.21.0 to 8.22.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4079">4079</a>] -
  Bump eslint from 8.21.0 to 8.22.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4078">4078</a>] -
  Bump eslint from 8.21.0 to 8.22.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4077">4077</a>] -
  Bump eslint from 8.21.0 to 8.22.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4071">4071</a>] -
  Bump html-validate from 7.2.0 to 7.3.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4070">4070</a>] -
  Bump html-validate from 7.2.0 to 7.3.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4069">4069</a>] -
  Bump sass from 1.54.3 to 1.54.4 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4060">4060</a>] -
  Bump eslint from 8.20.0 to 8.21.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4058">4058</a>] -
  Bump sass from 1.53.0 to 1.54.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4056">4056</a>] -
  Bump html-validate from 7.1.2 to 7.2.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4055">4055</a>] -
  Bump postgresql from 42.4.0 to 42.4.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4054">4054</a>] -
  Bump checker-qual from 3.23.0 to 3.24.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4052">4052</a>] -
  Bump chromedriver from 103.0.0 to 104.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4051">4051</a>] -
  Bump html-validate from 7.1.2 to 7.2.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4046">4046</a>] -
  Bump @babel/preset-env from 7.18.6 to 7.18.10 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4045">4045</a>] -
  Bump @babel/core from 7.18.6 to 7.18.10 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4042">4042</a>] -
  Bump eslint from 8.20.0 to 8.21.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4043">4043</a>] -
  Bump eslint from 8.20.0 to 8.21.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4041">4041</a>] -
  Bump i18next from 21.8.14 to 21.8.16 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4040">4040</a>] -
  Bump @fortawesome/free-solid-svg-icons from 6.1.1 to 6.1.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4039">4039</a>] -
  Bump @types/node from 18.0.6 to 18.6.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4038">4038</a>] -
  Bump react-i18next from 11.18.1 to 11.18.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4037">4037</a>] -
  Bump @fortawesome/fontawesome-svg-core from 6.1.1 to 6.1.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4036">4036</a>] -
  Bump eslint from 8.20.0 to 8.21.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4035">4035</a>] -
  Bump eslint from 8.20.0 to 8.21.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4021">4021</a>] -
  Bump paella-core from 1.1.5 to 1.2.4 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4020">4020</a>] -
  Bump markdownlint-cli from 0.31.1 to 0.32.1 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4019">4019</a>] -
  Bump webpack from 5.73.0 to 5.74.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4018">4018</a>] -
  Bump jasmine-core from 4.2.0 to 4.3.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4010">4010</a>] -
  Bump paella-zoom-plugin from 1.0.11 to 1.2.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4009">4009</a>] -
  Bump terser from 5.14.1 to 5.14.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4002">4002</a>] -
  Bump paella-slide-plugins from 1.0.11 to 1.2.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4001">4001</a>] -
  Bump @types/jest from 27.5.1 to 28.1.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4000">4000</a>] -
  Bump react-i18next from 11.18.0 to 11.18.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3999">3999</a>] -
  Bump bootstrap from 5.1.3 to 5.2.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3997">3997</a>] -
  Bump @types/node from 18.0.1 to 18.0.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3996">3996</a>] -
  Bump i18next from 21.8.12 to 21.8.14 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3992">3992</a>] -
  Bump @babel/eslint-parser from 7.18.2 to 7.18.9 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3989">3989</a>] -
  Bump paella-basic-plugins from 1.0.16 to 1.2.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3988">3988</a>] -
  Bump eslint from 8.19.0 to 8.20.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3987">3987</a>] -
  Bump eslint from 8.19.0 to 8.20.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3986">3986</a>] -
  Bump eslint from 8.19.0 to 8.20.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3985">3985</a>] -
  Bump eslint from 8.19.0 to 8.20.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3984">3984</a>] -
  Bump eslint from 8.19.0 to 8.20.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3980">3980</a>] -
  Bump checker-qual from 3.22.2 to 3.23.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3950">3950</a>] -
  Bump @types/react-dom from 18.0.5 to 18.0.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3949">3949</a>] -
  Bump react-i18next from 11.17.4 to 11.18.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3945">3945</a>] -
  Bump html-validate from 7.1.1 to 7.1.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3944">3944</a>] -
  Bump html-validate from 7.1.1 to 7.1.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3942">3942</a>] -
  Bump @types/node from 17.0.38 to 18.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3939">3939</a>] -
  Bump eslint from 8.18.0 to 8.19.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3938">3938</a>] -
  Bump eslint from 8.18.0 to 8.19.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3937">3937</a>] -
  Bump eslint from 8.18.0 to 8.19.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3936">3936</a>] -
  Bump eslint from 8.18.0 to 8.19.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3935">3935</a>] -
  Bump eslint from 8.18.0 to 8.19.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3918">3918</a>] -
  Bump webpack-dev-server from 4.9.2 to 4.9.3 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3917">3917</a>] -
  Bump jna from 5.11.0 to 5.12.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3916">3916</a>] -
  Bump mariadb-java-client from 3.0.5 to 3.0.6 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3900">3900</a>] -
  Bump sass from 1.52.3 to 1.53.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3899">3899</a>] -
  Bump chromedriver from 102.0.0 to 103.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3897">3897</a>] -
  Bump grunt-contrib-uglify from 5.2.1 to 5.2.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3889">3889</a>] -
  Bump eslint from 8.17.0 to 8.18.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3888">3888</a>] -
  Bump eslint from 8.17.0 to 8.18.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3887">3887</a>] -
  Bump eslint from 8.17.0 to 8.18.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3886">3886</a>] -
  Bump eslint from 8.17.0 to 8.18.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3885">3885</a>] -
  Bump eslint from 8.17.0 to 8.18.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3878">3878</a>] -
  Bump prometheus.version from 0.15.0 to 0.16.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3868">3868</a>] -
  Bump checker-qual from 3.22.1 to 3.22.2 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3863">3863</a>] -
  Bump karma from 6.3.20 to 6.4.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3859">3859</a>] -
  Bump webpack-cli from 4.9.2 to 4.10.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3858">3858</a>] -
  Bump @babel/core from 7.18.2 to 7.18.5 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3856">3856</a>] -
  Bump source-map-loader from 3.0.1 to 4.0.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3854">3854</a>] -
  Bump paella-basic-plugins from 1.0.15 to 1.0.16 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3852">3852</a>] -
  Bump postgresql from 42.3.6 to 42.4.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3850">3850</a>] -
  Bump jasmine-core from 4.1.1 to 4.2.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3846">3846</a>] -
  Bump sass from 1.52.1 to 1.52.3 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3843">3843</a>] -
  Bump webpack-dev-server from 4.9.1 to 4.9.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3842">3842</a>] -
  Bump chromedriver from 101.0.0 to 102.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3840">3840</a>] -
  Bump eslint from 8.16.0 to 8.17.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3839">3839</a>] -
  Bump eslint from 8.16.0 to 8.17.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3838">3838</a>] -
  Bump eslint from 8.16.0 to 8.17.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3836">3836</a>] -
  Bump eslint from 8.16.0 to 8.17.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3837">3837</a>] -
  Bump eslint from 8.16.0 to 8.17.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3834">3834</a>] -
  Bump underscore from 1.13.3 to 1.13.4 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3829">3829</a>] -
  Bump checker-qual from 3.22.0 to 3.22.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3826">3826</a>] -
  Bump webpack-dev-server from 4.9.0 to 4.9.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3825">3825</a>] -
  Bump @types/node from 17.0.33 to 17.0.38 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3824">3824</a>] -
  Bump @types/react-dom from 18.0.4 to 18.0.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3823">3823</a>] -
  Bump i18next from 21.8.1 to 21.8.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3822">3822</a>] -
  Bump react-bootstrap from 2.3.1 to 2.4.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3821">3821</a>] -
  Bump typescript from 4.6.4 to 4.7.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3815">3815</a>] -
  Bump paella-user-tracking from 1.0.13 to 1.0.14 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3804">3804</a>] -
  Bump @babel/eslint-parser from 7.17.0 to 7.18.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3801">3801</a>] -
  Bump mariadb-java-client from 3.0.4 to 3.0.5 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3796">3796</a>] -
  Bump postgresql from 42.3.5 to 42.3.6 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3793">3793</a>] -
  Bump eslint from 8.15.0 to 8.16.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3792">3792</a>] -
  Bump html-validate from 7.1.0 to 7.1.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3791">3791</a>] -
  Bump eslint from 8.15.0 to 8.16.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3790">3790</a>] -
  Bump eslint from 8.15.0 to 8.16.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3789">3789</a>] -
  Bump html-validate from 7.1.0 to 7.1.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3788">3788</a>] -
  Bump eslint from 8.15.0 to 8.16.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3787">3787</a>] -
  Bump sass from 1.50.1 to 1.52.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3786">3786</a>] -
  Bump eslint from 8.15.0 to 8.16.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3784">3784</a>] -
  Bump @babel/core from 7.17.10 to 7.18.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3783">3783</a>] -
  Bump @babel/preset-env from 7.17.10 to 7.18.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3776">3776</a>] -
  Bump copy-webpack-plugin from 10.2.4 to 11.0.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3771">3771</a>] -
  Bump paella-core from 1.0.49 to 1.0.51 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3757">3757</a>] -
  Bump webpack from 5.72.0 to 5.72.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3730">3730</a>] -
  Bump express from 4.17.3 to 4.18.1 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/3714">3714</a>] -
  Bump esapi from 2.1.0.1 to 2.3.0.0</li>
</ul>
</details>

Opencast 12
-----------

### Opencast 12.13

*Released on August 8th, 2023*

- [[#5158](https://github.com/opencast/opencast/pull/5158)] -
  Update maven-bundle-plugin to latest version to fix invalid
  zip headers in jar files distributed by Opencast; otherwise
  Opencast would not start up with OpenJDK 11.0.20.

### Opencast 12.12

*Released on June 15th, 2023*


- [[#5017](https://github.com/opencast/opencast/pull/5017)] -
  Properly encode adopter registration data
- [[#4964](https://github.com/opencast/opencast/pull/4964)] -
  Fix User ACL Wording
- [[#4963](https://github.com/opencast/opencast/pull/4963)] -
  Fix Wording of Tobira Integration
- [[#4697](https://github.com/opencast/opencast/pull/4697)] -
  Remove duplicated `oc_workflow` columns
- [[#4658](https://github.com/opencast/opencast/pull/4658)] -
  Editor service mediapackage locking

### Opencast 12.11

*Released on May 16th, 2023*

- [[#4929](https://github.com/opencast/opencast/pull/4929)] -
  Fix template losing user role when creating an event in the admin ui
- [[#4877](https://github.com/opencast/opencast/pull/4877)] -
  Engage UI docs
- [[#4874](https://github.com/opencast/opencast/pull/4874)] -
  Upload Allinone as Actions Workflow Artifact
- [[#4872](https://github.com/opencast/opencast/pull/4872)] -
  Fixing adopter statistics data gathering
- [[#4863](https://github.com/opencast/opencast/pull/4863)] -
  Fixed long WF description 11_to_12 DB upgrade script bug for Maria and PSQL
- [[#4862](https://github.com/opencast/opencast/pull/4862)] -
  Serviceregistry activatation optimization
- [[#4848](https://github.com/opencast/opencast/pull/4848)] -
  Service statistics DB query optimazation

### Opencast 12.10

*Released on March 27th, 2023*

- [[#4799](https://github.com/opencast/opencast/pull/4799)] -
  Sets values in specific config file to default

### Opencast 12.9

*Released on March 16th, 2023*

- [[#4764](https://github.com/opencast/opencast/pull/4764)] -
  Editor remote enforce charset for StringEntity
- [[#4757](https://github.com/opencast/opencast/pull/4757)] -
  added missing publish-uploaded-assets.xml workflow

### Opencast 12.8

*Released on February 20th, 2023*

- [[#4699](https://github.com/opencast/opencast/pull/4699)] -
  Emphasize need to configure debs
- [[#4659](https://github.com/opencast/opencast/pull/4659)] -
  Do not allow consecutive dots in file names
- [[#4653](https://github.com/opencast/opencast/pull/4653)] -
  Document all available workflow operations
- [[#4620](https://github.com/opencast/opencast/pull/4620)] -
  Also check custom actions for unsaved ACL changes
- [[#4616](https://github.com/opencast/opencast/pull/4616)] -
  Fix adopter data gathering bugs
- [[#4614](https://github.com/opencast/opencast/pull/4614)] -
  Remove RPM instructions from nightly documentation
- [[#4596](https://github.com/opencast/opencast/pull/4596)] -
  Always show save button for series access rights

### Opencast 12.7

*Released on January 18th, 2023*

- [[#4590](https://github.com/opencast/opencast/pull/4590)] -
  Add Tag-Engage Workflow Operation Handler
- [[#4563](https://github.com/opencast/opencast/pull/4563)] -
  Fix Wowza signing time scale
- [[#4550](https://github.com/opencast/opencast/pull/4550)] -
  Ensure segments.segment is array
- [[#4549](https://github.com/opencast/opencast/pull/4549)] -
  Sync login script of paella-6 and paella-7
- [[#4539](https://github.com/opencast/opencast/pull/4539)] -
  Make event updating faster after changes to series metadata/ACL

### Opencast 12.6

*Released on December 14th, 2022*

- [[#4528](https://github.com/opencast/opencast/pull/4528)] -
  Allow creating releases manually
- [[#4523](https://github.com/opencast/opencast/pull/4523)] -
  Allow `ROLE_ADMIN_UI` to run index.js
- [[#4518](https://github.com/opencast/opencast/pull/4518)] -
  Remove braces from error message
- [[#4496](https://github.com/opencast/opencast/pull/4496)] -
  Fix Firefox install for admin ui tests in GHA
- [[#4494](https://github.com/opencast/opencast/pull/4494)] -
  Decode URL before parsing in PaellaPlayer 6
- [[#4486](https://github.com/opencast/opencast/pull/4486)] -
  Do not forget videos detected previously in Paella
- [[#4459](https://github.com/opencast/opencast/pull/4459)] -
  Update Release Assemblies

### Opencast 12.5

*Released on November 16th, 2022*

- [[#4441](https://github.com/opencast/opencast/pull/4441)] -
  Azure transcription service improvements
- [[#4439](https://github.com/opencast/opencast/pull/4439)] -
  Fix Tobira API version
- [[#4436](https://github.com/opencast/opencast/pull/4436)] -
  Batch Dependabot Updates for Paella 7
- [[#4416](https://github.com/opencast/opencast/pull/4416)] -
  Allow dots in template file names
- [[#4411](https://github.com/opencast/opencast/pull/4411)] -
  Let Editor know if files are available locally
- [[#4410](https://github.com/opencast/opencast/pull/4410)] -
  Replace Entwine Functional Library in Editor
- [[#4406](https://github.com/opencast/opencast/pull/4406)] -
  Batch Dependabot Updates for Paella 7
- [[#4392](https://github.com/opencast/opencast/pull/4392)] -
  Batch Dependabot Updates for Paella 7
- [[#4391](https://github.com/opencast/opencast/pull/4391)] -
  Batch Dependabot Updates for Paella 7
- [[#4381](https://github.com/opencast/opencast/pull/4381)] -
  Fix workflow typo in docs
- [[#4378](https://github.com/opencast/opencast/pull/4378)] -
  Drop Hold State Test
- [[#4377](https://github.com/opencast/opencast/pull/4377)] -
  Do not add roleprefix to extra roles
- [[#4375](https://github.com/opencast/opencast/pull/4375)] -
  Use UTF-8 as charset for HTML mails
- [[#4370](https://github.com/opencast/opencast/pull/4370)] -
  Correctly identify the master playlist in the Tobira harvest
- [[#4353](https://github.com/opencast/opencast/pull/4353)] -
  Fully Automate GitHub Release
- [[#4352](https://github.com/opencast/opencast/pull/4352)] -
  Automate Release Process
- [[#4350](https://github.com/opencast/opencast/pull/4350)] -
  Lower JWT error logging to debug level
<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4373">4373</a>] -
  Bump html-validate from 7.6.0 to 7.7.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4367">4367</a>] -
  Bump eslint from 8.25.0 to 8.26.0 in /modules/engage-paella-player-7</li>
</ul>
</details>

### Opencast 12.4

*Released on October 19th, 2022*

- [[#4346](https://github.com/opencast/opencast/pull/4346)] -
  Generate more of the release notes
- [[#4345](https://github.com/opencast/opencast/pull/4345)] -
  Add Opencast 12.4 release notes
- [[#4340](https://github.com/opencast/opencast/pull/4340)] -
  Fix saving thumbnails if no previous ones existed
- [[#4329](https://github.com/opencast/opencast/pull/4329)] -
  Make Workflow Operation Docs Look Alike
- [[#4328](https://github.com/opencast/opencast/pull/4328)] -
  Specify Units in Video Clips
- [[#4324](https://github.com/opencast/opencast/pull/4324)] -
  Update hostname to use server url in adopter registration information
- [[#4322](https://github.com/opencast/opencast/pull/4322)] -
  Make Workflow Operation Docs Look Alike
- [[#4317](https://github.com/opencast/opencast/pull/4317)] -
  Fix NullPointerException when editor does not send subtitles
- [[#4314](https://github.com/opencast/opencast/pull/4314)] -
  Reuse mediapackage object from workflow instance where possible
- [[#4313](https://github.com/opencast/opencast/pull/4313)] -
  Skip check if user has global admin role
- [[#4308](https://github.com/opencast/opencast/pull/4308)] -
  Save button for event/series "Access policy"-tab
- [[#4307](https://github.com/opencast/opencast/pull/4307)] -
  Publish Captions by Default
- [[#4294](https://github.com/opencast/opencast/pull/4294)] -
  Batch Dependabot Updates for Paella 7
- [[#4287](https://github.com/opencast/opencast/pull/4287)] -
  Update editor release 2022-10-19
- [[#4276](https://github.com/opencast/opencast/pull/4276)] -
  Batch Dependabot Updates for Paella 7
- [[#4274](https://github.com/opencast/opencast/pull/4274)] -
  Fix single radio button in new-task UI
- [[#4270](https://github.com/opencast/opencast/pull/4270)] -
  Add back workflow API documentation
- [[#4269](https://github.com/opencast/opencast/pull/4269)] -
  Fix snapshot moving
- [[#4240](https://github.com/opencast/opencast/pull/4240)] -
  Group dependabot Paella 7 commits
- [[#4238](https://github.com/opencast/opencast/pull/4238)] -
  Fixed azure transcription error message in case of Error
- [[#4229](https://github.com/opencast/opencast/pull/4229)] -
  Make busy waiting explicit during InboxScannerService config update

<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4337">4337</a>] -
  Bump paella-core from 1.7.0 to 1.8.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4311">4311</a>] -
  Bump @babel/preset-env from 7.19.3 to 7.19.4 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4306">4306</a>] -
  Bump source-map-loader from 4.0.0 to 4.0.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4305">4305</a>] -
  Bump html-validate from 7.5.0 to 7.6.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4304">4304</a>] -
  Bump express from 4.18.1 to 4.18.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4303">4303</a>] -
  Bump eslint from 8.24.0 to 8.25.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4302">4302</a>] -
  Bump paella-slide-plugins from 1.2.1 to 1.7.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4247">4247</a>] -
  Bump eslint from 8.23.1 to 8.24.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4227">4227</a>] -
  Bump mariadb-java-client from 3.0.7 to 3.0.8 in /modules/db</li>
</ul>
</details>

### Opencast 12.3

*Released on September 21th, 2022*

- [[#4220](https://github.com/opencast/opencast/pull/4220)] -
  Remove unused configuration
- [[#4219](https://github.com/opencast/opencast/pull/4219)] -
  Ignore NoSuchFileException when cleaning directories
- [[#4213](https://github.com/opencast/opencast/pull/4213)] -
  Fix check if all workflows were loaded when using YAML
- [[#4207](https://github.com/opencast/opencast/pull/4207)] -
  Do not run dependabot pushes multiple times
- [[#4187](https://github.com/opencast/opencast/pull/4187)] -
  Dont run gh actions on dbot multiple times
- [[#4178](https://github.com/opencast/opencast/pull/4178)] -
  Fix filename dependency on locale
- [[#4151](https://github.com/opencast/opencast/pull/4151)] -
  Allow global admins to always get workflow instances for a media package
- [[#4137](https://github.com/opencast/opencast/pull/4137)] -
  Delete scheduled events fixed (fixes #4084)
- [[#4134](https://github.com/opencast/opencast/pull/4134)] -
  Update CentOS Stream/RHEL documentation
- [[#4129](https://github.com/opencast/opencast/pull/4129)] -
  Add Tobira module for Synchronization with an external Tobira application
- [[#4113](https://github.com/opencast/opencast/pull/4113)] -
  Add Dummy capture agent documentation
- [[#4111](https://github.com/opencast/opencast/pull/4111)] -
  add missing reference target
- [[#4107](https://github.com/opencast/opencast/pull/4107)] -
  Fix Publication to Workspace Operation Documentation
- [[#4104](https://github.com/opencast/opencast/pull/4104)] -
  Document broken MariaDB upgrade script
- [[#4103](https://github.com/opencast/opencast/pull/4103)] -
  Revert try-catch in 12.x MariaDB upgrade script
- [[#4101](https://github.com/opencast/opencast/pull/4101)] -
  Fixes mysql upgrade script stop error
- [[#4094](https://github.com/opencast/opencast/pull/4094)] -
  Replace DownloadDistributionService target filter with aws version

<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4197">4197</a>] -
  Bump eslint from 8.23.0 to 8.23.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4177">4177</a>] -
  Bump webpack-dev-server from 4.9.3 to 4.11.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4168">4168</a>] -
  Bump html-validate from 7.2.0 to 7.3.3 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/4165">4165</a>] -
  Bump eslint from 8.21.0 to 8.23.0 in /modules/engage-paella-player-7</li>
</ul>
</details>

### Opencast 12.2

*Released on August 17th, 2022*

- [[#4087](https://github.com/opencast/opencast/pull/4087)] -
  Link the docs for the publication to workspace WOH
- [[#4064](https://github.com/opencast/opencast/pull/4064)] -
  Fallback for Event Title in Publication
- [[#4063](https://github.com/opencast/opencast/pull/4063)] -
  Warn About Removed Workflow Endpoint
- [[#4057](https://github.com/opencast/opencast/pull/4057)] -
  Fixed OSGI annotation for OaiPmhServer
- [[#4049](https://github.com/opencast/opencast/pull/4049)] -
  Update Opencast Studio to 2022-08-03
- [[#4048](https://github.com/opencast/opencast/pull/4048)] -
  Don't Fail on Missing Jobs
- [[#4032](https://github.com/opencast/opencast/pull/4032)] -
  Clarify migration script dependencies and make aware of the mysql_connector_python error
- [[#4031](https://github.com/opencast/opencast/pull/4031)] -
  Remove ManagedService from LtiServiceImpl
- [[#4030](https://github.com/opencast/opencast/pull/4030)] -
  Removing remaining activemq docs
- [[#4027](https://github.com/opencast/opencast/pull/4027)] -
  Add new config `list-all-jobs-in-series` to LtiServiceImpl
- [[#4025](https://github.com/opencast/opencast/pull/4025)] -
  Update CAS docs
- [[#4016](https://github.com/opencast/opencast/pull/4016)] -
  Force inject ChainingMediaPackageSerializer as MediaPackageSeriailzer
- [[#4015](https://github.com/opencast/opencast/pull/4015)] -
  Remove ActiveMQ related steps from doc
- [[#4012](https://github.com/opencast/opencast/pull/4012)] -
  Prevent Redirect When Going to Studio
- [[#4005](https://github.com/opencast/opencast/pull/4005)] -
  Allow Admins Access to Editor Interface
- [[#4004](https://github.com/opencast/opencast/pull/4004)] -
  MariaDB Connector Bugfix Update
- [[#3995](https://github.com/opencast/opencast/pull/3995)] -
  Avoid GitHub Actions Scheduling Conflicts

<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/4059">4059</a>] -
  Bump mariadb-java-client from 3.0.6 to 3.0.7 in /modules/db</li>
</ul>
</details>

### Opencast 12.1

*Released on July 20th, 2022*

- [[#3966](https://github.com/opencast/opencast/pull/3966)] -
  Mark Opencast 12 RPMs as ready
- [[#3965](https://github.com/opencast/opencast/pull/3965)] -
  Update Editor to Version 2022-07-08
- [[#3964](https://github.com/opencast/opencast/pull/3964)] -
  Backport Paella 7 updates from develop
- [[#3958](https://github.com/opencast/opencast/pull/3958)] -
  Fail early on missing encoding profile
- [[#3957](https://github.com/opencast/opencast/pull/3957)] -
  Fix sendmail UnsupportedDataType exception
- [[#3954](https://github.com/opencast/opencast/pull/3954)] -
  Handle process stdout and stderr
- [[#3951](https://github.com/opencast/opencast/pull/3951)] -
  Allow for more diverse exception throwing WorkflowService.start()
- [[#3941](https://github.com/opencast/opencast/pull/3941)] -
  Fix MariaDB Database Driver
- [[#3920](https://github.com/opencast/opencast/pull/3920)] -
  Automate the language selection in the transcription workflow of amberscript
- [[#3913](https://github.com/opencast/opencast/pull/3913)] -
  Allow Inbox Scanner to parse date formats
- [[#3912](https://github.com/opencast/opencast/pull/3912)] -
  Always store ACE entries in JSON as array
- [[#3908](https://github.com/opencast/opencast/pull/3908)] -
  Add authentication entry point with redirect query parameter support
- [[#3906](https://github.com/opencast/opencast/pull/3906)] -
  Add "Create new event with metadata from another event" button to Admin UI
- [[#3901](https://github.com/opencast/opencast/pull/3901)] -
  Paella 7: Use master tracks in HLS serverless mode
- [[#3896](https://github.com/opencast/opencast/pull/3896)] -
  Add Azure Transcription Integration
- [[#3890](https://github.com/opencast/opencast/pull/3890)] -
  fix brightspace api with large datasets
- [[#3883](https://github.com/opencast/opencast/pull/3883)] -
  Add formal Opensearch support
- [[#3876](https://github.com/opencast/opencast/pull/3876)] -
  Remove Solr from Admin Node
- [[#3874](https://github.com/opencast/opencast/pull/3874)] -
  Update Changelog
- [[#3873](https://github.com/opencast/opencast/pull/3873)] -
  Finish the Opencast 12 release notes
- [[#3872](https://github.com/opencast/opencast/pull/3872)] -
  Opencast 12 Upgrade Guide


### Opencast 12.0

Dependabot's changes are excluded from this log.

*Released on June 15th, 2022*

- [[#3870](https://github.com/opencast/opencast/pull/3870)] -
  Update Editor to Version 2022-06-15
- [[#3867](https://github.com/opencast/opencast/pull/3867)] -
  Update Opencast Studio to 2022-06-15
- [[#3866](https://github.com/opencast/opencast/pull/3866)] -
  Add description to publish workflow
- [[#3865](https://github.com/opencast/opencast/pull/3865)] -
  Rename yaml workflow title
- [[#3864](https://github.com/opencast/opencast/pull/3864)] -
  Fix duplicate dependency declaration
- [[#3862](https://github.com/opencast/opencast/pull/3862)] -
  Make Delete Dialog Look Alike
- [[#3851](https://github.com/opencast/opencast/pull/3851)] -
  Fix submitter not being displayed in the event workflow details
- [[#3830](https://github.com/opencast/opencast/pull/3830)] -
  Paella7 fix usertracking
- [[#3828](https://github.com/opencast/opencast/pull/3828)] -
  Further workflow test race fixes
- [[#3819](https://github.com/opencast/opencast/pull/3819)] -
  Update paella7 configuration
- [[#3818](https://github.com/opencast/opencast/pull/3818)] -
  Workflow tests should no longer race
- [[#3816](https://github.com/opencast/opencast/pull/3816)] -
  Removing unnecessary workflow index data from rebuild
- [[#3813](https://github.com/opencast/opencast/pull/3813)] -
  Properly unregister OAI-PMH servlet
- [[#3812](https://github.com/opencast/opencast/pull/3812)] -
  Fix rest services are unavailable under certain circumstances
- [[#3811](https://github.com/opencast/opencast/pull/3811)] -
  Remove red check mark when editing metadata
- [[#3810](https://github.com/opencast/opencast/pull/3810)] -
  No Elasticsearch Use In Ingest
- [[#3806](https://github.com/opencast/opencast/pull/3806)] -
  Speech to text language fallback and placeholder
- [[#3780](https://github.com/opencast/opencast/pull/3780)] -
  Send waveform URIs to new editor
- [[#3775](https://github.com/opencast/opencast/pull/3775)] -
  Indexing only latest workflows for all events
- [[#3554](https://github.com/opencast/opencast/pull/3554)] -
  Copy Mediapackage elements from the publication to mediapackage
- [[#1370](https://github.com/opencast/opencast/pull/1370)] -
  Fix Conflict Detection
- [[#3766](https://github.com/opencast/opencast/pull/3766)] -
  Fix Conductor OSGi Bindings
- [[#3751](https://github.com/opencast/opencast/pull/3751)] -
  Update paella 7 dependencies
- [[#3749](https://github.com/opencast/opencast/pull/3749)] -
  Explore H2 Database
- [[#3718](https://github.com/opencast/opencast/pull/3718)] -
  fix transcription workflowoperation dependency
- [[#3696](https://github.com/opencast/opencast/pull/3696)] -
  Remove Entwine Library from Media Inspector
- [[#3695](https://github.com/opencast/opencast/pull/3695)] -
  Remove Unused Code
- [[#3693](https://github.com/opencast/opencast/pull/3693)] -
  fix cover-image-remote package import
- [[#3691](https://github.com/opencast/opencast/pull/3691)] -
  Deprecate Media Package Internal Metadata
- [[#3689](https://github.com/opencast/opencast/pull/3689)] -
  Less Entwine Functional Library
- [[#3687](https://github.com/opencast/opencast/pull/3687)] -
  Changed inbox behaviour for additional files for scheduled events
- [[#3685](https://github.com/opencast/opencast/pull/3685)] -
  Silence npm autoupdate
- [[#3671](https://github.com/opencast/opencast/pull/3671)] -
  Fix Misspelled Variable
- [[#3659](https://github.com/opencast/opencast/pull/3659)] -
  Fix Merge of JavaScript Auto-Updates
- [[#3658](https://github.com/opencast/opencast/pull/3658)] -
  Fix list formatting in developer guide
- [[#3632](https://github.com/opencast/opencast/pull/3632)] -
  Fix build of Paella Player 7  when local npm is being used (mvn install -Pfrontend-no-prebuilt)
- [[#3628](https://github.com/opencast/opencast/pull/3628)] -
  Sort Modules Alphabetically
- [[#3627](https://github.com/opencast/opencast/pull/3627)] -
  Fix missing OSGI annotation for Caption service converters
- [[#3620](https://github.com/opencast/opencast/pull/3620)] -
  Update link to the annotion-tool list in the docs
- [[#3601](https://github.com/opencast/opencast/pull/3601)] -
  Fix smil marshalling for mediapackage tracks typed as SmilMediaReferenceImpl
- [[#3584](https://github.com/opencast/opencast/pull/3584)] -
  Updated the list of mailinglists on the docs landing page
- [[#3566](https://github.com/opencast/opencast/pull/3566)] -
  Auto update engage-ui test libraries
- [[#3552](https://github.com/opencast/opencast/pull/3552)] -
  Fix version sorting in Matrix room bot
- [[#3551](https://github.com/opencast/opencast/pull/3551)] -
  Silence npm auto update in the case where no updates are needed
- [[#3549](https://github.com/opencast/opencast/pull/3549)] -
  Auto-update all Theodul JavaScript Test libraries
- [[#3542](https://github.com/opencast/opencast/pull/3542)] -
  Upgrade Karaf and CXF
- [[#3521](https://github.com/opencast/opencast/pull/3521)] -
  Support yaml workflow definition
- [[#3516](https://github.com/opencast/opencast/pull/3516)] -
  Keyboard shortcut permission
- [[#3507](https://github.com/opencast/opencast/pull/3507)] -
  Auto-update JavaScript Test libraries
- [[#3506](https://github.com/opencast/opencast/pull/3506)] -
  Admin UI Node Version
- [[#3500](https://github.com/opencast/opencast/pull/3500)] -
  Code Cleanup
- [[#3471](https://github.com/opencast/opencast/pull/3471)] -
  Matrix room description update fix
- [[#3461](https://github.com/opencast/opencast/pull/3461)] -
  Fix meeting time
- [[#3443](https://github.com/opencast/opencast/pull/3443)] -
  Regenerate `package-lock.json`
- [[#3441](https://github.com/opencast/opencast/pull/3441)] -
  Update EasyMock to 4.3
- [[#3440](https://github.com/opencast/opencast/pull/3440)] -
  Update Jackson Libraries to 2.13.1
- [[#3439](https://github.com/opencast/opencast/pull/3439)] -
  Update PostegreSQL Driver
- [[#3438](https://github.com/opencast/opencast/pull/3438)] -
  Update MariaDB Driver
- [[#3431](https://github.com/opencast/opencast/pull/3431)] -
  Remove ManagedService from OaiPmhServer
- [[#3424](https://github.com/opencast/opencast/pull/3424)] -
  Remove reference annotation name attribute if possible
- [[#3422](https://github.com/opencast/opencast/pull/3422)] -
  Refactor XACMLAuthorizationService
- [[#3385](https://github.com/opencast/opencast/pull/3385)] -
  Migrate to annotations cleanup
- [[#3377](https://github.com/opencast/opencast/pull/3377)] -
  Remove Solr from WorkflowService
- [[#3376](https://github.com/opencast/opencast/pull/3376)] -
  Change the way workflows are stored in the database from XML to tables
- [[#3366](https://github.com/opencast/opencast/pull/3366)] -
  Update pom version for develop
- [[#3326](https://github.com/opencast/opencast/pull/3326)] -
  Add recommended practices for Release manager
- [[#3323](https://github.com/opencast/opencast/pull/3323)] -
  Fix branches on docs.opencast.org
- [[#3296](https://github.com/opencast/opencast/pull/3296)] -
  Added missing endpoint to documentation of base api.
- [[#3262](https://github.com/opencast/opencast/pull/3262)] -
  Modernize Code
- [[#3255](https://github.com/opencast/opencast/pull/3255)] -
  Migrate to declarative services
- [[#3250](https://github.com/opencast/opencast/pull/3250)] -
  Improve deleting events from index
- [[#3204](https://github.com/opencast/opencast/pull/3204)] -
  Remove Solr from series service
- [[#3187](https://github.com/opencast/opencast/pull/3187)] -
  Mediapackage directory cleanup
- [[#3167](https://github.com/opencast/opencast/pull/3167)] -
  Fix#3164 assetmanager move to s3
- [[#3162](https://github.com/opencast/opencast/pull/3162)] -
  Paella player 7
- [[#3161](https://github.com/opencast/opencast/pull/3161)] -
  Removed Solr from Series Service
- [[#3159](https://github.com/opencast/opencast/pull/3159)] -
  Remove check for tracks for OAI-PMH
- [[#3100](https://github.com/opencast/opencast/pull/3100)] -
  Remove ActiveMQ dependencies

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

Opencast 10
----------

### Opencast 10.14

*Released on June 14th, 2022*

- [[#3634](https://github.com/opencast/opencast/pull/3634)] -
  Update adopter registration code
- [[#3781](https://github.com/opencast/opencast/pull/3781)] -
  Add Placeholders for Video Resolution to Rename-Files


### Opencast 10.13

*Released on May 11th, 2022*

- [[#3684](https://github.com/opencast/opencast/pull/3684)] -
  Backport matrix room title fixes into 10.x
- [[#3674](https://github.com/opencast/opencast/pull/3674)] -
  Avoid rewriteUrls being invoked twice while loading mediapackage.
- [[#3666](https://github.com/opencast/opencast/pull/3666)] -
  Update Paella 6.5.6 config.json


### Opencast 10.12

*Released on Apr 19th, 2022*

- [[#3660](https://github.com/opencast/opencast/pull/3660)] -
  Update Editor to 2022-03-22 and OC Studio to 2022-02-16 for OC 10.x
- [[#3650](https://github.com/opencast/opencast/pull/3650)] -
  Fixes admin-ui repeated series name display #3600
- [[#3626](https://github.com/opencast/opencast/pull/3626)] -
  Update paella player to paella 6.5.6


### Opencast 10.11

*Released on Mar 22nd, 2022*

- [[#3533](https://github.com/opencast/opencast/pull/3533)] -
  Add activemq upgrade note to 10.x upgrade guide
- [[#3531](https://github.com/opencast/opencast/pull/3531)] -
  Revert "Update paella player to paella 6.5.5 (#3142)" since HLS videos do not load on slow connections
- [[#3517](https://github.com/opencast/opencast/pull/3517)] -
  Admin-ui-frontend migrate node-sass to sass
- [[#3512](https://github.com/opencast/opencast/pull/3512)] -
  Improve Tooltips in Documentation
- [[#3490](https://github.com/opencast/opencast/pull/3490)] -
  Document that you cannot run Opencast in a sub-path
- [[#3467](https://github.com/opencast/opencast/pull/3467)] -
  Opencast 10.10 release notes and changelog


### Opencast 10.10

*Released on Feb 17th, 2022*

- [[#3427](https://github.com/opencast/opencast/pull/3427)] -
  Fix Typo in Opencast 10 Upgrade Guide
- [[#3415](https://github.com/opencast/opencast/pull/3415)] -
  Update pax logging from version 1.11.13 to 1.11.14
- [[#3414](https://github.com/opencast/opencast/pull/3414)] -
  Revert "update karaf from version 4.2.9 to 4.2.15"
- [[#3390](https://github.com/opencast/opencast/pull/3390)] -
  Endpoint to Remove Tracked Host Bundles
- [[#3389](https://github.com/opencast/opencast/pull/3389)] -
  Rename Files Workflow Operation Handler
- [[#3386](https://github.com/opencast/opencast/pull/3386)] -
  update karaf from version 4.2.9 to 4.2.15
- [[#3373](https://github.com/opencast/opencast/pull/3373)] -
  Adding Camtasia .trec support.
- [[#3372](https://github.com/opencast/opencast/pull/3372)] -
  Fix port checking
- [[#3364](https://github.com/opencast/opencast/pull/3364)] -
  Automatically Update Matrix Room
- [[#3363](https://github.com/opencast/opencast/pull/3363)] -
  Automated Release Title
- [[#3362](https://github.com/opencast/opencast/pull/3362)] -
  ConfigurableRetractWorkflowOperationHandle: fixed exception message
- [[#3361](https://github.com/opencast/opencast/pull/3361)] -
  Fix metadata for multitenancy
- [[#3359](https://github.com/opencast/opencast/pull/3359)] -
  Document Capture Agent Input Selection
- [[#3345](https://github.com/opencast/opencast/pull/3345)] -
  Fix Javadocs
- [[#2811](https://github.com/opencast/opencast/pull/2811)] -
  Add download artifacts in engage publication


### Opencast 10.9

*Released on January 18th, 2022*

- [[#3341](https://github.com/opencast/opencast/pull/3341)] -
  Update Nexus related documentation
- [[#3339](https://github.com/opencast/opencast/pull/3339)] -
  Fix Scheduler Conflict Checks
- [[#3334](https://github.com/opencast/opencast/pull/3334)] -
  Warn against using CIFS for Inbox
- [[#3322](https://github.com/opencast/opencast/pull/3322)] -
  Show input selection only if applicable
- [[#3305](https://github.com/opencast/opencast/pull/3305)] -
  Update to pax-logging 1.11.13
- [[#3300](https://github.com/opencast/opencast/pull/3300)] -
  Fixup osgi services and xmls
- [[#3299](https://github.com/opencast/opencast/pull/3299)] -
  Remove Unused Additional Log4J
- [[#3298](https://github.com/opencast/opencast/pull/3298)] -
  Automate GitHub Release
- [[#3295](https://github.com/opencast/opencast/pull/3295)] -
  Fix Database Connection Tests
- [[#3294](https://github.com/opencast/opencast/pull/3294)] -
  Update Debian install documentation
- [[#3293](https://github.com/opencast/opencast/pull/3293)] -
  Fix pax-logging in startup.properties
- [[#3283](https://github.com/opencast/opencast/pull/3283)] -
  Opencast 10.8 Release Notes
- [[#3251](https://github.com/opencast/opencast/pull/3251)] -
  Inbox may fail at startup


### Opencast 10.8

*Released on December 20th, 2021*

- [[#3282](https://github.com/opencast/opencast/pull/3282)]
  Security: Update to Pax Logging 1.11.12


### Opencast 10.7

*Released on December 17th, 2021*

- [[#3275](https://github.com/opencast/opencast/pull/3275)] -
  Update to Pax Logging 1.11.11
- [[#3261](https://github.com/opencast/opencast/pull/3261)] -
  Update Elasticsearch container image to 7.10.2
- [[#3248](https://github.com/opencast/opencast/pull/3248)] -
  Switch to Java Optional
- [[#3223](https://github.com/opencast/opencast/pull/3223)] -
  Fix multiple extended metadata catalogs for series

### Opencast 10.6

*Released on December 13th, 2021*

- [[GHSA-hcxx-mp6g-6gr9](https://github.com/opencast/opencast/security/advisories/GHSA-hcxx-mp6g-6gr9)] -
  Opencast publishes global system account credentials
- [[GHSA-59g4-hpg3-3gcp](https://github.com/opencast/opencast/security/advisories/GHSA-59g4-hpg3-3gcp)] -
  Files Accessible to External Parties
- [[#3252](https://github.com/opencast/opencast/pull/3252)] -
  Update Stand-alone Editor to 2021-12-10
- [[#3241](https://github.com/opencast/opencast/pull/3241)] -
  Fix Ingest Download Configuration
- [[#3222](https://github.com/opencast/opencast/pull/3222)] -
  Improve Inbox Documentation
- [[#3205](https://github.com/opencast/opencast/pull/3205)] -
  Fix Date Time Picker
- [[#3202](https://github.com/opencast/opencast/pull/3202)] -
  asset-manager move to S3 duplicates identical files #3164
- [[#3200](https://github.com/opencast/opencast/pull/3200)] -
  Handle multiple creators in Theodul
- [[#3199](https://github.com/opencast/opencast/pull/3199)] -
  Allow null value for (event) comment author email field
- [[#3186](https://github.com/opencast/opencast/pull/3186)] -
  Use comma as separator for WFR cleanup config
- [[#3183](https://github.com/opencast/opencast/pull/3183)] -
  fix editor remote endpoint 403 error
- [[#3182](https://github.com/opencast/opencast/pull/3182)] -
  handle null or empty workflow state
- [[#3181](https://github.com/opencast/opencast/pull/3181)] -
  Assure that S3 upload runtime exceptions are logged
- [[#3178](https://github.com/opencast/opencast/pull/3178)] -
  More context for graph about supported version
- [[#3174](https://github.com/opencast/opencast/pull/3174)] -
  Fix NullPointerException in `CachedJWT` if `exp` claim does not exist
- [[#3173](https://github.com/opencast/opencast/pull/3173)] -
  Ensure JWT Feature is Installed
- [[#3172](https://github.com/opencast/opencast/pull/3172)] -
  Improve Admin UI Performance
- [[#3160](https://github.com/opencast/opencast/pull/3160)] -
  Analyze mediapackage WOH
- [[#3147](https://github.com/opencast/opencast/pull/3147)] -
  Drop existing video chaptermarks on cutting
- [[#3142](https://github.com/opencast/opencast/pull/3142)] -
  Update paella player to paella 6.5.5
- [[#3058](https://github.com/opencast/opencast/pull/3058)] -
  Stream Security Fix


### Opencast 10.5

*Released on November 10th, 2021*

- [[#3107](https://github.com/opencast/opencast/pull/3107)] -
  Force same sample rate for audio files in PartialImport WOH encoding profiles
- [[#3105](https://github.com/opencast/opencast/pull/3105)] -
  Composer Output File Recognition
- [[#3084](https://github.com/opencast/opencast/pull/3084)] -
  Speed up audio normalization
- [[#3082](https://github.com/opencast/opencast/pull/3082)] -
  Fix Job Load Warning
- [[#3079](https://github.com/opencast/opencast/pull/3079)] -
  Put max load config of server into right place
- [[#3064](https://github.com/opencast/opencast/pull/3064)] -
  Disabling Static Files Authorization for external software
- [[#3063](https://github.com/opencast/opencast/pull/3063)] -
  Handle wildcard target flavors in execute-many WOH


### Opencast 10.4

*Released on October 13th, 2021*

- [[#3065](https://github.com/opencast/opencast/pull/3065)] -
  Deactivate security-jwt by default
- [[#3062](https://github.com/opencast/opencast/pull/3062)] -
  Remove unused constants
- [[#3060](https://github.com/opencast/opencast/pull/3060)] -
  Opencast 10x docs show "compose" instead of "encode" #3059
- [[#3040](https://github.com/opencast/opencast/pull/3040)] -
  Deactivate Parallel Builds in CI
- [[#3013](https://github.com/opencast/opencast/pull/3013)] -
  Amberscript attach WOH improvements
- [[#3006](https://github.com/opencast/opencast/pull/3006)] -
  Fix a spacing issue in the series LTI tool
- [[#3004](https://github.com/opencast/opencast/pull/3004)] -
  Check if series catalog exists
- [[#3003](https://github.com/opencast/opencast/pull/3003)] -
  Set jobLoad to 0 when jobCache is empty for very small jobLoads
- [[#2993](https://github.com/opencast/opencast/pull/2993)] -
  Fix Theodul Multithread Builds
- [[#2991](https://github.com/opencast/opencast/pull/2991)] -
  Fix a `NullPointerException` in the new "Servers"-table code
- [[#2956](https://github.com/opencast/opencast/pull/2956)] -
  Show link to annotation tool in the series LTI tool
- [[#2906](https://github.com/opencast/opencast/pull/2906)] -
  Retry Documentation Deployment


### Opencast 10.3

*Released on September 15th, 2021*

- [[#2982](https://github.com/opencast/opencast/pull/2982)] -
  Fix display of more than 20 workflows in admin interface
- [[#2971](https://github.com/opencast/opencast/pull/2971)] -
  Editor on admin node
- [[#2970](https://github.com/opencast/opencast/pull/2970)] -
  Fix ICLA Test
- [[#2969](https://github.com/opencast/opencast/pull/2969)] -
  Only allow access with write permissions
- [[#2968](https://github.com/opencast/opencast/pull/2968)] -
  Fix isActive check for workflows
- [[#2967](https://github.com/opencast/opencast/pull/2967)] -
  Prevent workflows from running in parallel on the same event
- [[#2964](https://github.com/opencast/opencast/pull/2964)] -
  ActiveMQ configuration needs an update
- [[#2963](https://github.com/opencast/opencast/pull/2963)] -
  Fix issue with duplicate entries in role list
- [[#2960](https://github.com/opencast/opencast/pull/2960)] -
  Fix SQL query in static file authorization by using `true` instead of `1` for bool comparison
- [[#2955](https://github.com/opencast/opencast/pull/2955)] -
  userTrackingSaverPlugin wrongly invoke PUT request
- [[#2948](https://github.com/opencast/opencast/pull/2948)] -
  Standalone editor REST endpoints configuration updated
- [[#2947](https://github.com/opencast/opencast/pull/2947)] -
  Improve `security-jwt` tests
- [[#2944](https://github.com/opencast/opencast/pull/2944)] -
  Fix Dependency Documentation
- [[#2943](https://github.com/opencast/opencast/pull/2943)] -
  Revert "Remove Series Service from Authorization Service"
- [[#2941](https://github.com/opencast/opencast/pull/2941)] -
  Opencast 10.2 Release Notes
- [[#2923](https://github.com/opencast/opencast/pull/2923)] -
  Fix Solr Metadata Field
- [[#2914](https://github.com/opencast/opencast/pull/2914)] -
  Metadata to ACL Operation
- [[#2593](https://github.com/opencast/opencast/pull/2593)] -
  Use UTF-8 for config files


### Opencast 10.2

*Released on August 18th, 2021*

- [[#2939](https://github.com/opencast/opencast/pull/2939)] -
  Document Version Warning Conditions
- [[#2937](https://github.com/opencast/opencast/pull/2937)] -
  Update supported Ubuntu versions
- [[#2935](https://github.com/opencast/opencast/pull/2935)] -
  Update Editor to 2021-08-17
- [[#2929](https://github.com/opencast/opencast/pull/2929)] -
  Display version updates only as warning in the admin interface
- [[#2922](https://github.com/opencast/opencast/pull/2922)] -
  Fix i18next import
- [[#2920](https://github.com/opencast/opencast/pull/2920)] -
  Add search form input field epFrom on sort if url parameter is set
- [[#2917](https://github.com/opencast/opencast/pull/2917)] -
  Remove execution of `npm cache verify`
- [[#2914](https://github.com/opencast/opencast/pull/2914)] -
  Metadata to ACL Operation
- [[#2913](https://github.com/opencast/opencast/pull/2913)] -
  Fix series endpoint update series method by skipping the ACL update if the parameter is empty
- [[#2912](https://github.com/opencast/opencast/pull/2912)] -
  Delete extra colon in paella description plugin's localization key
- [[#2897](https://github.com/opencast/opencast/pull/2897)] -
  LTI upload / edit form styling, adding some sensible padding and margins
- [[#2893](https://github.com/opencast/opencast/pull/2893)] -
  Add index changes to release notes of 10.1
- [[#2892](https://github.com/opencast/opencast/pull/2892)] -
  Fix checkstyle violations in 6 `userdirectory*` modules
- [[#2882](https://github.com/opencast/opencast/pull/2882)] -
  Silence Unknown Action Warning
- [[#2881](https://github.com/opencast/opencast/pull/2881)] -
  Player Plugin Loading
- [[#2880](https://github.com/opencast/opencast/pull/2880)] -
  Remove Unwanted Logs
- [[#2879](https://github.com/opencast/opencast/pull/2879)] -
  Editor Service Build
- [[#2874](https://github.com/opencast/opencast/pull/2874)] -
  Add JWT-based Authentication and Authorization
- [[#2873](https://github.com/opencast/opencast/pull/2873)] -
  Fix Paella Player usertracking log
- [[#2870](https://github.com/opencast/opencast/pull/2870)] -
  Don't run Transcription Services on Workers
- [[#2863](https://github.com/opencast/opencast/pull/2863)] -
  Fix Maven Plugin Multithreading Problems
- [[#2858](https://github.com/opencast/opencast/pull/2858)] -
  Remove Commented out Imports from `pom.xml`
- [[#2852](https://github.com/opencast/opencast/pull/2852)] -
  Document Database Defaults
- [[#2851](https://github.com/opencast/opencast/pull/2851)] -
  Server statistics can overwelm database
- [[#2850](https://github.com/opencast/opencast/pull/2850)] -
  Fix lti rest endpoint annotations and ogsi properties
- [[#2849](https://github.com/opencast/opencast/pull/2849)] -
  Add exception for extron smp351 technical catalog
- [[#2847](https://github.com/opencast/opencast/pull/2847)] -
  Notes in Admin UI
- [[#2836](https://github.com/opencast/opencast/pull/2836)] -
  Target Java 11
- [[#2835](https://github.com/opencast/opencast/pull/2835)] -
  Mark 10 as Stable in Docs
- [[#2833](https://github.com/opencast/opencast/pull/2833)] -
  Changed preencode encoding profile to handle divBy2 problems
- [[#2832](https://github.com/opencast/opencast/pull/2832)] -
  Default is defined now, for the admin interface user filter
- [[#2809](https://github.com/opencast/opencast/pull/2809)] -
  Ingest with tags
- [[#2804](https://github.com/opencast/opencast/pull/2804)] -
  Add dfxp as subtitle format to LTI upload
- [[#2784](https://github.com/opencast/opencast/pull/2784)] -
  Add opencast_major_version to docs
- [[#2701](https://github.com/opencast/opencast/pull/2701)] -
  Fix checkstyle violations in 6 modules
- [[#2700](https://github.com/opencast/opencast/pull/2700)] -
  Fix checkstyle violations in 5 `transcription-service*` modules
- [[#2666](https://github.com/opencast/opencast/pull/2666)] -
  Fix checkstyle violations in 11 modules

<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/2894">2894</a>] -
  Bump commons-compress from 1.20 to 1.21</li>
</ul>
</details>

### Opencast 10.1

*Released on July 14th, 2021*

- [[#2830](https://github.com/opencast/opencast/pull/2830)] -
  Remove Unnecessary Error Logging
- [[#2829](https://github.com/opencast/opencast/pull/2829)] -
  Handle Ingest with Unavailable Media Package Element
- [[#2827](https://github.com/opencast/opencast/pull/2827)] -
  Transformer Factory Identification
- [[#2816](https://github.com/opencast/opencast/pull/2816)] -
  Remove **all** Java 8 packages in the Java upgrade guide
- [[#2815](https://github.com/opencast/opencast/pull/2815)] -
  Fix publication of language tagged metadata
- [[#2813](https://github.com/opencast/opencast/pull/2813)] -
  Don't Ask Developers to Register
- [[#2812](https://github.com/opencast/opencast/pull/2812)] -
  Fix Possible NullPointerException During Ingest
- [[#2807](https://github.com/opencast/opencast/pull/2807)] -
  Fix Prepare-AV Target Tag Handling
- [[#2805](https://github.com/opencast/opencast/pull/2805)] -
  Remove Harvester Configuration
- [[#2791](https://github.com/opencast/opencast/pull/2791)] -
  Fix Assembly Builds
- [[#2787](https://github.com/opencast/opencast/pull/2787)] -
  Prevent workflow from failing if we have no logs
- [[#2776](https://github.com/opencast/opencast/pull/2776)] -
  Fix Workflow Operation Documentation
- [[#2768](https://github.com/opencast/opencast/pull/2768)] -
  Fix Upgrade Documentation
- [[#2766](https://github.com/opencast/opencast/pull/2766)] -
  Document Opencast 10 RPM Install
- [[#2765](https://github.com/opencast/opencast/pull/2765)] -
  Fix Wowza Streaming Misconfiguration
- [[#2764](https://github.com/opencast/opencast/pull/2764)] -
  Remove Unused Dependency on Series Service
- [[#2763](https://github.com/opencast/opencast/pull/2763)] -
  Hello World Configuration Example
- [[#2762](https://github.com/opencast/opencast/pull/2762)] -
  Small additions and improvements in search service (for the upcoming Tobira module)
- [[#2761](https://github.com/opencast/opencast/pull/2761)] -
  Prevent Users From Modifying ICLA Workflow
- [[#2748](https://github.com/opencast/opencast/pull/2748)] -
  Simplify Publication Check
- [[#2747](https://github.com/opencast/opencast/pull/2747)] -
  Fix `jakarta.mail` Dependency Problem
- [[#2740](https://github.com/opencast/opencast/pull/2740)] -
  Fix Log Statements
- [[#2724](https://github.com/opencast/opencast/pull/2724)] -
  Remove Usertracking from Worker
- [[#2721](https://github.com/opencast/opencast/pull/2721)] -
  Update Admin Interface Dependencies
- [[#2720](https://github.com/opencast/opencast/pull/2720)] -
  Fixed admin interface tests in certain timezones
- [[#2706](https://github.com/opencast/opencast/pull/2706)] -
  Add Integration Tests
- [[#2698](https://github.com/opencast/opencast/pull/2698)] -
  Improved Error Handling


### Opencast 10.0

*Released on June 15th, 2021*


- [[#2741](https://github.com/opencast/opencast/pull/2741)] -
  Add JVM Metrics to Release Notes
- [[#2730](https://github.com/opencast/opencast/pull/2730)] -
  Use version variable in poms
- [[#2713](https://github.com/opencast/opencast/pull/2713)] -
  Update Opencast Studio to 2021-06-11
- [[#2708](https://github.com/opencast/opencast/pull/2708)] -
  Add info about renamed enum name of search API to release notes
- [[#2694](https://github.com/opencast/opencast/pull/2694)] -
  Add JVM metrics to metrics exporter
- [[#2691](https://github.com/opencast/opencast/pull/2691)] -
  Document Start Task Endpoint
- [[#2677](https://github.com/opencast/opencast/pull/2677)] -
  Move just the latest snapshot to S3
- [[#2673](https://github.com/opencast/opencast/pull/2673)] -
  Don't list docs twice
- [[#2667](https://github.com/opencast/opencast/pull/2667)] -
  Removed Paella Player Play Button when pausing playback
- [[#2648](https://github.com/opencast/opencast/pull/2648)] -
  Spelling Fixes
- [[#2647](https://github.com/opencast/opencast/pull/2647)] -
  Fix broken distributions
- [[#2645](https://github.com/opencast/opencast/pull/2645)] -
  Update to Elasticsearch 7.10.2
- [[#2644](https://github.com/opencast/opencast/pull/2644)] -
  Use millisecond precision in Solr date range queries (instead of secs)
- [[#2643](https://github.com/opencast/opencast/pull/2643)] -
  Set modified date to deletion date when an event is deleted
- [[#2625](https://github.com/opencast/opencast/pull/2625)] -
  Add Description to republish-metadata
- [[#2624](https://github.com/opencast/opencast/pull/2624)] -
  Update ActiveMQ Client
- [[#2623](https://github.com/opencast/opencast/pull/2623)] -
  Update CXF
- [[#2619](https://github.com/opencast/opencast/pull/2619)] -
  Autoconfigure Job Dispatching
- [[#2554](https://github.com/opencast/opencast/pull/2554)] -
  Retract publications before deleting events
- [[#2387](https://github.com/opencast/opencast/pull/2387)] -
  Update Managed ACLs in Elasticsearch Indices directly
- [[#2354](https://github.com/opencast/opencast/pull/2354)] -
  Update Themes in Elasticsearch Indices directly
- [[#2311](https://github.com/opencast/opencast/pull/2311)] -
  Update Comments in ElasticSearch Indices directly
- [[#2612](https://github.com/opencast/opencast/pull/2612)] -
  Add board members to governance page
- [[#2605](https://github.com/opencast/opencast/pull/2605)] -
  Fix Graphs in Documentation
- [[#2590](https://github.com/opencast/opencast/pull/2590)] -
  New woh: conditional-config
- [[#2572](https://github.com/opencast/opencast/pull/2572)] -
  Dont index groups (fixes distributed develop)
- [[#2558](https://github.com/opencast/opencast/pull/2558)] -
  Add organization ID to the S3 distribution object path
- [[#2555](https://github.com/opencast/opencast/pull/2555)] -
  Add Java Version to Upgrade Docs
- [[#2531](https://github.com/opencast/opencast/pull/2531)] -
  Hand over Elasticsearch Index to Services for Index Rebuild
- [[#2529](https://github.com/opencast/opencast/pull/2529)] -
  Expose some of the S3 client configuration for assets
- [[#2524](https://github.com/opencast/opencast/pull/2524)] -
  Merge r/9.x into develop
- [[#2520](https://github.com/opencast/opencast/pull/2520)] -
  Do not build against Java 8
- [[#2518](https://github.com/opencast/opencast/pull/2518)] -
  Update Note About commons-lang/2.x
- [[#2517](https://github.com/opencast/opencast/pull/2517)] -
  Java Library Update
- [[#2515](https://github.com/opencast/opencast/pull/2515)] -
  Update Prometheus Libraries
- [[#2514](https://github.com/opencast/opencast/pull/2514)] -
  Update Database Driver
- [[#2508](https://github.com/opencast/opencast/pull/2508)] -
  Remove standard check-availability for publication
- [[#2507](https://github.com/opencast/opencast/pull/2507)] -
  Change Paella Usertracking Default
- [[#2471](https://github.com/opencast/opencast/pull/2471)] -
  Remove broken admin-frontend test
- [[#2465](https://github.com/opencast/opencast/pull/2465)] -
  Exclude Dependabot from ICLA Check
- [[#2459](https://github.com/opencast/opencast/pull/2459)] -
  Remove a duplicate dependency declaration
- [[#2441](https://github.com/opencast/opencast/pull/2441)] -
  fixed publish-configure argument in fast-HLS
- [[#2421](https://github.com/opencast/opencast/pull/2421)] -
  Create Dependabot config file
- [[#2396](https://github.com/opencast/opencast/pull/2396)] -
  Fix 9.x to develop merge conflicts
- [[#2392](https://github.com/opencast/opencast/pull/2392)] -
  [Security] Bump yargs-parser from 5.0.0 to 5.0.1 in /modules/engage-paella-player
- [[#2388](https://github.com/opencast/opencast/pull/2388)] -
  [Security] Bump elliptic from 6.5.3 to 6.5.4 in /modules/lti
- [[#2333](https://github.com/opencast/opencast/pull/2333)] -
  Added doc string to UserEndoint for the admin API
- [[#2307](https://github.com/opencast/opencast/pull/2307)] -
  Don't Store Documentation Redirect in History
- [[#2293](https://github.com/opencast/opencast/pull/2293)] -
  Added the access_policy field to elasticsearch and made it searchable
- [[#2289](https://github.com/opencast/opencast/pull/2289)] -
  Start Index Rebuild directly
- [[#2262](https://github.com/opencast/opencast/pull/2262)] -
  Remove dom4j
- [[#2255](https://github.com/opencast/opencast/pull/2255)] -
  Clean up PR review documentation
- [[#2240](https://github.com/opencast/opencast/pull/2240)] -
  [Security] Bump socket.io from 2.3.0 to 2.4.1 in /modules/admin-ui-frontend
- [[#2230](https://github.com/opencast/opencast/pull/2230)] -
  Don't ask Dependabot to sign ICLA
- [[#2226](https://github.com/opencast/opencast/pull/2226)] -
  [Security] Bump semver from 2.3.2 to 5.3.0 in /modules/admin-ui-frontend
- [[#2225](https://github.com/opencast/opencast/pull/2225)] -
  [Security] Bump handlebars from 4.5.3 to 4.7.6 in /modules/admin-ui-frontend
- [[#2212](https://github.com/opencast/opencast/pull/2212)] -
  Securing Static Files by Default
- [[#2211](https://github.com/opencast/opencast/pull/2211)] -
  Refactor Index Rebuild
- [[#2210](https://github.com/opencast/opencast/pull/2210)] -
  Document use of self-signed certificates
- [[#2209](https://github.com/opencast/opencast/pull/2209)] -
  Check ICLA only on pull request
- [[#2208](https://github.com/opencast/opencast/pull/2208)] -
  Fix user tracking duplicate session key error
- [[#2206](https://github.com/opencast/opencast/pull/2206)] -
  Update Development Process
- [[#2163](https://github.com/opencast/opencast/pull/2163)] -
  Added upload progressbar to the LTI upload tool
- [[#2156](https://github.com/opencast/opencast/pull/2156)] -
  Fix Google transcription service indefinite errors generation #1664 #2146
- [[#2154](https://github.com/opencast/opencast/pull/2154)] -
  Move governance document from website to docs
- [[#2151](https://github.com/opencast/opencast/pull/2151)] -
  Fix paths to docker-compose development files
- [[#2144](https://github.com/opencast/opencast/pull/2144)] -
  [Security] Bump ini from 1.3.5 to 1.3.8 in /modules/lti
- [[#2141](https://github.com/opencast/opencast/pull/2141)] -
  [Security] Bump ini from 1.3.5 to 1.3.8 in /modules/admin-ui-frontend
- [[#2140](https://github.com/opencast/opencast/pull/2140)] -
  [Security] Bump ini from 1.3.5 to 1.3.8 in /modules/engage-paella-player
- [[#2138](https://github.com/opencast/opencast/pull/2138)] -
  Adding Step-by-Step to Config docs and Minor Documentation Changes
- [[#2125](https://github.com/opencast/opencast/pull/2125)] -
  Standardization of Tag and Flavor handling
- [[#2102](https://github.com/opencast/opencast/pull/2102)] -
  Added link to recordings to docs landing page
- [[#2065](https://github.com/opencast/opencast/pull/2065)] -
  Fixes Maven dependencies in remaining modules
- [[#2053](https://github.com/opencast/opencast/pull/2053)] -
  Fixes Maven dependencies in modules: common-jpa-impl, common, and cover-image-impl
- [[#2052](https://github.com/opencast/opencast/pull/2052)] -
  Check for Apereo CLA
- [[#2019](https://github.com/opencast/opencast/pull/2019)] -
  Users in the admin ui filter can be reduced via regex now.
- [[#1955](https://github.com/opencast/opencast/pull/1955)] -
  Remove Ingest service reference from duplicate event WOH
- [[#1938](https://github.com/opencast/opencast/pull/1938)] -
  One place for streaming configuration
- [[#1909](https://github.com/opencast/opencast/pull/1909)] -
  Enable ESLint for Theodul Player
- [[#1902](https://github.com/opencast/opencast/pull/1902)] -
  Drop broken theodul-plugin-timeline-statistics
- [[#1877](https://github.com/opencast/opencast/pull/1877)] -
  Extended CoverImageWOH to be able to use extended and series metadata
- [[#1634](https://github.com/opencast/opencast/pull/1634)] -
  LDAP Group Mapping

<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/2611">2611</a>] -
  Bump @types/node from 15.0.2 to 15.3.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2610">2610</a>] -
  Bump i18next from 20.2.2 to 20.2.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2609">2609</a>] -
  Bump react-bootstrap from 1.5.2 to 1.6.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2608">2608</a>] -
  Bump react-select from 4.3.0 to 4.3.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2607">2607</a>] -
  Bump @types/react-dom from 17.0.3 to 17.0.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2606">2606</a>] -
  Bump bootstrap from 5.0.0 to 5.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2601">2601</a>] -
  Bump karma from 5.2.3 to 6.3.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2600">2600</a>] -
  Bump grunt-karma from 4.0.0 to 4.0.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2589">2589</a>] -
  Bump eslint from 7.25.0 to 7.26.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2588">2588</a>] -
  Bump eslint from 7.25.0 to 7.26.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2587">2587</a>] -
  Bump eslint from 7.25.0 to 7.26.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2586">2586</a>] -
  Bump eslint from 7.25.0 to 7.26.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2583">2583</a>] -
  Bump i18next-browser-languagedetector from 6.1.0 to 6.1.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2582">2582</a>] -
  Bump bootstrap from 4.6.0 to 5.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2581">2581</a>] -
  Bump @types/react from 17.0.4 to 17.0.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2580">2580</a>] -
  Bump @types/node from 15.0.1 to 15.0.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2579">2579</a>] -
  Bump node-sass from 5.0.0 to 6.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2578">2578</a>] -
  Bump eslint from 7.25.0 to 7.26.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2577">2577</a>] -
  Bump eslint from 7.25.0 to 7.26.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2551">2551</a>] -
  Bump @types/react from 17.0.3 to 17.0.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2550">2550</a>] -
  Bump react-i18next from 11.8.13 to 11.8.15 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2549">2549</a>] -
  Bump @types/node from 14.14.41 to 15.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2548">2548</a>] -
  Bump @types/jest from 26.0.22 to 26.0.23 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2547">2547</a>] -
  Bump i18next from 20.2.1 to 20.2.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2542">2542</a>] -
  Bump eslint from 7.24.0 to 7.25.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2541">2541</a>] -
  Bump eslint from 7.24.0 to 7.25.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2540">2540</a>] -
  Bump eslint from 7.24.0 to 7.25.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2539">2539</a>] -
  Bump eslint from 7.24.0 to 7.25.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2538">2538</a>] -
  Bump eslint from 7.24.0 to 7.25.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2537">2537</a>] -
  Bump eslint from 7.24.0 to 7.25.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2533">2533</a>] -
  Bump grunt from 1.3.0 to 1.4.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2523">2523</a>] -
  Bump @types/node from 14.14.37 to 14.14.41 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2522">2522</a>] -
  Bump @types/react-select from 4.0.14 to 4.0.15 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2521">2521</a>] -
  Bump react-i18next from 11.8.12 to 11.8.13 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2505">2505</a>] -
  Bump underscore from 1.13.0 to 1.13.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2503">2503</a>] -
  Bump chromedriver from 89.0.0 to 90.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2495">2495</a>] -
  Bump js-yaml from 4.0.0 to 4.1.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2488">2488</a>] -
  Bump underscore from 1.12.1 to 1.13.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2487">2487</a>] -
  Bump eslint from 7.23.0 to 7.24.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2486">2486</a>] -
  Bump eslint from 7.23.0 to 7.24.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2485">2485</a>] -
  Bump eslint from 7.23.0 to 7.24.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2484">2484</a>] -
  Bump eslint from 7.23.0 to 7.24.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2483">2483</a>] -
  Bump i18next from 20.1.0 to 20.2.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2482">2482</a>] -
  Bump @types/react-helmet from 6.1.0 to 6.1.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2481">2481</a>] -
  Bump eslint from 7.23.0 to 7.24.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2480">2480</a>] -
  Bump eslint from 7.23.0 to 7.24.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2469">2469</a>] -
  Bump @types/react-select from 3.0.21 to 4.0.14 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2468">2468</a>] -
  Bump grunt-cli from 1.4.1 to 1.4.2 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2466">2466</a>] -
  Bump y18n from 4.0.0 to 4.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2463">2463</a>] -
  Bump grunt-contrib-uglify from 5.0.0 to 5.0.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2461">2461</a>] -
  Bump y18n from 3.2.1 to 3.2.2 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2458">2458</a>] -
  Bump eslint from 7.22.0 to 7.23.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2457">2457</a>] -
  Bump eslint from 7.22.0 to 7.23.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2456">2456</a>] -
  Bump eslint from 7.22.0 to 7.23.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2455">2455</a>] -
  Bump eslint from 7.22.0 to 7.23.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2454">2454</a>] -
  Bump @types/node from 14.14.35 to 14.14.37 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2453">2453</a>] -
  Bump eslint from 7.22.0 to 7.23.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2452">2452</a>] -
  Bump eslint from 7.22.0 to 7.23.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2450">2450</a>] -
  Bump i18next from 19.9.2 to 20.1.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2449">2449</a>] -
  Bump react-i18next from 11.8.10 to 11.8.12 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2448">2448</a>] -
  Bump react and react-dom in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2447">2447</a>] -
  Bump i18next-browser-languagedetector from 6.0.1 to 6.1.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2445">2445</a>] -
  Bump @types/jest from 26.0.21 to 26.0.22 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2444">2444</a>] -
  Bump @types/react-dom from 17.0.2 to 17.0.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2439">2439</a>] -
  Bump grunt-cli from 1.3.2 to 1.4.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2434">2434</a>] -
  Bump @fortawesome/free-solid-svg-icons from 5.15.2 to 5.15.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2433">2433</a>] -
  Bump @fortawesome/fontawesome-svg-core from 1.2.34 to 1.2.35 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2432">2432</a>] -
  Bump query-string from 6.14.1 to 7.0.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2431">2431</a>] -
  Bump react-select from 4.2.1 to 4.3.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2430">2430</a>] -
  Bump @types/node from 14.14.34 to 14.14.35 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2429">2429</a>] -
  Bump @types/jest from 26.0.20 to 26.0.21 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2426">2426</a>] -
  Bump @types/react-select from 3.0.21 to 4.0.13 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2425">2425</a>] -
  Bump jasmine-core from 3.7.0 to 3.7.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2422">2422</a>] -
  Bump jasmine-core from 3.6.0 to 3.7.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2412">2412</a>] -
  Bump underscore from 1.12.0 to 1.12.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2408">2408</a>] -
  Bump react-scripts from 3.4.1 to 4.0.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2406">2406</a>] -
  Bump @types/node from 14.14.32 to 14.14.34 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2405">2405</a>] -
  Bump react-i18next from 11.8.9 to 11.8.10 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2404">2404</a>] -
  Bump @types/react-dom from 17.0.1 to 17.0.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2403">2403</a>] -
  Bump eslint from 7.21.0 to 7.22.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2402">2402</a>] -
  Bump eslint from 7.21.0 to 7.22.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2401">2401</a>] -
  Bump eslint from 7.21.0 to 7.22.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2400">2400</a>] -
  Bump eslint from 7.21.0 to 7.22.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2399">2399</a>] -
  Bump eslint from 7.21.0 to 7.22.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2398">2398</a>] -
  Bump eslint from 7.21.0 to 7.22.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2389">2389</a>] -
  Bump i18next from 19.8.9 to 19.9.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2385">2385</a>] -
  Bump @types/react from 17.0.2 to 17.0.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2384">2384</a>] -
  Bump react and react-dom in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2383">2383</a>] -
  Bump @types/node from 14.14.31 to 14.14.32 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2382">2382</a>] -
  Bump react-i18next from 11.0.0 to 11.8.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2381">2381</a>] -
  Bump react-select from 4.1.0 to 4.2.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2380">2380</a>] -
  Bump chromedriver from 88.0.0 to 89.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2379">2379</a>] -
  Bump jquery from 3.5.1 to 3.6.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2378">2378</a>] -
  Bump jquery from 3.5.1 to 3.6.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2377">2377</a>] -
  Bump markdownlint-cli from 0.27.0 to 0.27.1 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2376">2376</a>] -
  Bump jquery from 3.5.1 to 3.6.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2375">2375</a>] -
  Bump markdownlint-cli from 0.26.0 to 0.27.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2372">2372</a>] -
  Bump query-string from 6.14.0 to 6.14.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2369">2369</a>] -
  Bump react-dom from 16.13.1 to 16.14.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2367">2367</a>] -
  Bump eslint from 7.20.0 to 7.21.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2366">2366</a>] -
  Bump eslint from 7.20.0 to 7.21.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2365">2365</a>] -
  Bump eslint from 7.20.0 to 7.21.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2364">2364</a>] -
  Bump eslint from 7.20.0 to 7.21.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2363">2363</a>] -
  Bump eslint from 7.20.0 to 7.21.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2362">2362</a>] -
  Bump eslint from 7.20.0 to 7.21.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2350">2350</a>] -
  Bump react from 16.13.1 to 16.14.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2349">2349</a>] -
  Bump @types/jest from 26.0.14 to 26.0.20 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2348">2348</a>] -
  Bump typescript from 3.6.3 to 3.9.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2347">2347</a>] -
  Bump @fortawesome/react-fontawesome from 0.1.11 to 0.1.14 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2345">2345</a>] -
  Bump @types/react-dom from 16.9.8 to 17.0.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2342">2342</a>] -
  Bump @types/node from 14.14.21 to 14.14.31 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2341">2341</a>] -
  Bump i18next from 17.3.1 to 19.8.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2337">2337</a>] -
  Bump url-parse from 1.4.7 to 1.5.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2326">2326</a>] -
  Bump @types/react from 16.9.50 to 17.0.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2325">2325</a>] -
  Bump eslint from 7.19.0 to 7.20.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2324">2324</a>] -
  Bump grunt-contrib-cssmin from 3.0.0 to 4.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2323">2323</a>] -
  Bump eslint from 7.19.0 to 7.20.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2322">2322</a>] -
  Bump eslint from 7.19.0 to 7.20.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2321">2321</a>] -
  Bump eslint from 7.19.0 to 7.20.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2320">2320</a>] -
  Bump eslint from 7.19.0 to 7.20.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2319">2319</a>] -
  Bump eslint from 7.19.0 to 7.20.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2315">2315</a>] -
  Bump query-string from 6.13.1 to 6.14.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2303">2303</a>] -
  Bump react-select from 3.1.0 to 4.1.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2288">2288</a>] -
  Bump eslint-plugin-header from 3.1.0 to 3.1.1 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2287">2287</a>] -
  Bump eslint-plugin-header from 3.1.0 to 3.1.1 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2286">2286</a>] -
  Bump eslint-plugin-header from 3.1.0 to 3.1.1 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2285">2285</a>] -
  Bump eslint-plugin-header from 3.1.0 to 3.1.1 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2284">2284</a>] -
  Bump eslint-plugin-header from 3.1.0 to 3.1.1 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2283">2283</a>] -
  Bump eslint-plugin-header from 3.1.0 to 3.1.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2269">2269</a>] -
  Bump eslint from 7.18.0 to 7.19.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2268">2268</a>] -
  Bump eslint from 7.18.0 to 7.19.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2267">2267</a>] -
  Bump eslint from 7.18.0 to 7.19.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2266">2266</a>] -
  Bump eslint from 7.18.0 to 7.19.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2265">2265</a>] -
  Bump eslint from 7.18.0 to 7.19.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2264">2264</a>] -
  Bump eslint from 7.18.0 to 7.19.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2252">2252</a>] -
  Bump bootstrap from 4.5.3 to 4.6.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2250">2250</a>] -
  Bump chromedriver from 87.0.7 to 88.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2232">2232</a>] -
  Bump bower from 1.8.10 to 1.8.12 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2231">2231</a>] -
  Bump chromedriver from 87.0.5 to 87.0.7 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2229">2229</a>] -
  Bump @fortawesome/fontawesome-svg-core from 1.2.30 to 1.2.34 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2227">2227</a>] -
  Bump @types/node from 14.11.2 to 14.14.21 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2224">2224</a>] -
  Bump eslint from 7.17.0 to 7.18.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2223">2223</a>] -
  Bump eslint from 7.17.0 to 7.18.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2222">2222</a>] -
  Bump eslint from 7.17.0 to 7.18.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2221">2221</a>] -
  Bump eslint from 7.17.0 to 7.18.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2220">2220</a>] -
  Bump eslint from 7.17.0 to 7.18.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2219">2219</a>] -
  Bump eslint from 7.17.0 to 7.18.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2217">2217</a>] -
  Bump bower from 1.8.8 to 1.8.10 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2200">2200</a>] -
  Bump chromedriver from 87.0.4 to 87.0.5 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2195">2195</a>] -
  Bump eslint from 7.16.0 to 7.17.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2194">2194</a>] -
  Bump eslint from 7.16.0 to 7.17.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2193">2193</a>] -
  Bump eslint from 7.16.0 to 7.17.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2192">2192</a>] -
  Bump eslint from 7.16.0 to 7.17.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2191">2191</a>] -
  Bump eslint from 7.16.0 to 7.17.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2190">2190</a>] -
  Bump eslint from 7.16.0 to 7.17.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2189">2189</a>] -
  Bump js-yaml from 3.14.1 to 4.0.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2175">2175</a>] -
  Bump eslint from 7.15.0 to 7.16.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2174">2174</a>] -
  Bump eslint from 7.15.0 to 7.16.0 in /modules/engage-theodul-core</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2173">2173</a>] -
  Bump eslint from 7.15.0 to 7.16.0 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2172">2172</a>] -
  Bump eslint from 7.15.0 to 7.16.0 in /modules/runtime-info-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2171">2171</a>] -
  Bump eslint from 7.15.0 to 7.16.0 in /modules/engage-paella-player</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2170">2170</a>] -
  Bump eslint from 7.15.0 to 7.16.0 in /modules/runtime-info-ui-ng</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2159">2159</a>] -
  Bump markdownlint-cli from 0.25.0 to 0.26.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2126">2126</a>] -
  Bump js-yaml from 3.14.0 to 3.14.1 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/2027">2027</a>] -
  Bump node-sass from 4.14.1 to 5.0.0 in /modules/admin-ui-frontend</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1959">1959</a>] -
  Bump bootstrap from 4.5.0 to 4.5.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/1784">1784</a>] -
  Bump i18next-browser-languagedetector from 3.1.1 to 6.0.1 in /modules/lti</li>
</ul>
</details>

Opencast 9
----------


### Opencast 9.12

*Released on December 18th, 2021*

- [[#3280](https://github.com/opencast/opencast/pull/3280)]
  Security: Update to Pax Logging 1.11.12 (9.x)


### Opencast 9.11

*Released on December 17th, 2021*

- [[#3277](https://github.com/opencast/opencast/pull/3277)]
  Security: Fix Files Accessible to External Parties
- [[#3276](https://github.com/opencast/opencast/pull/3276)]
  Security: Update to Pax Logging 1.11.11 (9.x)


### Opencast 9.10

*Released on December 13th, 2021*

- [[GHSA-mf4f-j588-5xm8](https://github.com/opencast/opencast/security/advisories/GHSA-mf4f-j588-5xm8)] -
  Apache Log4j Remote Code Execution
- [[GHSA-j4mm-7pj3-jf7v](https://github.com/opencast/opencast/security/advisories/GHSA-j4mm-7pj3-jf7v)} -
  HTTP Method Spoofing
- [[#3080](https://github.com/opencast/opencast/pull/3080)] -
  Backport fixes: chrome tests and Safari fix


### Opencast 9.9

*Released on October 12th, 2021*

- [[#3041](https://github.com/opencast/opencast/pull/3041)] -
  Point out memory limits
- [[#2992](https://github.com/opencast/opencast/pull/2992)] -
  Fix create new event in admin UI when multiple extended catalogs are used
- [[#2951](https://github.com/opencast/opencast/pull/2951)] -
  Add Missing Translations Files

### Opencast 9.8

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

### Opencast 9.7

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

### Opencast 9.6

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

### Opencast 9.5

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

### Opencast 9.4

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

### Opencast 9.3

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

### Opencast 9.2

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

### Opencast 9.1

*Released on December 16th, 2020*

- [[#2150](https://github.com/opencast/opencast/pull/2150)] -
  Add note about Studio config changes to the 8->9 update guide
- [[#2133](https://github.com/opencast/opencast/pull/2133)] -
  Update Debian install documentation
- [[#2160](https://github.com/opencast/opencast/pull/2160)] -
  Fix Ingest by Non-privileged User

### Opencast 9.0

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
- [[MH-11555]](https://opencast.jira.com/browse/MH-11555) - Localization of Recordings-&gt;Events and
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
