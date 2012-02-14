#!/bin/bash
set -x
#
./replace_conf_host.sh localhost `hostname`
[ $? -ne 0 ] && exit 1
./replace_agent_host.sh some.capture.agent `hostname`
[ $? -ne 0 ] && exit 1
./replace_conf_port.sh 8080 8000
[ $? -ne 0 ] && exit 1
if [ ! -d /var/opencast ]; then
  sudo mkdir -p /var/opencast
  [ $? -ne 0 ] && exit 1
  sudo chown `id -u`:`id -g` /var/opencast
  [ $? -ne 0 ] && exit 1
  chmod 1777 /var/opencast
  [ $? -ne 0 ] && exit 1
fi
./replace_conf_var.sh org.opencastproject.storage.dir /var/opencast
[ $? -ne 0 ] && exit 1
#
exit 0
