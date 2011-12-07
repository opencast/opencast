#!/bin/bash
[ -z "$JAVA_HOME" ] && export JAVA_HOME=`./java_home.sh`
[ -z "$M2_HOME" ] && export M2_HOME=/usr/share/maven2
[ -z "$M2_REPO" ] && export M2_REPO=~/.m2/repository
[ -z "$TRUNK" ] && export TRUNK=/opt/matterhorn/trunk
[ -z "$FELIX_HOME" ] && export FELIX_HOME=/opt/matterhorn/felix

CLEAN=0
IGNERR=""
FLAGS=""

while [ $# -gt 0 ]; do
  case "$1" in
    -c ) CLEAN=1; shift ;;
    -i ) IGNERR="-Dmaven.test.failure.ignore=true -Dmaven.test.error.ignore=true"; shift ;;
    -e ) FLAGS="-e"; shift ;;
    * ) echo "Usage: ${0##*/} [-c] [-i] [-e]"; exit 1 ;;
  esac
done

if [ $CLEAN -gt 0 ]; then
  echo "Cleaning $FELIX_HOME/matterhorn"
  [ -d "$FELIX_HOME/matterhorn" ] && \rm -fr "$FELIX_HOME/matterhorn"
fi
[ ! -d "$FELIX_HOME/matterhorn" ] && mkdir -p "$FELIX_HOME/matterhorn"

cd "$TRUNK"
echo "Compiling complete project"

export MAVEN_OPTS='-Xms512m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m'
mvn $FLAGS clean install -DdeployTo="$FELIX_HOME/matterhorn" $IGNERR
