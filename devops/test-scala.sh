#!/usr/bin/env bash
set -xeuo pipefail

source ./devops/.env.sh

shellspec --format documentation --jobs "${NUMCPU}" -o junit --reportdir ./target/spec-reports/scala ./devops/spec/scala_spec.sh
