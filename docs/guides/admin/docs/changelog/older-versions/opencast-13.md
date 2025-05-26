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