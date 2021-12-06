#!/usr/bin/env bash

set -ex

export THISDIR="$( cd "$(dirname "$0")" ; pwd -P )"

pkgFile=package.json
pkgName='@izumi-framework/izumi-runtime-typescript'

pushd .
cd $THISDIR

cp -R ../main/resources/runtime/typescript/irt .
npm install

tsc -p ./tsconfig.json
tsc -p ./tsconfig.es.json

node -p "JSON.stringify({...require('./package.json'), name: '${pkgName}'}, null, 2)" > dist/package.json
node -p "JSON.stringify({...require('./package.json'), name: '${pkgName}-es'}, null, 2)" > dist-es/package.json

npm install json
./node_modules/json/lib/json.js -I -f dist/package.json -e "this.version=\"${IZUMI_VERSION}\""

( cd dist && npm publish --access public || exit 1 )
( cd dist-es && npm publish --access public || exit 1 )

popd
