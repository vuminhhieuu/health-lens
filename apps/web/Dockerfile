# ============================================
# HealthLens Web - Next.js Dockerfile
# Multi-stage build with optimized layer caching
# ============================================

# ============================================
# Stage 1: Dependencies layer (pnpm install)
# ============================================
FROM node:20-alpine AS deps

WORKDIR /app

# Install pnpm globally
RUN corepack enable && corepack prepare pnpm@9

# Copy only dependency files (smallest layer for cache hit)
COPY pnpm-workspace.yaml ./
COPY pnpm-lock.yaml ./
COPY package.json ./
COPY packages/shared/package.json ./packages/shared/
COPY apps/web/package.json ./apps/web/

# Install all dependencies at once (cached layer)
RUN pnpm install --frozen-lockfile --prefer-offline

# ============================================
# Stage 2: Builder
# ============================================
FROM deps AS builder

WORKDIR /app

# Copy source code AFTER dependency installation
COPY packages/shared ./packages/shared
COPY apps/web ./apps/web

# Build shared package (if needed for types)
WORKDIR /app/packages/shared
RUN pnpm build

# Build Next.js app with cache optimization
WORKDIR /app/apps/web
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm build

# ============================================
# Stage 3: Runtime (production minimal image)
# ============================================
FROM node:20-alpine

WORKDIR /app

# Install minimal runtime dependencies
RUN apk add --no-cache curl && \
    addgroup -g 1001 -S nodejs && \
    adduser -u 1001 -S nextjs -G nodejs

# Copy only production artifacts
COPY --from=builder /app/apps/web/.next/standalone ./
COPY --from=builder /app/apps/web/.next/static ./apps/web/.next/static
COPY --from=builder /app/apps/web/public ./apps/web/public

# Set ownership
RUN chown -R nextjs:nodejs /app

USER nextjs

EXPOSE 3000
ENV PORT=3000 \
    NODE_ENV=production

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:3000/ || exit 1

CMD ["node", "apps/web/server.js"]
