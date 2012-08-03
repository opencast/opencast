#!/bin/bash
TMPFILE=`mktemp /tmp/${0##*/}.XXXXX`
trap 'rm -f $TMPFILE' 0
#set -x

FILE="$1"
if [ -z "$FILE" ]; then
  echo "Config file name is required" 1>&2
  exit 2
fi

OLD_PORT="$2"
[ -z "$OLD_PORT" ] && OLD_PORT=8080
NEW_PORT="$3"
[ -z "$NEW_PORT" ] && NEW_PORT=8080

sed \
-e 's/'$OLD_PORT'/'$NEW_PORT'/g' \
"$FILE" > $TMPFILE
[ $? -ne 0 ] && exit 1
cp $TMPFILE "$FILE"
[ $? -ne 0 ] && exit 1

exit 0
