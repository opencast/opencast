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

Live working on the UI
----------------------

To speed up the development process for the UI you can test the code without
building the module with maven.
There is a grunt task for starting a standalone web server offering the UI from
source.
Changes to the source will (after a page reload) directly reflect in browser.

To set this up do the following:

First off, you need [Grunt][1] and [NodeJS][2]. Either install it on your System or use
the local version from the Opencast module:
```bash
cd modules/matterhorn-admin-ui-ng
export PATH=$PATH:node:node_modules/grunt-cli/bin
```

To start the standalone webserver you need to checkout and build Opencast and
then run:
```bash
cd modules/matterhorn-admin-ui-ng
grunt proxy --proxy.host=http://localhost:8080 --proxy.username=opencast_system_account --proxy.password=CHANGE_ME
```

*Note that the host, username and password have to match your config
(etc/custom.properties)*

Grunt should print out the URL where you can see the standalone page running
from sources.
If you make changes in the sources of Admin UI NG they should be visible in
browser after a page reload.
You can still access the assembly version when connecting to
`http://localhost:8080`.

[1]: http://gruntjs.com
[2]: https://nodejs.org
