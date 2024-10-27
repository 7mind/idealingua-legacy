#!/usr/bin/env bash

set -e
set -x

source ./devops/.env-basic.sh
printenv

source ./devops/.validate-publishing.sh

[[ -z "$TOKEN_NPM" ]] && echo "Missing TOKEN_NPM" && exit 0

echo "//registry.npmjs.org/:_authToken=${TOKEN_NPM}" > ~/.npmrc
npm whoami

./idealingua-v1/idealingua-v1-runtime-rpc-typescript/src/npmjs/publish.sh || exit 1
