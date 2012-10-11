#!/bin/bash
#set -x
#
JAVA_VERSION=`./java_version.sh`
[ -z "$JAVA_VERSION" ] && exit 1
# Ubuntu JDK installation location
JAVA_VERSION=${JAVA_VERSION//_/.}
echo /usr/lib/jvm/java-6-sun-${JAVA_VERSION}
exit 0
