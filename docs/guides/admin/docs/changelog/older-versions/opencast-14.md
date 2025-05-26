Opencast 14 Changelog
---------------------


## Opencast 14.13

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


## Opencast 14.12

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


## Opencast 14.11

*Released on April 16th, 2024*

- [[#5682](https://github.com/opencast/opencast/pull/5682)] -
  Quickfix dangling hard links on cephfs volumes
- [[#5667](https://github.com/opencast/opencast/pull/5667)] -
  Fixed NPE on filtering data

## Opencast 14.10

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


## Opencast 14.9

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

## Opencast 14.8

*Released on January 16th, 2024*

- [[#5538](https://github.com/opencast/opencast/pull/5538)] -
  Don't warn about using the default tool
- [[#5537](https://github.com/opencast/opencast/pull/5537)] -
  Fix ACL template selection breaking after first selection
- [[#5492](https://github.com/opencast/opencast/pull/5492)] -
  Add support for custom actions in ACL to Tobira harvest API
- [[#5481](https://github.com/opencast/opencast/pull/5481)] -
  Fix Paella Player 7 login redirect

## Opencast 14.7

*Released on December 12th, 2023*

- [[#5425](https://github.com/opencast/opencast/pull/5425)] -
  Fix Unsaved ACL Changes popup if user can't request ACL
- [[#5416](https://github.com/opencast/opencast/pull/5416)] -
  Silence detection: Skip attaching smil file if empty
- [[#5412](https://github.com/opencast/opencast/pull/5412)] -
  Update Karaf from 4.4.3 to 4.4.4
- [[#5270](https://github.com/opencast/opencast/pull/5270)] -
  Fix logging for WhisperEngine

## Opencast 14.6

*Released on November 16th, 2023*

- [[#5365](https://github.com/opencast/opencast/pull/5365)] -
  Stream media packages to avoid memory issues
- [[#5361](https://github.com/opencast/opencast/pull/5361)] -
  Avoid a potential nullpointer exception in thumbnail generation
- [[#5345](https://github.com/opencast/opencast/pull/5345)] -
  Fix/improve parts of the Tobira harvest API (subtitles & duration)
- [[#5315](https://github.com/opencast/opencast/pull/5315)] -
  Improve Performance of Permission Check in AssetManager

## Opencast 14.5

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

## Opencast 14.4

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


## Opencast 14.3

*Released on August 23rd, 2023*

- [[#5167](https://github.com/opencast/opencast/pull/5167)] -
  Fix admin interface permissions
- [[#5166](https://github.com/opencast/opencast/pull/5166)] -
  Fix Admin Interface Redirect
- [[#5165](https://github.com/opencast/opencast/pull/5165)] -
  Fix crowdin package name
- [[#5163](https://github.com/opencast/opencast/pull/5163)] -
  Add Opencast 14.2 release notes


## Opencast 14.2

*Released on August 9th, 2023*

- [[#5159](https://github.com/opencast/opencast/pull/5159)] -
  Remove obsolete maven-bundle-plugin config
- [[#5124](https://github.com/opencast/opencast/pull/5124)] -
  Fix changed pax web config keys
- [[#5114](https://github.com/opencast/opencast/pull/5114)] -
  Upgrade Crowdin Integration


## Opencast 14.1

*Released on July 13th, 2023*

- [[#5109](https://github.com/opencast/opencast/pull/5109)] -
  Paella Player 7 URL parameters documentation fixed
- [[#5065](https://github.com/opencast/opencast/pull/5065)] -
  Update Opencast 14 RPM Docs
- [[#5053](https://github.com/opencast/opencast/pull/5053)] -
  Paella7 fix trimming url params
- [[#5037](https://github.com/opencast/opencast/pull/5037)] -
  Switch to OpenSearch by default


## Opencast 14.0

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