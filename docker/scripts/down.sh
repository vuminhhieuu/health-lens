#!/usr/bin/env bash
# ============================================
# HealthLens Docker - Stop Development
# ============================================
#
# Stops and cleans up Docker containers.
#
# Usage:
#   ./down.sh           # Stop containers
#   ./down.sh -v        # Stop and remove volumes
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
NC='\033[0m'

echo_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
echo_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
echo_error() { echo -e "${RED}[ERROR]${NC} $1"; }

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
            exit 0
            ;;
        *)
            echo_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo_info "Stopping HealthLens containers..."

# Remove orphan containers (created manually, not by compose)
for container in healthlens-api-dev healthlens-web-dev healthlens-mailhog; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo_info "Removing orphan container: $container"
        docker rm -f "$container" 2>/dev/null || true
    fi
done

# Stop containers via compose
docker compose -f docker/compose.yml -f docker/compose.dev.yml down

# Remove volumes if requested
if [ "$REMOVE_VOLUMES" = true ]; then
    echo_warn "Removing volumes..."
    docker compose -f docker/compose.yml -f docker/compose.dev.yml down -v
fi

# Remove images if requested
if [ "$REMOVE_IMAGES" = true ]; then
    echo_warn "Removing images..."
    docker images | grep healthlens | awk '{print $3}' | xargs -r docker rmi -f 2>/dev/null || true
fi

echo_info "Cleanup complete!"
