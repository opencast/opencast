Development Process
===================

Building single modules
-----------------------

When working on a single opencast module, it can be extremely helpful to watch for a newly build version and include it automatically in the opencast OSGi infrastructure.
This can be done through the [bundle:watch](https://karaf.apache.org/manual/latest/commands/bundle-watch.html) command in karaf. The workflow would be as follows:

 - Start opencast and use `la -u` in the karaf console to list all installed bundles/modules. Note down the IDs of the bundles you want to watch.
 - Use `bundle:watch IDs` to watch the desired modules, e.g. `bundle:watch 190 199`
 - Make your changes and rebuild the module (e.g. execute `mvn clean install` in the module folder).
 - Watch how karaf automatically redeploys the changed jars from your local maven repository. You can verify that everything went smoothly by checking the log with `log:tail`.

The latest version of the bundle is available after a restart of opencast as well.