Style Guide
-----------

The style guide defines a set of guidelines that the design follows to maintain a consistent look and feel.
It is defined to be flexible, easy to update and consistent. Before delving deeper into the UI or
developing additional features we recommend familiarizing yourself with some of the items.


Prerequisites
-------------

For admin interface development, it may come handy to run npm or grunt from the command line. For that, you can either
install those tools globally on your machine, or use the versions Opencast installs locally when building via Maven. If
you want to do that, you may want to add the installation folders to your path variable:

```bash
cd modules/admin-ui
export PATH=node:node_modules/.bin:$PATH
```


Debugging Javascript unit tests
------------------------------------

Our Javascript unit tests are built in [Jasmine](http://jasmine.github.io/) (a behavior-driven development framework for
testing JavaScript code), and live in `modules/admin-ui/src/test/resources/test/unit`.

Occasionally something breaks, or you need to disable or focus on a single test.
While reading the Jasmine, Karma and Grunt docs are encouraged, here are a
few common recipes that might be useful:

### Disabling a unit test temporarily
Add `x` to the broken test. For example:

|Before|After|
|------|-----|
|`it('runs a test', function () {`|<code><strong style="color:#000">x</strong>it('runs a test', function () {</code>|

### Running a single unit test

Add `f` (for focus) to the relevant test.  For example:

|Before|After|
|------|-----|
|`it('runs a test', function () {`|<code><strong style="color:#000">f</strong>it('runs a test', function () {</code>|

### Triggering a browser debugging session

This triggers an instance of the selected browser(s) to open and begin running
the tests.  There will be a `Debug` button which will open another tab where the JavaScript has not been minified,
 use this second tab for debugging. Refreshing the debugging page will rerun the tests.

To run Karma

- <span style="display:inline-block; width:70px;">Chrome</span>
`npm run test-chrome`

- <span style="display:inline-block; width:70px;">Firefox</span>
`npm run test-firefox`

- <span style="display:inline-block; width:70px;">IE</span>
`npm run test-ie`

Additional browsers are supported, the full list can be found at [https://karma-runner.github.io/](https://karma-runner.github.io/1.0/config/browsers.html).

Live working with a running Opencast
------------------------------------

In order to speed up the UI development process, you can test the code without
building the module with Maven. There is a Grunt task for starting a standalone web server offering the UI from
the source and a separate task that will monitoring any change to the Sass, JavaScript and HTML files and reload the
page dynamically.

*Be warned that some functionality in this live setup can be limited.
Before reporting an issue, please test if you can reproduced the issue with a built Opencast.*

This setup may be configured as follows:

1. Follow the instructions in the Prerequisites section.

1. Start your Opencast instance.

1. Change to the Admin UI module directory.

        cd modules/admin-ui

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
The watch process monitors the `js`,`scss` and `sass` files for changes and should dynamically reload the page.

*Note: A refresh of the page might be required to start the live reload script*

Live working with a Mockup
--------------------------

If you do not want to keep a running Opencast instance for developing the
Admin UI NG, you can start a mockup.

*Be warned that __a lot__ of this mockup's functionality acts very differently from
an actual Opencast instance*

This setup may be configured as follows:

1. Follow the instructions in the Prerequisites section.

1. Change to the Admin UI module directory.

        cd modules/admin-ui

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

Update Node Dependencies
------------------------

Installing `npm-check-updates` and running it at the start of developing / improving a component can ensure that the
node modules stays up-to-date and dependency bugs are reduced.

*Note: Test the build (`mvn install`, `npm install`, `grunt`) thoroughly when upgrading modules as this might cause some
unexpected build failures (resetting the grunt version to "grunt": "^0.4.0" might resolve some of the initial issues).*

1. Installation.

        npm install -g npm-check-updates

1. Show any new dependencies for the project in the current directory.

        ncu

1. Upgrade a project's package file.

        ncu -u

A detailed reference of the command-line tool can be found at
[https://www.npmjs.com/package/npm-check-updates](https://www.npmjs.com/package/npm-check-updates).
