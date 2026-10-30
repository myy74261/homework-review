#!/usr/bin/env bash
set -euo pipefail

env_file="${1:-/opt/review-desk/.env.production}"

sed -i 's/^JPA_DDL_AUTO=.*/JPA_DDL_AUTO=validate/' "$env_file"

add_if_missing() {
  local key="$1"
  local value="$2"
  if ! grep -q "^${key}=" "$env_file"; then
    printf '%s=%s\n' "$key" "$value" >> "$env_file"
  fi
}

add_if_missing FLYWAY_ENABLED true
add_if_missing FLYWAY_BASELINE_ON_MIGRATE true
add_if_missing SESSION_STORE_TYPE jdbc
add_if_missing SESSION_TIMEOUT 8h
add_if_missing SESSION_CLEANUP_CRON '0 */15 * * * *'

chmod 600 "$env_file"
grep -E '^(JPA_DDL_AUTO|FLYWAY_ENABLED|FLYWAY_BASELINE_ON_MIGRATE|SESSION_STORE_TYPE|SESSION_TIMEOUT|SESSION_CLEANUP_CRON)=' "$env_file"
