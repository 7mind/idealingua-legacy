#!/usr/bin/env bash
set -xe

export THISDIR="$( cd "$(dirname "$0")" ; pwd -P )"

pushd .
cd $THISDIR

rm -rf *.nupkg

NUSPEC=irt.tmp.nuspec
cat irt.nuspec | sed 's/0.0.1-UNSET/'${IDEALINGUA_VERSION}'/g' > $NUSPEC
cat $NUSPEC
nuget pack $NUSPEC
rm $NUSPEC

#nuget setapikey $TOKEN_NUGET

for TRG in $(find . -name '*.nupkg' -type f -print)
do
    dotnet nuget push $TRG -k $TOKEN_NUGET --source https://api.nuget.org/v3/index.json || exit 1
done

popd
