#!/bin/bash
[ -z "$JAVA_HOME" ] && export JAVA_HOME=`./java_home.sh`
[ -z "$M2_HOME" ] && export M2_HOME=/usr/share/maven2
[ -z "$M2_REPO" ] && export M2_REPO=~/.m2/repository
[ -z "$TRUNK" ] && export TRUNK=/opt/matterhorn/trunk
[ -z "$FELIX_HOME" ] && export FELIX_HOME=/opt/matterhorn/felix

CLEAN=0
DEBUG=0
WAIT=0
export FELIX_BATCH_ECHO=""

while [ $# -gt 0 ]; do
  case "$1" in
    -c ) CLEAN=1; shift ;;
    -d ) DEBUG=1; WAIT=0; shift ;;
    -w ) DEBUG=1; WAIT=1; shift ;;
    -b ) FELIX_BATCH_ECHO=on; shift ;;
    * ) echo "Usage: ${0##*/} [-c] [-d|-w] [-b]"; exit 1 ;;
  esac
done

STORAGE=`awk '{
  line = $0;
  sub("^[ 	]*", "", line); sub("[ 	]*$", "", line);
  if (match(line, "^#") || length(line) == 0) {
    next;
  }
  if (match(line, "^org.opencastproject.storage.dir[ 	]*=")) {
    sub("^org.opencastproject.storage.dir[ 	]*=[ 	]*", "", line);
    gsub("\\\\${java.io.tmpdir}", "/tmp", line);
    print line
  }
}' $FELIX_HOME/conf/config.properties`
[ -z "$STORAGE" ] && STORAGE=/tmp/opencast

if [ $CLEAN -gt 0 ]; then
  echo "Cleaning $FELIX_HOME/felix-cache"
  [ -d "$FELIX_HOME/felix-cache" ] && \rm -fr "$FELIX_HOME/felix-cache"
  echo "Cleaning $STORAGE"
  [ -d "$STORAGE" ] && \rm -fr "$STORAGE"
  # This should trigger fresh felix configuration copy
  echo "Cleaning felix configuration"
  [ -d "$FELIX_HOME/load" ] && \rm -fr "$FELIX_HOME/load"
fi
[ ! -d "$FELIX_HOME/felix-cache" ] && mkdir -p "$FELIX_HOME/felix-cache"
[ ! -d "$STORAGE" ] && mkdir -p "$STORAGE"

export DEBUG_PORT=8000
if [ $WAIT -gt 0 ]; then
  DEBUG_SUSPEND=y
else
  DEBUG_SUSPEND=n
fi
export DEBUG_OPTS=""
if [ $DEBUG -gt 0 ]; then
  DEBUG_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=${DEBUG_PORT},server=y,suspend=${DEBUG_SUSPEND}"
fi

exec ./start_mh.sh
