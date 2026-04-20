#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

DEV_ENV="${DEV_ENV:-dev}"
STAGING_ENV="${STAGING_ENV:-staging}"
PROD_ENV="${PROD_ENV:-prod}"

STAGING_WEB_PATH="${STAGING_WEB_PATH:-/web}"
STAGING_API_PATH="${STAGING_API_PATH:-/api}"
DEV_PATH="${DEV_PATH:-/shared}"
PROD_PATH="${PROD_PATH:-/}"

DEV_FILE="${DEV_FILE:-.env}"
STAGING_WEB_FILE="${STAGING_WEB_FILE:-.env.staging}"
STAGING_API_FILE="${STAGING_API_FILE:-.env.staging.api}"
PROD_FILE="${PROD_FILE:-.env.production}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

usage() {
  cat <<'EOF'
Usage:
  ./infisical/scripts/infisical.sh <command>

Commands:
  bootstrap             Create staging folders (/web, /api) if missing
  check                 Print basic Infisical connectivity/folder info
  push-dev              Push .env -> dev (skip empty values automatically)
  push-staging-web      Push .env.staging -> staging:/web
  push-staging-api      Push .env.staging.api -> staging:/api
  push-staging          Push .env.staging -> staging:/web and .env.staging.api -> staging:/api
  push-prod             Push .env.production -> prod (default path /)
  push-all              Run push-dev + push-staging + push-prod
  pull-dev              Export dev -> .env
  pull-staging-web      Export staging:/web -> .env.staging
  pull-staging-api      Export staging:/api -> .env.staging.api
  pull-staging          Export staging:/web -> .env.staging and staging:/api -> .env.staging.api
  pull-prod             Export prod -> .env.production
  pull-all              Run pull-dev + pull-staging + pull-prod
  help                  Show this help

Notes:
  - Script expects repo already linked with Infisical (.infisical.json present).
  - Override env/path/file via environment variables:
      DEV_ENV, STAGING_ENV, PROD_ENV
      DEV_PATH, STAGING_WEB_PATH, STAGING_API_PATH, PROD_PATH
      DEV_FILE, STAGING_WEB_FILE, STAGING_API_FILE, PROD_FILE
EOF
}

assert_file_exists() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    echo "File not found: $file" >&2
    exit 1
  fi
}

create_staging_folders() {
  ensure_folder_path "$STAGING_ENV" "$STAGING_WEB_PATH"
  ensure_folder_path "$STAGING_ENV" "$STAGING_API_PATH"
}


ensure_folder_path() {
  local env_name="$1"
  local target_path="$2"

  if [[ "$target_path" == "/" ]]; then
    return
  fi

  local clean_path current segment
  clean_path="${target_path#/}"
  current="/"

  IFS='/' read -ra parts <<< "$clean_path"
  for segment in "${parts[@]}"; do
    [[ -z "$segment" ]] && continue
    infisical secrets folders create --env="$env_name" --path="$current" --name="$segment" >/dev/null 2>&1 || true
    if [[ "$current" == "/" ]]; then
      current="/$segment"
    else
      current="$current/$segment"
    fi
  done
}

filter_empty_dotenv() {
  local input_file="$1"
  local output_file="$2"
  local skipped_file="$3"

  # Keep comments/blank lines and non-empty assignments. Skip KEY= with empty value.
  awk '
  /^[[:space:]]*#/ { print; next }
  /^[[:space:]]*$/ { print; next }
  !/=/{ print; next }
  {
    split($0, parts, "=");
    key = parts[1];
    sub(/^[[:space:]]+|[[:space:]]+$/, "", key);
    val = substr($0, index($0, "=") + 1);
    sub(/^[[:space:]]+|[[:space:]]+$/, "", val);
    if (val == "") {
      print key >> skipped_file;
      next;
    }
    print;
  }' skipped_file="$skipped_file" "$input_file" >"$output_file"
}

