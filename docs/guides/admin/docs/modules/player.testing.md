# How To Test With Phantom.js and Jasmine

## Integration Of Jasmine Into The Build Process (Maven)

[Jasmine](http://pivotal.github.com/jasmine/) is integrated with the [jasmine-maven-plugin](http://pivotal.github.com/jasmine/) into the maven build process. Therefore only the pom.xml file will be enhanced by the following code, which specifies the [jasmine-maven-plugin](http://pivotal.github.com/jasmine/) as plugin for the build process. The configuration of the jasmine-maven-plugin is also done in this file. The meaning of every configuration parameter can be looked up on the jasmine-maven-plugin project page under this [link](http://searls.github.io/jasmine-maven-plugin/bdd-mojo.html). The following configuration uses a the specRunnerTemplate  REQUIRE_JS in order to function properly with [RequireJS](http://requirejs.org/). Further information about spec runner templates can be found [here](http://searls.github.io/jasmine-maven-plugin/spec-runner-templates.html). On the next build the needed dependencies will be automatically resolved just like it is in the nature of maven.

**pom.xml**

    <build>
    <plugins>
        ...
          <plugin>
            <groupId>com.github.searls</groupId>
            <artifactId>jasmine-maven-plugin</artifactId>
            <version>1.3.1.2</version>
            <executions>
              <execution>
                <goals>
                  <goal>test</goal>
                </goals>
              </execution>
            </executions>
            <configuration>
              <preloadSources>
                <source>${project.basedir}/src/test/resources/js/lib/require.js</source>
              </preloadSources>
              <jsSrcDir>${project.basedir}/src/main/resources/static</jsSrcDir>
              <sourceIncludes>
                <include>**/*.js</include>
                <include>**/*.coffee</include>
              </sourceIncludes>
              <jsTestSrcDir>${project.basedir}/src/test/resources/js/spec</jsTestSrcDir>
              <specIncludes>
                <include>**/spec_helper.js</include>
                <include>**/*.js</include>
                <include>**/*.coffee</include>
              </specIncludes>
              <specRunnerTemplate>REQUIRE_JS</specRunnerTemplate>
              <format>progress</format>
            </configuration>
          </plugin>
      </plugins>
    </build>

## Testing The Engage Core
This chapter gives an overview over the directory structure used for testing the theodul engage core module, the configuration for the specs in the **spec_helper.js** and how to write specs for the core.

### Directory Structure
The test relevant files are located in the **src/test/resources/ui/js/spec** tree. Files that filename ends on **_spec.js** are considered as files with executable tests. The **spec_helper.js** in configured in the **pom.xml** for the initial setup.

**Directory Structure Testing Engage Core**

    |-src
    |---main
    |-----java          #Java impl of the plugin manager
    |-----resources
    |-------ui          #UI of the core, core.html and engage_init.js
    |---------css       #Global CSS Styles
    |---------js        #JavaScript logic
    |-----------engage  #Core logic, engage_core.js and engage_model.js
    |-----------lib     #External libraries, backbone.js, jquery.js, require.js and underscore.js
    |---test            #Unit Tests
    |-----resources
    |-------ui          #JavaScript Unit Tests
    |---------js
    |-----------spec    #Tests the *_spec.js and the helper file spec_helper.js

### Spec Helper
The file **spec_helper.js** takes over the configuration of RequireJS which is usually done by the engage_init.js. The paths differ slighty from the player has at runtime.

**spec_helper for engage_core module**

    /*global requirejs*/
    requirejs.config({
      baseUrl: 'src/js/lib',
      paths: {
        require: 'require',
        jquery: 'jquery',
        underscore: 'underscore',
        backbone: 'backbone',
        engage: '../engage',
        plugins: '../engage/plugin/*/static'
      },
      shim: {
        'backbone': {
          //script dependencies
          deps: ['underscore', 'jquery'],
          //global variable
          exports: 'Backbone'
        },
        'underscore': {
          //global variable
          exports: '_'
        }
      }
    });
    var PLUGIN_MANAGER_PATH = '/engage/theodul/manager/list.json';
    var PLUGIN_PATH = '/engage/theodul/plugin/';

###Writing Specs
TODO

## Testing Engage Plugins
This chapter gives an overview over the directory structure used for testing a theodul engage plugin module, the configuration for the specs in the spec_helper.js and how to write specs for a plugin.

### Directory Structure

The test relevant files are located in the **src/test/resources/ui/js/spec** tree. Files that filename ends on **_spec.js** are considered as files with executable tests. The **spec_helper.js** in configured in the **pom.xml** for the initial setup. In the directory **test/resources/ui/js/engage** is a mockup of the theodul engage core module in order to be able to test the plugin module independent. The directory **test/resources/ui/js/lib** provides the libraries which are provides by the engage core module at runtime of the player, as well to be able to test the plugin module independently.

**Directory Structure Testing Plugins**

    |-src
    |---main
    |-----java          #Java impl of the plugin manager
    |-----resources
    |-------ui          #UI of the core, core.html and engage_init.js
    |---------css       #Global CSS Styles
    |---------js        #JavaScript logic
    |-----------engage  #Core logic, engage_core.js and engage_model.js
    |-----------lib     #External libraries, backbone.js, jquery.js, require.js and underscore.js
    |---test            #Unit Tests
    |-----resources
    |-------ui          #JavaScript Unit Tests
    |---------js
    |-----------engage  #Mockup of the engage_core.js and engage_model.js
    |-----------lib     #Libraries that are used and provided by the core (A copy of the lib directory in the engage core module)
    |-----------spec    #Tests the *_spec.js and the helper file spec_helper.js

### Spec Helper
The file **spec_helper.js** takes over the configuration of RequireJS which is usually done by the engage_init.js. The paths differ slighty from the player uses at runtime.

    /*global requirejs*/
    requirejs.config({
      baseUrl: 'src/',
      paths: {
        require: 'test/resources/js/lib/require',
        jquery: 'test/resources/js/lib/jquery',
        underscore: 'test/resources/js/lib/underscore',
        backbone: 'test/resources/js/lib/backbone',
        engage: 'test/resources/js/engage'
      },
      shim: {
        'backbone': {
          //script dependencies
          deps: ['underscore', 'jquery'],
          //global variable
          exports: 'Backbone'
        },
        'underscore': {
          //global variable
          exports: '_'
        }
      }
    });

### Writing Specs
TODO

## Running The Tests

Now you can start the build process and the jasmine specs will be executed. Each . stands for a successful test. F stands for a failure and will stop the build process like it is specified in the configuration. The example output shows a manipulated version of the tests for the theodul engage core in order to illustrate a failing test. Normally all three tests should succeed at this point.

**Testing on build**

    mvn install -DdeployTo=${FELIX_HOME}
        // some output before
        [INFO]
        -------------------------------------------------------
         J A S M I N E   S P E C S
        -------------------------------------------------------
        [INFO]
        F..

        1 failure:

          1.) EngageCore it should have a model <<< FAILURE!

            * Expected { cid : 'c3', ... _pending : false } not to be defined.

        Results: 3 specs, 1 failures
        // some output before

