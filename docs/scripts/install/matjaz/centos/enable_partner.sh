#!/bin/bash
TMPFILE=`mktemp /tmp/${0##*/}.XXXXX`
trap 'rm -f $TMPFILE' 0
set -x
#
FILE=/etc/apt/sources.list
sed \
-e 's/^[ 	]*#[ 	]*\(.*lucid partner\)/\1/' \
-e 's/^[ 	]*#[ 	]*\(.*maverick partner\)/\1/' \
-e 's/^[ 	]*#[ 	]*\(.*natty partner\)/\1/' \
-e 's/^[ 	]*#[ 	]*\(.*oneiric partner\)/\1/' \
"$FILE" > $TMPFILE
[ $? -ne 0 ] && exit 1
sudo cp $TMPFILE "$FILE"
[ $? -ne 0 ] && exit 1
exit 0
