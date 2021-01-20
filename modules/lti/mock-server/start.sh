#!/bin/sh

npx http-mock-server --config ./config.json "$@"
