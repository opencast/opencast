#!/bin/bash
set -x
#
FILE="$1"
if [ -z "$FILE" ]; then
  echo "Config file name is required" 1>&2
  exit 2
fi
#
awk '{
  line = $0;
  sub("^[ 	]*", "", line); sub("[ 	]*$", "", line);
  if (match(line, "^#") || length(line) == 0) {
    next;
  }
  if (match(line, "^" var "[ 	]*=")) {
    print var "=" value
  }
  else print $0;
}' "$FILE"
[ $? -ne 0 ] && exit 1
#
exit 0
