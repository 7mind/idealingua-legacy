#!/usr/bin/env bash
set -xeuo pipefail

export CI_BUILD_UNIQ_SUFFIX="${CI_BUILD_UNIQ_SUFFIX:-$(date +%s)}"

replacement="build.${CI_BUILD_UNIQ_SUFFIX}"

export IDEALINGUA_VERSION=$(cat version.sbt | sed -r 's/.*\"(.*)\".**/\1/' | sed -E "s/SNAPSHOT/${replacement}/")
