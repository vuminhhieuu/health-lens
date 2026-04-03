# HealthLens Web - Next.js
FROM node:20-alpine AS base

FROM base AS deps
WORKDIR /app

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY packages/shared/package.json ./packages/shared/
COPY apps/web/package.json ./apps/web/

RUN npm install -g pnpm@9 && pnpm install --frozen-lockfile

FROM base AS builder
WORKDIR /app

RUN npm install -g pnpm@9

# Copy toàn bộ workspace đã install từ deps (bao gồm node_modules của từng package)
# Dùng COPY /app thay vì chỉ /app/node_modules để giữ nguyên pnpm workspace structure
COPY --from=deps /app ./
COPY . .

RUN pnpm --filter web build

FROM base AS runner
WORKDIR /app

ENV NODE_ENV=production

RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

COPY --from=builder /app/apps/web/public ./apps/web/public
COPY --from=builder --chown=nextjs:nodejs /app/apps/web/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/apps/web/.next/static ./apps/web/.next/static

USER nextjs

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Trong monorepo, next standalone tạo server.js tại apps/web/server.js
CMD ["node", "apps/web/server.js"]
