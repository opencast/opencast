#!/bin/bash

echo "Install a classical SysV-Init script or a Systemd Unit file?"
echo -n '[i]nit/[s]ystemd (default: init): '
read INSTALLTYPE
if [[ $INSTALLTYPE == '' || ${INSTALLTYPE%nit} == 'i' ]]
then
  echo Installing SysV-init Scripts
  install -p -D -m 0755 etc-init.d-matterhorn  /etc/init.d/matterhorn
elif [[ ${INSTALLTYPE%ystemd} == 's' ]]
then
  echo Installing Systemd Unit file
  install -p -D -m 0644 etc-systemd-system-matterhorn.service \
    /etc/systemd/system/matterhorn.service
else
  echo Invalid selection
  exit
fi
install -p -D -m 0755 usr-sbin-matterhorn    /usr/sbin/matterhorn

echo Installing man-page
cat matterhorn.8 | gzip > matterhorn.8.gz
install -p -D -m 0644 matterhorn.8.gz  /usr/share/man/man8
rm matterhorn.8.gz

echo Installing service configuration
install -p -D -m 0644 opt-matterhorn-etc-service.conf /opt/matterhorn/etc
