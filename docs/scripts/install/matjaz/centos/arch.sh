#!/bin/bash
#set -x
#
# arch
#  i386, x86_64, <unknown>
#
case `uname -m` in
  i386 | i486 | i586 | i686 )
    echo i386
    ;;
  x86_64 | x64 )
    echo x86_64
    ;;
  * )
    uname -m
    ;;
esac
exit 0
