#!/usr/bin/env bash
# ============================================
# HealthLens Docker - Stop Development
# ============================================
#
# Stops and cleans up Docker containers.
#
# Usage:
#   ./scripts/docker/down.sh           # Stop containers
#   ./scripts/docker/down.sh -v        # Stop and remove volumes (data will be lost!)
#   ./scripts/docker/down.sh --clean   # Full cleanup (containers, volumes, images)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo_info() { echo -e "${GREEN}[OK]${NC} $1"; }
echo_warn() { echo -e "${YELLOW}[WARN]${NC} $1" >&2; }
echo_error() { echo -e "${RED}[ERR]${NC} $1" >&2; }
echo_header() { echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n${BLUE}$1${NC}\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"; }
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
remove_compose_dev_images() {
    local tmpfile
    tmpfile="$(mktemp)" || return 1
    if ! docker compose -f docker/compose.yml -f docker/compose.dev.yml config --images >"$tmpfile" 2>/dev/null; then
        rm -f "$tmpfile"
        return 0
    fi
    while IFS= read -r img || [ -n "$img" ]; do
        [ -z "$img" ] && continue
        docker image rm -f "$img" 2>/dev/null || true
    done <"$tmpfile"
    rm -f "$tmpfile"
}

confirm_destructive() {
    local message="$1"
    local response
    if [ "$FORCE" = true ] || [ ! -t 0 ]; then
        return 0
    fi
    echo_warn "$message"
    printf '%s' "Type 'yes' to continue: "
    read -r response
    [ "$response" = "yes" ]
}

# Parse arguments
REMOVE_VOLUMES=false
REMOVE_IMAGES=false
CI_MODE=false
FORCE=false

while [ "$#" -gt 0 ]; do
    case $1 in
        -v|--volumes)
            REMOVE_VOLUMES=true
            shift
            ;;
        --clean)
            REMOVE_VOLUMES=true
            REMOVE_IMAGES=true
            shift
            ;;
        --yes|--force)
            FORCE=true
            shift
            ;;
        --ci)
            CI_MODE=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -v, --volumes    Remove volumes (data will be lost!)"
            echo "  --clean          Full cleanup (volumes + images)"
            echo "  --yes, --force   Skip interactive confirmation"
            echo "  --ci             Disable ANSI formatting for CI logs"
            echo "  --help           Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./scripts/docker/down.sh                    # Stop containers only"
            echo "  ./scripts/docker/down.sh -v                 # Stop and remove data"
            echo "  ./scripts/docker/down.sh --clean            # Full cleanup"
            exit 0
            ;;
        *)
            echo_error "Unknown option: $1"
            exit 2
            ;;
    esac
done

if [ "$CI_MODE" = true ] || [ ! -t 1 ] || [ "${NO_COLOR:-}" = "1" ]; then
    RED=''
    GREEN=''
    YELLOW=''
    BLUE=''
    NC=''
fi

preflight_docker

echo_header "HealthLens - Shutdown"

echo_info "Stopping containers..."

if [ "$REMOVE_VOLUMES" = true ] || [ "$REMOVE_IMAGES" = true ]; then
    if ! confirm_destructive "Destructive mode enabled (volumes/images may be deleted)."; then
        echo_warn "Cancelled."
        exit 1
    fi
fi

# Stop containers via compose
if [ "$REMOVE_VOLUMES" = true ]; then
    echo_warn "Removing volumes (data will be lost)..."
    docker compose -f docker/compose.yml -f docker/compose.dev.yml down -v --remove-orphans
else
    docker compose -f docker/compose.yml -f docker/compose.dev.yml down --remove-orphans
fi

# Remove images if requested
if [ "$REMOVE_IMAGES" = true ]; then
    echo_warn "Removing Docker images from compose config..."
    remove_compose_dev_images
    echo_info "Images removed (best-effort)"
fi

echo_header "Services Stopped"
echo_info "All HealthLens services have been stopped"
[ "$REMOVE_VOLUMES" = true ] && echo_warn "Volumes have been removed (data is gone)"
[ "$REMOVE_IMAGES" = true ] && echo_warn "Images have been removed"
echo ""
echo_info "Next: Run './scripts/docker/up.sh' to start again"