push_dev() {
  assert_file_exists "$DEV_FILE"
  local tmp_file skipped_file
  tmp_file="$(mktemp)"
  skipped_file="$(mktemp)"

  filter_empty_dotenv "$DEV_FILE" "$tmp_file" "$skipped_file"
  ensure_folder_path "$DEV_ENV" "$DEV_PATH"
  infisical secrets set --file="$tmp_file" --env="$DEV_ENV" --path="$DEV_PATH"

  if [[ -s "$skipped_file" ]]; then
    echo
    echo "Skipped empty-value keys in $DEV_FILE:"
    awk '{print "  - " $0}' "$skipped_file"
  fi

  rm -f "$tmp_file" "$skipped_file"
}

push_staging() {
  create_staging_folders
  push_staging_web
  push_staging_api
}

push_staging_web() {
  assert_file_exists "$STAGING_WEB_FILE"
  create_staging_folders
  infisical secrets set --file="$STAGING_WEB_FILE" --env="$STAGING_ENV" --path="$STAGING_WEB_PATH"
}

push_staging_api() {
  assert_file_exists "$STAGING_API_FILE"
  create_staging_folders
  infisical secrets set --file="$STAGING_API_FILE" --env="$STAGING_ENV" --path="$STAGING_API_PATH"
}

push_prod() {
  assert_file_exists "$PROD_FILE"
  infisical secrets set --file="$PROD_FILE" --env="$PROD_ENV" --path="$PROD_PATH"
}

pull_dev() {
  export_or_print_pull_help "$DEV_ENV" "$DEV_PATH" "$DEV_FILE"
}

pull_staging() {
  pull_staging_web
  pull_staging_api
}

pull_staging_web() {
  export_or_print_pull_help "$STAGING_ENV" "$STAGING_WEB_PATH" "$STAGING_WEB_FILE"
}

pull_staging_api() {
  export_or_print_pull_help "$STAGING_ENV" "$STAGING_API_PATH" "$STAGING_API_FILE"
}

pull_prod() {
  export_or_print_pull_help "$PROD_ENV" "$PROD_PATH" "$PROD_FILE"
}

export_or_print_pull_help() {
  local env_name="$1"
  local path_name="$2"
  local output_file="$3"

  if ! infisical export --env="$env_name" --path="$path_name" --output-file="$output_file"; then
    echo "Unable to pull secrets for env '$env_name' at path '$path_name'." >&2
    echo "Pull commands do not create folders automatically." >&2
    echo "If the folder/path does not exist, run bootstrap/push with a write-capable account." >&2
    return 1
  fi
}

check() {
  echo "Project: $PROJECT_ROOT"
  echo "Infisical CLI: $(infisical --version)"
  echo
  echo "Dev folders at / (DEV_PATH=$DEV_PATH):"
  infisical secrets folders get --env="$DEV_ENV" --path=/
  echo
  echo "Staging folders at /:"
  infisical secrets folders get --env="$STAGING_ENV" --path=/
  echo
  echo "Prod folders at /:"
  infisical secrets folders get --env="$PROD_ENV" --path=/
}

main() {
  require_cmd infisical
  require_cmd awk
  if [[ ! -f ".infisical.json" ]]; then
    echo "Missing .infisical.json. Run: infisical init" >&2
    exit 1
  fi

  local cmd="${1:-help}"
  case "$cmd" in
    bootstrap) create_staging_folders ;;
    check) check ;;
    push-dev) push_dev ;;
    push-staging-web) push_staging_web ;;
    push-staging-api) push_staging_api ;;
    push-staging) push_staging ;;
    push-prod) push_prod ;;
    push-all) push_dev; push_staging; push_prod ;;
    pull-dev) pull_dev ;;
    pull-staging-web) pull_staging_web ;;
    pull-staging-api) pull_staging_api ;;
    pull-staging) pull_staging ;;
    pull-prod) pull_prod ;;
    pull-all) pull_dev; pull_staging; pull_prod ;;
    help|-h|--help) usage ;;
    *)
      echo "Unknown command: $cmd" >&2
      echo
      usage
      exit 1
      ;;
  esac
}

main "$@"
