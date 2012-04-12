---------------------------
 Installation instructions:
---------------------------

For the impatient, experienced, and brave, here's the short version:

1) Set FELIX_HOME by issuing "export FELIX_HOME=<felix home directory>"

2) Install the 3rd party tools by executing <felix home directory>/docs/scripts/3rd_party/menu3p as root

3) Run "mvn install -DdeployTo=<felix home directory>".  You may need to increase the memory available to maven
    using environment variables.  For example:
      $ export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m"

4) Start felix using <felix home>/bin/start_matterhorn.sh and navigate to http://localhost:8080

Logging configuration can be customized by modifying <felix home>/conf/services/org.ops4j.pax.logging.properties.

Detailed instructions for a variety of platforms and topologies are available at http://opencast.jira.com/wiki/display/MH/Installation+and+Configuration+%28Trunk%29
Questions should go to the Matterhorn list (matterhorn-users@opencastproject.org, see http://lists.opencastproject.org/mailman/listinfo/matterhorn-users for archives and registration) or the #opencast IRC channel at irc.freenode.net
