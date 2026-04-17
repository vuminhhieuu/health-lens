#!/usr/bin/env bash
# ============================================
# HealthLens Docker - Start Development
# ============================================
#
# Starts local development environment with all services.
# Automatically enables BuildKit for optimized caching.
#
# Usage:
#   ./up.sh                    # Start all services (detached)
#   ./up.sh --build            # Rebuild all images first (with cache)
#   ./up.sh --rebuild-api      # Rebuild API image only
#   ./up.sh --rebuild-web      # Rebuild Web image only
#   ./up.sh --rebuild-ocr      # Rebuild OCR image only
#   ./up.sh --no-cache         # Force rebuild without cache
#   ./up.sh --ocr              # Include OCR service (~2GB RAM)
#   ./up.sh --foreground       # Stream logs in foreground
#
# Services:
#   - API:      http://localhost:8080
#   - Web:      http://localhost:3000
#   - MinIO:    http://localhost:9001
#   - Mailhog:  http://localhost:8025 (captured SMTP mail)
#   - OCR:      http://localhost:8001 (with --ocr)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$SCRIPTS_DIR")"
cd "$PROJECT_DIR"

# ============================================
# Enable Docker BuildKit for optimized builds
# ============================================
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
export BUILDKIT_PROGRESS=plain

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
BUILD=false
NO_CACHE=false
PROFILES=""
FOREGROUND=false
REBUILD_SERVICES=()

while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            BUILD=true
            shift
            ;;
        --no-cache)
            NO_CACHE=true
            shift
            ;;
        --ocr)
            PROFILES="$PROFILES --profile ocr"
            shift
            ;;
        --foreground)
            FOREGROUND=true
            shift
            ;;
        --rebuild-api)
            REBUILD_SERVICES+=("api")
            shift
            ;;
        --rebuild-web)
            REBUILD_SERVICES+=("web")
            shift
            ;;
        --rebuild-ocr)
            REBUILD_SERVICES+=("ocr")
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --build          Rebuild all images before starting (uses cache)"
            echo "  --rebuild-api    Rebuild API image only"
            echo "  --rebuild-web    Rebuild Web image only"
            echo "  --rebuild-ocr    Rebuild OCR image only"
            echo "  --no-cache       Force rebuild without cache (slow)"
            echo "  --ocr            Include OCR service (~2GB RAM)"
            echo "  --foreground     Stream logs in foreground"
            echo "  --help           Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./up.sh                    # Start with cache"
            echo "  ./up.sh --build            # Rebuild with cache"
            echo "  ./up.sh --rebuild-api      # Rebuild API only"
            echo "  ./up.sh --no-cache         # Force full rebuild"
            exit 0
            ;;
        *)
            echo_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo_header "HealthLens - Development Environment"
echo_info "Running from: $(pwd)"
echo_info "BuildKit: Enabled (faster builds)"
echo_info "Docker directory: docker/"

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo_error "Docker is not running!"
    exit 1
fi

# Check .env file
if [ ! -f ".env" ]; then
    echo_warn ".env file not found!"
    echo_info "Creating from .env.example..."
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo_warn "Please edit .env and add your API keys (Groq, Qdrant)!"
    else
        echo_error ".env.example not found. Cannot create .env"
        exit 1
    fi
else
    echo_info ".env file found"
fi

# Build if requested
if [ ${#REBUILD_SERVICES[@]} -gt 0 ] && [ "$BUILD" = true ]; then
    echo_error "Use either --build or --rebuild-* options, not both."
    exit 1
fi

# Determine build cache option
BUILD_CACHE_FLAG=""
if [ "$NO_CACHE" = true ]; then
    BUILD_CACHE_FLAG="--no-cache"
    echo_warn "Rebuilding without cache (will be slow)..."
fi

echo_header "Building Docker Images"

if [ ${#REBUILD_SERVICES[@]} -gt 0 ]; then
    echo_info "Rebuilding selected service images: ${REBUILD_SERVICES[*]}"
    docker compose -f docker/compose.yml -f docker/compose.dev.yml build $BUILD_CACHE_FLAG "${REBUILD_SERVICES[@]}"
elif [ "$BUILD" = true ]; then
    echo_info "Building all Docker images..."
    docker compose -f docker/compose.yml -f docker/compose.dev.yml build $BUILD_CACHE_FLAG
else
    echo_info "Building all Docker images (with cache)..."
    docker compose -f docker/compose.yml -f docker/compose.dev.yml build
fi

# Start services
echo_header "Starting Services"
echo_info "Starting HealthLens development environment..."

if [ "$FOREGROUND" = true ]; then
    docker compose -f docker/compose.yml -f docker/compose.dev.yml $PROFILES up
else
    docker compose -f docker/compose.yml -f docker/compose.dev.yml $PROFILES up -d
fi

echo_header "Services Ready"
echo "  API:        http://localhost:8080"
echo "  Web:        http://localhost:3000"
echo "  MinIO:      http://localhost:9001"
echo "  Mailhog:    http://localhost:8025"
echo "  PostgreSQL: localhost:5432"
[ "$PROFILES" == *"--profile ocr"* ] && echo "  OCR:        http://localhost:8001"
echo ""

if [ "$FOREGROUND" = true ]; then
    echo_warn "Press Ctrl+C to stop"
else
    echo_info "Use './docker/scripts/logs.sh -f' to follow logs"
    echo_info "Use './docker/scripts/down.sh' to stop services"
fi

echo ""
echo_info "All systems go! 🚀"
