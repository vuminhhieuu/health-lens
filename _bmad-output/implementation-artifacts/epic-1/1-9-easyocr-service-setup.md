# Story 1.9: Thiết Lập EasyOCR Microservice cho OCR

**Status:** ready-for-dev

**Approved:** Option B+ Architecture (2026-04-01)

> **Note:** Đây là story **mới** - không thay thế story cũ  
> **Lý do:** Option B+ sử dụng EasyOCR làm primary OCR với local Python service, fallback sang AWS Textract

## Execution scope

**Phase 1 — Web MVP:** Story này phục vụ cho Epic 3 (OCR Pipeline).

## Story

As a dev team,
I want tích hợp EasyOCR service cho OCR,
so that hệ thống có thể extract text từ PDF/images với chi phí thấp và RAM usage thấp (so với PaddleOCR).

## Acceptance Criteria

1. **Given** EasyOCR service đang chạy, **When** gửi image URL, **Then** service trả về extracted text với confidence score.
2. **Given** image chứa tiếng Việt, **When** OCR được gọi, **Then** Vietnamese text được extract với accuracy ≥ 85%.
3. **Given** EasyOCR service fail hoặc timeout (10s), **When** OCR được gọi, **Then** hệ thống fallback sang AWS Textract.
4. **Given** EasyOCR service containerized, **When** docker-compose up, **Then** service khởi động với RAM limit 2GB.

## Tasks / Subtasks

- [ ] Task 1 — Infrastructure: Tạo EasyOCR Python service
  - [ ] Tạo project structure `services/ocr-service/`
  - [ ] Tạo FastAPI application với `/ocr` endpoint
  - [ ] Cấu hình EasyOCR Reader với languages `['vi', 'en']`
  - [ ] Implement `/health` endpoint cho health check

- [ ] Task 2 — Docker: Containerize EasyOCR service
  - [ ] Tạo `Dockerfile` với Python 3.11 + EasyOCR
  - [ ] Cấu hình RAM limit 2GB trong docker-compose
  - [ ] Test container build thành công
  - [ ] Push image to registry (nếu cần)

- [ ] Task 3 — Configuration: Cấu hình Spring Boot integration
  - [ ] Thêm OCR_SERVICE_URL vào environment
  - [ ] Tạo `OcrServiceConfig.java` cho RestTemplate
  - [ ] Implement circuit breaker pattern (Resilience4j)

- [ ] Task 4 — Service: Tạo OcrService với fallback
  - [ ] Tạo `OcrService.java` trong Spring Boot
  - [ ] Implement method `processImage(imageUrl)` gọi EasyOCR API
  - [ ] Implement AWS Textract fallback khi EasyOCR fail
  - [ ] Implement timeout handling (10s)

- [ ] Task 5 — Integration: Tích hợp với upload flow (Epic 3)
  - [ ] Kết nối OcrService với file upload controller
  - [ ] Implement async processing với Redis queue
  - [ ] Test end-to-end OCR flow

- [ ] Task 6 — Tests: Viết tests
  - [ ] `OcrServiceTest`: test primary OCR, fallback, timeout
  - [ ] `OcrApiIntegrationTest`: integration với running service
  - [ ] Mock tests cho offline development

## Dev Notes

### Architecture: OCR Pipeline in Option B+

