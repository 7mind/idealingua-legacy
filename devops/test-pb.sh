#!/usr/bin/env bash
set -xeuo pipefail

source ./devops/.env.sh

shellspec --format documentation --jobs "${NUMCPU}" -o junit --reportdir ./target/spec-reports/pb ./devops/spec/pb_spec.sh