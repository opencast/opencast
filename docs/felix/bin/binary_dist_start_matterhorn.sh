#!/bin/sh
# A simplified startup script for the binary matterhorn distribution
if [ ! -z $FELIX_HOME ]; then
  FELIX=$FELIX_HOME
else
  FELIX=.
fi

if [ ! -z $OPENCAST_LOGDIR ]; then
  LOGDIR=$OPENCAST_LOGDIR
else
  LOGDIR=$FELIX/logs
fi

##
# Only change the line below if you want to customize the server
##

FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$FELIX/load"
PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$FELIX/conf"
PAX_LOGGING_OPTS="-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN -Dopencast.logdir=$LOGDIR"
UTIL_LOGGING_OPTS="-Djava.util.logging.config.file=$FELIX/conf/services/java.util.logging.properties"
GRAPHICS_OPTS="-Djava.awt.headless=true -Dawt.toolkit=sun.awt.HeadlessToolkit"
TEMP_OPTS="-Djava.io.tmpdir=$FELIX_HOME/work"
MEM="-Xms1024m -Xmx1024m -XX:MaxPermSize=256m"

# Make sure matterhorn bundles are reloaded
FELIX_CACHE="$FELIX/felix-cache"
rm -rf $FELIX_CACHE

# Finally start felix
java $MEM $GRAPHICS_OPTS $TEMP_OPTS $FELIX_FILEINSTALL_OPTS $PAX_CONFMAN_OPTS $PAX_LOGGING_OPTS $UTIL_LOGGING_OPTS $CXF_OPTS -jar $FELIX/bin/felix.jar $FELIX_CACHE
