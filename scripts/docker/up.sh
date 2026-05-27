#!/usr/bin/env bash
# ============================================
# HealthLens Docker - Start Development
# ============================================
#
# Starts local development environment with all services.
# Automatically enables BuildKit for optimized caching.
#
# Usage:
#   ./scripts/docker/up.sh                    # Start all services (detached)
#   ./scripts/docker/up.sh --build            # Rebuild all images first (with cache)
#   ./scripts/docker/up.sh --rebuild-api      # Rebuild API image only
#   ./scripts/docker/up.sh --rebuild-web      # Rebuild Web image only
#   ./scripts/docker/up.sh --rebuild-ocr      # Rebuild OCR image only
#   ./scripts/docker/up.sh --no-cache         # Force rebuild without cache
#   ./scripts/docker/up.sh --ocr              # Include OCR service (~2GB RAM)
#   ./scripts/docker/up.sh --foreground       # Stream logs in foreground
#
# Services:
#   - API:      http://localhost:8080
#   - Web:      http://localhost:3000
#   - MinIO:    http://localhost:9001
#   - Mailhog:  http://localhost:8025 (captured SMTP mail)
#   - OCR:      http://localhost:8001 (with --ocr)

set -euo pipefail

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"
hl_docker_cd_project_root

# ============================================
# Enable Docker BuildKit for optimized builds
# ============================================
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
export BUILDKIT_PROGRESS=plain

CI_MODE=false

# Parse arguments
BUILD=false
NO_CACHE=false
ENABLE_OCR=false
FOREGROUND=false
REBUILD_SERVICES=()
START_TS="$(date +%s)"
BUILD_START_TS=0
BUILD_END_TS=0
UP_START_TS=0
UP_END_TS=0

while [ "$#" -gt 0 ]; do
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
            ENABLE_OCR=true
            shift
            ;;
        --foreground)
            FOREGROUND=true
            shift
            ;;
        --ci)
            CI_MODE=true
            disable_colors_if_needed
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
            echo "  --ci             Disable ANSI formatting for CI logs"
            echo "  --help           Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./scripts/docker/up.sh                    # Start with cache"
            echo "  ./scripts/docker/up.sh --build            # Rebuild with cache"
            echo "  ./scripts/docker/up.sh --rebuild-api      # Rebuild API only"
            echo "  ./scripts/docker/up.sh --no-cache         # Force full rebuild"
            exit 0
            ;;
        *)
            echo_error "Unknown option: $1"
            exit 2
            ;;
    esac
done

disable_colors_if_needed

echo_header "HealthLens - Development Environment"
echo_info "Running from: $(pwd)"
echo_info "BuildKit: Enabled (faster builds)"
echo_info "Docker directory: docker/"

# Preflight checks
preflight_docker

# Check .env file
if [ ! -f ".env" ]; then
    echo_warn ".env file not found!"
    echo_info "Creating from .env.example..."
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo_warn "Please edit .env and add your API keys (AI_CHAT_*, Qdrant)!"
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
    exit 2
fi
if [ "$NO_CACHE" = true ] && [ "$BUILD" = false ] && [ ${#REBUILD_SERVICES[@]} -eq 0 ]; then
    echo_error "--no-cache requires --build or a --rebuild-* option."
    exit 2
fi

# Determine build cache option
BUILD_CACHE_FLAG=""
if [ "$NO_CACHE" = true ]; then
    BUILD_CACHE_FLAG="--no-cache"
    echo_warn "Rebuilding without cache (will be slow)..."
fi

if [ ${#REBUILD_SERVICES[@]} -gt 0 ] || [ "$BUILD" = true ]; then
    BUILD_START_TS="$(date +%s)"
    echo_header "Building Docker Images"
    echo_info "[1/3] Build phase"
    if [ ${#REBUILD_SERVICES[@]} -gt 0 ]; then
        echo_info "Rebuilding selected service images: ${REBUILD_SERVICES[*]}"
        docker compose -f docker/compose.yml -f docker/compose.dev.yml build $BUILD_CACHE_FLAG "${REBUILD_SERVICES[@]}"
    else
        echo_info "Building all Docker images..."
        docker compose -f docker/compose.yml -f docker/compose.dev.yml build $BUILD_CACHE_FLAG
    fi
    BUILD_END_TS="$(date +%s)"
else
    echo_info "Skipping image build (fast start). Use --build to rebuild images."
fi

# Start services
echo_header "Starting Services"
echo_info "[2/3] Startup phase"
echo_info "Starting HealthLens development environment..."
UP_START_TS="$(date +%s)"

compose_up_args=(-f docker/compose.yml -f docker/compose.dev.yml)
if [ "$ENABLE_OCR" = true ]; then
    compose_up_args+=(--profile ocr)
fi

if [ "$FOREGROUND" = true ]; then
    docker compose "${compose_up_args[@]}" up
else
    docker compose "${compose_up_args[@]}" up -d
fi
UP_END_TS="$(date +%s)"

echo_header "Services Ready"
echo_info "[3/3] Summary phase"
echo "  API:        http://localhost:8080"
echo "  Web:        http://localhost:3000"
echo "  MinIO:      http://localhost:9001"
echo "  Mailhog:    http://localhost:8025"
echo "  PostgreSQL: localhost:5432"
[ "$ENABLE_OCR" = true ] && echo "  OCR:        http://localhost:8001"
echo ""

if [ "$FOREGROUND" = true ]; then
    echo_warn "Press Ctrl+C to stop"
else
    echo_info "Use './scripts/docker/logs.sh -f' to follow logs"
    echo_info "Use './scripts/docker/down.sh' to stop services"
fi

echo ""
echo_info "All systems go!"

TOTAL_END_TS="$(date +%s)"
if [ "$BUILD_START_TS" -gt 0 ]; then
    BUILD_DURATION="$((BUILD_END_TS - BUILD_START_TS))"
else
    BUILD_DURATION=-1
fi
UP_DURATION="$((UP_END_TS - UP_START_TS))"
TOTAL_DURATION="$((TOTAL_END_TS - START_TS))"

echo_header "Timing Summary"
if [ "$BUILD_DURATION" -ge 0 ]; then
    echo_info "Build: $(format_duration "$BUILD_DURATION")"
else
    echo_info "Build: skipped"
fi
echo_info "Startup: $(format_duration "$UP_DURATION")"
echo_info "Total: $(format_duration "$TOTAL_DURATION")"
