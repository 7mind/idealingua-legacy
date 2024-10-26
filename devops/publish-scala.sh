#!/usr/bin/env bash

set -e
set -x

source ./devops/.env.sh

[[ "$CI_PULL_REQUEST" != "false"  ]] && exit 0
[[ -z "$TOKEN_NUGET" ]] && exit 0
[[ -z "$TOKEN_NPM" ]] && exit 0
[[ ! ("$CI_BRANCH" == "develop" || "$CI_BRANCH_TAG" =~ ^v.*$ ) ]] && exit 0
[[ -f "$SONATYPE_SECRET"]] && exit 0


echo "PUBLISH SCALA LIBRARIES..."

if [[ "$CI_BRANCH" == "develop" ]] ; then
  sbt -batch -no-colors -v "$VERSION_COMMAND clean" "$VERSION_COMMAND package" "$VERSION_COMMAND publishSigned"
else
  sbt -batch -no-colors -v "$VERSION_COMMAND clean" "$VERSION_COMMAND package" "$VERSION_COMMAND publishSigned" sonatypeBundleRelease || exit 1
fi
