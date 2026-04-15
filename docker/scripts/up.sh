#!/usr/bin/env bash
# ============================================
# HealthLens Docker - Start Development
# ============================================
#
# Starts local development environment with all services.
#
# Usage:
#   ./up.sh              # Start with default settings
#   ./up.sh --build     # Rebuild images first
#   ./up.sh --mail      # Include Mailhog
#
# Services:
#   - API:      http://localhost:8080
#   - Web:      http://localhost:3000
#   - MinIO:    http://localhost:9001
#   - Mailhog:  http://localhost:8025 (with --mail)

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
BUILD=false
PROFILES=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            BUILD=true
            shift
            ;;
        --mail)
            PROFILES="$PROFILES --profile mail"
            shift
            ;;
        --ocr)
            PROFILES="$PROFILES --profile ocr"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --build    Rebuild images before starting"
            echo "  --mail     Include Mailhog email testing"
            echo "  --ocr      Include OCR service (~2GB RAM)"
            echo "  --help     Show this help message"
            exit 0
            ;;
        *)
            echo_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo_info "Running from: $(pwd)"
echo_info "Docker directory: docker/"

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
if [ "$BUILD" = true ]; then
    echo_info "Building Docker images (no cache)..."
    docker compose -f docker/compose.yml -f docker/compose.dev.yml build --no-cache
else
    echo_info "Building Docker images..."
    docker compose -f docker/compose.yml -f docker/compose.dev.yml build
fi

# Start services
echo_info "Starting HealthLens development environment..."
docker compose -f docker/compose.yml -f docker/compose.dev.yml $PROFILES up

echo ""
echo_info "Services starting..."
echo "  - API:      http://localhost:8080"
echo "  - Web:      http://localhost:3000"
echo "  - MinIO:    http://localhost:9001"
echo "  - PostgreSQL: localhost:5432"
echo "  - Mailhog:  http://localhost:8025"
[ "$PROFILES" == *"--profile ocr"* ] && echo "  - OCR:      http://localhost:8001"
echo ""
echo_warn "Press Ctrl+C to stop"
