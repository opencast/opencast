Opencast 21 Changelog
---------------------

## Opencast 21.0 (2026-11-18)

### Opencast Core changes
#### 🚀 Features, Enhancements, and API changes

- [[#7376]((https://github.com/opencast/opencast/pull/7376))] - Add metric for storage usage
- [[#7332]((https://github.com/opencast/opencast/pull/7332))] - Get Extended Metadata of Events from API via the Index
- [[#7496]((https://github.com/opencast/opencast/pull/7496))] - Add API endpoints for playlist management in the admin interface
- [[#7540]((https://github.com/opencast/opencast/pull/7540))] - Show user roles without associated users in the user table in the ACL…
- [[#7497]((https://github.com/opencast/opencast/pull/7497))] - The Admin UI search improvements
- [[#7705]((https://github.com/opencast/opencast/pull/7705))] - Automate Changelog Generation
- [[#7557]((https://github.com/opencast/opencast/pull/7557))] - Enhance HTTP Notify WOH to include mediaPackageId in request parameters
- [[#7547]((https://github.com/opencast/opencast/pull/7547))] - Add possibility to upload audio-only as presentation track
- [[#7870]((https://github.com/opencast/opencast/pull/7870))] - Reject unparsable dates in ingest instead of failing later
- [[#7956]((https://github.com/opencast/opencast/pull/7956))] - Limit queries to RoleProviders a bit in findRoles()
- [[#7867]((https://github.com/opencast/opencast/pull/7867))] - Report ACLs with deny rules once per index rebuild
- [[#7204]((https://github.com/opencast/opencast/pull/7204))] - Create YouTube playlists with non-public privacy status
- [[#7387]((https://github.com/opencast/opencast/pull/7387))] - Improve YouTube video retraction
- [[#7958]((https://github.com/opencast/opencast/pull/7958))] - Add "hasUser" to UserAndRoleDirectoryServiceImpl.findRoles([...])
- [[#7874]((https://github.com/opencast/opencast/pull/7874))] - Include WhisperC++ output when it produces no subtitles
- [[#7953]((https://github.com/opencast/opencast/pull/7953))] - Add `#{random:min:max}` expansion to FFmpeg parameter templating
- [[#7904]((https://github.com/opencast/opencast/pull/7904))] - Warn when max-attempts is set without a retry-strategy
- [[#8021]((https://github.com/opencast/opencast/pull/8021))] - Report SKIP action when analyze-tracks/select-tracks find no matching tracks
- [[#7205]((https://github.com/opencast/opencast/pull/7205))] - Set proper language in YouTube publication
- [[#7206]((https://github.com/opencast/opencast/pull/7206))] - Set YouTube license according to metadata
- [[#7774]((https://github.com/opencast/opencast/pull/7774))] - Elect the job dispatcher automatically via a database lease
- [[#8020]((https://github.com/opencast/opencast/pull/8020))] - Include the specific reason in engage publication criteria errors
- [[#7930]((https://github.com/opencast/opencast/pull/7930))] - Switch to Opensearch 3
- [[#7320]((https://github.com/opencast/opencast/pull/7320))] - Add thumbnail support to YouTube publication service
- [[#7383]((https://github.com/opencast/opencast/pull/7383))] - Add support for uploading captions to YouTube
- [[#7910]((https://github.com/opencast/opencast/pull/7910))] - Make check-availability configurable for auto-updates on series changes
- [[#8024]((https://github.com/opencast/opencast/pull/8024))] - Fix/lti upload invalid file warning
- [[#8061]((https://github.com/opencast/opencast/pull/8061))] - Don't retry or log a malformed search query as a server error
- [[#6139]((https://github.com/opencast/opencast/pull/6139))] - Add new feature LifeCycle Management
- [[#7963]((https://github.com/opencast/opencast/pull/7963))] - Refresh YouTube OAuth token on credential load
- [[#7892]((https://github.com/opencast/opencast/pull/7892))] - Centralize global/org admin bypass check for permission checking
- [[#7945]((https://github.com/opencast/opencast/pull/7945))] - Rename Elasticsearch to OpenSearch
- [[#8162]((https://github.com/opencast/opencast/pull/8162))] - Use first matching asset manager directory for local file access

#### 🐛 Fixes

- [[#7619]((https://github.com/opencast/opencast/pull/7619))] - Fix incorrect submodule versions in develop
- [[#7666]((https://github.com/opencast/opencast/pull/7666))] - Properly update git submodules
- [[#7692]((https://github.com/opencast/opencast/pull/7692))] - Service error states incorrectly cleared
- [[#7700]((https://github.com/opencast/opencast/pull/7700))] - Fix typo in mh_default_org.xml
- [[#7670]((https://github.com/opencast/opencast/pull/7670))] - Only switch org if orgs are different
- [[#7730]((https://github.com/opencast/opencast/pull/7730))] - Enforce absense of ACL deny rules
- [[#7713]((https://github.com/opencast/opencast/pull/7713))] - Properly sort the docs version
- [[#7745]((https://github.com/opencast/opencast/pull/7745))] - Fix POST /api/playlists returning 405 without a trailing path segment
- [[#7723]((https://github.com/opencast/opencast/pull/7723))] - Return "bad request" for bad sort param for /admin-ng/services.json
- [[#7773]((https://github.com/opencast/opencast/pull/7773))] - Fix bundle_name column too short for long install paths
- [[#7716]((https://github.com/opencast/opencast/pull/7716))] - Improvements for Deletion
- [[#7771]((https://github.com/opencast/opencast/pull/7771))] - Fix LTI upload tool error when no series is specified
- [[#7847]((https://github.com/opencast/opencast/pull/7847))] - Update documentation in organization-mh_default_org.cfg
- [[#7842]((https://github.com/opencast/opencast/pull/7842))] - Avoid failing ZipWOH at inopportune time
- [[#7841]((https://github.com/opencast/opencast/pull/7841))] - Fix some metrics in MetricsExporter not going back to 0
- [[#7901]((https://github.com/opencast/opencast/pull/7901))] - Use a valid charset when building the force-remove query string
- [[#7868]((https://github.com/opencast/opencast/pull/7868))] - Do not overwrite terminal state in WorkflowServiceImpl.stop()
- [[#7914]((https://github.com/opencast/opencast/pull/7914))] - Fix short-circuit skipping AssetManager cleanup on engage retraction
- [[#7786]((https://github.com/opencast/opencast/pull/7786))] - Adopter registration backend fixes
- [[#7934]((https://github.com/opencast/opencast/pull/7934))] - Log exception on storage cleanup failure
- [[#7931]((https://github.com/opencast/opencast/pull/7931))] - Stop logging and rethrowing snapshot failures
- [[#7801]((https://github.com/opencast/opencast/pull/7801))] - Remove namespaces added by merge.
- [[#7896]((https://github.com/opencast/opencast/pull/7896))] - Fix trailing slash in documented paths of root endpoints
- [[#7902]((https://github.com/opencast/opencast/pull/7902))] - Read the response body when checking for active workflows
- [[#7929]((https://github.com/opencast/opencast/pull/7929))] - Log ACL update rejections during workflows at debug
- [[#7936]((https://github.com/opencast/opencast/pull/7936))] - Fix NullPointerException when deleting assets
- [[#7932]((https://github.com/opencast/opencast/pull/7932))] - Do not warn about missing XACML files in workspace
- [[#7955]((https://github.com/opencast/opencast/pull/7955))] - Deduplicate roles before pagination in findRoles()
- [[#7843]((https://github.com/opencast/opencast/pull/7843))] - Respect overwrite parameter for track streams in inspect WOH
- [[#7897]((https://github.com/opencast/opencast/pull/7897))] - Reject JWTs whose algorithm does not match the JWK set's key type
- [[#7883]((https://github.com/opencast/opencast/pull/7883))] - Reject series/event creation with missing required metadata
- [[#7803]((https://github.com/opencast/opencast/pull/7803))] - Prevent scheduler from deleting events that have been processed
- [[#8038]((https://github.com/opencast/opencast/pull/8038))] - Fix ClassCastException when scheduling multiple events as non-admin
- [[#8056]((https://github.com/opencast/opencast/pull/8056))] - Fix missing dependency commons-text
- [[#8058]((https://github.com/opencast/opencast/pull/8058))] - Include commons-text in karaf assemblies
- [[#7960]((https://github.com/opencast/opencast/pull/7960))] - fix: Return 400 instead of 500 when creating a group without a name
- [[#8108]((https://github.com/opencast/opencast/pull/8108))] - Resolve build issue caused dependencies
- [[#7961]((https://github.com/opencast/opencast/pull/7961))] - fix: Return 400 instead of 500 when signing a URL with a malformed date
- [[#8011]((https://github.com/opencast/opencast/pull/8011))] - Fix workflow conditions silently misevaluating on case mismatches
- [[#8030]((https://github.com/opencast/opencast/pull/8030))] - Log clear "could not reach node" message on remote dispatch failure
- [[#8026]((https://github.com/opencast/opencast/pull/8026))] - Fix editor triggering two AssetManager snapshots per save
- [[#8027]((https://github.com/opencast/opencast/pull/8027))] - Fix crash from null-valued workflow properties (#5366)
- [[#8025]((https://github.com/opencast/opencast/pull/8025))] - Fix PUT /api/series/{id}/metadata rejecting the subjects field
- [[#8029]((https://github.com/opencast/opencast/pull/8029))] - Clear the search service's own OpenSearch index on index clear/rebuild
- [[#8028]((https://github.com/opencast/opencast/pull/8028))] - Fix incorrect field type for custom ACL actions in search index
- [[#8032]((https://github.com/opencast/opencast/pull/8032))] - Fix wrong database pool config default
- [[#8104]((https://github.com/opencast/opencast/pull/8104))] - Don't let an untouched blank required field block unrelated metadata updates
- [[#8046]((https://github.com/opencast/opencast/pull/8046))] - Make the service load comparators transitive
- [[#8042]((https://github.com/opencast/opencast/pull/8042))] - Don't count interrupted uploads against the ingest service
- [[#8037]((https://github.com/opencast/opencast/pull/8037))] - Fix NPE and dead fallback branch in applyAclToEpisode
- [[#8036]((https://github.com/opencast/opencast/pull/8036))] - Fix unreachable BAD_REQUEST handling for malformed cutoff in getCalendar
- [[#8033]((https://github.com/opencast/opencast/pull/8033))] - Fix wrong mpeg7 namespace URI in Mpeg7CatalogImpl
- [[#8096]((https://github.com/opencast/opencast/pull/8096))] - Don't abort directory cleanup when a directory vanishes mid-walk
- [[#8060]((https://github.com/opencast/opencast/pull/8060))] - Waveform ffmpeg error logging
- [[#8047]((https://github.com/opencast/opencast/pull/8047))] - Don't let CXF eat '+' from multipart ingest requests
- [[#8110]((https://github.com/opencast/opencast/pull/8110))] - Fix YouTube publication for events without a license
- [[#8106]((https://github.com/opencast/opencast/pull/8106))] - Make claimVersion() atomic to stop concurrent snapshots claiming the same version
- [[#8103]((https://github.com/opencast/opencast/pull/8103))] - Include failed job ids and hosts when a SMIL processing job fails
- [[#8123]((https://github.com/opencast/opencast/pull/8123))] - Sort roles ignoring case
- [[#8112]((https://github.com/opencast/opencast/pull/8112))] - Skip wildcard-led queries in LMS user providers
- [[#8143]((https://github.com/opencast/opencast/pull/8143))] - Fix workflow service remote calls for media package workflows
- [[#8138]((https://github.com/opencast/opencast/pull/8138))] - Return 400 for invalid sort in playlist endpoints
- [[#8160]((https://github.com/opencast/opencast/pull/8160))] - Fix OpenSearch client hanging on HTTPS connections
- [[#8144]((https://github.com/opencast/opencast/pull/8144))] - Ignore query of URIs when looking up local files
- [[#8294]((https://github.com/opencast/opencast/pull/8294))] - Update default delete workflow in the lifecycle module

#### Tests and Performance

- [[#7708]((https://github.com/opencast/opencast/pull/7708))] - Test used workflow definition element, configuration_panel
- [[#7769]((https://github.com/opencast/opencast/pull/7769))] - Replace Selenium/Python tests with Playwright
- [[#7893]((https://github.com/opencast/opencast/pull/7893))] - Add debug ports to multinode config

#### Submodule

- [[#7582]((https://github.com/opencast/opencast/pull/7582))] - Update develop Studio
- [[#7581]((https://github.com/opencast/opencast/pull/7581))] - Update develop Editor
- [[#7540]((https://github.com/opencast/opencast/pull/7540))] - Show user roles without associated users in the user table in the ACL…
- [[#7723]((https://github.com/opencast/opencast/pull/7723))] - Return "bad request" for bad sort param for /admin-ng/services.json
- [[#7958]((https://github.com/opencast/opencast/pull/7958))] - Add "hasUser" to UserAndRoleDirectoryServiceImpl.findRoles([...])
- [[#7698]((https://github.com/opencast/opencast/pull/7698))] - Editor - Thumbnails in backend
- [[#8123]((https://github.com/opencast/opencast/pull/8123))] - Sort roles ignoring case

#### Maintenance

- [[#7589]((https://github.com/opencast/opencast/pull/7589))] - Remove Paella 7 from OC 21
- [[#7619]((https://github.com/opencast/opencast/pull/7619))] - Fix incorrect submodule versions in develop
- [[#7451]((https://github.com/opencast/opencast/pull/7451))] - AI PR Policy
- [[#7627]((https://github.com/opencast/opencast/pull/7627))] - Remove Paella 7 from crowdin config file
- [[#7636]((https://github.com/opencast/opencast/pull/7636))] - Update node version
- [[#7665]((https://github.com/opencast/opencast/pull/7665))] - Opencast 19.5 changelogs
- [[#7669]((https://github.com/opencast/opencast/pull/7669))] - Install markdownlint if needed
- [[#7673]((https://github.com/opencast/opencast/pull/7673))] - Update meeting room links on docs landing page
- [[#7675]((https://github.com/opencast/opencast/pull/7675))] - Paging while we fetch the list of branches in Opencast
- [[#7687]((https://github.com/opencast/opencast/pull/7687))] - Shorten line lengths in develop release notes and upgrade docs
- [[#7688]((https://github.com/opencast/opencast/pull/7688))] - Shorten line lengths in develop release notes and upgrade docs
- [[#7671]((https://github.com/opencast/opencast/pull/7671))] - Modify targets of opencast.service unit file
- [[#7703]((https://github.com/opencast/opencast/pull/7703))] - Correct and expand configuration panel json documentation
- [[#7705]((https://github.com/opencast/opencast/pull/7705))] - Automate Changelog Generation
- [[#7725]((https://github.com/opencast/opencast/pull/7725))] - 19.6 release notes and changelog
- [[#7713]((https://github.com/opencast/opencast/pull/7713))] - Properly sort the docs version
- [[#7487]((https://github.com/opencast/opencast/pull/7487))] - Drop use of proxy_cookie_path for setting cookie flags
- [[#7767]((https://github.com/opencast/opencast/pull/7767))] - Fix fragile debug-flag detection in OpencastRest
- [[#7766]((https://github.com/opencast/opencast/pull/7766))] - Wire in existing en-GB and zh-TW locale files
- [[#7769]((https://github.com/opencast/opencast/pull/7769))] - Replace Selenium/Python tests with Playwright
- [[#7770]((https://github.com/opencast/opencast/pull/7770))] - Replace FontAwesome with Lucide in LTI tools
- [[#7875]((https://github.com/opencast/opencast/pull/7875))] - Remove unused config key from acl.default.create.properties
- [[#7912]((https://github.com/opencast/opencast/pull/7912))] - 19.8 changelog
- [[#7804]((https://github.com/opencast/opencast/pull/7804))] - Fix some maven warnings
- [[#7940]((https://github.com/opencast/opencast/pull/7940))] - Stop logging stack traces for HTTP client errors
- [[#7946]((https://github.com/opencast/opencast/pull/7946))] - Clean up duplicate and dead dependency declarations
- [[#7871]((https://github.com/opencast/opencast/pull/7871))] - Docs: resolve remaining thumbnail-config TODOs from #7835
- [[#7903]((https://github.com/opencast/opencast/pull/7903))] - Restructure workflow.md
- [[#7876]((https://github.com/opencast/opencast/pull/7876))] - Remove Collections.java
- [[#7948]((https://github.com/opencast/opencast/pull/7948))] - Drop unused bundles from the ext-core feature
- [[#7949]((https://github.com/opencast/opencast/pull/7949))] - Enforce dependency analysis during the build
- [[#7954]((https://github.com/opencast/opencast/pull/7954))] - Use effective human author for ICLA checks on Copilot-authored PRs
- [[#8053]((https://github.com/opencast/opencast/pull/8053))] - Opencast 19.9 release notes
- [[#7993]((https://github.com/opencast/opencast/pull/7993))] - Modernize deprecated JDK API usages
- [[#7992]((https://github.com/opencast/opencast/pull/7992))] - Remove deprecated AbstractWorkflowOperationHandler.getCfg and MimeTyp…
- [[#7990]((https://github.com/opencast/opencast/pull/7990))] - Remove deprecated WorkflowUtil.isActive(WorkflowState) and DublinCore…
- [[#7989]((https://github.com/opencast/opencast/pull/7989))] - Remove deprecated IoSupport.readFileFromURL methods
- [[#7988]((https://github.com/opencast/opencast/pull/7988))] - Remove deprecated restutils methods
- [[#7980]((https://github.com/opencast/opencast/pull/7980))] - Replace SmartIterator's manual loop with Stream skip/limit
- [[#7981]((https://github.com/opencast/opencast/pull/7981))] - Remove PathSupport
- [[#7978]((https://github.com/opencast/opencast/pull/7978))] - Remove unused Booleans/Eithers helpers, simplify Misc/Strings
- [[#7991]((https://github.com/opencast/opencast/pull/7991))] - Modernize deprecated stringutils usages
- [[#7995]((https://github.com/opencast/opencast/pull/7995))] - Modernize deprecated commons-lang3 API usages
- [[#8019]((https://github.com/opencast/opencast/pull/8019))] - Clarify editor WOH docs
- [[#7999]((https://github.com/opencast/opencast/pull/7999))] - Modernize deprecated url constructor usages
- [[#7998]((https://github.com/opencast/opencast/pull/7998))] - Modernize remaining one-off deprecated API usages
- [[#7997]((https://github.com/opencast/opencast/pull/7997))] - Modernize deprecated Apache HttpClient API usages
- [[#7996]((https://github.com/opencast/opencast/pull/7996))] - Modernize deprecated commons-collections4 API usages
- [[#7994]((https://github.com/opencast/opencast/pull/7994))] - Modernize deprecated commons-io usages
- [[#7982]((https://github.com/opencast/opencast/pull/7982))] - Back DateTimeSupport with java.time
- [[#8034]((https://github.com/opencast/opencast/pull/8034))] - Fix setRecordingState REST doc to match actual behavior
- [[#8035]((https://github.com/opencast/opencast/pull/8035))] - Fix workflowInstanceId typo in capture agent guide
- [[#8049]((https://github.com/opencast/opencast/pull/8049))] - Fix the stale REST doc for the health endpoint
- [[#7941]((https://github.com/opencast/opencast/pull/7941))] - Clean up JSON dependency declarations
- [[#7979]((https://github.com/opencast/opencast/pull/7979))] - Remove unused util.data.Iterators helper
- [[#7987]((https://github.com/opencast/opencast/pull/7987))] - Remove deprecated methods from EqualsUtil
- [[#8039]((https://github.com/opencast/opencast/pull/8039))] - Refactor paella-8 module: Fetch engage-paella-player via npm to remove duplicated code
- [[#8032]((https://github.com/opencast/opencast/pull/8032))] - Fix wrong database pool config default
- [[#8105]((https://github.com/opencast/opencast/pull/8105))] - Revert unintended change to sanitize configuration
- [[#8102]((https://github.com/opencast/opencast/pull/8102))] - Update maven, wrapper and plugins
- [[#8101]((https://github.com/opencast/opencast/pull/8101))] - Update release note generation
- [[#8062]((https://github.com/opencast/opencast/pull/8062))] - Add size-based log rotation documentation
- [[#8010]((https://github.com/opencast/opencast/pull/8010))] - Eliminate managed services
- [[#8041]((https://github.com/opencast/opencast/pull/8041))] - Split ServiceRegistry#getChildJobs into child and descendant jobs
- [[#8048]((https://github.com/opencast/opencast/pull/8048))] - Document how to check that Opencast is working
- [[#8045]((https://github.com/opencast/opencast/pull/8045))] - Explain the two LDAP configuration files
- [[#8044]((https://github.com/opencast/opencast/pull/8044))] - Document the requirements of the Brightspace user provider
- [[#8135]((https://github.com/opencast/opencast/pull/8135))] - Update the release manager documentation.
- [[#8122]((https://github.com/opencast/opencast/pull/8122))] - Replace anonymous curl calls to GH rest endpoints with authenticated gh
-cli calls
- [[#8146]((https://github.com/opencast/opencast/pull/8146))] - Prepare release documentation for Opencast 21
- [[#8159]((https://github.com/opencast/opencast/pull/8159))] - Opencast 19.10 release notes
- [[#8161]((https://github.com/opencast/opencast/pull/8161))] - Document multiple asset manager storage directories
- [[#8211]((https://github.com/opencast/opencast/pull/8211))] - Update readme for development dependency containers
- [[#8209]((https://github.com/opencast/opencast/pull/8209))] - Remove duplicated OpenSearch configuration from compose files
- [[#8208]((https://github.com/opencast/opencast/pull/8208))] - Use fully qualified base image for OpenSearch container
- [[#8182]((https://github.com/opencast/opencast/pull/8182))] - Clarify release note instructions
- [[#8181]((https://github.com/opencast/opencast/pull/8181))] - Update AGENTS.md now that Spotless fixes the imports
- [[#8139]((https://github.com/opencast/opencast/pull/8139))] - Remove duplicate intercept-url for admin-ng
- [[#8210]((https://github.com/opencast/opencast/pull/8210))] - Update development PostgreSQL container to version 18
- [[#8043]((https://github.com/opencast/opencast/pull/8043))] - Resolve the remaining documentation review markers
- [[#8228]((https://github.com/opencast/opencast/pull/8228))] - Remove Paella 8 source from Crowdin configuration
- [[#8283]((https://github.com/opencast/opencast/pull/8283))] - Recommend tenant-specific admin role in multi-tenancy guide
- [[#8298]((https://github.com/opencast/opencast/pull/8298))] - Add upgrade guide for Opencast 20 to 21
- [[#8264]((https://github.com/opencast/opencast/pull/8264))] - Opencast 21.0 release notes
- [[#8294]((https://github.com/opencast/opencast/pull/8294))] - Update default delete workflow in the lifecycle module

<details>
<summary>Dependencies</summary>

- [[#7594]((https://github.com/opencast/opencast/pull/7594))] - Build(deps): bump softprops/action-gh-release from 2 to 3
- [[#7751]((https://github.com/opencast/opencast/pull/7751))] - Bump globals from 17.4.0 to 17.7.0 in /docs/checkstyle/eslint-config
- [[#7750]((https://github.com/opencast/opencast/pull/7750))] - Bump mikepenz/release-changelog-builder-action from 6.2.2 to 6.2.3
- [[#7749]((https://github.com/opencast/opencast/pull/7749))] - Bump actions/checkout from 4 to 7
- [[#7748]((https://github.com/opencast/opencast/pull/7748))] - Bump actions/cache from 5 to 6
- [[#7744]((https://github.com/opencast/opencast/pull/7744))] - Bump js-yaml and markdownlint-cli in /docs/guides
- [[#7721]((https://github.com/opencast/opencast/pull/7721))] - Bump form-data in /modules/lti
- [[#7720]((https://github.com/opencast/opencast/pull/7720))] - Bump ws in /modules/lti
- [[#7712]((https://github.com/opencast/opencast/pull/7712))] - Bump shell-quote from 1.7.3 to 1.8.4 in /modules/lti
- [[#7643]((https://github.com/opencast/opencast/pull/7643))] - Build(deps): bump fast-uri from 3.1.0 to 3.1.2 in /modules/lti
- [[#7644]((https://github.com/opencast/opencast/pull/7644))] - Build(deps): bump @babel/plugin-transform-modules-systemjs from 7.18.6 to 7.29.4 in /modules/lti
- [[#7743]((https://github.com/opencast/opencast/pull/7743))] - Bump js-yaml from 4.1.1 to 4.3.0 in /modules/graphql-ui
- [[#7681]((https://github.com/opencast/opencast/pull/7681))] - Build(deps): bump qs and express in /modules/lti
- [[#7580]((https://github.com/opencast/opencast/pull/7580))] - Build(deps): bump follow-redirects from 1.15.11 to 1.16.0 in /modules/lti
- [[#7545]((https://github.com/opencast/opencast/pull/7545))] - Build(deps): bump lodash from 4.17.23 to 4.18.1 in /modules/lti
- [[#7520]((https://github.com/opencast/opencast/pull/7520))] - Build(deps): bump brace-expansion from 1.1.12 to 1.1.13 in /modules/lti
- [[#7757]((https://github.com/opencast/opencast/pull/7757))] - Bump axios from 1.15.0 to 1.18.1 in /modules/engage-paella-player-8
- [[#7683]((https://github.com/opencast/opencast/pull/7683))] - Update ltitools to React 19
- [[#7663]((https://github.com/opencast/opencast/pull/7663))] - Build(deps): bump ws from 8.18.3 to 8.20.1 in /modules/engage-paella-player-8
- [[#7623]((https://github.com/opencast/opencast/pull/7623))] - Build(deps): bump postcss from 8.5.6 to 8.5.12 in /modules/engage-paella-player-8
- [[#7599]((https://github.com/opencast/opencast/pull/7599))] - Build(deps-dev): bump vite-plugin-static-copy from 3.1.4 to 4.1.0 in /modules/engage-paella-player-8
- [[#7715]((https://github.com/opencast/opencast/pull/7715))] - Bump @grpc/grpc-js from 1.14.2 to 1.14.4 in /modules/engage-paella-player-8
- [[#7682]((https://github.com/opencast/opencast/pull/7682))] - Build(deps-dev): bump qs from 6.15.0 to 6.15.2 in /modules/engage-paella-player-8
- [[#7649]((https://github.com/opencast/opencast/pull/7649))] - Build(deps): bump @protobufjs/utf8 from 1.1.0 to 1.1.1 in /modules/engage-paella-player-8
- [[#7608]((https://github.com/opencast/opencast/pull/7608))] - Build(deps-dev): bump @tailwindcss/vite from 4.1.17 to 4.2.4 in /modules/engage-paella-player-8
- [[#7606]((https://github.com/opencast/opencast/pull/7606))] - Build(deps-dev): bump typescript from 5.9.3 to 6.0.3 in /modules/engage-paella-player-8
- [[#7579]((https://github.com/opencast/opencast/pull/7579))] - Build(deps): bump follow-redirects from 1.15.11 to 1.16.0 in /modules/engage-paella-player-8
- [[#7668]((https://github.com/opencast/opencast/pull/7668))] - Build(deps): bump protobufjs from 7.5.4 to 7.6.0 in /modules/engage-paella-player-8
- [[#7598]((https://github.com/opencast/opencast/pull/7598))] - Bump i18next from 26.3.4 to 26.3.6 in /modules/lti
- [[#7609]((https://github.com/opencast/opencast/pull/7609))] - Bump react-i18next from 17.0.8 to 17.0.9 in /modules/lti
- [[#7710]((https://github.com/opencast/opencast/pull/7710))] - Bump shell-quote from 1.8.1 to 1.9.0 in /modules/graphql-ui
- [[#7679]((https://github.com/opencast/opencast/pull/7679))] - Build(deps): bump org.springframework.ldap:spring-ldap-core from 2.4.1 to 2.4.4 in /modules/userdirectory-ldap
- [[#7678]((https://github.com/opencast/opencast/pull/7678))] - Build(deps): bump org.bouncycastle:bcpkix-jdk18on from 1.78.1 to 1.84 in /modules/security-cas-client-wrapper
- [[#7731]((https://github.com/opencast/opencast/pull/7731))] - Bump webpack-dev-server from 5.2.1 to 5.2.5 in /modules/graphql-ui
- [[#7611]((https://github.com/opencast/opencast/pull/7611))] - Build(deps): bump io.swagger.core.v3:swagger-annotations from 2.1.13 to 2.2.48 in /modules/metrics-exporter
- [[#7741]((https://github.com/opencast/opencast/pull/7741))] - Bump js-yaml from 4.1.1 to 4.3.0 in /modules/engage-paella-player-8
- [[#7823]((https://github.com/opencast/opencast/pull/7823))] - Bump @playwright/test from 1.61.1 to 1.62.0 in /modules/lti
- [[#7817]((https://github.com/opencast/opencast/pull/7817))] - Bump actions/setup-node from 6 to 7
- [[#7810]((https://github.com/opencast/opencast/pull/7810))] - Bump actions/setup-java from 5 to 5.6.0
- [[#7813]((https://github.com/opencast/opencast/pull/7813))] - Bump actions/setup-python from 6 to 7
- [[#7834]((https://github.com/opencast/opencast/pull/7834))] - Group dependabot PRs for minor/patch updates in lti module
- [[#7865]((https://github.com/opencast/opencast/pull/7865))] - Bump nanoid from 3.3.15 to 3.3.18 in /modules/lti
- [[#7819]((https://github.com/opencast/opencast/pull/7819))] - Bump typescript from 6.0.3 to 7.0.2 in /modules/lti
- [[#7917]((https://github.com/opencast/opencast/pull/7917))] - Bump the minor-and-patch group across 1 directory with 10 updates
- [[#7947]((https://github.com/opencast/opencast/pull/7947))] - Remove Commons HttpClient 3.1
- [[#7526]((https://github.com/opencast/opencast/pull/7526))] - Bump org.apache.commons:commons-lang3 from 3.18.0 to 3.20.0 in /modules/metrics-exporter
- [[#8016]((https://github.com/opencast/opencast/pull/8016))] - Bump REST Assured from 4.4.0 to 6.0.1
- [[#7862]((https://github.com/opencast/opencast/pull/7862))] - Bump com.mchange:mchange-commons-java from 0.2.20 to 0.6.1 in /modules/db
- [[#7825]((https://github.com/opencast/opencast/pull/7825))] - Bump org.apache.maven.plugins:maven-surefire-report-plugin from 2.22.2 to 3.6.0 in /modules/metrics-exporter
- [[#7826]((https://github.com/opencast/opencast/pull/7826))] - Bump org.apache.maven.plugins:maven-enforcer-plugin from 3.5.0 to 3.6.3 in /modules/metrics-exporter
- [[#7969]((https://github.com/opencast/opencast/pull/7969))] - Bump actions/setup-java from 5.6.0 to 6.0.0
- [[#8108]((https://github.com/opencast/opencast/pull/8108))] - Resolve build issue caused dependencies
- [[#8014]((https://github.com/opencast/opencast/pull/8014))] - Migrate to ical4j 4.3.0
- [[#8031]((https://github.com/opencast/opencast/pull/8031))] - Update MariaDB driver to 3.5.10
- [[#7930]((https://github.com/opencast/opencast/pull/7930))] - Switch to Opensearch 3
- [[#8015]((https://github.com/opencast/opencast/pull/8015))] - Remove unused dependencyManagement entries
- [[#8085]((https://github.com/opencast/opencast/pull/8085))] - Bump org.jacoco:jacoco-maven-plugin from 0.8.12 to 0.8.15 in /modules/metrics-exporter
- [[#8086]((https://github.com/opencast/opencast/pull/8086))] - Bump actions/setup-java from 6.0.0 to 6.0.1
- [[#8083]((https://github.com/opencast/opencast/pull/8083))] - Bump commons-codec:commons-codec from 1.21.0 to 1.22.1 in /modules/metrics-exporter
- [[#8080]((https://github.com/opencast/opencast/pull/8080))] - Bump org.apache.commons:commons-lang3 from 3.20.0 to 3.21.0 in /modules/db
- [[#8079]((https://github.com/opencast/opencast/pull/8079))] - Bump com.google.guava:failureaccess from 1.0.2 to 1.0.3 in /modules/metrics-exporter
- [[#8077]((https://github.com/opencast/opencast/pull/8077))] - Bump org.apache.maven.plugins:maven-plugin-plugin from 3.15.1 to 3.16.0 in /modules/metrics-exporter
- [[#7863]((https://github.com/opencast/opencast/pull/7863))] - Bump org.postgresql:postgresql from 42.7.2 to 42.7.14 in /modules/db
- [[#8129]((https://github.com/opencast/opencast/pull/8129))] - Switch graphql-ui to vite
- [[#7986]((https://github.com/opencast/opencast/pull/7986))] - Bump @humanfs/node from 0.16.7 to 0.16.8 in /docs/checkstyle/eslint-config
- [[#7783]((https://github.com/opencast/opencast/pull/7783))] - Bump linkify-it from 5.0.1 to 5.0.2 in /docs/guides
- [[#7533]((https://github.com/opencast/opencast/pull/7533))] - Bump org.apache.commons:commons-csv from 1.9.0 to 1.14.1 in /modules/db
- [[#8088]((https://github.com/opencast/opencast/pull/8088))] - Bump globals from 17.7.0 to 17.12.0 in /docs/checkstyle/eslint-config
- [[#8073]((https://github.com/opencast/opencast/pull/8073))] - Bump brace-expansion from 5.0.5 to 5.0.12 in /docs/checkstyle/eslint-config
- [[#8068]((https://github.com/opencast/opencast/pull/8068))] - Bump markdown-it and markdownlint-cli in /docs/guides
- [[#8071]((https://github.com/opencast/opencast/pull/8071))] - Bump brace-expansion from 5.0.7 to 5.0.12 in /docs/guides
- [[#6600]((https://github.com/opencast/opencast/pull/6600))] - Bump org.influxdb:influxdb-java from 2.15 to 2.25 in /modules/db
- [[#8136]((https://github.com/opencast/opencast/pull/8136))] - Revert push of 6600
- [[#7971]((https://github.com/opencast/opencast/pull/7971))] - Bump @types/node from 25.9.1 to 26.4.0 in /modules/lti
- [[#8114]((https://github.com/opencast/opencast/pull/8114))] - Bump source-map-js from 1.2.1 to 1.2.2 in /modules/lti
- [[#8092]((https://github.com/opencast/opencast/pull/8092))] - Bump com.mchange:c3p0 from 0.9.5.5 to 0.14.2 in /modules/db
- [[#8137]((https://github.com/opencast/opencast/pull/8137))] - Bump the minor-and-patch group across 1 directory with 12 updates
- [[#8183]((https://github.com/opencast/opencast/pull/8183))] - Bump org.mariadb.jdbc:mariadb-java-client from 3.5.10 to 3.5.11 in /modules/db

</details>



<details>
<summary>Uncategorized</summary>

- [[#7618]((https://github.com/opencast/opencast/pull/7618))] - Remove checkstyle suppression
- [[#7476]((https://github.com/opencast/opencast/pull/7476))] - Get listproviders for metadata from External API
- [[#7574]((https://github.com/opencast/opencast/pull/7574))] - Remove unused(?) waveform operation
- [[#7371]((https://github.com/opencast/opencast/pull/7371))] - Spring Security 5 Upgrade
- [[#7707]((https://github.com/opencast/opencast/pull/7707))] - Replace deprecated assert function
- [[#7685]((https://github.com/opencast/opencast/pull/7685))] - Make `/play` preserve query parameters
- [[#7697]((https://github.com/opencast/opencast/pull/7697))] - Fix missed subtitle-timeshift configuration key
- [[#7727]((https://github.com/opencast/opencast/pull/7727))] - engage-ui: Fix test after previous merge from r19
- [[#7753]((https://github.com/opencast/opencast/pull/7753))] - Fix tag operation by adding missing callback
- [[#7500]((https://github.com/opencast/opencast/pull/7500))] - Bug fix: hls m3u8 files rewritten but their size/checksum not updated in media package
- [[#7866]((https://github.com/opencast/opencast/pull/7866))] - Consistently use the configure-s3cmd action
- [[#7872]((https://github.com/opencast/opencast/pull/7872))] - Fix duplicate userEntryPoint bean id for Shibboleth auth
- [[#7869]((https://github.com/opencast/opencast/pull/7869))] - Fix intermittent SchedulerServiceImplTest failures
- [[#7040]((https://github.com/opencast/opencast/pull/7040))] - Make speech to text fully asynchronous
- [[#7878]((https://github.com/opencast/opencast/pull/7878))] - Fix 500 error on GET /api/groups for API versions < 1.6.0
- [[#7943]((https://github.com/opencast/opencast/pull/7943))] - Add logging to aid debugging of videosegmenter issues
- [[#7933]((https://github.com/opencast/opencast/pull/7933))] - Log media package merging at debug level
- [[#7899]((https://github.com/opencast/opencast/pull/7899))] - Match capture device keys by prefix, not substring
- [[#7895]((https://github.com/opencast/opencast/pull/7895))] - Add the anonymous role to every user
- [[#7898]((https://github.com/opencast/opencast/pull/7898))] - Resolve wildcard timeline previews target flavor per track
- [[#7900]((https://github.com/opencast/opencast/pull/7900))] - Validate target flavors, not target tags, for targetFlavors=atLeastOne
- [[#7913]((https://github.com/opencast/opencast/pull/7913))] - Add AGENTS.md with code style notes
- [[#7915]((https://github.com/opencast/opencast/pull/7915))] - Fix Import Order Automatically Using Spotless
- [[#7772]((https://github.com/opencast/opencast/pull/7772))] - Fix TypeScript check in modules/lti and add it to CI
- [[#8165]((https://github.com/opencast/opencast/pull/8165))] - Asset manager prefix dirs

</details>

### Admin Interface submodule changes
#### 🚀 Features

- [[opencast/admin-interface#1579](https://github.com/opencast/admin-interface/pull/1579)] - Add UI for playlist management
- [[opencast/admin-interface#1624](https://github.com/opencast/admin-interface/pull/1624)] - Make "Create User" dialog similar to other create dialogs
- [[opencast/admin-interface#1672](https://github.com/opencast/admin-interface/pull/1672)] - Reset sort when searching
- [[opencast/admin-interface#1671](https://github.com/opencast/admin-interface/pull/1671)] - Just show active chevron for sorted table column

#### 🐛 Fixes

- [[opencast/admin-interface#1610](https://github.com/opencast/admin-interface/pull/1610)] - Fix typo for cancelled wf processes
- [[opencast/admin-interface#1621](https://github.com/opencast/admin-interface/pull/1621)] - Enable PR test builds into GH pages
- [[opencast/admin-interface#1619](https://github.com/opencast/admin-interface/pull/1619)] - Enable writing to ghcr from pull requests
- [[opencast/admin-interface#1620](https://github.com/opencast/admin-interface/pull/1620)] - Fix use of NODE_VERSION variable
- [[opencast/admin-interface#1625](https://github.com/opencast/admin-interface/pull/1625)] - Flip quote types in pr filter
- [[opencast/admin-interface#1637](https://github.com/opencast/admin-interface/pull/1637)] - Additional GHA bugs
- [[opencast/admin-interface#1617](https://github.com/opencast/admin-interface/pull/1617)] - Reset to page 1 when clicking on filter cells
- [[opencast/admin-interface#1606](https://github.com/opencast/admin-interface/pull/1606)] - Allow only custom actions in ACL
- [[opencast/admin-interface#1616](https://github.com/opencast/admin-interface/pull/1616)] - Fix datetime-local field for workflow config
- [[opencast/admin-interface#1624](https://github.com/opencast/admin-interface/pull/1624)] - Make "Create User" dialog similar to other create dialogs
- [[opencast/admin-interface#1612](https://github.com/opencast/admin-interface/pull/1612)] - Fix workflow displayOrder not being respected
- [[opencast/admin-interface#1639](https://github.com/opencast/admin-interface/pull/1639)] - Fix eslint warning in PlaylistActionsCell
- [[opencast/admin-interface#1633](https://github.com/opencast/admin-interface/pull/1633)] - Fix workflow parameters in scheduled events (PR 1476)
- [[opencast/admin-interface#1640](https://github.com/opencast/admin-interface/pull/1640)] - Final GHA polish
- [[opencast/admin-interface#1600](https://github.com/opencast/admin-interface/pull/1600)] - Show registration modal only when unregistered
- [[opencast/admin-interface#1658](https://github.com/opencast/admin-interface/pull/1658)] - Avoid sorting formik in place in scheduling tab
- [[opencast/admin-interface#1655](https://github.com/opencast/admin-interface/pull/1655)] - Minor dropdown fixes
- [[opencast/admin-interface#1667](https://github.com/opencast/admin-interface/pull/1667)] - Fix "for" label in workflow config
- [[opencast/admin-interface#1674](https://github.com/opencast/admin-interface/pull/1674)] - Yet more GHA issues
- [[opencast/admin-interface#1686](https://github.com/opencast/admin-interface/pull/1686)] - Add style for disabled wizard 'next' button
- [[opencast/admin-interface#1685](https://github.com/opencast/admin-interface/pull/1685)] - Fix tabbing between fields not working with mixed_text field
- [[opencast/admin-interface#1689](https://github.com/opencast/admin-interface/pull/1689)] - Fix `undefined` sorting params in playlists table
- [[opencast/admin-interface#1687](https://github.com/opencast/admin-interface/pull/1687)] - Fix to event title in upload progress message

#### Accessibility

- [[opencast/admin-interface#1618](https://github.com/opencast/admin-interface/pull/1618)] - Fix various color contrast issues
- [[opencast/admin-interface#1623](https://github.com/opencast/admin-interface/pull/1623)] - Color variables everywhere
- [[opencast/admin-interface#1659](https://github.com/opencast/admin-interface/pull/1659)] - Notify user if series theme could not be saved
- [[opencast/admin-interface#1638](https://github.com/opencast/admin-interface/pull/1638)] - Add a simple error boundary to the app
- [[opencast/admin-interface#1663](https://github.com/opencast/admin-interface/pull/1663)] - Title tooltip in dropdowns
- [[opencast/admin-interface#1662](https://github.com/opencast/admin-interface/pull/1662)] - Render overflowing dropdown options with ellipsis
- [[opencast/admin-interface#1661](https://github.com/opencast/admin-interface/pull/1661)] - Add minwidth to acl dropdown table
- [[opencast/admin-interface#1671](https://github.com/opencast/admin-interface/pull/1671)] - Just show active chevron for sorted table column

<details>
<summary>Dependencies</summary>

- [[opencast/admin-interface#1575](https://github.com/opencast/admin-interface/pull/1575)] - Update vite to new major version 8
- [[opencast/admin-interface#1582](https://github.com/opencast/admin-interface/pull/1582)] - Bump the minor-and-patch group across 1 directory with 3 updates
- [[opencast/admin-interface#1599](https://github.com/opencast/admin-interface/pull/1599)] - Bump react-tooltip from 5.30.1 to 6.0.0
- [[opencast/admin-interface#1585](https://github.com/opencast/admin-interface/pull/1585)] - Update i18n to major version 26
- [[opencast/admin-interface#1614](https://github.com/opencast/admin-interface/pull/1614)] - Update Mui material to version 9
- [[opencast/admin-interface#1587](https://github.com/opencast/admin-interface/pull/1587)] - Remove @types/uuid
- [[opencast/admin-interface#1634](https://github.com/opencast/admin-interface/pull/1634)] - Update react-hotkeys-hook to 5.3.3
- [[opencast/admin-interface#1636](https://github.com/opencast/admin-interface/pull/1636)] - Update minor-patch group with 20 updates
- [[opencast/admin-interface#1648](https://github.com/opencast/admin-interface/pull/1648)] - Bump http-proxy-middleware from 3.0.7 to 4.2.0
- [[opencast/admin-interface#1646](https://github.com/opencast/admin-interface/pull/1646)] - Bump react-router from 7.18.1 to 8.3.0
- [[opencast/admin-interface#1651](https://github.com/opencast/admin-interface/pull/1651)] - Bump the minor-and-patch group across 1 directory with 13 updates
- [[opencast/admin-interface#1649](https://github.com/opencast/admin-interface/pull/1649)] - Bump @types/node from 25.6.0 to 26.1.2
- [[opencast/admin-interface#1664](https://github.com/opencast/admin-interface/pull/1664)] - Bump the minor-and-patch group with 16 updates

</details>



<details>
<summary>Uncategorized</summary>

- [[opencast/admin-interface#1601](https://github.com/opencast/admin-interface/pull/1601)] - Ensure node version matches parent pom
- [[opencast/admin-interface#1591](https://github.com/opencast/admin-interface/pull/1591)] - Bump actions/github-script from 7 to 9
- [[opencast/admin-interface#1586](https://github.com/opencast/admin-interface/pull/1586)] - Move to node 24 in gh workflows
- [[opencast/admin-interface#1622](https://github.com/opencast/admin-interface/pull/1622)] - Improve npm checks
- [[opencast/admin-interface#1611](https://github.com/opencast/admin-interface/pull/1611)] - Simplify configurationPanelJson typing
- [[opencast/admin-interface#1605](https://github.com/opencast/admin-interface/pull/1605)] - Enable eslint no-misused-promises
- [[opencast/admin-interface#1627](https://github.com/opencast/admin-interface/pull/1627)] - Bump dorny/paths-filter from 3 to 4
- [[opencast/admin-interface#1628](https://github.com/opencast/admin-interface/pull/1628)] - Bump actions/github-script from 7 to 9
- [[opencast/admin-interface#1609](https://github.com/opencast/admin-interface/pull/1609)] - Enable eslint no-explicit-any
- [[opencast/admin-interface#1631](https://github.com/opencast/admin-interface/pull/1631)] - Bump actions/upload-artifact from 4 to 7
- [[opencast/admin-interface#1630](https://github.com/opencast/admin-interface/pull/1630)] - Bump actions/download-artifact from 4 to 8
- [[opencast/admin-interface#1629](https://github.com/opencast/admin-interface/pull/1629)] - Bump docker/build-push-action from 6 to 7
- [[opencast/admin-interface#1657](https://github.com/opencast/admin-interface/pull/1657)] - Remove some debug logging statements
- [[opencast/admin-interface#1656](https://github.com/opencast/admin-interface/pull/1656)] - Fix <td> <td> nesting
- [[opencast/admin-interface#1626](https://github.com/opencast/admin-interface/pull/1626)] - Fix a bunch of eslint and type-checker complaints
- [[opencast/admin-interface#1669](https://github.com/opencast/admin-interface/pull/1669)] - Assorted CSS code style improvements
- [[opencast/admin-interface#1653](https://github.com/opencast/admin-interface/pull/1653)] - Type acl default config keys

</details>

### Editor submodules changes
#### 📦 Uncategorized

- Fix focus loss caused by sliders affecting keyboard shortcuts (follow-up to [[#1650](https://github.com/opencast/editor/pull/1650)])
   - PR: [[#1694](https://github.com/opencast/editor/pull/1694)]
- Allow typing time in current time field
   - PR: [[#1675](https://github.com/opencast/editor/pull/1675)]
- Add configurable hotkeys
   - PR: [[#1677](https://github.com/opencast/editor/pull/1677)]
- Bump actions/github-script from 8 to 9
   - PR: [[#1699](https://github.com/opencast/editor/pull/1699)]
- Ensure node version matches parent pom
   - PR: [[#1700](https://github.com/opencast/editor/pull/1700)]
- Properly label GHA dependeabot updates
   - PR: [[#1701](https://github.com/opencast/editor/pull/1701)]
- Enable PR test builds into GH pages
   - PR: [[#1709](https://github.com/opencast/editor/pull/1709)]
- Improve npm checks
   - PR: [[#1710](https://github.com/opencast/editor/pull/1710)]
- Additional GHA bugs
   - PR: [[#1712](https://github.com/opencast/editor/pull/1712)]
- Update to React 19
   - PR: [[#1705](https://github.com/opencast/editor/pull/1705)]
- Bump the minor-and-patch group across 1 directory with 22 updates
   - PR: [[#1716](https://github.com/opencast/editor/pull/1716)]
- Final GHA polish
   - PR: [[#1715](https://github.com/opencast/editor/pull/1715)]
- Flip quote types in pr filter, correct indentation level
   - PR: [[#1711](https://github.com/opencast/editor/pull/1711)]
- Enable chapter view for development
   - PR: [[#1719](https://github.com/opencast/editor/pull/1719)]
- Bump the minor-and-patch group with 15 updates
   - PR: [[#1726](https://github.com/opencast/editor/pull/1726)]
- Bump jsdom from 29.1.1 to 30.0.1
   - PR: [[#1728](https://github.com/opencast/editor/pull/1728)]
- Subtitle timeline consolidation
   - PR: [[#1731](https://github.com/opencast/editor/pull/1731)]
- Prevent user from making video too short
   - PR: [[#1717](https://github.com/opencast/editor/pull/1717)]
- Make editor audio-only proof
   - PR: [[#1706](https://github.com/opencast/editor/pull/1706)]
- Yet more GHA issues
   - PR: [[#1733](https://github.com/opencast/editor/pull/1733)]
- Thumbnails in backend
   - PR: [[#1708](https://github.com/opencast/editor/pull/1708)]
- Redesign editor.opencast.org landing page
   - PR: [[#1720](https://github.com/opencast/editor/pull/1720)]



### Studio submodule changes
#### 🐛 Fixes

- [[opencast/studio#1333](https://github.com/opencast/studio/pull/1333)] - Improve npm checks
- [[opencast/studio#1360](https://github.com/opencast/studio/pull/1360)] - Fix broken step navigation buttons

#### Accessibility

- no matching PRs

#### Everything Else

- [[opencast/studio#1315](https://github.com/opencast/studio/pull/1315)] - Build(deps-dev): Bump follow-redirects from 1.15.11 to 1.16.0
- [[opencast/studio#1323](https://github.com/opencast/studio/pull/1323)] - Build(deps): Bump dawidd6/action-download-artifact from 19 to 21
- [[opencast/studio#1324](https://github.com/opencast/studio/pull/1324)] - Build(deps): Bump softprops/action-gh-release from 2 to 3
- [[opencast/studio#1322](https://github.com/opencast/studio/pull/1322)] - Build(deps-dev): Bump eslint-webpack-plugin from 5.0.3 to 6.0.0
- [[opencast/studio#1325](https://github.com/opencast/studio/pull/1325)] - Ensure node version matches parent pom
- [[opencast/studio#1336](https://github.com/opencast/studio/pull/1336)] - Build(deps): Bump @babel/core from 7.29.0 to 7.29.6
- [[opencast/studio#1335](https://github.com/opencast/studio/pull/1335)] - Build(deps-dev): Bump launch-editor from 2.12.0 to 2.14.1
- [[opencast/studio#1334](https://github.com/opencast/studio/pull/1334)] - Build(deps): Bump js-yaml from 4.1.1 to 4.2.0
- [[opencast/studio#1331](https://github.com/opencast/studio/pull/1331)] - Build(deps): Bump the minor-and-patch group across 1 directory with 17 updates
- [[opencast/studio#1330](https://github.com/opencast/studio/pull/1330)] - Build(deps): Bump qs and express
- [[opencast/studio#1327](https://github.com/opencast/studio/pull/1327)] - Build(deps): Bump @babel/plugin-transform-modules-systemjs from 7.28.5 to 7.29.7
- [[opencast/studio#1326](https://github.com/opencast/studio/pull/1326)] - Build(deps): Bump fast-uri from 3.1.0 to 3.1.2
- [[opencast/studio#1337](https://github.com/opencast/studio/pull/1337)] - Build(deps): Bump serialize-javascript and copy-webpack-plugin
- [[opencast/studio#1338](https://github.com/opencast/studio/pull/1338)] - Build(deps): Bump the minor-and-patch group with 7 updates
- [[opencast/studio#1343](https://github.com/opencast/studio/pull/1343)] - Build(deps): Bump actions/checkout from 6 to 7
- [[opencast/studio#1345](https://github.com/opencast/studio/pull/1345)] - Build(deps-dev): Bump websocket-driver from 0.7.4 to 0.7.5
- [[opencast/studio#1344](https://github.com/opencast/studio/pull/1344)] - Build(deps-dev): Bump ws from 8.18.3 to 8.21.0
- [[opencast/studio#1347](https://github.com/opencast/studio/pull/1347)] - Build(deps): Bump fast-uri from 3.1.2 to 3.1.4
- [[opencast/studio#1346](https://github.com/opencast/studio/pull/1346)] - Build(deps): Bump svgo from 3.3.3 to 3.3.4
- [[opencast/studio#1349](https://github.com/opencast/studio/pull/1349)] - Build(deps-dev): Bump webpack-dev-server from 5.2.5 to 5.2.6
- [[opencast/studio#1350](https://github.com/opencast/studio/pull/1350)] - Build(deps-dev): Bump shell-quote from 1.8.4 to 1.10.0
- [[opencast/studio#1351](https://github.com/opencast/studio/pull/1351)] - Build(deps-dev): Bump http-proxy-middleware from 2.0.9 to 2.0.10
- [[opencast/studio#1352](https://github.com/opencast/studio/pull/1352)] - Build(deps): Bump actions/setup-node from 6 to 7
- [[opencast/studio#1355](https://github.com/opencast/studio/pull/1355)] - Build(deps): Bump fast-uri from 3.1.4 to 3.1.5
- [[opencast/studio#1356](https://github.com/opencast/studio/pull/1356)] - Build(deps): Bump js-yaml from 4.2.0 to 4.3.1
- [[opencast/studio#1357](https://github.com/opencast/studio/pull/1357)] - Build(deps-dev): Bump brace-expansion from 1.1.15 to 1.1.18
- [[opencast/studio#1361](https://github.com/opencast/studio/pull/1361)] - Build(deps): Bump fast-uri from 3.1.5 to 3.1.7
- [[opencast/studio#1362](https://github.com/opencast/studio/pull/1362)] - Build(deps-dev): Bump @humanfs/node from 0.16.7 to 0.16.8
- [[opencast/studio#1363](https://github.com/opencast/studio/pull/1363)] - Build(deps): Bump browserslist from 4.28.1 to 4.28.8
- [[opencast/studio#1364](https://github.com/opencast/studio/pull/1364)] - Build(deps): Bump svgo from 3.3.4 to 3.3.5
- [[opencast/studio#1366](https://github.com/opencast/studio/pull/1366)] - Build(deps): Bump js-yaml from 4.3.1 to 4.3.2
- [[opencast/studio#1368](https://github.com/opencast/studio/pull/1368)] - Build(deps): Bump fast-uri from 3.1.7 to 3.1.8
- [[opencast/studio#1369](https://github.com/opencast/studio/pull/1369)] - Build(deps-dev): Bump brace-expansion from 1.1.18 to 1.1.21
- [[opencast/studio#1367](https://github.com/opencast/studio/pull/1367)] - Build(deps): Bump dawidd6/action-download-artifact from 21 to 26
- [[opencast/studio#1370](https://github.com/opencast/studio/pull/1370)] - Build(deps-dev): Bump proxy-addr from 2.0.7 to 2.0.8
- [[opencast/studio#1371](https://github.com/opencast/studio/pull/1371)] - Build(deps): Bump source-map-js from 1.2.1 to 1.2.2
- [[opencast/studio#1373](https://github.com/opencast/studio/pull/1373)] - Build(deps-dev): Bump shell-quote from 1.10.0 to 1.12.0
- [[opencast/studio#1372](https://github.com/opencast/studio/pull/1372)] - Build(deps-dev): Bump compression from 1.8.1 to 1.8.2



<details>
<summary>Uncategorized</summary>

- [[opencast/studio#1315](https://github.com/opencast/studio/pull/1315)] - Build(deps-dev): Bump follow-redirects from 1.15.11 to 1.16.0
- [[opencast/studio#1323](https://github.com/opencast/studio/pull/1323)] - Build(deps): Bump dawidd6/action-download-artifact from 19 to 21
- [[opencast/studio#1324](https://github.com/opencast/studio/pull/1324)] - Build(deps): Bump softprops/action-gh-release from 2 to 3
- [[opencast/studio#1322](https://github.com/opencast/studio/pull/1322)] - Build(deps-dev): Bump eslint-webpack-plugin from 5.0.3 to 6.0.0
- [[opencast/studio#1325](https://github.com/opencast/studio/pull/1325)] - Ensure node version matches parent pom
- [[opencast/studio#1336](https://github.com/opencast/studio/pull/1336)] - Build(deps): Bump @babel/core from 7.29.0 to 7.29.6
- [[opencast/studio#1335](https://github.com/opencast/studio/pull/1335)] - Build(deps-dev): Bump launch-editor from 2.12.0 to 2.14.1
- [[opencast/studio#1334](https://github.com/opencast/studio/pull/1334)] - Build(deps): Bump js-yaml from 4.1.1 to 4.2.0
- [[opencast/studio#1331](https://github.com/opencast/studio/pull/1331)] - Build(deps): Bump the minor-and-patch group across 1 directory with 17 updates
- [[opencast/studio#1330](https://github.com/opencast/studio/pull/1330)] - Build(deps): Bump qs and express
- [[opencast/studio#1327](https://github.com/opencast/studio/pull/1327)] - Build(deps): Bump @babel/plugin-transform-modules-systemjs from 7.28.5 to 7.29.7
- [[opencast/studio#1326](https://github.com/opencast/studio/pull/1326)] - Build(deps): Bump fast-uri from 3.1.0 to 3.1.2
- [[opencast/studio#1337](https://github.com/opencast/studio/pull/1337)] - Build(deps): Bump serialize-javascript and copy-webpack-plugin
- [[opencast/studio#1338](https://github.com/opencast/studio/pull/1338)] - Build(deps): Bump the minor-and-patch group with 7 updates
- [[opencast/studio#1343](https://github.com/opencast/studio/pull/1343)] - Build(deps): Bump actions/checkout from 6 to 7
- [[opencast/studio#1345](https://github.com/opencast/studio/pull/1345)] - Build(deps-dev): Bump websocket-driver from 0.7.4 to 0.7.5
- [[opencast/studio#1344](https://github.com/opencast/studio/pull/1344)] - Build(deps-dev): Bump ws from 8.18.3 to 8.21.0
- [[opencast/studio#1347](https://github.com/opencast/studio/pull/1347)] - Build(deps): Bump fast-uri from 3.1.2 to 3.1.4
- [[opencast/studio#1346](https://github.com/opencast/studio/pull/1346)] - Build(deps): Bump svgo from 3.3.3 to 3.3.4
- [[opencast/studio#1349](https://github.com/opencast/studio/pull/1349)] - Build(deps-dev): Bump webpack-dev-server from 5.2.5 to 5.2.6
- [[opencast/studio#1350](https://github.com/opencast/studio/pull/1350)] - Build(deps-dev): Bump shell-quote from 1.8.4 to 1.10.0
- [[opencast/studio#1351](https://github.com/opencast/studio/pull/1351)] - Build(deps-dev): Bump http-proxy-middleware from 2.0.9 to 2.0.10
- [[opencast/studio#1352](https://github.com/opencast/studio/pull/1352)] - Build(deps): Bump actions/setup-node from 6 to 7
- [[opencast/studio#1355](https://github.com/opencast/studio/pull/1355)] - Build(deps): Bump fast-uri from 3.1.4 to 3.1.5
- [[opencast/studio#1356](https://github.com/opencast/studio/pull/1356)] - Build(deps): Bump js-yaml from 4.2.0 to 4.3.1
- [[opencast/studio#1357](https://github.com/opencast/studio/pull/1357)] - Build(deps-dev): Bump brace-expansion from 1.1.15 to 1.1.18
- [[opencast/studio#1361](https://github.com/opencast/studio/pull/1361)] - Build(deps): Bump fast-uri from 3.1.5 to 3.1.7
- [[opencast/studio#1362](https://github.com/opencast/studio/pull/1362)] - Build(deps-dev): Bump @humanfs/node from 0.16.7 to 0.16.8
- [[opencast/studio#1363](https://github.com/opencast/studio/pull/1363)] - Build(deps): Bump browserslist from 4.28.1 to 4.28.8
- [[opencast/studio#1364](https://github.com/opencast/studio/pull/1364)] - Build(deps): Bump svgo from 3.3.4 to 3.3.5
- [[opencast/studio#1366](https://github.com/opencast/studio/pull/1366)] - Build(deps): Bump js-yaml from 4.3.1 to 4.3.2
- [[opencast/studio#1368](https://github.com/opencast/studio/pull/1368)] - Build(deps): Bump fast-uri from 3.1.7 to 3.1.8
- [[opencast/studio#1369](https://github.com/opencast/studio/pull/1369)] - Build(deps-dev): Bump brace-expansion from 1.1.18 to 1.1.21
- [[opencast/studio#1367](https://github.com/opencast/studio/pull/1367)] - Build(deps): Bump dawidd6/action-download-artifact from 21 to 26
- [[opencast/studio#1370](https://github.com/opencast/studio/pull/1370)] - Build(deps-dev): Bump proxy-addr from 2.0.7 to 2.0.8
- [[opencast/studio#1371](https://github.com/opencast/studio/pull/1371)] - Build(deps): Bump source-map-js from 1.2.1 to 1.2.2
- [[opencast/studio#1373](https://github.com/opencast/studio/pull/1373)] - Build(deps-dev): Bump shell-quote from 1.10.0 to 1.12.0
- [[opencast/studio#1372](https://github.com/opencast/studio/pull/1372)] - Build(deps-dev): Bump compression from 1.8.1 to 1.8.2

</details>
