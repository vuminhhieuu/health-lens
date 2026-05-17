# Staging Deployment Guide

Hướng dẫn deploy HealthLens lên môi trường staging với:
- **Vercel**: Web Frontend (Next.js)
- **Render**: API Backend (Spring Boot) + OCR Service (FastAPI)
- **Neon**: PostgreSQL Database
- **Upstash**: Redis Cache
- **Cloudflare R2**: File Storage

---

## 1. Tạo tài khoản và dịch vụ

### 1.1 Neon PostgreSQL
1. Truy cập https://neon.tech
2. Đăng ký tài khoản (có free tier 0.5GB)
3. Tạo project mới: `healthlens-staging`
4. Copy connection string:
   ```
   postgresql://user:password@ep-xxx-xxx-123456.us-east-2.aws.neon.tech/healthlens_staging
   ```

### 1.2 Upstash Redis
1. Truy cập https://upstash.com
2. Sign up với GitHub
3. Redis → Create Database → Region: Singapore
4. Copy connection string

### 1.3 Cloudflare R2 (Storage)
1. Truy cập https://dash.cloudflare.com
2. Sign up / Login (không cần credit card)
3. R2 → Create Bucket → Name: `healthlens-staging`
4. Settings → Create API Token:
   - Permitted: Read, Write, Delete
5. Copy: Account ID, Access Key ID, Secret Access Key

### 1.4 Resend (Email)
1. Truy cập https://resend.com
2. Tạo API key mới
3. Verify domain hoặc dùng test inbox

### 1.5 Render
1. Truy cập https://render.com
2. Sign up với GitHub

### 1.6 Vercel
1. Truy cập https://vercel.com
2. Đăng nhập với GitHub
3. Import repo: `health-lens`

---

## 2. Deploy OCR Service (Render)

### 2.1 Tạo Web Service
1. Render Dashboard → **New** → **Web Service**
2. Connect GitHub repo: `health-lens`
3. Root Directory: `services/ocr-service`
4. Region: Singapore

### 2.2 Cấu hình Build
| Setting | Value |
|---------|-------|
| Runtime | **Python 3.11** |
| Build Command | `pip install -r requirements.txt` |
| Start Command | `uvicorn app:app --host 0.0.0.0 --port $PORT` |
| Plan | Free |

### 2.3 Environment Variables
```
APP_ENV=staging
PYTHONUNBUFFERED=1
LOG_LEVEL=INFO
```

### 2.4 Health Check
- Path: `/health`

### 2.5 Deploy
1. Click **Create Web Service**
2. Chờ build hoàn tất (~2-3 phút lần đầu do download OCR model)
3. Copy URL: `https://healthlens-ocr.onrender.com`

---

## 3. Deploy API (Render)

### 3.1 Tạo Web Service
1. Render Dashboard → **New** → **Web Service**
2. Connect GitHub repo: `health-lens`
3. Root Directory: `apps/api` ← **IMPORTANT**
4. Dockerfile Path: `Dockerfile` (auto-detect)
5. Region: Singapore

### 3.2 Cấu hình Build
| Setting | Value |
|---------|-------|
| Runtime | **Docker** |
| Plan | Free |

### 3.3 Environment Variables
```bash
# === Required ===
SPRING_PROFILES_ACTIVE=staging
APP_ENV=staging

# === Database (Neon) ===
DB_URL=postgresql://user:password@ep-xxx.us-east-2.aws.neon.tech/healthlens_staging?sslmode=require

# === Redis (Upstash) ===
REDIS_HOST=xxx.upstash.io
REDIS_PORT=6379
REDIS_PASSWORD=xxx

# === Email (Resend) ===
RESEND_API_KEY=re_xxxxx
MAIL_ENABLED=true
MAIL_DRIVER=resend
MAIL_FROM=noreply@healthlens.vn

# === URLs ===
WEB_VERIFY_URL=https://healthlens-staging.vercel.app/auth/verify-email
WEB_RESET_PASSWORD_URL=https://healthlens-staging.vercel.app/auth/reset-password
WEB_LOGIN_URL=https://healthlens-staging.vercel.app/auth/login

# === Storage (Cloudflare R2) ===
MINIO_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
MINIO_ACCESS_KEY=<your-r2-access-key>
MINIO_SECRET_KEY=<your-r2-secret-key>
MINIO_BUCKET=healthlens-staging

# === AI/Vector ===
QDRANT_HOST=xxx.qdrant.io
QDRANT_PORT=6333
QDRANT_API_KEY=xxx
QDRANT_COLLECTION=healthlens_staging
AI_CHAT_PROVIDER=openai-compatible
AI_CHAT_API_KEY=gsk_xxx
AI_CHAT_BASE_URL=https://api.groq.com/openai
AI_CHAT_MODEL=qwen-2.5-72b-versatile
AI_CHAT_TIMEOUT_MS=30000

# === Security ===
JWT_SECRET=your-32-char-minimum-secret-key-here
APP_COOKIE_SECURE=true

# === OCR (từ bước 2) ===
OCR_SERVICE_URL=https://healthlens-ocr.onrender.com
OCR_API_KEY=xxx
OCR_TIMEOUT_MS=30000

# === Logging ===
LOG_LEVEL=INFO
```

