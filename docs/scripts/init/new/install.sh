#!/bin/sh
echo Installing SysV-init Scripts
install -p -D -m 0755 etc-init.d-matterhorn  /etc/init.d/matterhorn
install -p -D -m 0755 usr-sbin-matterhorn    /usr/sbin/matterhorn

echo Installing man-page
cat matterhorn.8 | gzip > matterhorn.8.gz
install -p -D -m 0644 matterhorn.8.gz  /usr/share/man/man8
rm matterhorn.8.gz
