#!/usr/bin/env bash
# ============================================================================
# HealthLens Docker - View and Filter Logs
# ============================================================================
#
# Advanced log viewer with filtering, colors, and interactive service selection
#
# Usage:
#   ./logs.sh                      # Interactive menu to select service
#   ./logs.sh api                  # View API logs only
#   ./logs.sh web -f               # Follow web logs
#   ./logs.sh --errors             # Show only ERROR logs from all services
#   ./logs.sh --warnings           # Show only WARNING logs from all services
#   ./logs.sh --health             # Show only health check logs
#   ./logs.sh api --errors -f      # Follow API errors only
#   ./logs.sh --tail 50            # Show last 50 lines from all services
#   ./logs.sh api --grep "Started" # Show lines containing "Started"
#
# Options:
#   -f, --follow              Follow log output (tail -f behavior)
#   --errors                  Show only ERROR level logs
#   --warnings                # Show only WARNING level logs
#   --health                  # Show only health check related logs
#   --grep PATTERN            # Filter logs by pattern
#   --tail N                  # Show last N lines
#   --since TIME              # Show logs since TIME (e.g., 10m, 1h)
#   --menu                    # Show interactive service picker
#   --help                    # Show this help message
#
# Examples:
#   ./logs.sh --menu              # Interactive service selection
#   ./logs.sh api --errors -f     # Follow API errors
#   ./logs.sh --grep "ERROR" -f   # Follow all ERROR logs
#   ./logs.sh postgres --tail 100 # Last 100 lines from postgres
#
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$SCRIPTS_DIR")"
cd "$PROJECT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
GRAY='\033[0;90m'
NC='\033[0m' # No Color

# Service color mapping
declare -A SERVICE_COLORS=(
  ["api"]=$MAGENTA
  ["web"]=$CYAN
  ["postgres"]=$BLUE
  ["redis"]=$YELLOW
  ["minio"]=$GREEN
  ["mailhog"]=$WHITE
)

# Options
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

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -f|--follow)
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
      GREP_PATTERN="$2"
      shift 2
      ;;
    --tail)
      TAIL_LINES="$2"
      shift 2
      ;;
    --since)
      SINCE_TIME="$2"
      shift 2
      ;;
    --menu)
      SHOW_MENU=true
      shift
      ;;
    --help|-h)
      HELP=true
      shift
      ;;
    api|web|postgres|redis|minio)
      SERVICE="$1"
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use --help for usage information"
      exit 1
      ;;
  esac
done

# Show help
if [ "$HELP" = true ]; then
  cat << 'HELP_TEXT'
HealthLens Docker Log Viewer

USAGE:
  ./logs.sh [OPTIONS] [SERVICE]

SERVICES:
  api       - Spring Boot API (port 8080)
  web       - Next.js Web App (port 3000)
  postgres  - PostgreSQL Database (port 5432)
  redis     - Redis Cache (port 6379)
  minio     - MinIO S3 Storage (port 9000)

OPTIONS:
  -f, --follow          Follow log output in real-time
  --errors              Show only ERROR level logs
  --warnings            Show only WARNING level logs
  --health              Show only health check logs
  --grep PATTERN        Filter logs by pattern (case-sensitive)
  --tail N              Show last N lines (default: all)
  --since TIME          Show logs since TIME (e.g., 10m, 1h, 30s)
  --menu                Interactive service picker
  -h, --help            Show this help message

EXAMPLES:
  # Interactive service selection
  ./logs.sh --menu

  # View specific service
  ./logs.sh api
  ./logs.sh web -f

  # Filter by level
  ./logs.sh --errors
  ./logs.sh api --warnings -f

  # Filter by pattern
  ./logs.sh --grep "ERROR" -f
  ./logs.sh postgres --grep "Started"

  # Time-based filtering
  ./logs.sh --tail 100
  ./logs.sh --since 5m

  # Combine options
  ./logs.sh api --errors --tail 50 -f

HELP_TEXT
  exit 0
fi

# Show interactive menu if requested or no service specified
if [ "$SHOW_MENU" = true ] || ([ -z "$SERVICE" ] && [ "$FILTER_ERRORS" = false ] && [ "$FILTER_WARNINGS" = false ] && [ "$FILTER_HEALTH" = false ] && [ -z "$GREP_PATTERN" ]); then
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
  read -p "Enter choice (0-7): " choice

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
      echo "Invalid choice"
      exit 1
      ;;
  esac

  # Ask for follow mode
  read -p "Follow logs? (y/n): " follow_choice
  if [[ "$follow_choice" =~ ^[Yy]$ ]]; then
    FOLLOW=true
  fi

  echo ""
fi

# Build docker compose argv (avoid eval — safer with arbitrary --grep text)
compose_args=(compose -f docker/compose.yml -f docker/compose.dev.yml logs)
if [ -n "$SERVICE" ]; then
  compose_args+=("$SERVICE")
fi
if [ "$FOLLOW" = true ]; then
  compose_args+=(-f)
fi
if [ -n "$TAIL_LINES" ]; then
  compose_args+=(--tail="$TAIL_LINES")
fi
if [ -n "$SINCE_TIME" ]; then
  compose_args+=(--since="$SINCE_TIME")
fi

escape_for_ere() {
  printf '%s' "$1" | sed 's/[][\\.^$*+?()|{}]/\\&/g'
}

build_filter_regex() {
  local parts=()
  if [ "$FILTER_ERRORS" = true ]; then
    parts+=("ERROR")
  fi
  if [ "$FILTER_WARNINGS" = true ]; then
    parts+=("WARN")
  fi
  if [ "$FILTER_HEALTH" = true ]; then
    parts+=("health")
  fi
  if [ -n "$GREP_PATTERN" ]; then
    parts+=("$(escape_for_ere "$GREP_PATTERN")")
  fi
  if [ "${#parts[@]}" -eq 0 ]; then
    echo ""
    return 0
  fi
  local IFS='|'
  echo "${parts[*]}"
}

FILTER_REGEX="$(build_filter_regex)"

line_matches_filter() {
  local line="$1"
  local regex="$2"
  [ -z "$regex" ] && return 0
  printf '%s\n' "$line" | grep -Eq -- "$regex"
}

color_line() {
  local line="$1"
  local colored=false
  for service in "${!SERVICE_COLORS[@]}"; do
    if [[ $line == *"$service"* ]]; then
      echo -e "${SERVICE_COLORS[$service]}$line${NC}"
      colored=true
      break
    fi
  done
  if [ "$colored" = false ]; then
    echo "$line"
  fi
}

stream_logs_colored() {
  local use_filter="$1"
  docker "${compose_args[@]}" 2>&1 | while IFS= read -r line || [ -n "$line" ]; do
    if [ "$use_filter" = true ] && [ -n "$FILTER_REGEX" ]; then
      line_matches_filter "$line" "$FILTER_REGEX" || continue
    fi
    color_line "$line"
  done
}

if [ -n "$FILTER_REGEX" ]; then
  if [ "$FOLLOW" = true ]; then
    stream_logs_colored true
  else
    set -o pipefail
    docker "${compose_args[@]}" 2>&1 | grep -E -- "$FILTER_REGEX" || true
    set +o pipefail
  fi
else
  if [ "$FOLLOW" = true ]; then
    stream_logs_colored false
  else
    docker "${compose_args[@]}"
  fi
fi
