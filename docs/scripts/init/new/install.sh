#!/bin/sh
echo Installing SysV-init Scripts
install -p -D -m 0755 etc-init.d-matterhorn  /etc/init.d/matterhorn
install -p -D -m 0755 usr-sbin-matterhorn    /usr/sbin/matterhorn
