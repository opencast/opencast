---------------------------
 Installation instructions:
---------------------------

For the impatient, experienced, and brave, here's the short version:

1) Install felix version 3.0.1 or later
2) Copy the directories under docs/felix/ to your felix installation
3) Install the 3rd party tools by executing $FELIX/docs/scripts/3rd_party/menu3p as root
4) Run "mvn install -DdeployTo=$FELIX/matterhorn".  You may need to increate the memory available to maven
    using environment variables.  For example:
      $ export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m"
5) Start felix using $FELIX/bin/start_matterhorn.sh and navigate to http://localhost:8080

Logging configuration can be customized by modifying $FELIX/conf/services/org.ops4j.pax.logging.properties.

Detailed instructions for a variety of platforms and topologies are available at http://opencast.jira.com/wiki/display/MH/Installation+and+Configuration+%28Trunk%29
Questions should go to the Matterhorn list (matterhorn-users@opencastproject.org, see http://lists.opencastproject.org/mailman/listinfo/matterhorn-users for archives and registration) or the #opencast IRC channel at irc.freenode.net
