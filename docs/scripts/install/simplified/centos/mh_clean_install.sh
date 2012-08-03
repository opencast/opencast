#!/bin/bash
HWD=`cd "${0%/*}" 2>/dev/null; echo $PWD`
#set -x
CLEAN=0
IGNERR=""
FLAGS=""

MH_VER=`cat "$HWD/mh_version" 2>/dev/null`
while [ $# -gt 0 ]; do
  case "$1" in
    -c ) CLEAN=1; shift ;;
    -i ) IGNERR="-Dmaven.test.failure.ignore=true -Dmaven.test.error.ignore=true"; shift ;;
    -e ) FLAGS="-e"; shift ;;
    -? ) echo "Usage: ${0##*/} [-c] [-i] [-e] [<MH_ver>]" 1>&2; exit 1 ;;
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

[ -z "$JAVA_HOME" ] && export JAVA_HOME=`"$HWD/java_home.sh"`
[ -z "$M2_HOME" ] && export M2_HOME=/usr/share/maven2
[ -z "$M2_REPO" ] && export M2_REPO=~/.m2/repository
[ -z "$TRUNK" ] && export TRUNK=/opt/matterhorn/trunk
. "$HWD/felix_dirs.sh" "$MH_VER"
[ $? -ne 0 ] && exit 1

if [ $CLEAN -gt 0 ]; then
  echo "Cleaning $FELIX_JARS_DIR"
  [ -d "$FELIX_JARS_DIR" ] && \rm -fr "$FELIX_JARS_DIR"
fi
[ ! -d "$FELIX_JARS_DIR" ] && \mkdir -p "$FELIX_JARS_DIR"

cd "$TRUNK"
echo "Compiling complete project"

export MAVEN_OPTS="-Xms512m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m"
mvn $FLAGS clean install -DdeployTo="$FELIX_DEPLOY_DIR" $IGNERR
[ $? -ne 0 ] && exit 1

exit 0
