#!/usr/bin/env bash
# ============================================
# HealthLens Docker - View Logs
# ============================================
#
# View logs from running containers.
#
# Usage:
#   ./logs.sh              # View all logs
#   ./logs.sh api          # View API logs only
#   ./logs.sh web -f       # Follow web logs

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$SCRIPTS_DIR")"
cd "$PROJECT_DIR"

docker compose -f docker/compose.yml -f docker/compose.dev.yml logs "$@"
