#!/usr/bin/env bash
# ============================================================================
# HealthLens Docker - View and Filter Logs
# ============================================================================
#
# Portable: Bash 3.2+ (macOS /bin/bash). No associative arrays, no mapfile.
#
# ============================================================================

set -euo pipefail

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"
hl_docker_cd_project_root

SERVICE=""
FOLLOW=false
FILTER_ERRORS=false
FILTER_WARNINGS=false
FILTER_HEALTH=false
GREP_PATTERN=""
TAIL_LINES=""
SINCE_TIME=""
SHOW_MENU=false
HELP=false
CI_MODE=false

while [ "$#" -gt 0 ]; do
  case $1 in
    -f | --follow)
      FOLLOW=true
      shift
      ;;
    --errors)
      FILTER_ERRORS=true
      shift
      ;;
    --warnings)
      FILTER_WARNINGS=true
      shift
      ;;
    --health)
      FILTER_HEALTH=true
      shift
      ;;
    --grep)
      if [ "$#" -lt 2 ]; then
        echo "Missing value for --grep" >&2
        exit 2
      fi
      GREP_PATTERN="$2"
      shift 2
      ;;
    --tail)
      if [ "$#" -lt 2 ]; then
        echo "Missing value for --tail" >&2
        exit 2
      fi
      TAIL_LINES="$2"
      shift 2
      ;;
    --since)
      if [ "$#" -lt 2 ]; then
        echo "Missing value for --since" >&2
        exit 2
      fi
      SINCE_TIME="$2"
      shift 2
      ;;
    --menu)
      SHOW_MENU=true
      shift
      ;;
    --ci)
      CI_MODE=true
      shift
      ;;
    --help | -h)
      HELP=true
      shift
      ;;
    api | web | postgres | redis | minio | mailhog)
      SERVICE="$1"
      shift
      ;;
    *)
      echo "Unknown option: $1" >&2
      echo "Use --help for usage information" >&2
      exit 2
      ;;
  esac
done

if [ "$HELP" = true ]; then
  cat <<'HELP_TEXT'
HealthLens Docker Log Viewer

USAGE:
  ./scripts/docker/logs.sh [OPTIONS] [SERVICE]

SERVICES:
  api       - Spring Boot API (port 8080)
  web       - Next.js Web App (port 3000)
  postgres  - PostgreSQL Database (port 5432)
  redis     - Redis Cache (port 6379)
  minio     - MinIO S3 Storage (port 9000)
  mailhog   - Mailhog SMTP/UI (ports 1025/8025)

OPTIONS:
  -f, --follow          Follow log output in real-time
  --errors              Show only ERROR level logs
  --warnings            Show only WARNING level logs
  --health              Show only health check logs
  --grep PATTERN        Filter logs by pattern (case-sensitive)
  --tail N              Show last N lines (default: all)
  --since TIME          Show logs since TIME (e.g., 10m, 1h, 30s)
  --menu                Interactive service picker (requires a TTY)
  --ci                  Disable ANSI formatting for CI logs
  -h, --help            Show this help message

HELP_TEXT
  exit 0
fi

disable_colors_if_needed
preflight_docker

if [ "$SHOW_MENU" = true ]; then
  if [ ! -t 0 ]; then
    echo "--menu requires an interactive terminal (stdin must be a TTY)" >&2
    exit 2
  fi
  echo -e "${BLUE}========================================${NC}"
  echo -e "${BLUE}HealthLens Docker - Log Viewer Menu${NC}"
  echo -e "${BLUE}========================================${NC}"
  echo ""
  echo "Select a service to view logs:"
  echo ""
  echo "  1) API (Spring Boot)"
  echo "  2) Web (Next.js)"
  echo "  3) PostgreSQL"
  echo "  4) Redis"
  echo "  5) MinIO"
  echo "  6) Mailhog"
  echo "  7) All Services"
  echo "  0) Exit"
  echo ""
  printf '%s' "Enter choice (0-7): "
  read -r choice

  case $choice in
    1) SERVICE="api" ;;
    2) SERVICE="web" ;;
    3) SERVICE="postgres" ;;
    4) SERVICE="redis" ;;
    5) SERVICE="minio" ;;
    6) SERVICE="mailhog" ;;
    7) SERVICE="" ;;
    0) exit 0 ;;
    *)
      echo "Invalid choice" >&2
      exit 2
      ;;
  esac

  printf '%s' "Follow logs? (y/n): "
  read -r follow_choice
  case "$follow_choice" in
    [yY]) FOLLOW=true ;;
  esac
  echo ""
