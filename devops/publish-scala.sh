#!/usr/bin/env bash
set -xeuo pipefail

source ./devops/.env.sh

source ./devops/.validate-publishing.sh

[[ ! -f "$SONATYPE_SECRET" ]] && echo "SONATYPE_SECRET=$SONATYPE_SECRET is not a file" && exit 0


echo "PUBLISH SCALA LIBRARIES..."

if [[ "$CI_BRANCH" == "develop" ]] ; then
  sbt -batch -no-colors -v \
    "$VERSION_COMMAND clean" \
    "$VERSION_COMMAND package" \
    "$VERSION_COMMAND publishSigned"
else
  sbt -batch -no-colors -v \
    "$VERSION_COMMAND clean" \
    "$VERSION_COMMAND package" \
    "$VERSION_COMMAND publishSigned" \
    sonatypeBundleRelease || exit 1
fi
