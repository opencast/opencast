Development Environment
=======================

Developer Builds
----------------

Besides the default `dist` Maven profile, the assemblies project defines a second `dev` profile which will cause only
one allinone distribution to be created. It is already unpacked and ready to be started. Activate the profile using:

    mvn clean install -Pdev

The administrative user interface needs nodejs to build and phantomjs for testing purposes. These will be downloaded as
prebuilt binaries during the maven build process. If there are no prebuilt binaries for your operating system, you can
build the tools manually and then build opencast using the `frontend-no-prebuilt` maven profile:

    mvn clean install -Pdev,frontend-no-prebuilt

Logging During Builds
---------------------

While building Opencast, the default log level for Opencast modules is `WARN`. To increase logging for development,
edit the log level configuration in `docs/log4j/log4j.properties`.

Building single modules
-----------------------

When working on a single Opencast module, it can be extremely helpful to watch the newly built version and include
it automatically in the Opencast OSGi infrastructure. This can be done through the
[bundle:watch](https://karaf.apache.org/manual/latest/commands/bundle-watch.html) command in Karaf. The workflow would
be as follows:

* Start Opencast and use `la -u` in the Karaf console to list all installed bundles/modules. Note down the IDs of the
  bundles you want to watch.
* Use `bundle:watch IDs` to watch the desired modules, e.g. `bundle:watch 190 199`
* Make your changes and rebuild the module (e.g. execute `mvn clean install` in the module folder).
* Watch how Karaf automatically redeploys the changed jars from your local Maven repository. You can verify that
  everything went smoothly by checking the log with `log:tail`.

The updated bundles are only available in the currently running Karaf instance. To create a Opencast version that has
this changes permanently, you have to run `mvn clean install` in the the assemblies directory again. Your current
instance will be deleted by the new assembly!

In several cases the bundle:watch can bring Karaf in an unstable condition, as dependencies between bundles will not
correctly be restored, after the new bundle has been deployed.


Attaching a Remote Debugger to Karaf
------------------------------------

To debug a running Opencast system, you can attach a remote debugger in your IDE (Eclipse or NetBeans, i.e.). For that
you have to enable the remote debugging in Karaf OSGI server that runs Opencast.

You have to add "debug" as an additional paramenter to the Opencast start script:

    bin/start-opencast debug

If you want to enable debug permanently you can export the variable in the shell:

    export DEFAULT_JAVA_DEBUG_OPTS='-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005'

You can connect the remote debugger of your IDE on port `5005`.

### Enabling debugger for package installations

Albeit you can use the afforemented method for package installations, you can't start debug mode via system services.
The recommended way is to enable the debug mode in the `setenv` file, normally found in:

    /usr/share/opencast/bin/

And add this line:

    export KARAF_DEBUG=true

In case you need to change the port for debbuging, you can adding this another line:

    export JAVA_DEBUG_PORT={{PORT}}

Where `{{PORT}}` is the desired port.


***
For more information on remote debugging with Karaf you can visit [this
site.](https://karaf.apache.org/manual/latest/#_debugging)

It is **not recommended** to enable remote debugging on production systems!

