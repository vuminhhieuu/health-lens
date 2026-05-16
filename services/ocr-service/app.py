"""
HealthLens OCR Service — EasyOCR FastAPI Microservice

Microservice sử dụng EasyOCR để extract text từ images.
Hỗ trợ tiếng Việt và tiếng Anh.

Endpoints:
    GET  /health  — Health check cho container orchestration
    POST /ocr     — Extract text từ image URL hoặc base64

Architecture: Story 1.9 (Option B+)
"""

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import AliasChoices, BaseModel, Field, field_validator, model_validator
import easyocr
import uvicorn
import logging
import time
import base64
import math
import numpy as np
from typing import Optional
from io import BytesIO

MAX_IMAGE_SIZE_BYTES = 30 * 1024 * 1024
MAX_IMAGE_BASE64_LENGTH = math.ceil(MAX_IMAGE_SIZE_BYTES / 3) * 4
MAX_IMAGE_DIMENSION = 10000

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("ocr-service")

# Initialize FastAPI
app = FastAPI(
    title="HealthLens OCR Service",
    description="EasyOCR microservice for Vietnamese/English text extraction",
    version="1.0.0",
)

# =========================================
# EasyOCR Reader (lazy-loaded singleton)
# =========================================
_reader: Optional[easyocr.Reader] = None


def get_reader() -> easyocr.Reader:
    """
    Lazy-load EasyOCR Reader.
    First call downloads models (~100MB) and is slow.
    Subsequent calls return cached reader instantly.
    """
    global _reader
    if _reader is None:
        logger.info("Initializing EasyOCR reader with languages: ['vi', 'en']...")
        _reader = easyocr.Reader(
            ["vi", "en"],
            gpu=False,  # CPU-only for Docker containers without GPU
            verbose=False,
        )
        logger.info("EasyOCR reader initialized successfully")
    return _reader


