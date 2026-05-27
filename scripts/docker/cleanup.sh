#!/usr/bin/env bash
# ============================================================================
# HealthLens Disk Space Cleanup Script - Enhanced
#
# Purpose: Clean up Docker images, containers, volumes, build caches
#          and dependencies to free up disk space
#
# Usage:
#   ./scripts/docker/cleanup.sh            # Interactive mode (shows what will be deleted)
#   ./scripts/docker/cleanup.sh --force    # Force delete without confirmation
#   ./scripts/docker/cleanup.sh --aggressive # Remove ALL images (including tagged)
#   ./scripts/docker/cleanup.sh --dry-run  # Show what would be deleted (no actual deletion)
#   ./scripts/docker/cleanup.sh --docker-only # Only clean Docker images/containers/volumes
#   ./scripts/docker/cleanup.sh --buildkit # Include BuildKit cache cleanup
#   ./scripts/docker/cleanup.sh --analyze  # Show disk usage analysis
#
# Options:
#   --dry-run           Show what would be deleted without actually deleting
#   --force             Force delete without confirmations
#   --aggressive        Remove ALL unused images (including tagged ones)
#   --docker-only       Only clean Docker-related items
#   --buildkit          Include Docker BuildKit cache cleanup
#   --analyze           Show disk usage breakdown and volume sizes
#   --help              Show this help message
#
# ============================================================================

set -euo pipefail

. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"
hl_docker_cd_project_root

# Flags
FORCE_DELETE=false
AGGRESSIVE=false
DRY_RUN=false
DOCKER_ONLY=false
INCLUDE_BUILDKIT=false
ANALYZE_ONLY=false
SHOW_HELP=false
CI_MODE=false

# Parse arguments
while [ "$#" -gt 0 ]; do
  case $1 in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --force|--yes)
      FORCE_DELETE=true
      shift
      ;;
    --aggressive)
      AGGRESSIVE=true
      shift
      ;;
    --docker-only)
      DOCKER_ONLY=true
      shift
      ;;
    --buildkit)
      INCLUDE_BUILDKIT=true
      shift
      ;;
    --analyze)
      ANALYZE_ONLY=true
      shift
      ;;
    --help|-h)
      SHOW_HELP=true
      shift
      ;;
    --ci)
      CI_MODE=true
      shift
      ;;
    *)
      echo "Unknown option: $1" >&2
      echo "Use --help for usage information" >&2
      exit 2
      ;;
  esac
done

# Show help
if [ "$SHOW_HELP" = true ]; then
  cat << 'HELP_TEXT'
HealthLens Disk Space Cleanup Script - Enhanced

USAGE:
  ./scripts/docker/cleanup.sh [OPTIONS]

OPTIONS:
  --dry-run          Show what would be deleted without actually deleting
  --force, --yes     Force delete without confirmations
  --aggressive       Remove ALL unused images (including tagged ones)
  --docker-only      Only clean Docker-related items
  --buildkit         Include Docker BuildKit cache cleanup
  --analyze          Show disk usage breakdown and volume analysis
  --ci               Disable ANSI formatting for CI logs
  --help             Show this help message

EXAMPLES:
  # Interactive mode (with confirmations)
  ./scripts/docker/cleanup.sh

  # Preview what will be deleted
  ./scripts/docker/cleanup.sh --dry-run

  # Full cleanup without confirmations
  ./scripts/docker/cleanup.sh --force

  # Aggressive cleanup (remove all images)
  ./scripts/docker/cleanup.sh --aggressive --force

  # Docker only (keep project files)
  ./scripts/docker/cleanup.sh --docker-only --force

  # Show what will be deleted and analyze volumes
  ./scripts/docker/cleanup.sh --analyze --docker-only

  # Include BuildKit cache
  ./scripts/docker/cleanup.sh --buildkit --force

