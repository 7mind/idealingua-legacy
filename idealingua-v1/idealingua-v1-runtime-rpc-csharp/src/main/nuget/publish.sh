#!/usr/bin/env bash
set -xe

THISDIR="$( cd "$(dirname "$0")" ; pwd -P )"

pushd .
cd $THISDIR/solution

#rm -rf *.nupkg

[[ "$CI_PULL_REQUEST" != "false"  ]] && exit 0
[[ -z "$TOKEN_NUGET" ]] && exit 0
[[ -z "$CI_BUILD_UNIQ_SUFFIX" ]] && exit 0

sed -i 's/0.0.1-build.0/'${IDEALINGUA_VERSION}'/g' Izumi.RPC.Runtime.CS.IRT/Izumi.RPC.Runtime.CS.IRT.csproj

if [[ "$CI_BRANCH_TAG" =~ ^v.*$ ]] ; then
    dotnet build -c Release
else
    dotnet build -c Release --version-suffix "alpha.${CI_BUILD_UNIQ_SUFFIX}"
fi

find . -name '*.nupkg' -type f -print0 | xargs -I % -n 1 -0 dotnet nuget push % -k "${TOKEN_NUGET}" --source https://api.nuget.org/v3/index.json

popd