### 3.4 Health Check
- Path: `/actuator/health/liveness`

### 3.5 Deploy
1. Click **Create Web Service**
2. Chờ build hoàn tất (~5-10 phút lần đầu)
3. Copy URL: `https://healthlens-api.onrender.com`

---

## 4. Deploy Web (Vercel)

### 4.1 Import Project
1. Vào https://vercel.com/new
2. Import `health-lens` repo
3. Root Directory: `apps/web`
4. Framework: Next.js

### 4.2 Environment Variables
```bash
NODE_ENV=staging
NEXT_PUBLIC_APP_ENV=staging
NEXT_PUBLIC_API_BASE_URL=https://healthlens-api.onrender.com
NEXT_PUBLIC_OCR_SERVICE_URL=https://healthlens-ocr.onrender.com
NEXT_PUBLIC_STORAGE_BUCKET=healthlens-staging
NEXT_PUBLIC_STORAGE_ENDPOINT=<account-id>.r2.cloudflarestorage.com
NEXT_PUBLIC_FLAG_AI_ANALYSIS=true
NEXT_PUBLIC_FLAG_OCR=true
NEXT_PUBLIC_FLAG_DARK_MODE=true
NEXT_PUBLIC_FLAG_NOTIFICATIONS=true
```

### 4.3 Deploy
1. Click **Deploy**
2. Chờ build hoàn tất

---

## 5. Kiểm tra sau Deploy

### 5.1 Health Checks
```bash
# API
curl https://healthlens-api.onrender.com/actuator/health/liveness

# OCR
curl https://healthlens-ocr.onrender.com/health

# Web
curl -I https://healthlens-staging.vercel.app
```

### 5.2 URLs Staging

| Service | Platform | URL |
|---------|----------|-----|
| Web | Vercel | https://healthlens-staging.vercel.app |
| API | Render | https://healthlens-api.onrender.com |
| OCR | Render | https://healthlens-ocr.onrender.com |
| API Docs | Render | https://healthlens-api.onrender.com/swagger-ui.html |

---

## 6. Troubleshooting

### 6.1 Render Free Tier Auto-Sleep
- Auto-sleep sau 15 phút không có traffic
- Lần đầu wake-up có thể chậm (30-60s)
- OCR model download lần đầu ~2-3 phút

### 6.2 API không connect được Database
```bash
# Kiểm tra DB_URL format:
postgresql://user:password@ep-xxx.us-east-2.aws.neon.tech/healthlens_staging?sslmode=require
```

### 6.3 Redis Connection Failed
```bash
# Kiểm tra REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
```

### 6.4 Storage Upload Failed
```bash
# Kiểm tra MINIO_ENDPOINT format:
https://<account-id>.r2.cloudflarestorage.com
```

### 6.5 Email không gửi được
```bash
# Kiểm tra RESEND_API_KEY
# Kiểm tra domain verification trong Resend
```

---

## 7. Monitoring

### 7.1 Render
- Logs: Render Dashboard → Service → Logs
- Metrics: Render Dashboard → Service → Insights

### 7.2 Vercel
- Logs: Vercel Dashboard → Deployments → Logs

### 7.3 Neon
- Metrics: Neon Dashboard → Metrics

### 7.4 Upstash
- Metrics: Upstash Console → Metrics
