Coding Principles
-----------------

<dl>
  <dt>Modularity</dt>
  <dd>Reusable interface components to build out new views based on the existing code structure.</dd>
  <dt>Validation</dt>
  <dd>All code languages are HTML5 and CSS3 and adhere to W3C standards.</dd>
  <dt>Extension</dt>
  <dd>For ease of UI and UX changes the styling elements are driven by SASS CSS pre-compiler.</dd>
</dl>

Style Guide
-----------

The style guide defines a set of guidelines that the design follows to maintain a consistent look and feel.
It is defined to be flexible, easily updateable and consistent. Before delving deeper into the UI or
developing additional features we recommend familiarizing yourself with some of the items.

Modifying Sources
-----------------

When you make changes to the sources, it should be sufficient to rebuild the
Admin UI NG module and copy the packaged module file into the Opencast assembly.

Example:
```bash
cd modules/matterhorn-admin-ui-ng
mvn install
cp ./target/matterhorn-admin-ui-ng-2.2-SNAPSHOT.jar ../../build/opencast-dist-allinone-2.2-SNAPSHOT/system/org/opencastproject/matterhorn-admin-ui-ng/2.2-SNAPSHOT/matterhorn-admin-ui-ng-2.2-SNAPSHOT.jar
```

*Note: Before you run `mvn install` from a module's root directory,
please make sure that you have built the complete software at least once
--i.e. you should have already run `mvn install` from the repository root
at some point.*

*Note: In the example above, the paths are for a specific Opencast version.
Your paths might look different.*

Prerequisites for live working and debugging
------------------------------

1. Checkout and build Opencast.

2. You will need to install [NodeJS][2] (which includes npm).

3. Before setting up the local server ensure that your npm is up-to-date (this might require `sudo` on certain systems): 

        npm update -g npm

You can also run a local version of [Grunt][1] and [NodeJS][2] from the Opencast module:

```bash
cd modules/matterhorn-admin-ui-ng
export PATH=$PATH:node:node_modules/grunt-cli/bin:node_modules/karma/bin
```

*Note: The node and node_modules folders are created during the Maven build process.*

*Note: We already had reports of Grunt behaving differently on different systems.
Watch out for local or system-wide installations of Grunt and/or NodeJS as they can
change the build behavior of Admin UI NG.*

<<<<<<< HEAD:docs/guides/developer/docs/modules/admin-ui/development.md
Debugging Javascript unit tests
------------------------------------

Our Javascript unit tests are built in Jasmine, and live in `modules/matterhorn-admin-ui-ng/src/test/resources/test/unit`.
Occasionally something breaks, or you need to disable or focus on a single test.
While reading the Jasmine (and Karma, and Grunt) docs is encouraged, here are a
few common recipies that might be useful:

### Disabling a unit test temporarily
Add `x` to the broken test.  For example:

|Before|After|
|------|-----|
|`it('runs a test', function () {`|`xit('runs a test', function () {`|

### Running a single unit test

Add `f` (for focus) to the relevant test.  For example:

|Before|After|
|------|-----|
|`it('runs a test', function () {`|`fit('runs a test', function () {`|

### Triggering a browser debugging session

This triggers an instance of the selected browser(s) to open and begin running
the tests.  There will be a `Debug` button which will open another tab without
the javascript minification - use this second tab for your debugging!
Refreshing the debugging page will rerun the tests

To run Karma for Chrome
`npm run test-chrome`

To run Karma for Firefox
`npm run test-firefox`

To run Karma for IE
`npm run test-ie`

Additional browsers are supported, the full list can be found at [here](https://karma-runner.github.io/1.0/config/browsers.html)


Live working with running Opencast
----------------------------------
=======
Live working with a running Opencast
------------------------------------
>>>>>>> 04ef604... MH-11756: Updated Admin UI Node packages and Grunt configuration for dynamic reloading of pages and documentation to that effect.:docs/guides/developer/docs/modules/admin-ui.md

In order to speed up the UI development process, you can test the code without
building the module with Maven. There is a Grunt task for starting a standalone web server offering the UI from
the source and a separate task that will monitoring any change to the Sass, JavaScript and HTML files and reload the page dynamically.

*Be warned that some functionality in this live setup can be limited.
Before reporting an issue, please test if you can reproduced the issue with a built Opencast.*

This setup may be configured as follows:

1. Follow the instructions in the Prerequisites section.

1. Start your Opencast instance.

1. Change to the Admin UI module directory.

        cd modules/matterhorn-admin-ui-ng

1. Install project dependencies.

        npm install

1. Start the standalone web server by running:

        grunt proxy --proxy.host=http://localhost:8080 --proxy.username=opencast_system_account --proxy.password=CHANGE_ME

*Note: host, username and password have to match your configuration `../etc/custom.properties`*

Grunt should print out the [URL][3] where you can see the standalone page running
from source.
```
Started connect web server on http://localhost:9000
```

To run the watcher that updates the displayed page dynamically, run in the same folder: 
```
grunt watch
```

Which should then display:
```
Running "watch" task
Waiting...
```

Live working with a Mockup
--------------------------

If you do not want to keep a running Opencast instance for developing the
Admin UI NG, you can start a mockup.

*Be warned that __a lot__ of this mockup's functionality acts very differently from
an actual Opencast instance*

This setup may be configured as follows:

1. Follow the instructions in the Prerequisites section.

1. Change to the Admin UI module directory.

        cd modules/matterhorn-admin-ui-ng

1. Install project dependencies.

        npm install

1. Start the mockup webserver by running:

        grunt serve


Grunt should print out the [URL][3] where you can see the standalone page running
from source.
```
Started connect web server on http://localhost:9000
```

If you make changes to the Admin UI NG source files, the page should auto reload to display the changes.

[1]: http://gruntjs.com
[2]: https://nodejs.org
[3]: http://localhost:9000