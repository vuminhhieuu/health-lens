# Staging Deployment Guide

Hướng dẫn deploy HealthLens lên môi trường staging với:
- **Vercel**: Web Frontend (Next.js)
- **Render**: API Backend (Spring Boot) + OCR Service
- **Neon**: PostgreSQL Database
- **MinIO Cloud / AWS S3**: File Storage

---

## 1. Tạo tài khoản và dịch vụ

### 1.1 Neon PostgreSQL
```bash
# 1. Truy cập https://neon.tech
# 2. Đăng ký tài khoản (có free tier 0.5GB)
# 3. Tạo project mới: healthlens-staging
# 4. Copy connection string:
#    postgresql://user:password@ep-xxx-xxx-123456.us-east-2.aws.neon.tech/healthlens_staging
```

### 1.2 Render
```bash
# 1. Truy cập https://render.com
# 2. Đăng nhập với GitHub
# 3. Tạo Web Service mới
```

### 1.3 Vercel
```bash
# 1. Truy cập https://vercel.com
# 2. Đăng nhập với GitHub
# 3. Import repo: health-lens
```

### 1.4 Resend (Email)
```bash
# 1. Truy cập https://resend.com
# 2. Tạo API key mới
# 3. Verify domain hoặc dùng test inbox
```

---

## 2. Cấu hình Render - API

### 2.1 Tạo Web Service
```
1. Dashboard → New → Web Service
2. Connect GitHub repo
3. Root Directory: apps/api
4. Region: Singapore
5. Branch: staging
6. Runtime: Docker
7. Dockerfile Path: apps/api/Dockerfile (auto-detect)
```

### 2.2 Environment Variables (trong Render Dashboard)
```bash
# === Required ===
SPRING_PROFILES_ACTIVE=staging
DB_URL=postgresql://user:pass@ep-xxx.us-east-2.aws.neon.tech/healthlens_staging?sslmode=require
REDIS_URL=rediss://user:pass@xxx-12345.upstash.io:6379
RESEND_API_KEY=re_xxxxx

# === URLs ===
WEB_BASE_URL=https://healthlens-staging.vercel.app

# === Storage ===
MINIO_ACCESS_KEY=xxx
MINIO_SECRET_KEY=xxx
MINIO_ENDPOINT=https://s3.ap-southeast-1.amazonaws.com
MINIO_BUCKET=healthlens-staging

# === AI/Vector ===
QDRANT_HOST=xxx-xxx-12345.us-east-2-0.aws.cloud.qdrant.io
QDRANT_API_KEY=xxx
QDRANT_COLLECTION=healthlens_staging
GROQ_API_KEY=gsk_xxx

# === Security ===
JWT_SECRET=your-32-char-minimum-secret-key-here

# === OCR ===
OCR_SERVICE_URL=https://healthlens-ocr.onrender.com

# === Logging ===
LOG_LEVEL=INFO
```

### 2.3 Health Check
```
Path: /actuator/health
Port: 8080
```

### 2.4 Plan: Starter (Free)
- 512MB RAM
- 0.5 CPU
- Sleeps after 15 min inactivity
- Cold start ~30s
MINIO_SECRET_KEY=xxx
MINIO_ENDPOINT=https://s3.amazonaws.com
MINIO_BUCKET=healthlens-staging
QDRANT_HOST=xxx.qdrant.io
QDRANT_API_KEY=xxx
QDRANT_COLLECTION=healthlens_staging
GROQ_API_KEY=gsk_xxx
JWT_SECRET=your-32-char-secret
OCR_SERVICE_URL=https://healthlens-ocr.up.railway.app
LOG_LEVEL=INFO
```

### 2.2 Redis
```bash
# Trong Railway:
# 1. Add New Service → "Database" → "Redis"
# 2. Copy REDIS_URL từ connection string
```

### 2.3 OCR Service
```bash
# Trong Railway:
# 1. Add New Service → "Empty Service"
# 2. Connect GitHub repo
# 3. Set root directory: services/ocr-service
# 4. Deploy sẽ tự động
```

---

## 3. Cấu hình Vercel

### 3.1 Import Project
```bash
# 1. Vào https://vercel.com/new
# 2. Import health-lens repo
# 3. Root Directory: apps/web
# 4. Build Command: pnpm build
# 5. Output Directory: .next (default)
```

### 3.2 Environment Variables (Staging)
```bash
# Trong Vercel Dashboard → Settings → Environment Variables:
NODE_ENV=staging
NEXT_PUBLIC_APP_ENV=staging
NEXT_PUBLIC_API_BASE_URL=https://healthlens-api-staging.up.railway.app
NEXT_PUBLIC_OCR_SERVICE_URL=https://healthlens-ocr-staging.up.railway.app
NEXT_PUBLIC_STORAGE_BUCKET=healthlens-staging
NEXT_PUBLIC_STORAGE_ENDPOINT=storage.staging.healthlens.vn
NEXT_PUBLIC_FLAG_AI_ANALYSIS=true
NEXT_PUBLIC_FLAG_OCR=true
NEXT_PUBLIC_FLAG_DARK_MODE=true
NEXT_PUBLIC_FLAG_NOTIFICATIONS=true
```

### 3.3 Branch Configuration
```bash
# Tạo branch staging từ dev:
git checkout dev
git checkout -b staging
git push origin staging

