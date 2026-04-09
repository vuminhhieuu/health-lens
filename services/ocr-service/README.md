# HealthLens OCR Service

EasyOCR microservice cho HealthLens — extract text từ images hỗ trợ tiếng Việt và tiếng Anh.

## Architecture

Story 1.9 — Option B+ Architecture. Sử dụng EasyOCR làm primary OCR engine.

| Đặc điểm | Giá trị |
|-----------|---------|
| OCR Engine | EasyOCR 1.7+ |
| Languages | Vietnamese (`vi`), English (`en`) |
| Framework | FastAPI |
| Port | 8001 |
| RAM Usage | ~500 MB (CPU mode) |
| Accuracy | ~90% Vietnamese text |

## API Endpoints

### GET /health

Health check endpoint cho container orchestration.

```json
{
  "status": "healthy",
  "service": "easyocr",
  "reader_loaded": true
}
```

### POST /ocr

Extract text từ image URL.

**Request:**
```json
{
  "image_url": "https://example.com/test-image.png"
}
```

**Response:**
```json
{
  "text": "Kết quả xét nghiệm máu...",
  "confidence": 0.92,
  "language_detected": "vi",
  "processing_time_ms": 3500,
  "block_count": 12
}
```

**Error Codes:**
| Code | Mô tả |
|------|--------|
| 400 | Failed to download image hoặc invalid URL |
| 504 | Image download timeout (30s) |
| 500 | OCR processing error |

## Local Development

### Prerequisites
- Python 3.11+
- pip

### Setup
```bash
cd services/ocr-service
pip install -r requirements.txt
python app.py
```

Service sẽ chạy tại `http://localhost:8001`.

### Test
```bash
curl http://localhost:8001/health

curl -X POST http://localhost:8001/ocr \
  -H "Content-Type: application/json" \
  -d '{"image_url": "https://example.com/test-image.png"}'
```

## Docker

### Build
```bash
docker build -t healthlens-ocr services/ocr-service/
```

### Run
```bash
docker run -p 8001:8001 --memory=2g healthlens-ocr
```

### Via Docker Compose
```bash
docker compose -f docker/docker-compose.dev.yml --profile with-ocr up ocr-service
```

## Performance

| Metric | Giá trị |
|--------|---------|
| Cold start (first request) | ~30-60s (model download) |
| Warm request | 3-8s (CPU) |
| RAM usage | ~500 MB |
| Docker image size | ~2 GB |

## Notes

- **First request** sẽ chậm vì EasyOCR cần download models (~100MB).
  Models được cache trong volume `ocr-models` cho container restarts.
- **GPU mode**: Set `gpu=True` trong `app.py` nếu có NVIDIA GPU.
  Cần thêm `nvidia/cuda` base image trong Dockerfile.
- **Fallback**: Khi service này fail, Spring Boot API sẽ fallback sang AWS Textract (Story 1.9 Task 4).
