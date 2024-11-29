#!/usr/bin/env bash

set -xe

./sbtgen.sc

git add .

sbt "++2.13" "release"