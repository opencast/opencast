#!/bin/bash

set -ue

ret=0

echo Checking pom.xml files for tabs or trailing spaces…
if grep -rn $'\t' modules assemblies docs/checkstyle pom.xml --include=pom.xml; then
  echo "Tabs found!"
  ret=1
fi
if grep -rn ' $' modules assemblies docs/checkstyle pom.xml --include=pom.xml; then
  echo "Trailing spaces found!"
  ret=1
fi

echo Checking configuration files for tabs or trailing spaces…
if grep -rn $'\t' etc; then
  echo "Tabs found in config files!"
  ret=1
fi
if grep -rn ' $' etc; then
  echo "Trailing spaces found in config files!"
  ret=1
fi

echo Checking that all modules include a build number…
if ! grep -L '<Build-Number>${buildNumber}</Build-Number>' modules/*/pom.xml | wc -l | grep -q '^0$'; then
  echo "Build number is missing from a module!"
  ret=1
fi

echo Checking that modules use the maven-dependency-plugin…
grep -L maven-dependency-plugin modules/*/pom.xml > maven-dependency-plugin.list
if ! diff -q maven-dependency-plugin.list docs/checkstyle/maven-dependency-plugin.exceptions; then
  ret=1
else
  #Cleanup the file
  rm -f maven-dependency-plugin.list
fi

exit $ret
