#!/bin/bash
HWD=`cd "${0%/*}" 2>/dev/null; echo $PWD`
#set -x
#
MH_VER=`cat "$HWD/version" 2>/dev/null`
while [ $# -gt 0 ]; do
  case "$1" in
    -? ) echo "Usage: ${0##*/} <MH_ver>" 1>&2; exit 1 ;;
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

case $MH_VER in
  1.3.* )
    MAIN_CFG=/opt/matterhorn/felix/conf/config.properties
    AGENT_CFG=/opt/matterhorn/felix/load/org.opencastproject.capture.agent-demo.cfg
    ;;
  1.4.* )
    MAIN_CFG=/opt/matterhorn/trunk/etc/config.properties
    AGENT_CFG=""
    ;;
  trunk )
    MAIN_CFG=/opt/matterhorn/trunk/etc/config.properties
    AGENT_CFG=""
    ;;
esac
#
"$HWD/replace_conf_host.sh" "$MAIN_CFG" localhost `"$HWD/hostname.sh"`
[ $? -ne 0 ] && exit 1
#
"$HWD/replace_conf_port.sh" "$MAIN_CFG" 8080 8000
[ $? -ne 0 ] && exit 1
#
if [ -n "$AGENT_CFG" ]; then
  "$HWD/replace_agent_host.sh" "$AGENT_CFG" some.capture.agent `"$HWD/hostname.sh"`
  [ $? -ne 0 ] && exit 1
fi
#
if [ ! -d /var/opencast ]; then
  sudo mkdir -p /var/opencast
  [ $? -ne 0 ] && exit 1
  sudo chown `id -u`:`id -g` /var/opencast
  [ $? -ne 0 ] && exit 1
  chmod 1777 /var/opencast
  [ $? -ne 0 ] && exit 1
fi
"$HWD/replace_conf_var.sh" "$MAIN_CFG" org.opencastproject.storage.dir /var/opencast
[ $? -ne 0 ] && exit 1
#
exit 0
