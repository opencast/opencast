#!/bin/bash
HWD=`cd "${0%/*}" 2>/dev/null; echo $PWD`
#set -x
#
JAVA_VERSION=`"$HWD/java_version.sh"`
[ -z "$JAVA_VERSION" ] && exit 1
# CentOS JDK installation location
if [ -d "/usr/java/jdk$JAVA_VERSION" ]; then
  JAVA_HOME=`readlink -e "/usr/java/jdk$JAVA_VERSION" 2>/dev/null`
  if [ -x "$JAVA_HOME/bin/javac" ]; then
    echo "$JAVA_HOME"
    exit 0
  fi
elif [ -d /usr/lib/jvm/java-1.6.0-openjdk ]; then
  JAVA_HOME=`readlink -e /usr/lib/jvm/java-1.6.0-openjdk 2>/dev/null`
  if [ -x "$JAVA_HOME/bin/javac" ]; then
    echo "$JAVA_HOME"
    exit 0
  fi
elif [ -d /usr/lib/jvm/java-1.6.0-openjdk.x86_64 ]; then
  JAVA_HOME=`readlink -e /usr/lib/jvm/java-1.6.0-openjdk.x86_64 2>/dev/null`
  if [ -x "$JAVA_HOME/bin/javac" ]; then
    echo "$JAVA_HOME"
    exit 0
  fi
fi
#
exit 1
