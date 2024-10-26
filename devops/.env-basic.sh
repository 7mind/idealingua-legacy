#!/usr/bin/env bash

set -e
set -x

export IDEALINGUA_VERSION=$(cat version.sbt | sed -r 's/.*\"(.*)\".**/\1/' | sed -E "s/SNAPSHOT/build."${CI_BUILD_UNIQ_SUFFIX}"/")
