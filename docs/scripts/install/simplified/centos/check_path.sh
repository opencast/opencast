#!/bin/bash
#set -x

echo $PATH | awk -v SP="$1" 'BEGIN {
  gsub("/+", "/", SP);
  if (SP != "/") sub("/$", "", SP);
  if (SP == ".") SP = "";
} {
  n = split($0, a, ":");
  for (ii = 1; ii <= n; ii++) {
    gsub("/+", "/", a[ii]);
    if (a[ii] != "/") sub("/$", "", a[ii]);
    if (a[ii] == ".") a[ii] = "";
    curr = a[ii];
    f[curr] = 1;
  }
} END {
  for (path in f) {
    if (path == SP) exit(0);
  }
  exit(1);
}'
[ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1

exit 0