fi

compose_args=(compose -f docker/compose.yml -f docker/compose.dev.yml logs)
if [ -n "$SERVICE" ]; then
  compose_args+=("$SERVICE")
fi
if [ "$FOLLOW" = true ]; then
  compose_args+=(-f)
fi
if [ -n "$TAIL_LINES" ]; then
  compose_args+=("--tail=$TAIL_LINES")
fi
if [ -n "$SINCE_TIME" ]; then
  compose_args+=("--since=$SINCE_TIME")
fi

hl_docker_logs() {
  docker "${compose_args[@]}"
}

escape_for_ere() {
  printf '%s' "$1" | sed 's/[][\\.^$*+?()|{}]/\\&/g'
}

build_filter_regex() {
  hl_parts=""
  if [ "$FILTER_ERRORS" = true ]; then
    hl_parts="${hl_parts}|ERROR"
  fi
  if [ "$FILTER_WARNINGS" = true ]; then
    hl_parts="${hl_parts}|WARN"
  fi
  if [ "$FILTER_HEALTH" = true ]; then
    hl_parts="${hl_parts}|health"
  fi
  if [ -n "$GREP_PATTERN" ]; then
    hl_parts="${hl_parts}|$(escape_for_ere "$GREP_PATTERN")"
  fi
  hl_parts="${hl_parts#|}"
  printf '%s' "$hl_parts"
}

FILTER_REGEX="$(build_filter_regex)"

line_matches_filter() {
  hl_line="$1"
  hl_regex="$2"
  [ -z "$hl_regex" ] && return 0
  printf '%s\n' "$hl_line" | grep -Eq -- "$hl_regex"
}

color_line() {
  hl_line="$1"
  case "$hl_line" in
    *postgres*) echo -e "${BLUE}${hl_line}${NC}" ;;
    *mailhog*) echo -e "${WHITE}${hl_line}${NC}" ;;
    *minio*) echo -e "${GREEN}${hl_line}${NC}" ;;
    *redis*) echo -e "${YELLOW}${hl_line}${NC}" ;;
    *api*) echo -e "${MAGENTA}${hl_line}${NC}" ;;
    *web*) echo -e "${CYAN}${hl_line}${NC}" ;;
    *) echo "$hl_line" ;;
  esac
}

stream_logs_colored() {
  hl_use_filter="$1"
  hl_docker_logs 2>&1 | while IFS= read -r hl_line || [ -n "$hl_line" ]; do
    if [ "$hl_use_filter" = true ] && [ -n "$FILTER_REGEX" ]; then
      line_matches_filter "$hl_line" "$FILTER_REGEX" || continue
    fi
    color_line "$hl_line"
  done
}

if [ -n "$FILTER_REGEX" ]; then
  if [ "$FOLLOW" = true ]; then
    stream_logs_colored true
  else
    if ! logs_output="$(hl_docker_logs 2>&1)"; then
      printf '%s\n' "$logs_output" >&2
      exit 1
    fi
    printf '%s\n' "$logs_output" | grep -E -- "$FILTER_REGEX" || true
  fi
else
  if [ "$FOLLOW" = true ]; then
    stream_logs_colored false
  else
    hl_docker_logs
  fi
fi
