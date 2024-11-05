#!/usr/bin/env bash
set -xeuo pipefail

source ./devops/.env.sh

shellspec --format documentation --jobs "${NUMCPU}" -o junit --reportdir ./target/spec-reports/ts ./devops/spec/ts_spec.sh