#!/bin/bash

set -uex

ret=0

echo Checking pom.xml files for tabs or trailing spaces…
if grep -rn $'\t' modules assemblies pom.xml --include=pom.xml; then
  echo "Tabs found!"
  ret=1
fi
if grep -rn ' $' modules assemblies pom.xml --include=pom.xml; then
  echo "Trailing spaces found!"
  ret=1
fi

echo Checking configuration files for tabs or trailing spaces…
if grep -rnI $'\t' etc; then
  echo "Tabs found in config files!"
  ret=1
fi
if grep -rnI ' $' etc; then
  echo "Trailing spaces found in config files!"
  ret=1
fi

echo Checking that all modules include a build number…
if ! grep -L '<Build-Number>${buildNumber}</Build-Number>' modules/*/pom.xml | wc -l | grep -q '^0$'; then
  echo "Build number is missing from a module!"
  ret=1
fi

echo Checking that modules use the maven-dependency-plugin…
grep -L maven-dependency-plugin modules/*/pom.xml | cat > maven-dependency-plugin.list
if ! diff -q maven-dependency-plugin.list docs/checkstyle/maven-dependency-plugin.exceptions; then
  echo ERROR: Detected modules without active dependency plugin:
  cat maven-dependency-plugin.list
  ret=1
fi

echo "Checking that all plugins are listed for inclusion in the assembly pom"
for plugin in $(sed -n 's/^.*<feature.*"\(opencast-plugin-[^"]*\)".*$/\1/p' assemblies/karaf-features/src/main/feature/feature.xml); do
    if ! grep -q "$plugin" assemblies/pom.xml; then
      echo "ERROR: Plugin $plugin not listed in assemblies/karaf-features/src/main/feature/feature.xml"
      ret=1
    fi
done

exit $ret
