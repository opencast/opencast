Opencast 18 Changelog
---------------------
## Opencast 18.6 (2026-04-07)
- [[#7510](https://github.com/opencast/opencast/pull/7510)] -
  Document submodule usage
- [[#7509](https://github.com/opencast/opencast/pull/7509)] -
  18.x submodules
- [[#7477](https://github.com/opencast/opencast/pull/7477)] -
  Reenable Access by Episode ID
- [[#7475](https://github.com/opencast/opencast/pull/7475)] -
  Remove redundant @Produces from API methods
- [[#7461](https://github.com/opencast/opencast/pull/7461)] -
  Refactor docker compose service configuration
- [[#7460](https://github.com/opencast/opencast/pull/7460)] -
  Improve frontend-less and -reduced builds
- [[#7399](https://github.com/opencast/opencast/pull/7399)] -
  Add new MIME types for Microsoft Word, Excel, and PowerPoint documents
- [[#7384](https://github.com/opencast/opencast/pull/7384)] -
  Fix most POSIX compliance errors in upgrade shell scripts
- [[#7378](https://github.com/opencast/opencast/pull/7378)] -
  Prevent build tests from building modules in parallel
- [[#7372](https://github.com/opencast/opencast/pull/7372)] -
  Add endpoint for batch-creating users
- [[#7369](https://github.com/opencast/opencast/pull/7369)] -
  Switch to bash for upgrade scripts
- [[#7348](https://github.com/opencast/opencast/pull/7348)] -
  Add missing GHA checks
- [[#7338](https://github.com/opencast/opencast/pull/7338)] -
  Remove internal SSL documentation


Changes marked with \* were also included in 17.x.
## Opencast 18.5 (2026-01-29)
- [[#7341](https://github.com/opencast/opencast/pull/7341)] -
  Updating studio to 2025-12-17
- [[#7340](https://github.com/opencast/opencast/pull/7340)] -
  Update r/18.x Admin UI to 18.x-2026-01-29
- [[#7339](https://github.com/opencast/opencast/pull/7339)] -
  Update r/18.x Editor to 18.x-2026-01-29
- [[#7314](https://github.com/opencast/opencast/pull/7314)] -
  Fix unsupported operation exception
- [[#7292](https://github.com/opencast/opencast/pull/7292)] -
  Clarify encoding profile count logging
- [[#7291](https://github.com/opencast/opencast/pull/7291)] -
  Add developers back to the main pom
- [[#7273](https://github.com/opencast/opencast/pull/7273)] -
  Fix syntax in partial-publish and partial-preview
- [[#7213](https://github.com/opencast/opencast/pull/7213)] -
  Add `?download=1` support for static files
- [[#7175](https://github.com/opencast/opencast/pull/7175)] -
  Handle org wildcard for catalog ui adapters

## Opencast 18.4 (2025-11-28)
- [[#7219](https://github.com/opencast/opencast/pull/7219)] -
  Update r/18.x Editor to 18.x-2025-11-28
- [[#7218](https://github.com/opencast/opencast/pull/7218)] -
  Update r/18.x Admin UI to 18.x-2025-11-28
- [[#7195](https://github.com/opencast/opencast/pull/7195)] -
  Fix incorrect escaping in site GHA build
- [[#7173](https://github.com/opencast/opencast/pull/7173)] -
  docs: Download npm packages to common folder
- [[#7140](https://github.com/opencast/opencast/pull/7140)] -
  Cleanup GHA workflows
- [[#7137](https://github.com/opencast/opencast/pull/7137)] -
  Add coverage report documentation
- [[#7118](https://github.com/opencast/opencast/pull/7118)] -
  Fixes direct publishing of live events
- [[#7116](https://github.com/opencast/opencast/pull/7116)] -
  Add brief Tobira "documentation"
- [[#7112](https://github.com/opencast/opencast/pull/7112)] -
  Fix checkstyle violations in scheduler modules
- [[#7111](https://github.com/opencast/opencast/pull/7111)] -
  Fix checkstyle violations in module working-file-repository-service-impl
- [[#7109](https://github.com/opencast/opencast/pull/7109)] -
  Fix checkstyle violations in workflow modules

## Opencast 18.3 (2025-10-24)
- [[#7132](https://github.com/opencast/opencast/pull/7132)] -
  Update r/18.x Admin UI to 18.x-2025-10-24
- [[#7133](https://github.com/opencast/opencast/pull/7133)] -
  Update r/18.x Editor to 18.x-2025-10-24
- [[#7069](https://github.com/opencast/opencast/pull/7069)] -
  Fix silence detection for newer FFmpeg versions

## Opencast 18.2 (2025-09-29)
- [[#7071](https://github.com/opencast/opencast/pull/7071)] -
  Update r/18.x Admin UI to 18.x-2025-09-29
- [[#7070](https://github.com/opencast/opencast/pull/7070)] -
  Update r/18.x Editor to 18.x-2025-09-29
- [[#7061](https://github.com/opencast/opencast/pull/7061)] -
  Remove workflow operation from worker
- [[#7038](https://github.com/opencast/opencast/pull/7038)] -
  Ensure mvnw is used in our github actions workflows
- [[#7006](https://github.com/opencast/opencast/pull/7006)] -
  Update installation docs
- [[#6984](https://github.com/opencast/opencast/pull/6984)] -
  Fix index rebuild stuff (again)
- [[#6924](https://github.com/opencast/opencast/pull/6924)] -
  Don't query for series collection in admin ui event endpoints
- [[#7056](https://github.com/opencast/opencast/pull/7056)] -
  Fix paella download plugins.\*
- [[#7060](https://github.com/opencast/opencast/pull/7060)] -
  Allow `mvnw` to work with symlinks\*

## Opencast 18.1 (2025-08-29)
- [[#7004](https://github.com/opencast/opencast/pull/7004)] -
  Update r/18.x Admin UI to 18.x-2025-08-29
- [[#7003](https://github.com/opencast/opencast/pull/7003)] -
  Update r/18.x Editor to 18.x-2025-08-29
- [[#6938](https://github.com/opencast/opencast/pull/6938)] -
  Reactivate user role filter for admin ui
- [[#6925](https://github.com/opencast/opencast/pull/6925)] -
  Add support for source-tags to speechtotext woh
- [[#6879](https://github.com/opencast/opencast/pull/6879)] -
  Adds Opencast 18 release notes + upgrade docs
- [[#6619](https://github.com/opencast/opencast/pull/6619)] -
  Add tobira auth callback
- [[#6996](https://github.com/opencast/opencast/pull/6996)] -
  Split whispercpp args\*
- [[#6973](https://github.com/opencast/opencast/pull/6973)] -
  Fix missing `isPresent` check for optional workflow\*
- [[#6980](https://github.com/opencast/opencast/pull/6980)] -
  Fix javadoc build\*
- [[#6983](https://github.com/opencast/opencast/pull/6983)] -
  Ensure removing the correct reference\*
- [[#6985](https://github.com/opencast/opencast/pull/6985)] -
  Fix maven central publication\*
- [[#6979](https://github.com/opencast/opencast/pull/6979)] -
  Fix partial path traversal vulnerability in UI config\*
- [[#6969](https://github.com/opencast/opencast/pull/6969)] -
  Update Debian package docs\*
- [[#6966](https://github.com/opencast/opencast/pull/6966)] -
  Add option to pass additional args to WhisperCpp\*
- [[#6965](https://github.com/opencast/opencast/pull/6965)] -
  Remove reencoding to utf-8 of JWTs\*

## Opencast 18.0 (2025-07-24)
- [[#6934](https://github.com/opencast/opencast/pull/6934)] -
  Merge sql migrations 17 to 18
- [[#6930](https://github.com/opencast/opencast/pull/6930)] -
  Update Studio to 2025-07-23
- [[#6929](https://github.com/opencast/opencast/pull/6929)] -
  Update r/18.x Admin UI to 18.x-2025-07-23
- [[#6928](https://github.com/opencast/opencast/pull/6928)] -
  Update r/18.x Editor to 18.x-2025-07-23
- [[#6894](https://github.com/opencast/opencast/pull/6894)] -
  Mux WOH
- [[#6888](https://github.com/opencast/opencast/pull/6888)] -
  Have admin ui endpoint for workflow errors return root job id
- [[#6846](https://github.com/opencast/opencast/pull/6846)] -
  Switch to Ubuntu 24.04 Runner Image
- [[#6845](https://github.com/opencast/opencast/pull/6845)] -
  Update to Python 3.13 on GitHub Actions
- [[#6843](https://github.com/opencast/opencast/pull/6843)] -
  Update GitHub Action Workflow trigger
- [[#6842](https://github.com/opencast/opencast/pull/6842)] -
  Faster Tests (Build JavaDocs only once)
- [[#6841](https://github.com/opencast/opencast/pull/6841)] -
  Use Maven Wrapper on GitHub Actions
- [[#6840](https://github.com/opencast/opencast/pull/6840)] -
  Remove Unused Dependencies from GitHub Actions Workflow
- [[#6838](https://github.com/opencast/opencast/pull/6838)] -
  Add Voice Activity Detection (VAD) configs to whisperCpp
- [[#6832](https://github.com/opencast/opencast/pull/6832)] -
  Fix Unused Import
- [[#6815](https://github.com/opencast/opencast/pull/6815)] -
  Remove full-text search escaping
- [[#6794](https://github.com/opencast/opencast/pull/6794)] -
  Update Whisper.cpp defaults to reflect new binary names
- [[#6622](https://github.com/opencast/opencast/pull/6622)] -
  User Settings Now Working
- [[#5856](https://github.com/opencast/opencast/pull/5856)] -
  Add support for processing and playing audio only files
- [[#6801](https://github.com/opencast/opencast/pull/6801)] -
  OC-18 release schedule in docs
- [[#6788](https://github.com/opencast/opencast/pull/6788)] -
  Update project infrastructure docs
- [[#6781](https://github.com/opencast/opencast/pull/6781)] -
  Fix OpenMetrics Endpoint
- [[#6749](https://github.com/opencast/opencast/pull/6749)] -
  Remove entwine from the scheduler
- [[#6746](https://github.com/opencast/opencast/pull/6746)] -
  Remove entwine from various modules
- [[#6740](https://github.com/opencast/opencast/pull/6740)] -
  Re-introduce PR 6382 Add User Info to Roles to develop
- [[#6682](https://github.com/opencast/opencast/pull/6682)] -
  Remove SoX modules
- [[#6656](https://github.com/opencast/opencast/pull/6656)] -
  Remove entwine from SeriesWorkflowOperationHandler.java
- [[#6655](https://github.com/opencast/opencast/pull/6655)] -
  Remove entwine from PublishOaiPmhWorkflowOperationHandler.java
- [[#6654](https://github.com/opencast/opencast/pull/6654)] -
  Remove entwine from PartialImportWorkflowOperationHandler.java
- [[#6652](https://github.com/opencast/opencast/pull/6652)] -
  Remove entwine from WebvttToCutMarksWorkflowOperationHandler.java
- [[#6651](https://github.com/opencast/opencast/pull/6651)] -
  Remove entwine from Import-/ExportWorkflowPropertiesWOH.java
- [[#6650](https://github.com/opencast/opencast/pull/6650)] -
  Remove entwine from ThemeWorkflowOperationHandler.java
- [[#6626](https://github.com/opencast/opencast/pull/6626)] -
  Format Canvas Userprovider pom.xml file
- [[#6624](https://github.com/opencast/opencast/pull/6624)] -
  Fix typo in org.organization-mh_default_org.cfg
- [[#6618](https://github.com/opencast/opencast/pull/6618)] -
  Remove an unused variable
- [[#6617](https://github.com/opencast/opencast/pull/6617)] -
  Get ghost user in comment reply if user doesn't exist
- [[#6612](https://github.com/opencast/opencast/pull/6612)] -
  Add check in comment re-index to prevent 'No Value present' Exception
- [[#6580](https://github.com/opencast/opencast/pull/6580)] -
  Add check in WorkflowServiceImpl to prevent 'No Value present' Error
- [[#6574](https://github.com/opencast/opencast/pull/6574)] -
  Switch to OpenMetrics
- [[#6566](https://github.com/opencast/opencast/pull/6566)] -
  Add FLOSS/Fund file
- [[#6540](https://github.com/opencast/opencast/pull/6540)] -
  Changing Logger Level for Jetty
- [[#6525](https://github.com/opencast/opencast/pull/6525)] -
  Update Documentation for Latest Docker Compose Usage
- [[#6520](https://github.com/opencast/opencast/pull/6520)] -
  Remove the File Upload Service from Opencast
- [[#6519](https://github.com/opencast/opencast/pull/6519)] -
  Remove the Hunspell Dictionary Service from Opencast
- [[#6518](https://github.com/opencast/opencast/pull/6518)] -
  Remove the Animate Service from Opencast
- [[#6500](https://github.com/opencast/opencast/pull/6500)] -
  Unify constants for episode ID role access configuration
- [[#6498](https://github.com/opencast/opencast/pull/6498)] -
  Optimize search
- [[#6481](https://github.com/opencast/opencast/pull/6481)] -
  Disable series button in paella player
- [[#6480](https://github.com/opencast/opencast/pull/6480)] -
  Disable editor button in paella player
- [[#6479](https://github.com/opencast/opencast/pull/6479)] -
  Disable login button in paella player
- [[#6472](https://github.com/opencast/opencast/pull/6472)] -
  Bump java version to 21
- [[#6443](https://github.com/opencast/opencast/pull/6443)] -
  Introduce series creator field
- [[#6441](https://github.com/opencast/opencast/pull/6441)] -
  Activate dependency plugin for message-broker-api
- [[#6436](https://github.com/opencast/opencast/pull/6436)] -
  Changelog for Opencast 16.8
- [[#6420](https://github.com/opencast/opencast/pull/6420)] -
  Make Dependabot group FontAwesome pull requests
- [[#6393](https://github.com/opencast/opencast/pull/6393)] -
  Document GitHub Discussions on Docs Landing Page
- [[#6391](https://github.com/opencast/opencast/pull/6391)] -
  Add el10 repository
- [[#6377](https://github.com/opencast/opencast/pull/6377)] -
  Specify version of commons action
- [[#6376](https://github.com/opencast/opencast/pull/6376)] -
  Fix pom.xml versions
- [[#6368](https://github.com/opencast/opencast/pull/6368)] -
  Allow Byte-Order-Marks in WEBVTT subtitles
- [[#6326](https://github.com/opencast/opencast/pull/6326)] -
  Update documentation on Consensus Building and Lazy Consensus after PR 6297
- [[#6325](https://github.com/opencast/opencast/pull/6325)] -
  Proposal: Require 2FA for all committers
- [[#6323](https://github.com/opencast/opencast/pull/6323)] -
  Document Security Issue Process
- [[#6320](https://github.com/opencast/opencast/pull/6320)] -
  Update documentation about how to create a release
- [[#6319](https://github.com/opencast/opencast/pull/6319)] -
  Switch from `docker-compose` to `docker compose`
- [[#6312](https://github.com/opencast/opencast/pull/6312)] -
  Document OpenSearch plugin requirements
- [[#6308](https://github.com/opencast/opencast/pull/6308)] -
  Enable awt headless mode
- [[#6304](https://github.com/opencast/opencast/pull/6304)] -
  Explain what a PR does before explaining why it should go into legacy
- [[#6303](https://github.com/opencast/opencast/pull/6303)] -
  Plugins with different modules on different distributions
- [[#6301](https://github.com/opencast/opencast/pull/6301)] -
  RPM » Fix repository path
- [[#6300](https://github.com/opencast/opencast/pull/6300)] -
  RPM » Automatically create new repositories
- [[#6297](https://github.com/opencast/opencast/pull/6297)] -
  Move #Proposals to PRs
- [[#6253](https://github.com/opencast/opencast/pull/6253)] -
  Add per-organization bucket name configuration
- [[#6247](https://github.com/opencast/opencast/pull/6247)] -
  Allow generating subtitles in the background
- [[#6177](https://github.com/opencast/opencast/pull/6177)] -
  Proposal: standard JWT schema and path for better JWT support
- [[#5768](https://github.com/opencast/opencast/pull/5768)] -
  Opencast-Paella usertracking error reading currentTime when video container is not ready

<details><summary>Dependency updates</summary>

<ul>
<li>[<a href="https://github.com/opencast/opencast/pull/6777">6777</a>] - 
  Build(deps): bump org.apache.maven.plugins:maven-surefire-plugin from 3.5.2 to 3.5.3 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6775">6775</a>] - 
  Build(deps-dev): bump markdownlint-cli from 0.44.0 to 0.45.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6774">6774</a>] - 
  Build(deps): bump react-bootstrap from 2.10.9 to 2.10.10 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6771">6771</a>] - 
  Build(deps-dev): bump @types/node from 22.15.3 to 22.15.29 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6770">6770</a>] - 
  Build(deps): bump bootstrap from 5.3.5 to 5.3.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6768">6768</a>] - 
  Build(deps): bump globals from 16.0.0 to 16.2.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6767">6767</a>] - 
  Build(deps): bump @eslint/js from 9.25.1 to 9.28.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6766">6766</a>] - 
  Build(deps): bump eslint-plugin-headers from 1.2.1 to 1.3.2 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6765">6765</a>] - 
  Build(deps): bump bootbox from 6.0.3 to 6.0.4 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6763">6763</a>] - 
  Build(deps-dev): bump webpack from 5.99.8 to 5.99.9 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6761">6761</a>] - 
  Build(deps): bump org.apache.commons:commons-collections4 from 4.4 to 4.5.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6759">6759</a>] - 
  Build(deps-dev): bump eslint from 9.25.1 to 9.28.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6758">6758</a>] - 
  Build(deps-dev): bump html-validate from 9.5.3 to 9.5.5 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6687">6687</a>] - 
  Build(deps): bump org.eclipse.jetty:jetty-server from 9.4.55.v20240627 to 9.4.56.v20240826 in /modules/rest-test-environment</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6679">6679</a>] - 
  Build(deps-dev): bump html-validate from 9.5.2 to 9.5.3 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6678">6678</a>] - 
  Build(deps-dev): bump @babel/eslint-parser from 7.27.0 to 7.27.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6677">6677</a>] - 
  Build(deps-dev): bump @babel/preset-env from 7.26.9 to 7.27.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6676">6676</a>] - 
  Build(deps-dev): bump @babel/core from 7.26.10 to 7.27.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6675">6675</a>] - 
  Build(deps-dev): bump webpack from 5.98.0 to 5.99.7 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6674">6674</a>] - 
  Build(deps-dev): bump @playwright/test from 1.50.1 to 1.52.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6673">6673</a>] - 
  Build(deps-dev): bump junit5.version from 5.10.2 to 5.12.2 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6672">6672</a>] - 
  Build(deps-dev): bump eslint from 9.23.0 to 9.25.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6671">6671</a>] - 
  Build(deps): bump org.apache.maven.plugins:maven-pmd-plugin from 3.25.0 to 3.26.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6670">6670</a>] - 
  Build(deps): bump @eslint/js from 9.23.0 to 9.25.1 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6669">6669</a>] - 
  Build(deps): bump i18next-browser-languagedetector from 8.0.4 to 8.1.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6668">6668</a>] - 
  Build(deps): bump bootstrap from 5.3.3 to 5.3.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6664">6664</a>] - 
  Build(deps): bump iframe-resizer from 5.4.2 to 5.4.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6663">6663</a>] - 
  Build(deps-dev): bump @types/node from 22.13.15 to 22.15.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6662">6662</a>] - 
  Build(deps): bump bootbox from 6.0.2 to 6.0.3 in /modules/engage-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6645">6645</a>] - 
  Build(deps): bump http-proxy-middleware from 2.0.7 to 2.0.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6644">6644</a>] - 
  Build(deps-dev): bump http-proxy-middleware from 2.0.7 to 2.0.9 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6639">6639</a>] - 
  Build(deps-dev): bump http-proxy-middleware from 2.0.6 to 2.0.9 in /modules/graphql-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6634">6634</a>] - 
  Build(deps): bump @babel/runtime from 7.26.0 to 7.27.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6609">6609</a>] - 
  Build(deps): bump iframe-resizer from 5.3.3 to 5.4.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6608">6608</a>] - 
  Build(deps): bump react-select from 5.10.0 to 5.10.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6606">6606</a>] - 
  Build(deps): bump axios from 1.8.2 to 1.8.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6604">6604</a>] - 
  Build(deps-dev): bump @types/node from 22.13.8 to 22.13.15 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6599">6599</a>] - 
  Build(deps-dev): bump copy-webpack-plugin from 12.0.2 to 13.0.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6598">6598</a>] - 
  Build(deps-dev): bump babel-loader from 9.2.1 to 10.0.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6597">6597</a>] - 
  Build(deps-dev): bump express from 4.21.2 to 5.1.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6596">6596</a>] - 
  Build(deps-dev): bump @babel/core from 7.26.9 to 7.26.10 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6595">6595</a>] - 
  Build(deps-dev): bump @babel/preset-env from 7.26.0 to 7.26.9 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6594">6594</a>] - 
  Build(deps): bump paella-basic-plugins from 1.44.10 to 1.50.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6593">6593</a>] - 
  Build(deps): bump joda-time:joda-time from 2.12.7 to 2.14.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6592">6592</a>] - 
  Build(deps-dev): bump html-validate from 9.2.0 to 9.5.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6590">6590</a>] - 
  Build(deps-dev): bump webpack-dev-server from 5.2.0 to 5.2.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6589">6589</a>] - 
  Build(deps-dev): bump @babel/eslint-parser from 7.26.5 to 7.27.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6588">6588</a>] - 
  Build(deps-dev): bump eslint from 9.21.0 to 9.23.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6586">6586</a>] - 
  Build(deps): bump @eslint/js from 9.21.0 to 9.23.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6534">6534</a>] - 
  Build(deps): bump @babel/helpers from 7.18.6 to 7.26.10 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6533">6533</a>] - 
  Build(deps): bump @babel/runtime-corejs3 from 7.18.6 to 7.26.10 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6529">6529</a>] - 
  Build(deps): bump axios from 1.8.1 to 1.8.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6526">6526</a>] - 
  Build(deps): bump cross-spawn from 7.0.3 to 7.0.6 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6515">6515</a>] - 
  Build(deps): bump i18next-browser-languagedetector from 8.0.2 to 8.0.4 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6513">6513</a>] - 
  Build(deps-dev): bump @types/node from 22.13.1 to 22.13.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6512">6512</a>] - 
  Build(deps): bump react-i18next from 15.4.0 to 15.4.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6511">6511</a>] - 
  Build(deps): bump iframe-resizer from 5.3.2 to 5.3.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6510">6510</a>] - 
  Build(deps): bump axios from 1.7.9 to 1.8.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6509">6509</a>] - 
  Build(deps): bump @eslint/js from 9.19.0 to 9.21.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6508">6508</a>] - 
  Build(deps): bump globals from 15.14.0 to 16.0.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6507">6507</a>] - 
  Build(deps-dev): bump webpack from 5.96.1 to 5.98.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6506">6506</a>] - 
  Build(deps-dev): bump eslint from 9.17.0 to 9.21.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6505">6505</a>] - 
  Build(deps-dev): bump @babel/core from 7.25.7 to 7.26.9 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6470">6470</a>] - 
  Build(deps): bump react-select from 5.9.0 to 5.10.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6469">6469</a>] - 
  Build(deps-dev): bump @types/node from 22.13.0 to 22.13.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6467">6467</a>] - 
  Build(deps-dev): bump @playwright/test from 1.49.1 to 1.50.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6466">6466</a>] - 
  Build(deps): bump the fontawesome group in /modules/lti with 2 updates</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6459">6459</a>] - 
  Build(deps-dev): bump markdownlint-cli from 0.43.0 to 0.44.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6458">6458</a>] - 
  Build(deps): bump @eslint/js from 9.17.0 to 9.19.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6455">6455</a>] - 
  Build(deps): bump commons-codec:commons-codec from 1.17.1 to 1.18.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6454">6454</a>] - 
  Build(deps): bump org.codehaus.mojo:buildnumber-maven-plugin from 1.4 to 3.2.1 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6453">6453</a>] - 
  Build(deps-dev): bump @babel/eslint-parser from 7.25.9 to 7.26.5 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6452">6452</a>] - 
  Build(deps-dev): bump html-validate from 8.27.0 to 9.2.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6450">6450</a>] - 
  Build(deps): bump i18next-browser-languagedetector from 8.0.0 to 8.0.2 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6449">6449</a>] - 
  Build(deps): bump react-bootstrap from 2.10.7 to 2.10.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6448">6448</a>] - 
  Build(deps): bump react-i18next from 15.1.3 to 15.4.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6445">6445</a>] - 
  Build(deps-dev): bump @types/node from 22.10.3 to 22.13.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6414">6414</a>] - 
  Build(deps): bump react-bootstrap from 2.10.6 to 2.10.7 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6411">6411</a>] - 
  Build(deps-dev): bump @types/node from 22.10.1 to 22.10.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6410">6410</a>] - 
  Build(deps): bump react-select from 5.8.3 to 5.9.0 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6407">6407</a>] - 
  Build(deps): bump axios from 1.7.8 to 1.7.9 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6406">6406</a>] - 
  Build(deps-dev): bump webpack-cli from 5.1.4 to 6.0.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6404">6404</a>] - 
  Build(deps-dev): bump eslint from 9.16.0 to 9.17.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6403">6403</a>] - 
  Build(deps-dev): bump @playwright/test from 1.49.0 to 1.49.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6402">6402</a>] - 
  Build(deps-dev): bump webpack-dev-server from 5.1.0 to 5.2.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6401">6401</a>] - 
  Build(deps-dev): bump express from 4.21.1 to 4.21.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6400">6400</a>] - 
  Build(deps): bump org.hamcrest:hamcrest-core from 2.2 to 3.0 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6399">6399</a>] - 
  Build(deps): bump @eslint/js from 9.16.0 to 9.17.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6398">6398</a>] - 
  Build(deps): bump globals from 15.12.0 to 15.14.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6397">6397</a>] - 
  Build(deps): bump eslint-plugin-headers from 1.2.0 to 1.2.1 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6395">6395</a>] - 
  Build(deps): bump org.owasp.esapi:esapi from 2.5.5.0 to 2.6.0.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6394">6394</a>] - 
  Build(deps): bump org.apache.maven.plugins:maven-project-info-reports-plugin from 3.7.0 to 3.8.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6380">6380</a>] - 
  Build(deps): bump nanoid from 3.3.4 to 3.3.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6375">6375</a>] - 
  Build(deps-dev): bump express from 4.19.2 to 4.21.2 in /modules/graphql-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6372">6372</a>] - 
  Build(deps-dev): bump webpack from 5.93.0 to 5.97.1 in /modules/graphql-ui</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6374">6374</a>] - 
  Build(deps): bump path-to-regexp and express in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6357">6357</a>] - 
  Build(deps): bump paella-core from 1.49.7 to 1.50.2 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6356">6356</a>] - 
  Build(deps-dev): bump @playwright/test from 1.48.2 to 1.49.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6355">6355</a>] - 
  Build(deps): bump paella-skins from 1.48.0 to 1.48.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6354">6354</a>] - 
  Build(deps): bump paella-slide-plugins from 1.48.1 to 1.50.1 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6353">6353</a>] - 
  Build(deps-dev): bump html-validate from 8.24.2 to 8.27.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6352">6352</a>] - 
  Build(deps-dev): bump eslint from 9.13.0 to 9.16.0 in /modules/engage-paella-player-7</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6351">6351</a>] - 
  Build(deps-dev): bump markdownlint-cli from 0.42.0 to 0.43.0 in /docs/guides</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6350">6350</a>] - 
  Build(deps): bump globals from 15.11.0 to 15.12.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6349">6349</a>] - 
  Build(deps): bump @eslint/js from 9.13.0 to 9.16.0 in /docs/checkstyle/eslint-config</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6348">6348</a>] - 
  Build(deps): bump org.apache.maven.plugins:maven-jxr-plugin from 2.5 to 3.6.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6346">6346</a>] - 
  Build(deps): bump com.google.guava:guava from 33.3.0-jre to 33.3.1-jre in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6345">6345</a>] - 
  Build(deps): bump org.apache.maven.plugins:maven-checkstyle-plugin from 3.1.1 to 3.6.0 in /modules/metrics-exporter</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6341">6341</a>] - 
  Build(deps): bump commons-codec:commons-codec from 1.15 to 1.17.1 in /modules/db</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6340">6340</a>] - 
  Build(deps-dev): bump @types/node from 22.8.6 to 22.10.1 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6338">6338</a>] - 
  Build(deps): bump axios from 1.7.7 to 1.7.8 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6337">6337</a>] - 
  Build(deps): bump react-i18next from 15.1.0 to 15.1.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6334">6334</a>] - 
  Build(deps): bump react-bootstrap from 2.10.5 to 2.10.6 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6332">6332</a>] - 
  Build(deps): bump react-select from 5.8.2 to 5.8.3 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6327">6327</a>] - 
  Build(deps): bump org.owasp.esapi:esapi from 2.5.5.0 to 2.6.0.0</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6311">6311</a>] - 
  Build(deps): bump cross-spawn from 7.0.3 to 7.0.5 in /modules/lti</li>
<li>[<a href="https://github.com/opencast/opencast/pull/6209">6209</a>] - 
  Bump org.antlr:antlr4-maven-plugin from 4.13.1 to 4.13.2 in /modules/metrics-exporter</li>
</ul>
</details>
