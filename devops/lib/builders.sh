function test_scala_sbt_prj() {
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

function test_scala_plain_prj() {
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

function test_ts_yarn_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "$VERSION_COMMAND ; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :typescript -d layout=YARN"

  pushd .
  cd $tmpdir/typescript
  [[ -f tsconfig.json ]] || exit 1

  yarn install
  yarn build

  popd
  echo "IDL TEST DONE: $1"
}

function test_ts_plain_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "$VERSION_COMMAND ; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :typescript -d layout=PLAIN"

  pushd .
  cd $tmpdir/typescript
  [[ -f tsconfig.json ]] || exit 1

  yarn install
  tsc -p tsconfig.json

  popd
  echo "IDL TEST DONE: $1"
}

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

function test_cs_msbuild_prj() {
  echo "IDL TEST ABOUT TO START: $1"
  testname="$(basename $1)"
  tmpdir="$(mktemp -d -t "$testname".XXXXXXXX)"

  sbt "$VERSION_COMMAND ; idealingua-v1-compiler/run --root=$1 --source=$1/source --overlay=$1/overlay --target=$tmpdir :csharp -d layout=NUGET"

  pushd .
  cd $tmpdir/csharp

  msbuild /t:Restore /t:Rebuild

  for f in nuspec/*.nuspec
  do
    nuget pack "$f"
  done

  popd
  echo "IDL TEST DONE: $1"
}

function test_cs_plain_prj() {
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