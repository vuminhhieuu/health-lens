# Huong dan handoff cho dev team (HealthLens)

Tai lieu nay giup dev khac vao lam theo cung mot flow, dua tren BMAD artifacts da co san.

> **Cập Nhật 2026-04-01:** Chuyển sang **Option B+ Architecture** - sử dụng cloud services thay vì local infrastructure.

## 1) Tai lieu can doc truoc khi code

- Epic + Story goc: `_bmad-output/planning-artifacts/epics.md`
- Backlog/trang thai sprint: `_bmad-output/implementation-artifacts/sprint-status.yaml`
- Config BMAD: `_bmad/bmm/config.yaml`
- **Architecture:** `_bmad-output/planning-artifacts/architecture.md`
- **Option B+ Research:** `_bmad-output/planning-artifacts/research/technical-option-bplus-feasibility-2026-04-01.md`

## Option B+ Quick Reference

### Cloud Services (Free Tiers)

| Service | Provider | Free Tier | Setup |
|---------|----------|-----------|-------|
| LLM | Groq API | 14.4k req/min | `1-7-groq-api-setup.md` |
| Embeddings | Groq API | 14.4k req/min | Included in 1-7 |
| Vector DB | Qdrant Cloud | 1 GB | `1-8-qdrant-cloud-setup.md` |
| Database | Neon PostgreSQL | 512 MB | Connection string in `.env` |
| OCR | EasyOCR (local) + AWS Textract | Free | `1-9-easyocr-service-setup.md` |

### Environment Setup

```bash
# Copy environment template
cp .env.example .env

# Required environment variables:
# - NEON_DATABASE_URL (PostgreSQL connection)
# - GROQ_API_KEY (LLM + Embeddings)
# - QDRANT_HOST, QDRANT_API_KEY (Vector DB)
# - OCR_SERVICE_URL=http://localhost:8001 (EasyOCR)
```

### Docker Services

```bash
# Start local services (EasyOCR + MinIO)
docker-compose -f docker/docker-compose.dev.yml up -d

# Services running:
# - minio: http://localhost:9000 (S3)
# - ocr-service: http://localhost:8001 (EasyOCR)
```

### Stories Timeline (Option B+)

| Phase | Stories | Notes |
|-------|---------|-------|
| 1a | `1-1` → `1-3` | Auth basics |
| 1b | `1-7`, `1-8`, `1-9` | **Infrastructure (NEW Order)** |
| 2 | `1-4` → `1-6` | Auth advanced |
| 3 | `2-1` → `2-3` | Profiles |
| 4 | `3-1` → `3-6` | OCR pipeline (uses EasyOCR) |
| 5 | `4-1` → `4-5` | LLM explanations (uses Groq) |

## 2) Nguyen tac phan cong

- Don vi giao viec la **story key** trong `sprint-status.yaml`.
- Moi dev nen nhan 1-2 stories/luc, uu tien theo thu tu backlog.
- Khong nhan story co dependency chua xong trong cung epic.

Quy uoc branch de de theo doi:

- `feature/<story-key>`
- Vi du: `feature/1-1-starter-template-setup`

## 3) Flow chuan cho moi story

### Buoc A - Chuyen story tu backlog sang ready-for-dev

1. Scrum/Lead chay `create-story` (hoac chi dinh ro story key).
2. He thong tao file context cho story tai:
   - `_bmad-output/implementation-artifacts/epic-<N>/<story-key>.md` (product epics 1–9)
   - `_bmad-output/implementation-artifacts/infrastructure/<story-key>.md` (Epic infrastructure / monorepo & CI)
3. `sprint-status.yaml` tu dong update story sang `ready-for-dev`.

### Buoc B - Dev implement story

1. Tao branch theo quy uoc.
2. Chay `dev-story` voi file story duoc giao.
3. Implement dung theo Acceptance Criteria trong story file.
4. Dam bao test/lint/build pass.
5. Khi xong, story duoc dua ve `review`.

### Buoc C - Review va dong story

1. Chay `code-review` (khuyen nghi dung model/nguoi review khac).
2. Neu co findings: quay lai sua tren cung branch.
3. Khi pass review va merge, cap nhat story sang `done`.
4. Dong bo lai `sprint-status.yaml` neu can.

## 4) Checklist giao viec cho tung dev

Khi giao 1 story, lead gui toi thieu:

- Story key (vi du: `3-1-upload-pdf-image-library`; mobile Phase 2: `9-1-mobile-camera-capture-ux`)
- File story context (`_bmad-output/implementation-artifacts/epic-<N>/<story-key>.md` hoac `infrastructure/<story-key>.md`)
- Branch name can tao
- Deadline + owner
- Yeu cau test bat buoc (unit/integration/e2e neu co)

## 5) Dinh nghia Done (toi thieu)

Mot story chi duoc xem la xong khi:

- Tat ca AC (Given/When/Then) da dat
- Test lien quan pass
- Khong lam vo regression chinh
- Co PR + review pass
- `sprint-status.yaml` phan anh dung trang thai cuoi

## 6) De xuat van hanh theo sprint

- Dau sprint:
  - Chot nhom stories se lam (Sprint scope) — **uu tien Web MVP (Epic 1–8) truoc khi mo Epic 9 mobile**
  - Gan owner tung story key
- Giua sprint:
  - Daily update theo `sprint-status.yaml`
- Cuoi sprint:
  - Chot story done/chua done
  - Danh dau `epic-X-retrospective` neu da retro

## 7) Khoi dong nhanh (goi y)

Thu tu an toan de bat dau (Phase 1 — Web):

1. `1-1-starter-template-setup`
2. `1-2-email-password-registration`
3. `1-3-secure-login-logout-session`

**Infrastructure Stories (Option B+):**
4. `1-7-groq-api-setup` - Groq API cho LLM + Embeddings
5. `1-8-qdrant-cloud-setup` - Qdrant Cloud cho Vector DB
6. `1-9-easyocr-service-setup` - EasyOCR Python microservice

Sau khi Epic 1 on dinh, tiep tuc Epic 2 va Epic 3, roi theo thu tu nghiep vu 4 → 5 → 6 → 7 → 8.

Phase 2 — Mobile: Epic 9 (`9-1`, `9-2`, `9-3`) giu `backlog` trong `sprint-status.yaml` cho toi khi Web MVP (Epic 1–8) da chot; story file van co tai `epic-9/` de tham chiếu.

PR/CI: remote chinh dung **GitHub** (workflow CI trong repo, khong con GitLab CI mac dinh).

## 8) Troubleshooting (Option B+)

### LLM/Language Model Errors

```bash
# Check Groq API key is set
echo $GROQ_API_KEY

# Test Groq API connection
curl -X POST "https://api.groq.com/openai/v1/chat/completions" \
  -H "Authorization: Bearer $GROQ_API_KEY" \
  -d '{"model": "qwen-2.5-72b-versatile", "messages": [{"role": "user", "content": "test"}]}'
```

### Vector DB Issues

```bash
# Check Qdrant Cloud connection
curl -X GET "$QDRANT_HOST/collections" \
  -H "api-key: $QDRANT_API_KEY"
```

### OCR Service Issues

```bash
# Check EasyOCR service health
curl http://localhost:8001/health

# View EasyOCR logs
docker logs healthlens-ocr-service-1

# Restart EasyOCR service
docker-compose -f docker/docker-compose.dev.yml restart ocr-service
```

### Database Connection

```bash
# Test Neon connection
psql "$NEON_DATABASE_URL"

# For offline development, use local PostgreSQL
# Uncomment postgres service in docker-compose.dev.yml
```