```
┌─────────────────────────────────────────────────────────────────┐
│                     OCR Processing Flow                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Upload          Spring API           OCR Service       Fallback │
│    │                 │                    │                │      │
│    │  1. Upload     │                    │                │      │
│    │───────────────▶│                    │                │      │
│    │                 │  2. Call OCR     │                │      │
│    │                 │─────────────────▶│                │      │
│    │                 │                    │                │      │
│    │                 │  3. Extract text  │                │      │
│    │                 │◀─────────────────│                │      │
│    │                 │                    │                │      │
│    │                 │  Success?        │                │      │
│    │                 │  ├── Yes → Continue                 │      │
│    │                 │  └── No  → Call Textract            │      │
│    │                 │                    │                │      │
│    │                 │                    │         ┌──────▼─────┐│
│    │                 │                    │         │ AWS        ││
│    │                 │────────────────────────────────▶│Textract ││
│    │                 │                    │         │(fallback) ││
│    │                 │                    │         └────────────┘│
│    │                 │                    │                      │
│    │  4. Result      │                    │                      │
│    │◀───────────────│                    │                      │
│    │                 │                    │                      │
└──┴─────────────────┴────────────────────┴──────────────────────┘
```

### Python FastAPI Service Structure

```
services/
└── ocr-service/
    ├── app.py              # FastAPI application
    ├── requirements.txt    # Python dependencies
    ├── Dockerfile          # Container definition
    └── README.md          # Service documentation
```

### app.py (FastAPI Application)

```python
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import easyocr
import uvicorn
import logging
from typing import Optional

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI
app = FastAPI(title="HealthLens OCR Service")

# Initialize EasyOCR Reader (lazy loading)
# Note: First inference is slow (model download), subsequent are fast
_reader = None

def get_reader():
    global _reader
    if _reader is None:
        logger.info("Initializing EasyOCR reader...")
        # GPU disabled for CPU-only environments
        # Set gpu=True if you have NVIDIA GPU
        _reader = easyocr.Reader(
            ['vi', 'en'],  # Vietnamese and English
            gpu=False,
            verbose=False
        )
        logger.info("EasyOCR reader initialized")
    return _reader


class OcrRequest(BaseModel):
    image_url: str


class OcrResult(BaseModel):
    text: str
    confidence: float
    language_detected: str
    processing_time_ms: int


class ErrorResponse(BaseModel):
    error: str
    detail: Optional[str] = None


@app.get("/health")
async def health_check():
    """Health check endpoint for container orchestration"""
    return {"status": "healthy", "service": "easyocr"}


@app.post("/ocr", response_model=OcrResult)
async def extract_text(request: OcrRequest):
    """
    Extract text from image URL using EasyOCR.
    
    Returns:
        - text: Extracted text
        - confidence: Average confidence score (0-1)
        - language_detected: Primary language detected
        - processing_time_ms: Time taken for processing
    """
    import time
    import requests
    from PIL import Image
    from io import BytesIO
    
    start_time = time.time()
    
    try:
        # Download image
        logger.info(f"Downloading image from: {request.image_url}")
        response = requests.get(request.image_url, timeout=30)
        response.raise_for_status()
        
        # Load image
        image = Image.open(BytesIO(response.content))
        
        # Perform OCR
        reader = get_reader()
        results = reader.readtext(image)
        
        if not results:
            return OcrResult(
                text="",
                confidence=0.0,
                language_detected="unknown",
                processing_time_ms=int((time.time() - start_time) * 1000)
            )
        
        # Extract text and confidence
        extracted_texts = []
        confidences = []
        
        for bbox, text, confidence in results:
            if text.strip():  # Skip empty results
                extracted_texts.append(text.strip())
                confidences.append(confidence)
        
        combined_text = " ".join(extracted_texts)
        avg_confidence = sum(confidences) / len(confidences) if confidences else 0.0
        
        # Detect primary language (simple heuristic)
        # In production, use langdetect library
        language = "vi" if any('\u00C0' <= c <= '\u024F' for c in combined_text) else "en"
        
        processing_time = int((time.time() - start_time) * 1000)
        
        logger.info(f"OCR completed: {len(extracted_texts)} text blocks, "
                   f"confidence: {avg_confidence:.2f}, time: {processing_time}ms")
        
        return OcrResult(
            text=combined_text,
            confidence=avg_confidence,
            language_detected=language,
            processing_time_ms=processing_time
        )
        
    except requests.exceptions.Timeout:
        logger.error("Image download timeout")
        raise HTTPException(status_code=504, detail="Image download timeout")
    except requests.exceptions.RequestException as e:
        logger.error(f"Failed to download image: {e}")
        raise HTTPException(status_code=400, detail=f"Failed to download image: {str(e)}")
    except Exception as e:
        logger.error(f"OCR processing error: {e}")
        raise HTTPException(status_code=500, detail=f"OCR processing error: {str(e)}")


if __name__ == "__main__":
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8001,
        timeout_keep_alive=60
    )
```

