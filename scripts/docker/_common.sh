#!/usr/bin/env bash
# Shared helpers for HealthLens Docker scripts.

HL_DOCKER_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$HL_DOCKER_SCRIPT_DIR/../.." && pwd)"

hl_docker_cd_project_root() {
  cd "$PROJECT_ROOT"
}

hl_set_default_colors() {
  RED='\033[0;31m'
  GREEN='\033[0;32m'
  YELLOW='\033[1;33m'
  BLUE='\033[0;34m'
  MAGENTA='\033[0;35m'
  CYAN='\033[0;36m'
  WHITE='\033[1;37m'
  GRAY='\033[0;90m'
  NC='\033[0m'
}

disable_colors_if_needed() {
  if [ "${CI_MODE:-false}" = true ] || [ ! -t 1 ] || [ "${NO_COLOR:-}" = "1" ]; then
    RED=''
    GREEN=''
    YELLOW=''
    BLUE=''
    MAGENTA=''
    CYAN=''
    WHITE=''
    GRAY=''
    NC=''
  fi
}

echo_info() { echo -e "${GREEN}[OK]${NC} $1"; }
echo_warn() { echo -e "${YELLOW}[WARN]${NC} $1" >&2; }
echo_error() { echo -e "${RED}[ERR]${NC} $1" >&2; }
echo_header() {
  echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n${BLUE}$1${NC}\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

preflight_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo_error "docker command not found"
    exit 1
  fi
  if ! docker info >/dev/null 2>&1; then
    echo_error "Docker daemon is not running"
    exit 1
  fi
  if ! docker compose version >/dev/null 2>&1; then
    echo_error "Docker Compose v2 is not available"
    exit 1
  fi
}

format_duration() {
  local seconds="${1:-0}"
  local minutes
  local remain_seconds
  if [ "$seconds" -lt 60 ]; then
    printf "%ss" "$seconds"
    return
  fi
  minutes="$((seconds / 60))"
  remain_seconds="$((seconds % 60))"
  printf "%sm %ss" "$minutes" "$remain_seconds"
}

hl_format_human_bytes() {
  local hl_bytes="${1:-0}"
  local hl_kb
  local hl_mb
  local hl_gb
  case "$hl_bytes" in
    '' | *[!0-9]*) hl_bytes=0 ;;
  esac
  if [ "$hl_bytes" -lt 1024 ]; then
    printf '%sB' "$hl_bytes"
    return 0
  fi
  hl_kb=$((hl_bytes / 1024))
  if [ "$hl_kb" -lt 1024 ]; then
    printf '%sKB' "$hl_kb"
    return 0
  fi
  hl_mb=$((hl_kb / 1024))
  if [ "$hl_mb" -lt 1024 ]; then
    printf '%sMB' "$hl_mb"
    return 0
  fi
  hl_gb=$((hl_mb / 1024))
  printf '%sGB' "$hl_gb"
}

hl_dir_size_bytes() {
  local hl_dir="$1"
  if [ ! -d "$hl_dir" ]; then
    printf '%s' "0"
    return 0
  fi
  if du -sb "$hl_dir" >/dev/null 2>&1; then
    du -sb "$hl_dir" 2>/dev/null | awk '{print $1; exit}'
  else
    du -sk "$hl_dir" 2>/dev/null | awk '{print $1 * 1024; exit}'
  fi
}

format_size() {
  hl_format_human_bytes "$1"
}

get_dir_size() {
  hl_dir_size_bytes "$1"
}

hl_set_default_colors
