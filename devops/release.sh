#!/usr/bin/env bash

set -xe

./sbtgen.sc

git add . || true

sbt "++2.13" "release"