### requirements.txt

```
fastapi>=0.109.0
uvicorn[standard]>=0.27.0
easyocr>=1.7.0
Pillow>=10.0.0
requests>=2.31.0
pydantic>=2.0.0
python-multipart>=0.0.6
```

### Dockerfile

```dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app.py .

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8001/health || exit 1

# Run application
EXPOSE 8001

# Run with limited resources (handled by docker-compose)
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
```

### Docker Compose Configuration

```yaml
# docker-compose.dev.yml
services:
  # ... other services (postgres, redis, minio)
  
  ocr-service:
    build:
      context: ./services/ocr-service
      dockerfile: Dockerfile
    ports:
      - "8001:8001"
    environment:
      - EASYOCR_MODEL_PATH=/models
    volumes:
      - ocr-models:/root/.EasyOCR
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 512M
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8001/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 120s

volumes:
  ocr-models:
```

### Spring Boot OcrService Integration

```java
package com.healthlens.api.service;

import com.healthlens.api.exception.OcrProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OcrService {

    private final RestTemplate restTemplate;
    private final AwsTextractClient textractFallback;
    private final String ocrServiceUrl;

    public OcrService(
            RestTemplate restTemplate,
            AwsTextractClient textractFallback,
            @Value("${app.ocr.service.url:http://localhost:8001}") String ocrServiceUrl) {
        this.restTemplate = restTemplate;
        this.textractFallback = textractFallback;
        this.ocrServiceUrl = ocrServiceUrl;
    }

    public OcrResult processImage(String imageUrl) {
        try {
            // Primary: EasyOCR
            return callEasyOcr(imageUrl);
        } catch (OcrException e) {
            log.warn("EasyOCR failed, falling back to AWS Textract: {}", e.getMessage());
            return callTextractFallback(imageUrl);
        }
    }

    private OcrResult callEasyOcr(String imageUrl) {
        try {
            EasyOcrResponse response = restTemplate.postForObject(
                ocrServiceUrl + "/ocr",
                new OcrRequest(imageUrl),
                EasyOcrResponse.class
            );
            
            return OcrResult.builder()
                .text(response.getText())
                .confidence(response.getConfidence())
                .source("easyocr")
                .language(response.getLanguageDetected())
                .build();
                
        } catch (ResourceAccessException | HttpClientErrorException e) {
            log.error("EasyOCR service unavailable: {}", e.getMessage());
            throw new OcrException("EasyOCR service failed", e);
        }
    }

    private OcrResult callTextractFallback(String imageUrl) {
        log.info("Using AWS Textract fallback for: {}", imageUrl);
        return textractFallback.extract(imageUrl);
    }
}

@Data
class OcrRequest {
    private String imageUrl;
}

@Data
class EasyOcrResponse {
    private String text;
    private Float confidence;
    private String languageDetected;
    private Integer processingTimeMs;
}
```

### OcrServiceConfig.java

```java
package com.healthlens.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class OcrServiceConfig {

    @Value("${app.ocr.service.timeout-ms:10000}")
    private int ocrTimeoutMs;

    @Bean
    public RestTemplate ocrRestTemplate(RestTemplateBuilder builder) {
        return builder
            .connectTimeout(Duration.ofMillis(5000))
            .readTimeout(Duration.ofMillis(ocrTimeoutMs))
            .build();
    }
}
```

### AWS Textract Fallback

