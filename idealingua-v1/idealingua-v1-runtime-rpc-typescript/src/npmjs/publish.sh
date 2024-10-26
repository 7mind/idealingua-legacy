#!/usr/bin/env bash

set -ex

export THISDIR="$( cd "$(dirname "$0")" ; pwd -P )"

pushd .
cd $THISDIR

pkgFile='package.json'
pkgName=$(cat package.json | node -pe 'JSON.parse(fs.readFileSync(0)).name')
pkgPath='dist'

cp -R ../main/resources/runtime/typescript/irt .
npm install

tsc -p ./tsconfig.json
tsc -p ./tsconfig.es.json

cp package.json ${pkgPath}/
node -p "JSON.stringify({...require(path.resolve(__dirname, 'package.json')), name: '${pkgName}-es'}, null, 2)" > ${pkgPath}-es/package.json

npm install json
./node_modules/json/lib/json.js -I -f ${pkgPath}/package.json -e "this.version=\"${IDEALINGUA_VERSION}\""
./node_modules/json/lib/json.js -I -f ${pkgPath}-es/package.json -e "this.version=\"${IDEALINGUA_VERSION}\""

( cd ${pkgPath} && npm publish --access public || exit 1 )
( cd ${pkgPath}-es && npm publish --access public || exit 1 )

popd
