#!/bin/bash

# Ensure that every Opencast bundle referenced in the Karaf feature descriptor is also declared as dependency of
# opencast-karaf-features and vice versa. Without these dependencies, Maven does not know that the assemblies need
# the bundles and a parallel build (-T) may package outdated bundles from the local Maven repository.

set -o pipefail

cd "$(dirname "$0")/.." || exit 1

FEATURES=assemblies/karaf-features/src/main/feature/feature.xml
POM=assemblies/karaf-features/pom.xml

features="$(grep -o 'mvn:org\.opencastproject/[^/]*/\${project\.version}' "$FEATURES" \
  | cut -d / -f 2 \
  | LC_ALL=C sort -u)" || exit 1

dependencies="$(sed -n '/^  <dependencies>/,/^  <\/dependencies>/p' "$POM" \
  | grep -o '<artifactId>[^<]*</artifactId>' \
  | sed 's/<[^>]*>//g' \
  | LC_ALL=C sort -u)" || exit 1

ret=0

missing="$(LC_ALL=C comm -23 <(echo "$features") <(echo "$dependencies"))"
if [ -n "$missing" ]; then
  echo "Bundles referenced in $FEATURES but missing as dependency in $POM:"
  echo "$missing" | sed 's/^/  /'
  ret=1
fi

unused="$(LC_ALL=C comm -13 <(echo "$features") <(echo "$dependencies"))"
if [ -n "$unused" ]; then
  echo "Dependencies in $POM which are not referenced in $FEATURES:"
  echo "$unused" | sed 's/^/  /'
  ret=1
fi

exit $ret
