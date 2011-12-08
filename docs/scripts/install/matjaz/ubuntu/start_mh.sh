#!/bin/bash
[ "$FELIX_BATCH_ECHO" == "on" ] && set -x
if [ -z "$FELIX_HOME" ]; then
  echo "FELIX_HOME environment variable is not set!"
  exit 1
fi

# $FELIX_HOME/load is used just to check if configuration has aleady been copied
if ! `\ls "$FELIX_HOME/load"/* >/dev/null 2>&1`; then
  if [ -z "$TRUNK" ]; then
    echo TRUNK environment variable is not set!
    exit 1
  fi
  echo "Copying felix configuration to $FELIX_HOME"
  PRES="--preserve-permissions --same-owner"
  (cd "$TRUNK/docs/felix" >/dev/null 2>&1 && tar $PRES --exclude=.svn -cf - .) | \
  (cd "$FELIX_HOME" >/dev/null 2>&1 && umask 0 && tar $PRES -xf -)
  [ $? -ne 0 ] && exit 1
  echo "Customize felix configuration"
  ./customize_conf.sh
  [ $? -ne 0 ] && exit 1
fi

if ! `\ls "$FELIX_HOME/matterhorn"/*.jar >/dev/null 2>&1`; then
  echo "No Matterhorn jars found in $FELIX_HOME/matterhorn - please recompile!"
  exit 1
fi

[ -z "$M2_REPO" ] && M2_REPO=~/.m2/repository
[ -z "$OPENCAST_LOGDIR" ] && OPENCAST_LOGDIR="$FELIX_HOME/logs"

MAVEN_ARG="-DM2_REPO=$M2_REPO"
FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$FELIX_HOME/load"
PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$FELIX_HOME/conf"
PAX_LOGGING_OPTS="-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN"
ECLIPSELINK_LOGGING_OPTS="-Declipselink.logging.level=SEVERE"
MATTERHORN_LOGGING_OPTS="-Dopencast.logdir=$OPENCAST_LOGDIR"
UTIL_LOGGING_OPTS="-Djava.util.logging.config.file=$FELIX_HOME/conf/services/java.util.logging.properties"
GRAPHICS_OPTS="-Djava.awt.headless=true -Dawt.toolkit=sun.awt.HeadlessToolkit"
JAVA_OPTS="-Xms1024m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m"
FELIX_CACHE="$FELIX_HOME/felix-cache"

# Make sure matterhorn bundles are reloaded
if [ -d "$FELIX_CACHE" ]; then
  echo "Removing cached Matterhorn bundles from $FELIX_CACHE"
  for bundle in `find "$FELIX_CACHE" -type f -name bundle.location | xargs grep --files-with-match -e '^file:' | sed -e s!/bundle.location!!`; do
    \rm -fr $bundle
  done
fi

HOST_PORT=`awk '{
  line = $0;
  sub("^[ 	]*", "", line); sub("[ 	]*$", "", line);
  if (match(line, "^#") || length(line) == 0) {
    next;
  }
  if (match(line, "^org.opencastproject.server.url[ 	]*=")) {
    sub("^org.opencastproject.server.url[ 	]*=[ 	]*", "", line);
    print line
  }
}' $FELIX_HOME/conf/config.properties`

# Finally start felix
if [ -z "$DEBUG_OPTS" ]; then
  echo "Running Matterhorn on $HOST_PORT"
else
  HOST="${HOST_PORT%:*}"
  [ "$HOST" == "http" ] && HOST="$HOST_PORT"
  echo "Start remote debugger and connect to port $HOST:$DEBUG_PORT"
fi
cd "$FELIX_HOME"
java $DEBUG_OPTS $GRAPHICS_OPTS "$MAVEN_ARG" $JAVA_OPTS "$FELIX_FILEINSTALL_OPTS" "$PAX_CONFMAN_OPTS" $PAX_LOGGING_OPTS $ECLIPSELINK_LOGGING_OPTS "$MATTERHORN_LOGGING_OPTS" "$UTIL_LOGGING_OPTS" -jar "$FELIX_HOME/bin/felix.jar" "$FELIX_CACHE"
