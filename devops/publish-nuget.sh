#!/usr/bin/env bash
set -xeuo pipefail

source ./devops/.env-basic.sh
env

source ./devops/.validate-publishing.sh

[[ -z "$TOKEN_NUGET" ]] && echo "Missing TOKEN_NUGET" && exit 0

./idealingua-v1/idealingua-v1-runtime-rpc-csharp/src/main/nuget/publish.sh || exit 1