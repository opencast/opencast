#!/bin/bash

# Test file used by buildbot

set -ue

cd "$(dirname $(realpath -s $0))"
./docs/checkstyle/check-config.sh
./docs/checkstyle/check-docs.sh
