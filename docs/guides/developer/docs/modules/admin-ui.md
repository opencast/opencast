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

Checkout and build Opencast.

You need [Grunt][1] and [NodeJS][2].
Either install it on your system or use the local version from the Opencast module:

```bash
cd modules/matterhorn-admin-ui-ng
export PATH=$PATH:node:node_modules/grunt-cli/bin:node_modules/karma/bin
```
*Note: The node and node_modules folders are created during the Maven build process.*

*Note: We already had reports of Grunt behaving differently on different systems.
Watch out for local or system-wide installations of Grunt and/or NodeJS as they can
change the build behavior of Admin UI NG.*

Debugging unit Javascript unit tests
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
`karma start src/test/resources/karma.conf.js --browsers=Chrome --single-run=false`


Live working with running Opencast
----------------------------------

In order to speed up the UI development process, you can test the code without
building the module with Maven.
There is a Grunt task for starting a standalone web server offering the UI from
the source.
Changes to the source will (after a page reload) be directly reflected in the browser.

*Be warned that some functionality in this live setup can be limited.
Before reporting an issue, please test if you can reproduced the issue with a built Opencast.*

This setup may be configured as follows:

1. Follow the instructions in the Prerequisites section.

2. Start your Opencast instance.

3. Start the standalone webserver by running:
```bash
cd modules/matterhorn-admin-ui-ng
grunt proxy --proxy.host=http://localhost:8080 --proxy.username=opencast_system_account --proxy.password=CHANGE_ME
```

*Note: host, username and password have to match your configuration
`etc/custom.properties`*

Grunt should print out the URL where you can see the standalone page running
from sources.
Example:
```
[I 160420 16:35:29 server:281] Serving on http://127.0.0.1:8000
```

If you make changes to the Admin UI NG sources, they should be visible in
the browser after a page reload.
You can still access the builtin UI by accessing
`http://localhost:8080`.

[1]: http://gruntjs.com
[2]: https://nodejs.org

Live working with a Mockup
--------------------------

If you do not want to keep a running Opencast instance for developing the
Admin UI NG, you can start a mockup.

*Be warned that __a lot__ of this mockup's functionality acts very differently from
an actual Opencast instance*

This setup may be configured as follows:

1. Follow the instructions in the Prerequisites section.

2. Start the mockup webserver by running:
```bash
cd modules/matterhorn-admin-ui-ng
grunt serve
```

Grunt should print out the URL where you can see the standalone page running
from sources.
Example:
```
[I 160420 16:35:29 server:281] Serving on http://127.0.0.1:8000
```

If you make changes to the Admin UI NG sources, they should be visible in
the browser after a page reload.