CLEANUP OPERATIONS:
  1. Docker System Prune
     - Removes stopped containers, dangling images, unused networks, dangling volumes

  2. Maven Cache (~500MB typical)
     - Clears ~/.m2/repository

  3. Node Modules
     - Removes node_modules directories from project

  4. Build Artifacts
     - Clears target/, dist/, build/ directories

  5. Gradle Cache (~1GB typical)
     - Clears ~/.gradle/caches

  6. BuildKit Cache (optional)
     - Clears Docker build cache

  7. Temporary Files
     - Removes .cache, tmp, temp directories and *.tmp/*.bak files

HELP_TEXT
  exit 0
fi

disable_colors_if_needed
preflight_docker

# Function to ask for confirmation
confirm() {
  if [ "$FORCE_DELETE" = true ] || [ "$DRY_RUN" = true ]; then
    return 0
  fi

  local prompt="$1"
  local response
  read -p "$prompt (y/n) " response
  case "$response" in
    [yY][eE][sS]|[yY])
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

# Header
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}HealthLens Disk Cleanup Script${NC}"
if [ "$DRY_RUN" = true ]; then
  echo -e "${YELLOW}(DRY-RUN MODE - No changes will be made)${NC}"
fi
echo -e "${BLUE}========================================${NC}\n"

# Show current disk usage
echo -e "${YELLOW}Current Disk Usage:${NC}"
df -h / | tail -1
echo ""

# ============================================================================
# DISK ANALYSIS SECTION
# ============================================================================
if [ "$ANALYZE_ONLY" = true ] || [ "$DRY_RUN" = true ]; then
  echo -e "${BLUE}Disk Usage Analysis:${NC}\n"

  # Docker volumes
  echo -e "${CYAN}Docker Volumes:${NC}"
  docker volume ls --format "{{.Name}}" 2>/dev/null | while read -r vol; do
    # Get volume mount point and size
    vol_path=$(docker volume inspect "$vol" --format '{{.Mountpoint}}' 2>/dev/null)
    if [ -d "$vol_path" ]; then
      vol_size=$(get_dir_size "$vol_path")
      size_human=$(format_size "$vol_size")
      echo -e "  ${MAGENTA}$vol${NC}: $size_human ($vol_path)"
    fi
  done
  echo ""

  # Docker images
  echo -e "${CYAN}Docker Images:${NC}"
  docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" 2>/dev/null | tail -n +2 | while read -r repo tag size; do
    echo -e "  ${MAGENTA}$repo:$tag${NC}: $size"
  done
  echo ""

  # Docker containers
  echo -e "${CYAN}Docker Containers:${NC}"
  container_count=$(docker ps -a --format "{{.ID}}" 2>/dev/null | wc -l | tr -d ' ')
  echo -e "  Total containers: $container_count"
  echo ""

  # Cache directories
  echo -e "${CYAN}Cache Directories:${NC}"

  if [ -d ~/.m2/repository ]; then
    m2_size=$(get_dir_size ~/.m2/repository)
    echo -e "  Maven cache: $(format_size "$m2_size")"
  fi

  if [ -d ~/.gradle/caches ]; then
    gradle_size=$(get_dir_size ~/.gradle/caches)
    echo -e "  Gradle cache: $(format_size "$gradle_size")"
  fi

  node_modules_count=$(find "$PROJECT_ROOT" -name node_modules -type d 2>/dev/null | wc -l | tr -d ' ')
  if [ "$node_modules_count" -gt 0 ]; then
    echo -e "  Node modules directories: $node_modules_count found"
  fi

  echo ""
fi

# Exit if analyze only
if [ "$ANALYZE_ONLY" = true ]; then
  exit 0
fi

# ============================================================================
# 1. Docker Cleanup
# ============================================================================
echo -e "${BLUE}1. Docker Cleanup${NC}"
echo "   Removing stopped containers, dangling images, unused networks, dangling volumes..."

if confirm "   Proceed with Docker cleanup?"; then
  if [ "$DRY_RUN" = true ]; then
    echo -e "${GRAY}   [DRY-RUN] Would run: docker system prune -af --volumes${NC}"
    echo -e "${GRAY}   [DRY-RUN] Docker does not support --dry-run for system prune.${NC}"
  else
    RECLAIMED=$(docker system prune -af --volumes 2>&1 | grep "Total reclaimed" || echo "0B")
    echo -e "${GREEN}   ✓ Docker cleanup complete${NC}"
    echo "   $RECLAIMED"
  fi
else
  echo -e "${YELLOW}   ⊘ Skipped${NC}"
fi

# Docker volume analysis
echo -e "\n${BLUE}   Docker Volume Analysis${NC}"
docker volume ls --format "{{.Name}}" 2>/dev/null | while read -r vol; do
  vol_path=$(docker volume inspect "$vol" --format '{{.Mountpoint}}' 2>/dev/null)
  if [ -d "$vol_path" ]; then
    vol_size=$(get_dir_size "$vol_path")
    size_human=$(format_size "$vol_size")
    echo -e "   ${MAGENTA}●${NC} $vol: $size_human"
  fi
done

# Additional aggressive Docker cleanup
if [ "$AGGRESSIVE" = true ]; then
    echo -e "\n${BLUE}   2.1 Aggressive Docker Cleanup${NC}"
    echo "   Removing ALL unused images (including tagged)..."
    if confirm "   Proceed with aggressive cleanup?"; then
      if [ "$DRY_RUN" = true ]; then
        echo -e "${GRAY}   [DRY-RUN] Would run: docker image prune -a --force${NC}"
      else
        docker image prune -a --force 2>&1 | tail -3
        echo -e "${GREEN}   ✓ Aggressive cleanup complete${NC}"
      fi
    fi
fi

# BuildKit cache cleanup
if [ "$INCLUDE_BUILDKIT" = true ]; then
    echo -e "\n${BLUE}   2.2 Docker BuildKit Cache Cleanup${NC}"
    echo "   Clearing Docker build cache..."
    if confirm "   Proceed?"; then
      if [ "$DRY_RUN" = true ]; then
        echo -e "${GRAY}   [DRY-RUN] Would run: docker builder prune -a --all --force${NC}"
      else
        docker builder prune -a --all --force 2>&1 | tail -3
        echo -e "${GREEN}   ✓ BuildKit cache cleanup complete${NC}"
      fi
    fi
fi

echo ""

# Only continue with other cleanups if not docker-only
if [ "$DOCKER_ONLY" = false ]; then

  # ============================================================================
  # 2. Maven Cache Cleanup
  # ============================================================================
  echo -e "${BLUE}2. Maven Cache Cleanup${NC}"

  if [ -d ~/.m2/repository ]; then
    M2_SIZE=$(du -sh ~/.m2/repository 2>/dev/null | cut -f1)
    echo "   Current Maven cache size: ${MAGENTA}$M2_SIZE${NC}"
    echo "   Removing ~/.m2/repository..."

    if confirm "   Proceed with Maven cache cleanup?"; then
      if [ "$DRY_RUN" = true ]; then
        echo -e "${GRAY}   [DRY-RUN] Would delete: ~/.m2/repository${NC}"
      else
        rm -rf ~/.m2/repository
        echo -e "${GREEN}   ✓ Maven cache removed${NC}"
      fi
    else
      echo -e "${YELLOW}   ⊘ Skipped${NC}"
    fi
  else
    echo -e "${GREEN}   ✓ No Maven cache found${NC}"
  fi

  echo ""

  # ============================================================================
  # 3. Node Modules Cleanup
  # ============================================================================
  echo -e "${BLUE}3. Node Modules Cleanup${NC}"
  echo "   Finding and removing node_modules directories..."

  NODE_MODULES_COUNT=$(find "$PROJECT_ROOT" -name node_modules -type d 2>/dev/null | wc -l | tr -d ' ')
  if [ "$NODE_MODULES_COUNT" -gt 0 ]; then
    echo "   Found ${MAGENTA}$NODE_MODULES_COUNT${NC} node_modules directories"

    if confirm "   Proceed with removal?"; then
      if [ "$DRY_RUN" = true ]; then
        echo -e "${GRAY}   [DRY-RUN] Would delete $NODE_MODULES_COUNT directories${NC}"
      else
        find "$PROJECT_ROOT" -name node_modules -type d -exec rm -rf {} + 2>/dev/null
        echo -e "${GREEN}   ✓ All node_modules removed${NC}"
      fi
    else
      echo -e "${YELLOW}   ⊘ Skipped${NC}"
    fi
  else
    echo -e "${GREEN}   ✓ No node_modules found${NC}"
  fi

  echo ""

  # ============================================================================
  # 4. Build Artifacts Cleanup
  # ============================================================================
  echo -e "${BLUE}4. Build Artifacts Cleanup${NC}"
  echo "   Removing build directories (target/, dist/, build/)..."

  if confirm "   Proceed?"; then
    if [ "$DRY_RUN" = true ]; then
      echo -e "${GRAY}   [DRY-RUN] Would delete: target/, dist/, build/ directories${NC}"
    else
      find "$PROJECT_ROOT" -type d \( -name target -o -name dist -o -name build \) -exec rm -rf {} + 2>/dev/null
      echo -e "${GREEN}   ✓ Build artifacts cleaned${NC}"
    fi
  else
    echo -e "${YELLOW}   ⊘ Skipped${NC}"
  fi

  echo ""

  # ============================================================================
  # 5. Gradle Cache Cleanup
  # ============================================================================
  echo -e "${BLUE}5. Gradle Cache Cleanup${NC}"

  if [ -d ~/.gradle/caches ]; then
    GRADLE_SIZE=$(du -sh ~/.gradle/caches 2>/dev/null | cut -f1)
    echo "   Current Gradle cache size: ${MAGENTA}$GRADLE_SIZE${NC}"

    if confirm "   Proceed with Gradle cache cleanup?"; then
      if [ "$DRY_RUN" = true ]; then
        echo -e "${GRAY}   [DRY-RUN] Would delete: ~/.gradle/caches${NC}"
      else
        rm -rf ~/.gradle/caches
        echo -e "${GREEN}   ✓ Gradle cache removed${NC}"
      fi
    else
      echo -e "${YELLOW}   ⊘ Skipped${NC}"
    fi
  else
    echo -e "${GREEN}   ✓ No Gradle cache found${NC}"
  fi

  echo ""

  # ============================================================================
  # 6. Temporary Files Cleanup
  # ============================================================================
  echo -e "${BLUE}6. Temporary Files Cleanup${NC}"
  echo "   Removing temporary and cache files..."

  if confirm "   Proceed?"; then
    if [ "$DRY_RUN" = true ]; then
      echo -e "${GRAY}   [DRY-RUN] Would delete: .cache/, tmp/, temp/, *.tmp, *.bak files${NC}"
    else
      find "$PROJECT_ROOT" -type d \( -name .cache -o -name tmp -o -name temp \) -exec rm -rf {} + 2>/dev/null
      find "$PROJECT_ROOT" -type f \( -name "*.tmp" -o -name "*.bak" \) -delete 2>/dev/null
      echo -e "${GREEN}   ✓ Temporary files removed${NC}"
    fi
  else
    echo -e "${YELLOW}   ⊘ Skipped${NC}"
  fi

  echo ""
fi

# ============================================================================
# Final Summary
# ============================================================================
echo -e "${BLUE}========================================${NC}"
if [ "$DRY_RUN" = true ]; then
  echo -e "${YELLOW}Dry-Run Complete!${NC}"
else
  echo -e "${GREEN}Cleanup Complete!${NC}"
fi
echo -e "${BLUE}========================================${NC}\n"

echo -e "${YELLOW}Final Disk Usage:${NC}"
df -h / | tail -1
echo ""

if [ "$DRY_RUN" = true ]; then
  echo -e "${YELLOW}Note: This was a dry-run. To actually delete files, run without --dry-run flag${NC}"
  echo ""
fi

echo -e "${GREEN}✓ Next steps:${NC}"
echo "   1. Rebuild Docker images: ./scripts/docker/up.sh --build"
echo "   2. Or reinstall dependencies:"
echo "      - Frontend: cd apps/web && pnpm install"
echo "      - Backend: cd apps/api && ./gradlew build"
echo ""
