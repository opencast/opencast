#!/bin/bash
HWD=`cd "${0%/*}" 2>/dev/null; echo $PWD`
#set -x
CLEAN=0
DEBUG=0
WAIT=0
export FELIX_BATCH_ECHO=""

MH_VER=`cat "$HWD/mh_version" 2>/dev/null`
while [ $# -gt 0 ]; do
  case "$1" in
    -c ) CLEAN=1; shift ;;
    -d ) DEBUG=1; WAIT=0; shift ;;
    -w ) DEBUG=1; WAIT=1; shift ;;
    -b ) FELIX_BATCH_ECHO=on; shift ;;
    -? ) echo "Usage: ${0##*/} [-c] [-d|-w] [-b] [<MH_ver>]" 1>&2; exit 1 ;;
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

STORAGE_DIR=`"$HWD/get_conf_var.sh" "$FELIX_CONFIG_DIR/config.properties" org.opencastproject.storage.dir`
[ $? -ne 0 ] && exit 1
[ -z "$STORAGE_DIR" ] && STORAGE_DIR=/var/opencast

if [ $CLEAN -gt 0 ]; then
  echo "Cleaning $FELIX_CACHE_DIR"
  [ -d "$FELIX_CACHE_DIR" ] && \rm -fr "$FELIX_CACHE_DIR"/*
  echo "Cleaning $FELIX_INBOX_DIR"
  [ -d "$FELIX_INBOX_DIR" ] && \rm -fr "$FELIX_INBOX_DIR"/*
  echo "Cleaning $STORAGE_DIR"
  [ -d "$STORAGE_DIR" ] && \rm -fr "$STORAGE_DIR"/*
  # This should trigger fresh felix configuration copy
  echo "Cleaning felix configuration"
  [ -d "$FELIX_LOAD_DIR" ] && \rm -f "$FELIX_LOAD_DIR"/* 2>/dev/null
  [ -d "$FELIX_CONFIG_DIR" ] && \rm -f "$FELIX_CONFIG_DIR"/* 2>/dev/null
fi
[ ! -d "$FELIX_CACHE_DIR" ] && \mkdir -p "$FELIX_CACHE_DIR"
[ ! -d "$FELIX_INBOX_DIR" ] && \mkdir -p "$FELIX_INBOX_DIR"
if [ ! -d "$STORAGE_DIR" ]; then
  sudo mkdir -p "$STORAGE_DIR"
  [ $? -ne 0 ] && exit 1
  sudo chown `id -u`:`id -g` "$STORAGE_DIR"
  [ $? -ne 0 ] && exit 1
  chmod 1777 "$STORAGE_DIR"
  [ $? -ne 0 ] && exit 1
fi

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

exec "$HWD/start_mh.sh" "$MH_VER"
