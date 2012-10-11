#!/bin/bash
HWD=`cd "${0%/*}" 2>/dev/null; echo $PWD`
[ "$FELIX_BATCH_ECHO" == "on" ] && set -x

MH_VER=`cat "$HWD/mh_version" 2>/dev/null`
while [ $# -gt 0 ]; do
  case "$1" in
    -? ) echo "Usage: ${0##*/} [<MH_ver>]" 1>&2; exit 1 ;;
    * ) export MH_VER="$1"; shift ;;
  esac
done
[ -z "$MH_VER" ] && MH_VER=trunk
case "$MH_VER" in
  1.3.0 | 1.3.1 ) ;;
  1.3.x | 1.4.x ) ;;
  trunk ) ;;
  * ) echo "Unsupported version $MH_VER" 1>&2; exit 1 ;;
esac

if [ -z "$FELIX_HOME" -o -z "$FELIX_LOAD_DIR" -o -z "$FELIX_WORK_DIR" -o \
     -z "$FELIX_CONFIG_DIR" -o -z "$FELIX_JARS_DIR" -o -z "$FELIX_CACHE_DIR" ]; then
  echo "FELIX environment variables are not set!" 1>&2; exit 1
fi

# Empty $FELIX_LOAD_DIR triggers new configuration
if ! `\ls "$FELIX_LOAD_DIR"/* >/dev/null 2>&1`; then
  case "$MH_VER" in
    1.3.* )
      if [ -z "$TRUNK" ]; then
        echo "TRUNK environment variable is not set!" 1>&2; exit 1
      fi
      echo "Copying new felix configuration to $FELIX_HOME"
      PRES="--preserve-permissions --same-owner"
      (cd "$TRUNK/docs/felix" >/dev/null 2>&1 && tar $PRES --exclude=.svn -cf - .) | \
      (cd "$FELIX_HOME" >/dev/null 2>&1 && umask 0 && tar $PRES -xf -)
      [ $? -ne 0 ] && exit 1
      ;;
    1.4.* )
      echo "Copying new felix configuration to $FELIX_HOME"
      cd "$FELIX_LOAD_DIR"
      [ $? -ne 0 ] && exit 1
      svn update
      [ $? -ne 0 ] && exit 1
      cd "$FELIX_CONFIG_DIR"
      [ $? -ne 0 ] && exit 1
      svn update
      [ $? -ne 0 ] && exit 1
      ;;
    trunk )
      echo "Copying new felix configuration to $FELIX_HOME"
      cd "$FELIX_LOAD_DIR"
      [ $? -ne 0 ] && exit 1
      svn update
      [ $? -ne 0 ] && exit 1
      cd "$FELIX_CONFIG_DIR"
      [ $? -ne 0 ] && exit 1
      svn update
      [ $? -ne 0 ] && exit 1
      ;;
  esac
  cp "$FELIX_CONFIG_DIR/config.properties" "$FELIX_CONFIG_DIR/config.properties.org"
  [ $? -ne 0 ] && exit 1
  echo "Customize felix configuration"
  "$HWD/customize_conf.sh" "$MH_VER"
  [ $? -ne 0 ] && exit 1
fi

if ! `\ls "$FELIX_JARS_DIR"/*.jar >/dev/null 2>&1`; then
  echo "No Matterhorn jars found in $FELIX_JARS_DIR - please recompile!" 1>&2; exit 1
fi

[ -z "$M2_REPO" ] && M2_REPO=~/.m2/repository
[ -z "$OPENCAST_LOGDIR" ] && OPENCAST_LOGDIR="$FELIX_HOME/logs"

MAVEN_ARG="-DM2_REPO=$M2_REPO"

FELIX="-Dfelix.home=$FELIX_HOME"
FELIX_WORK="-Dfelix.work=$FELIX_WORK_DIR"
FELIX_CONFIG_OPTS="-Dfelix.config.properties=file:$FELIX_CONFIG_DIR/config.properties -Dfelix.system.properties=file:$FELIX_CONFIG_DIR/system.properties"
FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$FELIX_LOAD_DIR"

FELIX_OPTS="$FELIX $FELIX_WORK $FELIX_CONFIG_OPTS $FELIX_FILEINSTALL_OPTS"

PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$FELIX_CONFIG_DIR"

PAX_LOGGING_OPTS="-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN"
MATTERHORN_LOGGING_OPTS="-Dopencast.logdir=$OPENCAST_LOGDIR"
UTIL_LOGGING_OPTS="-Djava.util.logging.config.file=$FELIX_CONFIG_DIR/services/java.util.logging.properties"

LOG_OPTS="$PAX_LOGGING_OPTS $MATTERHORN_LOGGING_OPTS $ECLIPSELINK_LOGGING_OPTS $UTIL_LOGGING_OPTS"

GRAPHICS_OPTS="-Djava.awt.headless=true -Dawt.toolkit=sun.awt.HeadlessToolkit"

TEMP_OPTS="-Djava.io.tmpdir=/tmp"

JAVA_OPTS="-Xms1024m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m"

FELIX_CACHE="$FELIX_CACHE_DIR"

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
    print line;
    exit 0;
  }
}' "$FELIX_CONFIG_DIR/config.properties"`

# Finally start felix
if [ -z "$DEBUG_OPTS" ]; then
  echo "Running Matterhorn on $HOST_PORT"
else
  HOST="${HOST_PORT%:*}"
  [ "$HOST" == "http" ] && HOST="$HOST_PORT"
  echo "Start remote debugger and connect to port $HOST:$DEBUG_PORT"
fi

cd "$FELIX_HOME"
java $DEBUG_OPTS $FELIX_OPTS $GRAPHICS_OPTS "$MAVEN_ARG" $JAVA_OPTS "$PAX_CONFMAN_OPTS" $LOG_OPTS -jar "$FELIX_HOME/bin/felix.jar" "$FELIX_CACHE"
[ $? -ne 0 ] && exit 1

exit 0
