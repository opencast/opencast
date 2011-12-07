#!/bin/bash
TMPFILE=`mktemp /tmp/${0##*/}.XXXXX`
trap 'rm -f $TMPFILE' 0
set -x
#
OLD_PORT=$1
[ -z "$OLD_PORT" ] && OLD_PORT=8080
NEW_PORT=$2
[ -z "$NEW_PORT" ] && NEW_PORT=8080
#
FILE=/opt/matterhorn/felix/conf/config.properties
sed \
-e 's/'$OLD_PORT'/'$NEW_PORT'/g' \
"$FILE" > $TMPFILE
[ $? -ne 0 ] && exit 1
cp $TMPFILE "$FILE"
[ $? -ne 0 ] && exit 1
exit 0
