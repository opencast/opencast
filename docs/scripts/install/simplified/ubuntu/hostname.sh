#!/bin/bash
TMPFILE=`mktemp /tmp/${0##*/}.XXXXX`
trap 'rm -f $TMPFILE' 0
#set -x

IPADDR=""
for eth in eth0 eth1 eth2 eth3; do
  IPADDR=`ifconfig $eth 2>/dev/null | grep "inet " | cut -d' ' -f12 | cut -d':' -f2`
  [ -n "$IPADDR" ] && break
done
[ -z "$IPADDR" ] && exit 1

nslookup "$IPADDR" > $TMPFILE 2>/dev/null
HOSTNAME=`awk '{
  if (match($0, "\\\.in-addr\\\.arpa[ 	]+name[ ]*=[ ]*")) {
    sub(".*\\\.in-addr\\\.arpa[ 	]+name[ ]*=[ ]*", "");
    sub("\\\.$", "");
    print $0;
    exit 0;
  }
}' $TMPFILE 2>/dev/null`
[ -z "$HOSTNAME" ] && HOSTNAME="$IPADDR"
echo "$HOSTNAME"

exit 0
