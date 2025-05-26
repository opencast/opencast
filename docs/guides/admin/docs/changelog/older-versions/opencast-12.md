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
  Add formal OpenSearch support
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