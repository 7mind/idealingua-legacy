#!/usr/bin/env bash

set -e
set -x

source ./devops/.env.sh
printenv

function test_yarn_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "++ 2.13; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :typescript -d layout=YARN"

  pushd .
  cd $tmpdir/typescript
  [[ -f tsconfig.json ]] || exit 1

  yarn install
  yarn build

  popd
  echo "IDL TEST DONE: $1"
}

function test_plain_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "++ 2.13; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :typescript -d layout=PLAIN"

  pushd .
  cd $tmpdir/typescript
  [[ -f tsconfig.json ]] || exit 1

  yarn install
  tsc -p tsconfig.json

  popd
  echo "IDL TEST DONE: $1"
}

find ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs -maxdepth 1 -mindepth 1  -type d | while IFS= read -r file; do
    test_yarn_prj "$file"
    test_plain_prj "$file"
done