The jasmine-maven-plugin can also be executed manually and show the result in a browser. This can be achieved by the following command:

**Manual testing**

    mvn jasmine:bdd
        [INFO] Scanning for projects...
        [INFO]
        [INFO] ------------------------------------------------------------------------
        [INFO] Building matterhorn-engage-theodul-core 1.5-SNAPSHOT
        [INFO] ------------------------------------------------------------------------
        [INFO]
        [INFO] --- jasmine-maven-plugin:1.3.1.2:bdd (default-cli) @ matterhorn-engage-theodul-core ---
        2014-01-28 14:33:30.722:INFO:oejs.Server:jetty-8.1.10.v20130312
        2014-01-28 14:33:30.746:INFO:oejs.AbstractConnector:Started SelectChannelConnector@0.0.0.0:8234
        [INFO]

        Server started--it's time to spec some JavaScript! You can run your specs as you develop by visiting this URL in a web browser:

        http://localhost:8234

        The server will monitor these two directories for scripts that you add, remove, and change:

        source directory: src/main/resources/ui

        spec directory: src/test/resources/ui/js/spec

        Just leave this process running as you test-drive your code, refreshing your browser window to re-run your specs.
        You can kill the server with Ctrl-C when you're done.

In a browser you should see an output like it is shown on the next screenshot.

![Jasmin screen](modules/player.testing1.png)
