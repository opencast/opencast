#!/bin/bash
#set -x

java -version 2>&1 | awk '{
  if (match($0, "java version")) {
    sub("[^\"]*\"", ""); sub("\".*", "");
    print $0;
    exit 0;
  }
}'
[ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1

exit 0
