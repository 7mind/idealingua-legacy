#!/usr/bin/env bash
set -xeuo pipefail

source ./devops/.env.sh

classpath="$(TERM=dumb sbt --batch --error "$VERSION_COMMAND ; export idealingua-v1-compiler/runtime:fullClasspath")"

function test_sbt_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "$VERSION_COMMAND ; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :scala -d layout=SBT"

  pushd .
  cd $tmpdir/scala
  [[ -f build.sbt ]] || exit 1
  sbt clean compile
  popd
  echo "IDL TEST DONE: $1"
}

function test_plain_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "$VERSION_COMMAND ; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :scala -d layout=PLAIN"

  pushd .
  cd $tmpdir/scala
  files=$(find . -name '*.scala' -print0 | xargs -0)

  mkdir ./target
  cs \
    launch \
    scalac:"${SCALA_VERSION}" \
    -- \
    -deprecation \
    -opt-warnings:_ \
    -d \
    ./target \
    -classpath "$classpath" \
    ${files}
  popd
  echo "IDL TEST DONE: $1"
}

find ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs -maxdepth 1 -mindepth 1  -type d | while IFS= read -r file; do
  test_sbt_prj "$file"
  test_plain_prj "$file"
done


find ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs-special -name 'scala-*' -maxdepth 1 -mindepth 1  -type d | while IFS= read -r file; do
  test_plain_prj "$file"
done
