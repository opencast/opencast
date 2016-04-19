Prerequisites
-------------

You need [Grunt][1] and [NodeJS][2].
Either install it on your System or use the local version from the Opencast module:

```bash
cd modules/matterhorn-admin-ui-ng
export PATH=$PATH:node:node_modules/grunt-cli/bin
```

*Note: We already had reports of Grunt behaving differently on different systems.
Watch out for local-/system-installations of Grunt and or NodeJS as they can
change the build behavior of Admin UI NG.*

Modifying Sources
-----------------

When you make changes to the sources it should be sufficient to rebuild the
Admin UI NG module and copy the packaged module file into the Opencast assembly.

Example:
```bash
cd modules/matterhorn-admin-ui-ng
mvn install
cp ./target/matterhorn-admin-ui-ng-2.2-SNAPSHOT.jar ../../build/opencast-dist-allinone-2.2-SNAPSHOT/system/org/opencastproject/matterhorn-admin-ui-ng/2.2-SNAPSHOT/matterhorn-admin-ui-ng-2.2-SNAPSHOT.jar
```

*Note that in the example above, the paths are for a specific Opencast version.
Your paths might look different.*

Live working with running Opencast
----------------------------------

To speed up the development process for the UI you can test the code without
building the module with Maven.
There is a Grunt task for starting a standalone web server offering the UI from
source.
Changes to source will (after a page reload) directly reflect in browser.

*Be warned that some functionality in this live setup can be limited.
Before creating a issue please test with a build Opencast.*

To set this up do the following:

Follow the instruction for Prerequisites.

To start the standalone webserver you need to checkout and build Opencast and
run:
```bash
cd modules/matterhorn-admin-ui-ng
grunt proxy --proxy.host=http://localhost:8080 --proxy.username=opencast_system_account --proxy.password=CHANGE_ME
```

*Note: that the host, username and password have to match your config
(etc/custom.properties)*

Grunt should print out the URL where you can see the standalone page running
from sources.
If you make changes in the sources of Admin UI NG they should be visible in
browser after a page reload.
You can still access the assembly version when connecting to
`http://localhost:8080`.

[1]: http://gruntjs.com
[2]: https://nodejs.org

Live working with a Mockup
--------------------------

If you do not want to keep a running Opencast instance for developing the
Admin UI NG you can use the mockup.

*Be warned that a lot of functionality in this mockup acts very different from
a actual Opencast instance*

To set this up do the following:

Follow the instruction for Prerequisites.

To start the mockup webserver you need to checkout and build Opencast and
then run:
```bash
cd modules/matterhorn-admin-ui-ng
grunt serve
```

Grunt should print out the URL where you can see the standalone page running
from sources.
If you make changes in the sources of Admin UI NG they should be visible in
browser after a page reload.
