#!/bin/bash
TMPFILE=`mktemp /tmp/${0##*/}.XXXXX`
trap 'rm -f $TMPFILE' 0
#set -x
#
FILE="$1"
if [ -z "$FILE" ]; then
  echo "Config file name is required" 1>&2
  exit 2
fi
VAR_NAME="$2"
if [ -z "$VAR_NAME" ]; then
  echo "Variable name is required" 1>&2
  exit 2
fi
VAR_VALUE="$3"
if [ -z "$VAR_VALUE" ]; then
  echo "Variable value is required" 1>&2
  exit 2
fi
#
awk -v var="$VAR_NAME" -v value="$VAR_VALUE" '{
  line = $0;
  sub("^[ 	]*", "", line); sub("[ 	]*$", "", line);
  if (match(line, "^#") || length(line) == 0) {
    print $0;
    next;
  }
  if (match(line, "^" var "[ 	]*=")) {
    print var "=" value
  }
  else print $0;
}' "$FILE" > $TMPFILE
[ $? -ne 0 ] && exit 1
cp $TMPFILE "$FILE"
[ $? -ne 0 ] && exit 1
#
exit 0
