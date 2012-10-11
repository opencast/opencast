#!/bin/bash
#set -x
#
# os
#  CentOS, RHEL, Ubuntu, Debian, openSUSE, SLES, Amazon, MacOS, <unknown>
#
case `uname` in
  Linux )
    # CentOS, RHEL, Ubuntu, Debian, openSUSE, Amazon
    awk '{
  if (length($0) == 0) next;
  sub("Welcome to",""); # Appears in openSUSE and SLES
  if (match($0, "Red Hat Enterprise Linux")) { print "RHEL"; }
  else if (match($0, "SUSE Linux Enterprise Server")) { print "SLES"; }
  else { print $1; }
  exit 0;
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
#
exit 0
