#!/bin/bash
#set -x
#
JAVA_VERSION=`./java_version.sh`
[ -z "$JAVA_VERSION" ] && exit 1
# CentOS JDK installation location
echo /usr/java/jdk${JAVA_VERSION}
exit 0
