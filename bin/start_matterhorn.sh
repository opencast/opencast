#!/bin/bash

##
# Configure these variables to match your environment
##

if [ -z "$FELIX_HOME" ]; then
  PWD=`pwd`
  if [ -f "$PWD/bin/felix.jar" ]; then
	  export FELIX_HOME="$PWD"
  else
	  echo "FELIX_HOME is not set"
	  exit 1
  fi
fi

if [ ! -z "$M2_REPO" ]; then
  M2_REPO="$M2_REPO"
else
    echo "Error: M2_REPO is not set."
    exit 2
fi

if [ ! -z "$OPENCAST_LOGDIR" ]; then
  LOGDIR="$OPENCAST_LOGDIR"
else
  LOGDIR="$FELIX_HOME/logs"
fi

##
# To enable the debugger on the vm, enable all of the following options
##

DEBUG_PORT="8000"
DEBUG_SUSPEND="n"
DEBUG_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$DEBUG_SUSPEND"

##
# Only change the line below if you want to customize the server
##

MAVEN_ARG="-DM2_REPO=$M2_REPO"

FELIX_CONFIG_DIR="$FELIX_HOME/etc"
FELIX_WORK_DIR="$FELIX_HOME/work"

FELIX="-Dfelix.home=$FELIX_HOME"
FELIX_WORK="-Dfelix.work=$FELIX_WORK_DIR"
FELIX_CONFIG_OPTS="-Dfelix.config.properties=file:${FELIX_CONFIG_DIR}/config.properties -Dfelix.system.properties=file:${FELIX_CONFIG_DIR}/system.properties"
FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$FELIX_CONFIG_DIR/load"

FELIX_OPTS="$FELIX $FELIX_WORK $FELIX_CONFIG_OPTS $FELIX_FILEINSTALL_OPTS"

# -1 for no limit
JETTY_OPTS="-Dorg.mortbay.jetty.Request.maxFormContentSize=1000000"

JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$FELIX_CONFIG_DIR"

PAX_LOGGING_OPTS="-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN"
MATTERHORN_LOGGING_OPTS="-Dopencast.logdir=$LOGDIR"
ECLIPSELINK_LOGGING_OPTS="-Declipselink.logging.level=CONFIG"
UTIL_LOGGING_OPTS="-Djava.util.logging.config.file=$FELIX_CONFIG_DIR/services/java.util.logging.properties"

LOG_OPTS="$PAX_LOGGING_OPTS $MATTERHORN_LOGGING_OPTS $ECLIPSELINK_LOGGING_OPTS $UTIL_LOGGING_OPTS"

GRAPHICS_OPTS="-Djava.awt.headless=true -Dawt.toolkit=sun.awt.HeadlessToolkit"
JAVA_OPTS="-Xms1024m -Xmx1024m -XX:MaxPermSize=256m"

#Added in response to MH-9831
LIBRARY_OPTS="-Dnet.sf.ehcache.skipUpdateCheck=true -Dorg.terracotta.quartz.skipUpdateCheck=true"

#!/bin/sh
# If this computer is OS X and $DYLD_FALLBACK_LIBRARY_PATH environment variable
# is not defined, then set it to /opt/local/lib. 
unameResult=`uname`
if [ $unameResult = 'Darwin' ];
then 
 	if [ "$DYLD_FALLBACK_LIBRARY_PATH" = "" ];
	then
		export DYLD_FALLBACK_LIBRARY_PATH="/opt/local/lib";
	fi
fi

FELIX_CACHE="$FELIX_WORK_DIR/felix-cache"

# Make sure matterhorn bundles are reloaded
if [ -d "$FELIX_CACHE" ]; then
  cd "$FELIX_CACHE"
  echo "Removing cached matterhorn bundles from $FELIX_CACHE"
  for bundle in `find . -type f -name "bundle.location" | xargs grep --files-with-match -e "file:" | sed -e 's/.\/\(.*\)\/bundle.location/\1/'`; do
    rm -r "$FELIX_CACHE/$bundle"
  done
fi

# Finally start felix
cd "$FELIX_HOME"
java $DEBUG_OPTS $FELIX_OPTS $GRAPHICS_OPTS $LIBRARY_OPTS $MAVEN_ARG $JAVA_OPTS $PAX_CONFMAN_OPTS $LOG_OPTS $JMX_OPTS $JETTY_OPTS -jar "$FELIX_HOME/bin/felix.jar" "$FELIX_CACHE"
