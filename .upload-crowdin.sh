#!/bin/bash

set -eux
# Check for pull requests
if [ "${TRAVIS_PULL_REQUEST}" != false ]; then
  echo "No crowdin upload on pull requests"
  return 0
fi
# Check for non release branches or develop
if ! echo "${TRAVIS_BRANCH}" | grep -Eq '^(r/[0-9]+\.x|develop)$'; then
  echo "crowdin upload is only allowrd on release branches and develop, not on $TRAVIS_BRANCH"
  return 0
fi
# Crowdin branches do not use the `r/` prefix
CROWDIN_BRANCH="${TRAVIS_BRANCH//r\//}"
command -v crowdin >/dev/null 2>&1 || {
  wget --quiet https://artifacts.crowdin.com/repo/deb/crowdin.deb
  sudo dpkg -i crowdin.deb
}
echo "api_key: ${CROWDIN_API_KEY}" > ~/.crowdin.yaml
crowdin --config .crowdin.yaml upload sources -b "${CROWDIN_BRANCH}"
