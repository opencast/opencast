#!/bin/bash

set -ue

ret=0

cd "$(dirname $(realpath -s $0))"

if grep -rn $'\t' modules assemblies pom.xml --include=pom.xml; then
  echo "Tabs found!"
  ret=1
fi
if grep -rn ' $' modules assemblies pom.xml --include=pom.xml; then
  echo "Trailing spaces found!"
  ret=1
fi
if grep -rn $'\t' etc; then
  echo "Tabs found in config files!"
  ret=1
fi
if grep -rn ' $' etc; then
  echo "Trailing spaces found in config files!"
  ret=1
fi


# build number must be present in all modules
if ! grep -L '<Build-Number>${buildNumber}</Build-Number>' modules/*/pom.xml | wc -l | grep -q '^0$'; then
  echo "Build number is missing from a module!"
  ret=1
fi

# maven-dependency-plugin should be active for all new modules
#grep -L maven-dependency-plugin modules/*/pom.xml > maven-dependency-plugin.list
#if ! diff -q maven-dependency-plugin.list docs/checkstyle/maven-dependency-plugin.exceptions; then
#  ret=1
#fi

cd docs/guides

for docs in admin developer user; do
  cd $docs
  echo "Markdown doc $docs build log:"
  mkdocs build 2>&1 | tee mkdocs.log
  if grep \
       -e 'WARNING.*Documentation file' \
       -e 'pages exist in the docs directory' \
       -e 'is not found in the documentation files' \
        mkdocs.log;
    then
      echo "$docs did not build correctly!"
      ret=1
  fi
  cd ..
done

if ! npm test; then
  echo "npm test failed!"
  ret=1
fi

exit $ret
