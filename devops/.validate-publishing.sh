#!/usr/bin/env bash

set -e
set -x

[[ "$CI_PULL_REQUEST" != "false"  ]] && echo "Publishing not allowed on P/Rs" && exit 0
[[ ! ("$CI_BRANCH" == "develop" || "$CI_BRANCH_TAG" =~ ^v.*$ ) ]] && echo "Publishing not allowed (CI_BRANCH=$CI_BRANCH, CI_BRANCH_TAG=$CI_BRANCH_TAG)" && exit 0
