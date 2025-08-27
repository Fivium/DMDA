#!/usr/bin/env bash
set -euo pipefail

FILE="${1:?Usage: $0 <file>}"

# Ensure environment variables exist
: "${JDBC_URL:?Environment variable JDBC_URL not set}"
: "${JDBC_USER:?Environment variable JDBC_USER not set}"
: "${JDBC_PASSWORD:?Environment variable JDBC_PASSWORD not set}"

# Replace placeholders with values
sed \
  -e "s|\$\$JDBC_URL\$\$|${JDBC_URL}|g" \
  -e "s|\$\$JDBC_USER\$\$|${JDBC_USER}|g" \
  -e "s|\$\$JDBC_PASSWORD\$\$|${JDBC_PASSWORD}|g" \
  "$FILE" > "$FILE"