```java
@Component
@Slf4j
public class AwsTextractClient {

    private final AmazonTextract textract;

    public AwsTextractClient() {
        this.textract = AmazonTextractClient.builder()
            .region(Region.AP_SOUTHEAST_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    public OcrResult extract(String imageUrl) {
        try {
            DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                .document(Document.builder()
                    .s3Object(S3Object.builder()
                        .bucket(bucketName)
                        .name(key)
                        .build())
                    .build())
                .build();

            DetectDocumentTextResponse response = textract.detectDocumentText(request);

            StringBuilder text = new StringBuilder();
            float totalConfidence = 0;
            int blockCount = 0;

            for (Block block : response.blocks()) {
                if (block.blockType() == BlockType.LINE) {
                    text.append(block.text()).append(" ");
                    totalConfidence += block.confidence().floatValue();
                    blockCount++;
                }
            }

            return OcrResult.builder()
                .text(text.toString().trim())
                .confidence(totalConfidence / blockCount)
                .source("textract")
                .build();

        } catch (Exception e) {
            log.error("Textract extraction failed: {}", e.getMessage());
            throw new OcrException("All OCR providers failed", e);
        }
    }
}
```

### Performance Comparison

| OCR Engine | Vietnamese Accuracy | Speed (CPU) | RAM Usage | Cost |
|------------|--------------------|-------------|-----------|------|
| **EasyOCR** | ~90% | 3-8s | 500 MB | Free |
| PaddleOCR | ~95% | 2-5s | 3-5 GB | Free |
| AWS Textract | ~97% | 2-5s | N/A | $1.5/1000 pages |
| Google Vision | ~97% | 1-3s | N/A | $1.5/1000 pages |

**Note:** EasyOCR accuracy ~90% is acceptable for MVP with confidence-based UI feedback.

### Unit Test Example

```java
package com.healthlens.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.ocr.service.url=http://localhost:8001"
})
class OcrServiceTest {

    @Autowired
    private OcrService ocrService;

    @Test
    void shouldReturnResultFromEasyOcr() {
        // This test requires running OCR service
        // For CI, use @MockBean to mock the service
        OcrResult result = ocrService.processImage("https://example.com/test.jpg");
        
        assertNotNull(result);
        assertNotNull(result.getText());
        assertTrue(result.getConfidence() >= 0 && result.getConfidence() <= 1);
    }

    @Test
    void shouldFallbackToTextractOnError() {
        // Mock EasyOCR failure and verify Textract fallback
        OcrResult result = ocrService.processImage("https://example.com/test.jpg");
        
        // Should get result from fallback
        assertNotNull(result);
        assertEquals("textract", result.getSource());
    }
}
```

### Environment Variables

```bash
# OCR Service
OCR_SERVICE_URL=http://localhost:8001
OCR_TIMEOUT_MS=10000

# AWS Textract (fallback)
AWS_ACCESS_KEY_ID=xxx
AWS_SECRET_ACCESS_KEY=xxx
AWS_REGION=ap-southeast-1
S3_BUCKET=healthlens-ocr
```

### References

- [Source: architecture.md#ADR-006-EasyOCR-AWS-Textract]
- [Source: technical-option-bplus-feasibility-2026-04-01.md]
- [EasyOCR GitHub](https://github.com/JaidedAI/EasyOCR)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

_[To be filled during implementation]_

### Completion Notes List

_[To be filled upon completion]_

### File List

| File | Action |
|------|--------|
| `services/ocr-service/app.py` | Create |
| `services/ocr-service/requirements.txt` | Create |
| `services/ocr-service/Dockerfile` | Create |
| `services/ocr-service/README.md` | Create |
| `docker-compose.dev.yml` | Update (add ocr-service) |
| `OcrService.java` | Create/Update |
| `OcrServiceConfig.java` | Create |
| `AwsTextractClient.java` | Create |
| `OcrServiceTest.java` | Create |

