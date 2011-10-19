#!/bin/bash

##
# Configure these variables to match your environment
##

if [ ! -z "$FELIX_HOME" ]; then
  FELIX_HOME="$FELIX_HOME"
else
  FELIX_HOME="/Applications/Matterhorn"
fi

if [ ! -z "$M2_REPO" ]; then
  M2_REPO="$M2_REPO"
else
  M2_REPO="/Users/johndoe/.m2/repository"
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
FELIX_OPTS="-Dfelix.home=$FELIX_HOME"
FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$FELIX_HOME/load"
PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$FELIX_HOME/conf"
MATTERHORN_LOGGING_OPTS="-Dopencast.logdir=$LOGDIR"
PAX_LOGGING_OPTS="-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN"
ECLIPSELINK_LOGGING_OPTS="-Declipselink.logging.level=SEVERE"
UTIL_LOGGING_OPTS="-Djava.util.logging.config.file=$FELIX_HOME/conf/services/java.util.logging.properties"
GRAPHICS_OPTS="-Djava.awt.headless=true -Dawt.toolkit=sun.awt.HeadlessToolkit"
TEMP_OPTS="-Djava.io.tmpdir=$FELIX_HOME/work"
JAVA_OPTS="-Xms1024m -Xmx1024m -XX:MaxPermSize=256m"

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

FELIX_CACHE="$FELIX_HOME/felix-cache"

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
java $DEBUG_OPTS $GRAPHICS_OPTS "$FELIX_OPTS" "$TEMP_OPTS" $MAVEN_ARG $JAVA_OPTS "$FELIX_FILEINSTALL_OPTS" "$PAX_CONFMAN_OPTS" $PAX_LOGGING_OPTS $ECLIPSELINK_LOGGING_OPTS "$MATTERHORN_LOGGING_OPTS" "$UTIL_LOGGING_OPTS" -jar "$FELIX_HOME/bin/felix.jar" "$FELIX_CACHE"
