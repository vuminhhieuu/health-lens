# Infisical Onboarding

Tham khảo: [Infisical CLI – Initialize project](https://github.com/infisical/cli/blob/main/README.md) (`infisical init` tạo `.infisical.json`).

## 1) Cài Infisical CLI

Chọn **một** cách phù hợp hệ điều hành. Các lệnh dưới đây lấy từ [README chính thức của Infisical CLI](https://github.com/infisical/cli/blob/main/README.md).

### macOS (Homebrew)

```bash
brew install infisical/get-cli/infisical
infisical --version
```

### Windows

**Winget:**

```powershell
winget install infisical
```

**Scoop:**

```powershell
scoop install infisical
```

Sau đó mở terminal mới và kiểm tra:

```powershell
infisical --version
```

### Ubuntu / Debian

```bash
curl -1sLf 'https://artifacts-cli.infisical.com/setup.deb.sh' | sudo -E bash
sudo apt-get update
sudo apt-get install -y infisical
infisical --version
```

### Cách chung: npm (macOS, Windows, Linux)

Package npm chính thức là `@infisical/cli`:

```bash
npm install -g @infisical/cli
infisical --version
```

*(Một số môi trường cũ có thể dùng `npm i -g infisical`; nên ưu tiên `@infisical/cli` theo [npm README của Infisical](https://github.com/infisical/cli/blob/main/npm/README.md).)*

### EU Cloud hoặc self-hosted

Nếu org dùng region EU hoặc server riêng, khi `login` / `run` cần chỉ domain, ví dụ:

```bash
infisical login --domain=https://eu.infisical.com
```

(Xem thêm `infisical login --help`.)

## 2) Đăng nhập

```bash
infisical login
```

## 3) Link repo với project

Tại root repo:

```bash
infisical init
```

Sau bước này phải có file `.infisical.json` ở root.

## 4) Pull secrets về local

```bash
./infisical/scripts/infisical.sh pull-dev
./infisical/scripts/infisical.sh pull-staging-web
./infisical/scripts/infisical.sh pull-staging-api
./infisical/scripts/infisical.sh pull-staging
```

Mặc định script đã dùng folder dev shared (`DEV_PATH=/shared`), nên chỉ cần:

```bash
./infisical/scripts/infisical.sh pull-dev
```

## 5) Chạy ứng dụng local

Bạn có thể chạy app theo flow hiện tại của team sau khi pull env.

## 6) Quy định phân quyền

- Tech lead: push/update secrets.
- Member: pull/read-only.
- Không tự ý ghi đè secret trên cloud nếu không được phân quyền.
- Có thể tạo thêm folder `backup` trong `dev` và chỉ cấp quyền cho tech lead; member chỉ dùng folder shared (`/shared`).
- Khi cần thao tác backup riêng, override:
  ```bash
  DEV_PATH=/backup ./infisical/scripts/infisical.sh push-dev
  DEV_PATH=/backup ./infisical/scripts/infisical.sh pull-dev
  ```

## 7) Khi có thông báo thay đổi ENV

1. Pull lại env tương ứng:
   ```bash
   ./infisical/scripts/infisical.sh pull-dev
   ```
2. Restart service local.

## 8) Troubleshooting nhanh

- Lỗi `Environment ... not found`:
  - Kiểm tra slug env đúng (`dev`, `staging`, `prod`).
- Lỗi `Folder with path ... not found`:
  - Chạy:
    ```bash
    ./infisical/scripts/infisical.sh bootstrap
    ```
- Lỗi empty value khi push:
  - `push-dev` tự bỏ qua key rỗng.
