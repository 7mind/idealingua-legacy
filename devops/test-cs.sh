#!/usr/bin/env bash
set -xeuo pipefail

source ./devops/.env.sh

shellspec --format documentation --jobs "${NUMCPU}" -o junit --reportdir ./target/spec-reports/cs ./devops/spec/cs_spec.sh
