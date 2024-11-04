#!/usr/bin/env bash

set -e
set -x

source ./devops/.env.sh
printenv

refsdir="$PWD/idealingua-v1/idealingua-v1-test-defs/src/main/resources/refs/csharp"
refs="$(find "$refsdir" -name "*.dll" -exec echo "/reference:{}" \; | xargs -0 | tr '\n' ' ')"


function test_msbuild_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "$VERSION_COMMAND ; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :csharp -d layout=NUGET"

  pushd .
  cd $tmpdir/csharp

  msbuild /t:Restore /t:Rebuild
  find . -name '*.nuspec' -print0 | xargs -I % -n 1 -0 nuget pack %

  popd
  echo "IDL TEST DONE: $1"
}

function test_plain_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "$VERSION_COMMAND ; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :csharp -d layout=PLAIN"

  pushd .
  cd $tmpdir/csharp

  csc -target:library -out:tests.dll "-recurse:\\*.cs" $refs
  cp "$refsdir"/*.dll .
  nunit3-console tests.dll

  popd
  echo "IDL TEST DONE: $1"
}

find ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs -maxdepth 1 -mindepth 1  -type d | while IFS= read -r file; do
    test_msbuild_prj "$file"
    test_plain_prj "$file"
done
