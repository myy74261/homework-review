#!/usr/bin/env bash
set -euo pipefail

base_url="${1:-http://127.0.0.1:8091}"
cookie_file="$(mktemp)"
trap 'rm -f "$cookie_file"' EXIT

login_page="$(curl -sS -c "$cookie_file" "$base_url/login")"
csrf_token="$(
  printf '%s' "$login_page" \
    | grep -o 'name="_csrf" value="[^"]*"' \
    | sed 's/.*value="//;s/"$//'
)"

if [[ -z "$csrf_token" ]]; then
  echo "无法读取登录页 CSRF 令牌" >&2
  exit 1
fi

login_code="$(
  curl -sS -b "$cookie_file" -c "$cookie_file" -o /dev/null -w '%{http_code}' \
    --data-urlencode "username=${DEMO_TEACHER_USERNAME}" \
    --data-urlencode "password=${DEMO_TEACHER_PASSWORD}" \
    --data-urlencode "_csrf=${csrf_token}" \
    "$base_url/login"
)"
page_code="$(
  curl -sS -b "$cookie_file" -o /dev/null -w '%{http_code}' \
    "$base_url/app/teacher"
)"

printf 'login=%s page=%s\n' "$login_code" "$page_code"
test "$login_code" = "302"
test "$page_code" = "200"
