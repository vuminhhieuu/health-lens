#!/usr/bin/env bash
# ============================================
# HealthLens Docker - Stop Development
# ============================================
#
# Stops and cleans up Docker containers.
#
# Usage:
#   ./down.sh           # Stop containers
#   ./down.sh -v        # Stop and remove volumes (data will be lost!)
#   ./down.sh --clean   # Full cleanup (containers, volumes, images)

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
NC='\033[0m'

echo_info() { echo -e "${GREEN}✓${NC} $1"; }
echo_warn() { echo -e "${YELLOW}⚠${NC} $1"; }
echo_error() { echo -e "${RED}✗${NC} $1"; }
echo_header() { echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n${BLUE}$1${NC}\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"; }

# Parse arguments
REMOVE_VOLUMES=false
REMOVE_IMAGES=false

while [[ $# -gt 0 ]]; do
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
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -v, --volumes    Remove volumes (data will be lost!)"
            echo "  --clean          Full cleanup (volumes + images)"
            echo "  --help           Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./down.sh                    # Stop containers only"
            echo "  ./down.sh -v                 # Stop and remove data"
            echo "  ./down.sh --clean            # Full cleanup"
            exit 0
            ;;
        *)
            echo_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo_header "HealthLens - Shutdown"

echo_info "Stopping containers..."

# Remove orphan containers (created manually, not by compose)
for container in healthlens-api-dev healthlens-web-dev healthlens-mailhog healthlens-ocr; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo_info "Removing orphan container: $container"
        docker rm -f "$container" 2>/dev/null || true
    fi
done

# Stop containers via compose
if [ "$REMOVE_VOLUMES" = true ]; then
    echo_warn "Removing volumes (data will be lost)..."
    docker compose -f docker/compose.yml -f docker/compose.dev.yml down -v
else
    docker compose -f docker/compose.yml -f docker/compose.dev.yml down
fi

# Remove images if requested
if [ "$REMOVE_IMAGES" = true ]; then
    echo_warn "Removing Docker images..."
    docker images | grep healthlens | awk '{print $3}' | xargs -r docker rmi -f 2>/dev/null || true
    echo_info "Images removed"
fi

echo_header "Services Stopped"
echo_info "All HealthLens services have been stopped"
[ "$REMOVE_VOLUMES" = true ] && echo_warn "Volumes have been removed (data is gone)"
[ "$REMOVE_IMAGES" = true ] && echo_warn "Images have been removed"
echo ""
echo_info "Next: Run './docker/scripts/up.sh' to start again"
