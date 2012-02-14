#!/bin/bash
TMPFILE=`mktemp /tmp/${0##*/}.XXXXX`
trap 'rm -f $TMPFILE' 0
set -x
#
OLD_HOST=$1
[ -z "$OLD_HOST" ] && OLD_HOST=localhost
NEW_HOST=$2
[ -z "$NEW_HOST" ] && NEW_HOST=localhost
#
FILE=/opt/matterhorn/felix/load/org.opencastproject.capture.agent-demo.cfg
sed \
-e 's/'$OLD_HOST':/'$NEW_HOST':/g' \
-e 's/'$OLD_HOST'\//'$NEW_HOST'\//g' \
"$FILE" > $TMPFILE
[ $? -ne 0 ] && exit 1
cp $TMPFILE "$FILE"
[ $? -ne 0 ] && exit 1
#
exit 0
