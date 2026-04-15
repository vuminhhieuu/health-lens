# ============================================
# HealthLens Web - Next.js Dockerfile
# ============================================
FROM node:20-alpine

WORKDIR /app

# Install pnpm
RUN corepack enable && corepack prepare pnpm@9

# Copy workspace config and lockfile
COPY pnpm-workspace.yaml ./
COPY pnpm-lock.yaml ./

# Copy all package.json files for dependency resolution
COPY package.json ./
COPY packages/shared/package.json ./packages/shared/
COPY apps/web/package.json ./apps/web/

# Install dependencies (will resolve workspace packages)
RUN pnpm install --frozen-lockfile

# Copy source code
COPY packages/shared ./packages/shared
COPY apps/web ./apps/web

# Build shared package first (creates proper exports)
WORKDIR /app/packages/shared
RUN pnpm build || true

# Build Next.js
WORKDIR /app/apps/web
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm build

# Final stage
FROM node:20-alpine

WORKDIR /app

COPY --from=0 /app/apps/web/.next/standalone ./
COPY --from=0 /app/apps/web/.next/static ./apps/web/.next/static
COPY --from=0 /app/apps/web/public ./apps/web/public

RUN addgroup -g 1001 -S nodejs && adduser -u 1001 -S nextjs -G nodejs
USER nextjs

EXPOSE 3000
ENV PORT=3000
ENV NODE_ENV=production

CMD ["node", "apps/web/server.js"]
