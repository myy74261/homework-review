#!/usr/bin/env bash
set -euo pipefail

cd /opt/review-desk
set -a
source .env.production
set +a

if ! runuser -u postgres -- psql -tAc \
  "SELECT 1 FROM pg_roles WHERE rolname = '$DATABASE_USERNAME'" | grep -q 1; then
  runuser -u postgres -- psql -v ON_ERROR_STOP=1 -c \
    "CREATE ROLE $DATABASE_USERNAME LOGIN PASSWORD '$DATABASE_PASSWORD';"
else
  runuser -u postgres -- psql -v ON_ERROR_STOP=1 -c \
    "ALTER ROLE $DATABASE_USERNAME WITH LOGIN PASSWORD '$DATABASE_PASSWORD';"
fi

if ! runuser -u postgres -- psql -tAc \
  "SELECT 1 FROM pg_database WHERE datname = '$POSTGRES_DB'" | grep -q 1; then
  runuser -u postgres -- createdb --owner="$DATABASE_USERNAME" "$POSTGRES_DB"
fi

runuser -u postgres -- psql -tAc \
  "SELECT datname, pg_get_userbyid(datdba) FROM pg_database WHERE datname = '$POSTGRES_DB';"

