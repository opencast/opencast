#!/bin/bash
TMPFILE=`mktemp /tmp/${0##*/}.XXXXX`
trap 'rm -f $TMPFILE' 0
#set -x

FILE="$1"
if [ -z "$FILE" ]; then
  echo "Config file name is required" 1>&2
  exit 2
fi

OLD_HOST="$2"
[ -z "$OLD_HOST" ] && OLD_HOST=localhost
NEW_HOST="$3"
[ -z "$NEW_HOST" ] && NEW_HOST=localhost

sed \
-e 's/'$OLD_HOST':/'$NEW_HOST':/g' \
-e 's/'$OLD_HOST'\//'$NEW_HOST'\//g' \
"$FILE" > $TMPFILE
[ $? -ne 0 ] && exit 1
cp $TMPFILE "$FILE"
[ $? -ne 0 ] && exit 1

exit 0
