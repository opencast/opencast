#!/bin/sh

set -eu

mkdir -p src/i18n
cp ../admin-ui/src/main/resources/public/org/opencastproject/adminui/languages/* src/i18n