# =========================================
# Request / Response Models
# =========================================
class OcrRequest(BaseModel):
    """Request body for OCR extraction."""

    image_url: Optional[str] = Field(
        None,
        validation_alias=AliasChoices("image_url", "imageUrl"),
        description="URL of the image to process (http/https) or presigned S3/MinIO URL",
        examples=["https://example.com/test-image.png"],
    )
    image_base64: Optional[str] = Field(
        None,
        validation_alias=AliasChoices("image_base64", "imageBase64"),
        description="Base64 encoded image bytes for internal PDF page rendering",
    )

    @field_validator("image_url")
    @classmethod
    def validate_url_scheme(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return v
        lower = v.lower()
        if not (lower.startswith("http://") or lower.startswith("https://")):
            raise ValueError("Only http:// and https:// URLs are allowed")
        return v

    @model_validator(mode="after")
    def require_one_image_source(self):
        if bool(self.image_url) == bool(self.image_base64):
            raise ValueError("Provide exactly one of image_url or image_base64")
        return self


class OcrResult(BaseModel):
    """Response body with extracted text and metadata."""

    text: str = Field(..., description="Extracted text from the image")
    confidence: float = Field(
        ..., ge=0.0, le=1.0, description="Average confidence score (0-1)"
    )
    language_detected: str = Field(
        ..., description="Primary language detected: 'vi' or 'en'"
    )
    processing_time_ms: int = Field(
        ..., description="Total processing time in milliseconds"
    )
    block_count: int = Field(..., description="Number of text blocks detected")


class HealthResponse(BaseModel):
    """Health check response."""

    status: str
    service: str
    reader_loaded: bool


# =========================================
# Endpoints
# =========================================
@app.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Health check endpoint for container orchestration.
    Reports whether the EasyOCR reader is loaded.
    """
    return HealthResponse(
        status="healthy",
        service="easyocr",
        reader_loaded=_reader is not None,
    )


@app.post("/ocr", response_model=OcrResult)
async def extract_text(request: OcrRequest):
    """
    Extract text from image URL using EasyOCR.

    Downloads the image from the provided URL and performs OCR
    using EasyOCR with Vietnamese and English language support.

    Returns extracted text, confidence score, detected language,
    and processing time.

    Raises:
        504: Image download timeout
        400: Failed to download or invalid image
        500: OCR processing error
    """
    import requests as http_requests
    from PIL import Image

    start_time = time.time()

    try:
        if request.image_url:
            logger.info("Downloading image from: %s", request.image_url[:100])
            response = http_requests.get(request.image_url, timeout=30, stream=True)
            response.raise_for_status()

            content_type = response.headers.get("Content-Type", "")
            if content_type and not content_type.startswith(("image/", "application/octet-stream")):
                logger.warning("Unexpected content type: %s — rejecting", content_type)
                raise HTTPException(status_code=400, detail="URL does not point to an image")

            raw_bytes = response.content
        else:
            logger.info("Reading image from base64 payload")
            base64_payload = (request.image_base64 or "").strip()
            if len(base64_payload) > MAX_IMAGE_BASE64_LENGTH:
                raise HTTPException(
                    status_code=400,
                    detail=f"Base64 payload too large: {len(base64_payload)} chars (max: {MAX_IMAGE_BASE64_LENGTH})"
                )
            try:
                raw_bytes = base64.b64decode(base64_payload, validate=True)
            except Exception:
                raise HTTPException(status_code=400, detail="Invalid base64 image")

        if len(raw_bytes) > MAX_IMAGE_SIZE_BYTES:
            raise HTTPException(
                status_code=400,
                detail=f"Image file too large: {len(raw_bytes)} bytes (max: {MAX_IMAGE_SIZE_BYTES})"
            )

        try:
            image = Image.open(BytesIO(raw_bytes))
            width, height = image.size
            if width > MAX_IMAGE_DIMENSION or height > MAX_IMAGE_DIMENSION:
                raise HTTPException(
                    status_code=400,
                    detail=f"Image dimensions too large: {width}x{height}px (max: {MAX_IMAGE_DIMENSION}x{MAX_IMAGE_DIMENSION})"
                )
        except Exception as e:
            if isinstance(e, HTTPException):
                raise
            raise HTTPException(status_code=400, detail="Invalid or corrupted image file")

        if image.mode not in ("RGB", "L"):
            image = image.convert("RGB")

        image_array = np.array(image)

        # 3. Perform OCR
        reader = get_reader()
        results = reader.readtext(image_array)

        # 4. Process results
        if not results:
            processing_time = int((time.time() - start_time) * 1000)
            return OcrResult(
                text="",
                confidence=0.0,
                language_detected="unknown",
                processing_time_ms=processing_time,
                block_count=0,
            )

        extracted_texts = []
        confidences = []

        for bbox, text, confidence in results:
            text_stripped = text.strip()
            if text_stripped:  # Skip empty blocks
                extracted_texts.append(text_stripped)
                confidences.append(confidence)

        combined_text = " ".join(extracted_texts)
        avg_confidence = (
            sum(confidences) / len(confidences) if confidences else 0.0
        )

        # 5. Detect primary language (heuristic: Vietnamese diacritics)
        vietnamese_chars = set("àáảãạăắằẳẵặâấầẩẫậèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵđ"
                               "ÀÁẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÙÚỦŨỤƯỨỪỬỮỰỲÝỶỸỴĐ")
        has_vietnamese = any(c in vietnamese_chars for c in combined_text)
        language = "vi" if has_vietnamese else "en"

        processing_time = int((time.time() - start_time) * 1000)

        logger.info(
            "OCR completed: %d text blocks, confidence: %.2f, "
            "language: %s, time: %dms",
            len(extracted_texts),
            avg_confidence,
            language,
            processing_time,
        )

        return OcrResult(
            text=combined_text,
            confidence=round(avg_confidence, 4),
            language_detected=language,
            processing_time_ms=processing_time,
            block_count=len(extracted_texts),
        )

    except http_requests.exceptions.Timeout:
        logger.error("Image download timeout for URL: %s", (request.image_url or "")[:100])
        raise HTTPException(status_code=504, detail="Image download timeout")
    except http_requests.exceptions.RequestException as e:
        logger.error("Failed to download image: %s", str(e))
        raise HTTPException(
            status_code=400, detail="Failed to download image"
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error("OCR processing error: %s", str(e), exc_info=True)
        raise HTTPException(
            status_code=500, detail="OCR processing error"
        )


# =========================================
# Startup / Shutdown Events
# =========================================
@app.on_event("startup")
async def startup_event():
    """Pre-load EasyOCR reader on startup to reduce first-request latency."""
    logger.info("OCR Service starting up...")
    # Note: Reader is loaded lazily on first request to avoid
    # blocking container startup. The /health endpoint will
    # report reader_loaded=false until first OCR request.


@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown."""
    logger.info("OCR Service shutting down...")


# =========================================
# Main Entry Point
# =========================================
if __name__ == "__main__":
    uvicorn.run(
        "app:app",
        host="0.0.0.0",
        port=8001,
        timeout_keep_alive=60,
        log_level="info",
    )
