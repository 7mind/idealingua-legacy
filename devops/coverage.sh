#!/usr/bin/env bash

set -e
set -x

source ./devops/.env.sh
printenv

sbt -batch -no-colors -v clean coverage "$VERSION_COMMAND test" "$VERSION_COMMAND coverageReport"