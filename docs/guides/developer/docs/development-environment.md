Development Environment
======================

Building single modules
-----------------------

When working on a single opencast module, it can be extremely helpful to watch for a newly build version and include it automatically in the opencast OSGi infrastructure.
This can be done through the [bundle:watch](https://karaf.apache.org/manual/latest/commands/bundle-watch.html) command in karaf. The workflow would be as follows:

 - Start Opencast and use `la -u` in the Karaf console to list all installed bundles/modules. Note down the IDs of the bundles you want to watch.
 - Use `bundle:watch IDs` to watch the desired modules, e.g. `bundle:watch 190 199`
 - Make your changes and rebuild the module (e.g. execute `mvn clean install` in the module folder).
 - Watch how Karaf automatically redeploys the changed jars from your local Maven repository. You can verify that everything went smoothly by checking the log with `log:tail`.

The updated bundles are only available in the curently running Karaf instance. To create a Opencast version that has this changes permanently, you have to run `mvn clean install` in the the assemblies directory again. Your current instance will be deleted by the new assembly! 

In several cases the bundle:watch can bring Karaf in an unstable condition, as dependencies between bundles will not correctly be restored, after the new bundle has been deployed.


Attaching a Remote Debugger to Karaf
------------------------------------

To debug a running Opencast system, you can attach a remote debugger in your IDE (Eclipse or NetBeans, i.e.). For that you have to enable the remote debugging in Karaf OSGI server that runs Opencast.

To enable the connection of a remote debugger you have to export the debugging options in the Shell where you will start your Opencast server:

`export DEFAULT_JAVA_DEBUG_OPTS='-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005'`

You now can connect the remote debugger of your IDE on port `5005`.

For more information on remote debugging with Karaf you can visit [this site.](https://karaf.apache.org/manual/latest-2.2.x/developers-guide/debugging.html)

It is not recommended to enable remote debugging on production systems!