# Vercel sẽ auto-deploy branch 'staging'
```

---

## 4. Storage Setup (MinIO Cloud / AWS S3)

### 4.1 MinIO Cloud (Recommended for staging)
```bash
# 1. Truy cập https://min.io/cloud
# 2. Sign up với GitHub
# 3. Tạo free tier deployment
# 4. Tạo bucket: healthlens-staging
# 5. Tạo access key
```

### 4.2 AWS S3 (Alternative)
```bash
# 1. AWS Console → S3 → Create bucket
# 2. Bucket name: healthlens-staging
# 3. Region: ap-southeast-1
# 4. Enable public access nếu cần
# 5. IAM → Create access key
```

---

## 5. Deploy thứ tự

### 5.1 Bước 1: Database Migration
```bash
# Kết nối Neon với Railway và chạy Flyway:
# Railway sẽ tự động chạy migration khi API start
```

### 5.2 Bước 2: Deploy API
```bash
# 1. Railway → API Service → Deploy
# 2. Chờ health check pass
# 3. Copy URL: https://healthlens-api-staging.up.railway.app
```

### 5.3 Bước 3: Deploy OCR
```bash
# 1. Railway → OCR Service → Deploy
# 2. Copy URL: https://healthlens-ocr-staging.up.railway.app
```

### 5.4 Bước 4: Deploy Web
```bash
# 1. Vercel → Deployments → New Deployment
# 2. Select branch: staging
# 3. Environment: Production (vì đây là staging)
# 4. Wait for build...
```

---

## 6. Kiểm tra sau Deploy

### 6.1 Health Checks
```bash
# API
curl https://healthlens-api-staging.up.railway.app/actuator/health

# OCR
curl https://healthlens-ocr-staging.up.railway.app/health

# Web
curl -I https://healthlens-staging.vercel.app
```

### 6.2 Test Integration
```bash
# Kiểm tra web gọi được API
curl https://healthlens-staging.vercel.app/api/v1/health
# (nếu có public endpoint)
```

---

## 7. Troubleshooting

### 7.1 API không connect được Database
```bash
# Kiểm tra NEON_DATABASE_URL format:
postgresql://user:password@ep-xxx.us-east-2.aws.neon.tech/healthlens_staging?sslmode=require
```

### 7.2 Redis Connection Failed
```bash
# Kiểm tra REDIS_URL:
redis://default:password@host:port
```

### 7.3 Email không gửi được
```bash
# Kiểm tra RESEND_API_KEY
# Kiểm tra domain verification trong Resend
```

### 7.4 Web 500 Error
```bash
# Kiểm tra NEXT_PUBLIC_API_BASE_URL có đúng không
# Kiểm tra CORS config trong API
```

---

## 8. URLs Staging

| Service | URL |
|---------|-----|
| Web | https://healthlens-staging.vercel.app |
| API | https://healthlens-api-staging.up.railway.app |
| OCR | https://healthlens-ocr-staging.up.railway.app |
| API Docs | https://healthlens-api-staging.up.railway.app/swagger-ui.html |
| MinIO Console | https://console.min.io |

---

## 9. Monitoring

### 9.1 Railway
- Logs: Railway Dashboard → Service → Logs
- Metrics: Railway Dashboard → Service → Metrics

### 9.2 Vercel
- Logs: Vercel Dashboard → Deployments → Logs
- Functions: Vercel Dashboard → Functions

### 9.3 Neon
- Metrics: Neon Dashboard → Metrics
- Logs: Neon Dashboard → Logs
