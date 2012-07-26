#!/bin/bash
#set -x
#
# os_ver
#  <M.N>
#
case `uname` in
  Linux )
    # CentOS, RHEL, Ubuntu, Debian, openSUSE, Amazon
    awk '{
  if (length($0) == 0) next;
  sub("^[^0-9.]*", ""); sub("[^0-9.]*$", "");
  print $0;
  exit 0
}' /etc/issue
    [ $? -ne 0 ] && exit 1
    ;;
  Darwin )
    sw_vers | awk '/^ProductVersion:/ {
  sub("^[^0-9.]*", ""); sub("[^0-9.]*$", "");
  print $0;
  exit 0
}'
    [ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1
    ;;
  * )
    exit 1
    ;;
esac
#
exit 0
