# Infisical Workspace

Folder này chứa tài liệu và script vận hành Infisical cho dự án `health-lens`.

## Mục tiêu

- Quản lý secrets tập trung trên Infisical Cloud.
- Vẫn giữ trải nghiệm local với file `.env` vật lý.
- Chỉ định rõ quyền push/pull cho team.

## Cấu trúc

- `scripts/infisical/infisical.sh`: script all-in-one để push/pull/check/bootstrap.
- `ONBOARDING.md`: hướng dẫn thành viên mới.

## Script chính

Chạy từ root repo:

```bash
./scripts/infisical/infisical.sh help
```

Sau khi pull env, dùng package script ở root để chạy stack local:

```bash
pnpm docker:up
pnpm docker:up:ocr
pnpm docker:down
```

Các lệnh hay dùng:

```bash
# tạo folder staging /web và /api nếu thiếu
./scripts/infisical/infisical.sh bootstrap

# push
./scripts/infisical/infisical.sh push-dev
./scripts/infisical/infisical.sh push-staging-web
./scripts/infisical/infisical.sh push-staging-api
./scripts/infisical/infisical.sh push-staging
./scripts/infisical/infisical.sh push-prod

# pull
./scripts/infisical/infisical.sh pull-dev
./scripts/infisical/infisical.sh pull-staging-web
./scripts/infisical/infisical.sh pull-staging-api
./scripts/infisical/infisical.sh pull-staging
./scripts/infisical/infisical.sh pull-prod

# ví dụ: dùng dev path riêng cho team
DEV_PATH=/shared ./scripts/infisical/infisical.sh push-dev
DEV_PATH=/shared ./scripts/infisical/infisical.sh pull-dev
```

## Mapping mặc định

- `dev:/shared` -> `.env` (mặc định)
- `staging:/web` -> `.env.staging`
- `staging:/api` -> `.env.staging.api`
- `prod:/` -> `.env.production`

Mặc định script dùng `DEV_PATH=/shared`. Khi cần backup riêng, override tạm:

```bash
DEV_PATH=/backup ./scripts/infisical/infisical.sh push-dev
DEV_PATH=/backup ./scripts/infisical/infisical.sh pull-dev
```

## Ghi chú quan trọng

- `.infisical.json` ở repo root là **cấu hình chung** (project id), thường **cả team dùng một file** trong git; **không** phải mỗi người một file cho đăng nhập — phần cá nhân là session/token CLI trên máy từng dev.
- Không commit secret thật vào git.
- Chỉ commit template như `.env.example` và `config/.infisical.json.example`.
