#!/bin/bash
#set -x
#
# os
#  CentOS, RHEL, Ubuntu, Debian, openSUSE, Amazon, MacOS, <unknown>
#
case `uname` in
  Linux )
    # CentOS, RHEL, Ubuntu, Debian, openSUSE, Amazon
    awk '{
  sub("Welcome to",""); # Appears in openSUSE
  if (match($0, "Red Hat Enterprise Linux")) { print "RHEL"; }
  else { print $1; }
  exit 0
}' /etc/issue
    [ $? -ne 0 ] && exit 1
    ;;
  Darwin )
    echo MacOS
    ;;
  * )
    uname
    ;;
esac
exit 0
