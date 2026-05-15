Opencast 20 Changelog
---------------------

## Opencast 20.0 (2026-05-15)

- [[#7612](https://github.com/opencast/opencast/pull/7612)] -
  Update 20.x Admin Interface
- [[#7590](https://github.com/opencast/opencast/pull/7590)] -
  Add missing modules to jacoco pom
- [[#7585](https://github.com/opencast/opencast/pull/7585)] -
  Move ContributorsListProvider configuration
- [[#7578](https://github.com/opencast/opencast/pull/7578)] -
  Update develop Editor
- [[#7575](https://github.com/opencast/opencast/pull/7575)] -
  Add editor config "Stop on typing"
- [[#7571](https://github.com/opencast/opencast/pull/7571)] -
  Update develop Editor
- [[#7570](https://github.com/opencast/opencast/pull/7570)] -
  Update develop Admin Interface
- [[#7554](https://github.com/opencast/opencast/pull/7554)] -
  Fix typo in library tag in pom.xml
- [[#7552](https://github.com/opencast/opencast/pull/7552)] -
  Update develop Editor
- [[#7550](https://github.com/opencast/opencast/pull/7550)] -
  Update develop Studio
- [[#7543](https://github.com/opencast/opencast/pull/7543)] -
  Update develop Admin Interface
- [[#7541](https://github.com/opencast/opencast/pull/7541)] -
  Add config option to filter available roles in admin ui dropdowns
- [[#7517](https://github.com/opencast/opencast/pull/7517)] -
  20.x submodules
- [[#7486](https://github.com/opencast/opencast/pull/7486)] -
  Revert #6292 with updated Pax web
- [[#7482](https://github.com/opencast/opencast/pull/7482)] -
  Remove unused `YouTubeKey.scopes` enum entry
- [[#7450](https://github.com/opencast/opencast/pull/7450)] -
  Disable error states by default
- [[#7394](https://github.com/opencast/opencast/pull/7394)] -
  Remove redundant `<filtering>false</filtering>`
- [[#7393](https://github.com/opencast/opencast/pull/7393)] -
  Fix build date in BundleInfoRestEndpoint
- [[#7388](https://github.com/opencast/opencast/pull/7388)] -
  Simplify/improve checkstyle configuration
- [[#7370](https://github.com/opencast/opencast/pull/7370)] -
  Only cache ffmpeg upstream
- [[#7346](https://github.com/opencast/opencast/pull/7346)] -
  Update develop Admin UI to 20.x-2026-01-29
- [[#7336](https://github.com/opencast/opencast/pull/7336)] -
  Follow up on Paella 8 default player configuration
- [[#7334](https://github.com/opencast/opencast/pull/7334)] -
  Refactor docker compose service configuration
- [[#7317](https://github.com/opencast/opencast/pull/7317)] -
  Remove jmx beans
- [[#7315](https://github.com/opencast/opencast/pull/7315)] -
  Remove AssetDtos.java
- [[#7312](https://github.com/opencast/opencast/pull/7312)] -
  Add @Deactivate method to JobDispatcher.java
- [[#7311](https://github.com/opencast/opencast/pull/7311)] -
  Deactivate heartbeat on non-dispatching nodes
- [[#7290](https://github.com/opencast/opencast/pull/7290)] -
  Document release notes in PR template
- [[#7266](https://github.com/opencast/opencast/pull/7266)] -
  Update develop Admin UI to 20.x-2025-12-05
- [[#7214](https://github.com/opencast/opencast/pull/7214)] -
  Deprecate Paella 7 with OC 20
- [[#7200](https://github.com/opencast/opencast/pull/7200)] -
  Update develop Editor to 19.x-2025-11-20
- [[#7199](https://github.com/opencast/opencast/pull/7199)] -
  Update develop Admin UI to 19.x-2025-11-20
- [[#7198](https://github.com/opencast/opencast/pull/7198)] -
  Silence unconfigured Matomo stats provider
- [[#7197](https://github.com/opencast/opencast/pull/7197)] -
  Update Studio to 2025-11-19
- [[#7196](https://github.com/opencast/opencast/pull/7196)] -
  Fix a typo in JWT docs
- [[#7193](https://github.com/opencast/opencast/pull/7193)] -
  Rename to Dev meeting in docs
- [[#7190](https://github.com/opencast/opencast/pull/7190)] -
  Change default `mediapackage-element-type` to `Track` for WebVttCaptionConverter
- [[#7182](https://github.com/opencast/opencast/pull/7182)] -
  Fix APIs replying 302 for unauthorized requests
- [[#7176](https://github.com/opencast/opencast/pull/7176)] -
  Extend internal queries to allow filtering events by extended metadata
- [[#7168](https://github.com/opencast/opencast/pull/7168)] -
  Add Caddy Reverse Proxy documentation
- [[#7142](https://github.com/opencast/opencast/pull/7142)] -
  Chapter Editor Backend
- [[#7128](https://github.com/opencast/opencast/pull/7128)] -
  Remove problematic circular reference
- [[#6891](https://github.com/opencast/opencast/pull/6891)] -
  Update FFmpeg Cache
- [[#6649](https://github.com/opencast/opencast/pull/6649)] -
  Introduce configuration "atLeastOne" for getting tags and flavors
- [[#5955](https://github.com/opencast/opencast/pull/5955)] -
  Refactor Activator.java to improve database connection handling

<details><summary>Dependency updates</summary>
  <ul>
    <li>[<a href="https://github.com/opencast/opencast/pull/7572">7572</a>] -
      Build(deps): bump follow-redirects from 1.15.6 to 1.16.0 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7567">7567</a>] -
      Bump axios from 1.13.5 to 1.15.0 in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7546">7546</a>] -
      Build(deps-dev): bump vite from 7.2.7 to 7.3.2 in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7535">7535</a>] -
      Build(deps): bump actions/upload-artifact from 5 to 7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7519">7519</a>] -
      Bump serialize-javascript, terser-webpack-plugin and copy-webpack-plugin in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7515">7515</a>] -
      Build(deps): bump org.codehaus.plexus:plexus-utils from 4.0.1 to 4.0.3</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7514">7514</a>] -
      Build(deps): bump brace-expansion from 5.0.4 to 5.0.5 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7513">7513</a>] -
      Build(deps): bump node-forge from 1.3.3 to 1.4.0 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7511">7511</a>] -
      Build(deps): bump yaml from 2.8.2 to 2.8.3 in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7508">7508</a>] -
      Build(deps-dev): bump node-forge from 1.3.1 to 1.4.0 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7506">7506</a>] -
      Build(deps-dev): bump picomatch from 4.0.3 to 4.0.4 in /docs/guides</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7505">7505</a>] -
      Build(deps): bump picomatch in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7504">7504</a>] -
      Build(deps): bump picomatch from 2.3.1 to 2.3.2 in /modules/graphql-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7503">7503</a>] -
      Build(deps): bump picomatch in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7502">7502</a>] -
      Build(deps): bump picomatch in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7501">7501</a>] -
      Build(deps): bump yaml from 1.10.2 to 1.10.3 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7495">7495</a>] -
      Build(deps): bump flatted from 3.4.1 to 3.4.2 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7494">7494</a>] -
      Build(deps): bump flatted from 3.4.1 to 3.4.2 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7492">7492</a>] -
      Build(deps-dev): bump flatted from 3.3.1 to 3.4.2 in /modules/graphql-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7491">7491</a>] -
      Build(deps-dev): bump flatted from 3.3.1 to 3.4.2 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7479">7479</a>] -
      Build(deps): bump flatted from 3.3.1 to 3.4.1 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7478">7478</a>] -
      Build(deps): bump flatted from 3.2.6 to 3.4.1 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7466">7466</a>] -
      Build(deps): bump qs and body-parser in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7447">7447</a>] -
      Build(deps): bump @eslint/js from 9.39.2 to 10.0.1 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7446">7446</a>] -
      Build(deps): bump globals from 17.3.0 to 17.4.0 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7445">7445</a>] -
      Build(deps): bump eslint-plugin-headers from 1.3.3 to 1.3.4 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7444">7444</a>] -
      Build(deps-dev): bump @types/node from 24.6.1 to 25.3.3 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7440">7440</a>] -
      Build(deps): bump i18next-browser-languagedetector from 8.2.0 to 8.2.1 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7438">7438</a>] -
      Build(deps): bump iframe-resizer from 5.5.4 to 5.5.9 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7436">7436</a>] -
      Build(deps): bump axios from 1.13.5 to 1.13.6 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7432">7432</a>] -
      Build(deps): bump commons-codec:commons-codec from 1.18.0 to 1.21.0 in /modules/metrics-exporter</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7433">7433</a>] -
      Build(deps): bump actions/download-artifact from 7 to 8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7431">7431</a>] -
      Build(deps): bump the fontawesome group in /modules/lti with 3 updates</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7430">7430</a>] -
      Build(deps): bump org.slf4j:slf4j-api from 2.0.16 to 2.0.17 in /modules/metrics-exporter</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7429">7429</a>] -
      Build(deps): bump actions/upload-artifact from 6 to 7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7428">7428</a>] -
      Build(deps): bump commons-io:commons-io from 2.20.0 to 2.21.0 in /modules/metrics-exporter</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7427">7427</a>] -
      Build(deps-dev): bump eslint from 9.39.3 to 10.0.2 in /modules/engage-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7426">7426</a>] -
      Build(deps-dev): bump org.apache.maven.plugins:maven-gpg-plugin from 3.1.0 to 3.2.8 in /modules/db</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7425">7425</a>] -
      Build(deps): bump org.apache.felix:maven-bundle-plugin from 5.1.9 to 6.0.2 in /modules/db</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7424">7424</a>] -
      Build(deps-dev): bump org.apache.maven.plugins:maven-resources-plugin from 3.3.1 to 3.4.0 in /modules/metrics-exporter</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7423">7423</a>] -
      Build(deps): bump underscore from 1.13.7 to 1.13.8 in /modules/engage-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7416">7416</a>] -
      Build(deps-dev): bump minimatch from 3.1.2 to 3.1.5 in /modules/graphql-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7415">7415</a>] -
      Build(deps): bump minimatch in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7411">7411</a>] -
      Build(deps): bump rollup from 2.79.2 to 2.80.0 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7410">7410</a>] -
      Build(deps): bump rollup from 4.53.3 to 4.59.0 in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7405">7405</a>] -
      Build(deps): bump minimatch in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7403">7403</a>] -
      Build(deps): bump minimatch from 3.1.2 to 3.1.3 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7401">7401</a>] -
      Build(deps): bump ajv from 6.12.6 to 6.14.0 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7400">7400</a>] -
      Build(deps): bump ajv from 6.12.6 to 6.14.0 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7392">7392</a>] -
      Build(deps-dev): bump qs from 6.14.0 to 6.14.2 in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7391">7391</a>] -
      Build(deps): bump qs and express in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7389">7389</a>] -
      Build(deps): bump markdown-it from 14.1.0 to 14.1.1 in /modules/graphql-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7382">7382</a>] -
      Build(deps): bump axios from 1.13.2 to 1.13.5 in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7381">7381</a>] -
      Build(deps): bump axios from 1.13.4 to 1.13.5 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7362">7362</a>] -
      Bump webpack from 5.94.0 to 5.105.0 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7361">7361</a>] -
      Bump webpack from 5.97.1 to 5.105.0 in /modules/graphql-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7360">7360</a>] -
      Bump webpack from 5.102.1 to 5.105.0 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7357">7357</a>] -
      Bump eslint from 9.2.0 to 9.39.2 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7356">7356</a>] -
      Bump globals from 16.5.0 to 17.3.0 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7355">7355</a>] -
      Bump @babel/core from 7.28.5 to 7.29.0 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7354">7354</a>] -
      Bump @babel/preset-env from 7.28.5 to 7.29.0 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7353">7353</a>] -
      Bump html-validate from 10.2.1 to 10.7.0 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7352">7352</a>] -
      Bump @playwright/test from 1.55.1 to 1.58.1 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7351">7351</a>] -
      Bump css-loader from 7.1.2 to 7.1.3 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7350">7350</a>] -
      Build(deps-dev): bump webpack-dev-server from 5.2.2 to 5.2.3 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7349">7349</a>] -
      Bump jquery from 3.7.1 to 4.0.0 in /modules/runtime-info-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7331">7331</a>] -
      Bump lodash from 4.17.21 to 4.17.23 in /modules/graphql-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7330">7330</a>] -
      Bump lodash from 4.17.21 to 4.17.23 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7316">7316</a>] -
      Bump preact from 10.28.0 to 10.28.2 in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7307">7307</a>] -
      Bump glob and markdownlint-cli in /docs/guides</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7305">7305</a>] -
      Bump qs and express in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7302">7302</a>] -
      Bump @eslint/js from 9.39.0 to 9.39.2 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7301">7301</a>] -
      Bump actions/upload-artifact from 5 to 6</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7300">7300</a>] -
      Bump actions/download-artifact from 6 to 7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7299">7299</a>] -
      Bump actions/cache from 4 to 5</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7298">7298</a>] -
      Bump eslint from 9.39.0 to 9.39.2 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7297">7297</a>] -
      Bump webpack from 5.102.1 to 5.104.1 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7295">7295</a>] -
      Bump markdownlint-cli from 0.45.0 to 0.47.0 in /docs/guides</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7294">7294</a>] -
      Bump langchain from 0.3.36 to 0.3.37 in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7293">7293</a>] -
      Build(deps): bump @langchain/core from 0.3.79 to 0.3.80 in /modules/engage-paella-player-8</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7248">7248</a>] -
      Build(deps): bump org.mozilla:rhino from 1.7.13 to 1.7.14.1 in /modules/cover-image-impl</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7247">7247</a>] -
      Build(deps): bump node-forge from 1.3.1 to 1.3.3 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7233">7233</a>] -
      Build(deps): bump org.owasp.esapi:esapi from 2.6.0.0 to 2.7.0.0 in /modules/metrics-exporter</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7229">7229</a>] -
      Build(deps): bump actions/checkout from 5 to 6</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7228">7228</a>] -
      Build(deps): bump requirejs from 2.3.7 to 2.3.8 in /modules/engage-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7222">7222</a>] -
      Build(deps): bump org.osgi:org.osgi.service.cm from 1.6.0 to 1.6.1 in /modules/db</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7221">7221</a>] -
      Build(deps): bump net.java.dev.jna:jna from 5.14.0 to 5.18.1 in /modules/db</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7211">7211</a>] -
      Build(deps-dev): bump node-forge from 1.3.1 to 1.3.2 in /modules/graphql-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7194">7194</a>] -
      Build(deps): bump js-yaml in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7188">7188</a>] -
      Build(deps-dev): bump js-yaml from 4.1.0 to 4.1.1 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7187">7187</a>] -
      Build(deps-dev): bump js-yaml from 4.1.0 to 4.1.1 in /modules/graphql-ui</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7186">7186</a>] -
      Build(deps): bump js-yaml from 4.1.0 to 4.1.1 in /docs/checkstyle/eslint-config</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7185">7185</a>] -
      Build(deps-dev): bump js-yaml from 4.1.0 to 4.1.1 in /docs/guides</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7152">7152</a>] -
      Build(deps-dev): bump @babel/eslint-parser from 7.28.4 to 7.28.5 in /modules/engage-paella-player-7</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7148">7148</a>] -
      Build(deps): bump com.google.code.gson:gson from 2.10.1 to 2.13.2 in /modules/metrics-exporter</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7147">7147</a>] -
      Build(deps): bump org.codehaus.mojo:exec-maven-plugin from 3.5.0 to 3.6.2 in /modules/db</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7145">7145</a>] -
      Build(deps): bump the fontawesome group across 1 directory with 3 updates</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7144">7144</a>] -
      Build(deps-dev): bump junit5.version from 5.12.2 to 6.0.1 in /modules/metrics-exporter</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7084">7084</a>] -
      Build(deps-dev): bump @types/node from 24.1.0 to 24.6.1 in /modules/lti</li>
    <li>[<a href="https://github.com/opencast/opencast/pull/7079">7079</a>] -
      Build(deps): bump axios from 1.11.0 to 1.12.2 in /modules/lti</li>
  </ul>
</details>

### Admin Interface

- [[#1570](https://github.com/opencast/admin-interface/pull/1570)] -
  Fixing more a11y issues in the embed dialog
- [[#1569](https://github.com/opencast/admin-interface/pull/1569)] -
  Switch to react-tooltips
- [[#1568](https://github.com/opencast/admin-interface/pull/1568)] -
  Use react-select for page size dropdown
- [[#1567](https://github.com/opencast/admin-interface/pull/1567)] -
  Show upload OR schedule workflows in Create Event
- [[#1566](https://github.com/opencast/admin-interface/pull/1566)] -
  Preselect all capture agent inputs when scheduling new event
- [[#1564](https://github.com/opencast/admin-interface/pull/1564)] -
  Unify date formatting, base it on locale
- [[#1561](https://github.com/opencast/admin-interface/pull/1561)] -
  Filter available roles in dropdown by config
- [[#1560](https://github.com/opencast/admin-interface/pull/1560)] -
  Fixing more a11y issues in the create event dialog
- [[#1541](https://github.com/opencast/admin-interface/pull/1541)] -
  Add 20.x submodule components
- [[#1511](https://github.com/opencast/admin-interface/pull/1511)] -
  Update react-hotkeys-hook to 5.2.4
- [[#1484](https://github.com/opencast/admin-interface/pull/1484)] -
  Update react-window to major version 2
- [[#1447](https://github.com/opencast/admin-interface/pull/1447)] -
  Feature Add next/previous tab hotkeys
- [[#1431](https://github.com/opencast/admin-interface/pull/1431)] -
  Css color consistency
- [[#1411](https://github.com/opencast/admin-interface/pull/1411)] -
  Fix that scheduling an event currently in progress gives you the wrong error message

<details><summary>Dependency updates</summary>
  <ul>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1556">1556</a>] -
      Bump @redux-devtools/extension from 3.3.0 to 4.0.0</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1549">1549</a>] -
      Bump docker/login-action from 3 to 4</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1548">1548</a>] -
      Bump dorny/paths-filter from 3 to 4</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1547">1547</a>] -
      Bump docker/build-push-action from 6 to 7</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1546">1546</a>] -
      Bump docker/metadata-action from 5 to 6</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1512">1512</a>] -
      Bump the minor-and-patch group across 1 directory with 19 updates</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1505">1505</a>] -
      Bump focus-trap-react from 11.0.4 to 12.0.0</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1493">1493</a>] -
      Bump vitest from 3.2.4 to 4.0.16</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1492">1492</a>] -
      Bump @types/node from 24.9.2 to 25.0.3</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1490">1490</a>] -
      Bump vite-tsconfig-paths from 5.1.4 to 6.0.3</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1488">1488</a>] -
      Bump @types/uuid from 10.0.0 to 11.0.0</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1487">1487</a>] -
      Bump the minor-and-patch group with 17 updates</li>
    <li>[<a href="https://github.com/opencast/admin-interface/pull/1455">1455</a>] -
      Bump react-i18next from 15.6.1 to 16.0.0</li>
  </ul>
</details>

### Editor
- [[#1688](https://github.com/opencast/editor/pull/1688)] -
  Add POM file to 20.x
- [[#1681](https://github.com/opencast/editor/pull/1681)] -
  Move timeline when scrubber goes out of frame
- [[#1680](https://github.com/opencast/editor/pull/1680)] -
  Add timeline stamps
- [[#1679](https://github.com/opencast/editor/pull/1679)] -
  Copy current playtime into input field
- [[#1671](https://github.com/opencast/editor/pull/1671)] -
  Slightly less generic error message for unauthorized users
- [[#1670](https://github.com/opencast/editor/pull/1670)] -
  Revert develop GHA release changes
- [[#1669](https://github.com/opencast/editor/pull/1669)] -
  Add linebreaks in tooltips
- [[#1668](https://github.com/opencast/editor/pull/1668)] -
  Rename "Shortcuts" to "Hotkeys"
- [[#1667](https://github.com/opencast/editor/pull/1667)] -
  Allow translating preview mode status in tooltip
- [[#1663](https://github.com/opencast/editor/pull/1663)] -
  Overhaul thumbnail view
- [[#1661](https://github.com/opencast/editor/pull/1661)] -
  Remove extra dashes from license designators
- [[#1647](https://github.com/opencast/editor/pull/1647)] -
  Chapter Editor

<details><summary>Dependency updates</summary>
  <ul>
    <li>[<a href="https://github.com/opencast/editor/pull/1691">1691</a>] -
      Bump dorny/paths-filter from 3 to 4</li>
    <li>[<a href="https://github.com/opencast/editor/pull/1684">1684</a>] -
      Bump actions/upload-artifact from 4 to 7</li>
    <li>[<a href="https://github.com/opencast/editor/pull/1653">1653</a>] -
      Bump softprops/turnstyle from 2 to 3</li>
    <li>[<a href="https://github.com/opencast/editor/pull/1652">1652</a>] -
      Bump actions/checkout from 4 to 6</li>
    <li>[<a href="https://github.com/opencast/editor/pull/1649">1649</a>] -
      Bump actions/setup-node from 4 to 6</li>
  </ul>
</details>

### Studio
- [[#1313](https://github.com/opencast/studio/pull/1313)] -
  Sort language selector

<details><summary>Dependency updates</summary>
  <ul>
    <li>[<a href="https://github.com/opencast/studio/pull/1311">1311</a>] -
      Build(deps): Bump lodash from 4.17.23 to 4.18.1</li>
    <li>[<a href="https://github.com/opencast/studio/pull/1307">1307</a>] -
      Build(deps): Bump dawidd6/action-download-artifact from 16 to 19</li>
    <li>[<a href="https://github.com/opencast/studio/pull/1305">1305</a>] -
      Build(deps): Bump i18next from 25.8.13 to 26.0.3</li>
    <li>[<a href="https://github.com/opencast/studio/pull/1303">1303</a>] -
      Build(deps): Bump react-i18next from 16.5.4 to 17.0.2</li>
    <li>[<a href="https://github.com/opencast/studio/pull/1302">1302</a>] -
      Build(deps): Bump the minor-and-patch group with 7 updates</li>
  </ul>
</details>
