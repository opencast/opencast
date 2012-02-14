#!/bin/bash
set -x
#
FILE=/opt/matterhorn/felix/conf/config.properties
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
