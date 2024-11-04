#!/usr/bin/env bash

set -e
set -x

source ./devops/.env.sh
printenv

function test_pb_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "$VERSION_COMMAND ; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :protobuf"

  pushd .
  cd $tmpdir/protobuf

  mkdir ./java-out
  protoc --java_out=./java-out $(find ./ -iname '*.proto')

  popd
  echo "IDL TEST DONE: $1"
}

test_pb_prj ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs/main-tests
test_pb_prj ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs/overlays

# subdir is broken (nested generics)

#find ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs -maxdepth 1 -mindepth 1  -type d | while IFS= read -r file; do
#    test_pb_prj "$file"
#done
