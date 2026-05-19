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
import os
import socket
import ipaddress
import numpy as np
from typing import Optional
from io import BytesIO
from urllib.parse import urlparse, urljoin

MAX_IMAGE_SIZE_BYTES = 30 * 1024 * 1024
MAX_IMAGE_BASE64_LENGTH = math.ceil(MAX_IMAGE_SIZE_BYTES / 3) * 4
MAX_IMAGE_DIMENSION = 10000
REMOTE_FETCH_TIMEOUT_SECONDS = int(os.getenv("OCR_REMOTE_FETCH_TIMEOUT_SECONDS", "15"))
MAX_REDIRECTS = int(os.getenv("OCR_REMOTE_FETCH_MAX_REDIRECTS", "3"))
ALLOWED_FETCH_DOMAINS = tuple(
    domain.strip().lower()
    for domain in os.getenv(
        "OCR_ALLOWED_FETCH_DOMAINS",
        "amazonaws.com,s3.amazonaws.com,storage.googleapis.com,googleusercontent.com,cloudfront.net",
    ).split(",")
    if domain.strip()
)

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


def _is_host_allowed(host: str) -> bool:
    normalized = host.lower().rstrip(".")
    return any(normalized == allowed or normalized.endswith(f".{allowed}") for allowed in ALLOWED_FETCH_DOMAINS)


def _validate_public_ip(ip_text: str, source_url: str) -> None:
    ip_addr = ipaddress.ip_address(ip_text)
    is_blocked = (
        ip_addr.is_private
        or ip_addr.is_loopback
        or ip_addr.is_link_local
        or ip_addr.is_multicast
        or ip_addr.is_unspecified
        or ip_addr.is_reserved
    )
    if is_blocked:
        logger.warning("SSRF blocked for %s: resolved blocked IP %s", source_url, ip_text)
        raise HTTPException(status_code=400, detail="Blocked remote URL")


def _validate_fetch_url(url: str) -> str:
    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https"):
        raise HTTPException(status_code=400, detail="Only http:// and https:// URLs are allowed")
    if not parsed.hostname:
        raise HTTPException(status_code=400, detail="Invalid URL")

    host = parsed.hostname.lower().rstrip(".")
    if not _is_host_allowed(host):
        logger.warning("SSRF blocked for %s: host is not in allowlist", url)
        raise HTTPException(status_code=400, detail="Blocked remote URL")

    try:
        ipaddress.ip_address(host)
        _validate_public_ip(host, url)
        return host
    except ValueError:
        pass

    try:
        addr_infos = socket.getaddrinfo(host, None, proto=socket.IPPROTO_TCP)
    except socket.gaierror:
        raise HTTPException(status_code=400, detail="Failed to resolve URL host")
    if not addr_infos:
        raise HTTPException(status_code=400, detail="Failed to resolve URL host")

    for addr_info in addr_infos:
        resolved_ip = addr_info[4][0]
        _validate_public_ip(resolved_ip, url)
    return host


def _extract_peer_ip(response) -> Optional[str]:
    raw = getattr(response, "raw", None)
    if raw is None:
        return None
    connection = getattr(raw, "connection", None)
    if connection is None:
        connection = getattr(raw, "_connection", None)
    if connection is None:
        return None
    sock = getattr(connection, "sock", None)
    if sock is None:
        return None
    try:
        peer = sock.getpeername()
    except OSError:
        return None
    if not peer:
        return None
    return peer[0]


def _download_remote_image(url: str) -> bytes:
    import requests as http_requests

    current_url = url
    for redirect_count in range(MAX_REDIRECTS + 1):
        validated_host = _validate_fetch_url(current_url)
        response = None
        try:
            response = http_requests.get(
                current_url,
                timeout=REMOTE_FETCH_TIMEOUT_SECONDS,
                stream=True,
                allow_redirects=False,
                headers={"Host": validated_host},
            )
            peer_ip = _extract_peer_ip(response)
            if peer_ip:
                _validate_public_ip(peer_ip, current_url)

            if 300 <= response.status_code < 400:
                location = response.headers.get("Location")
                if not location:
                    raise HTTPException(status_code=400, detail="Failed to download image")
                response.close()
                current_url = urljoin(current_url, location)
                continue

            response.raise_for_status()

            content_type = response.headers.get("Content-Type", "")
            if content_type and not content_type.startswith(("image/", "application/octet-stream")):
                logger.warning("Unexpected content type: %s — rejecting", content_type)
                raise HTTPException(status_code=400, detail="URL does not point to an image")

            content_length = response.headers.get("Content-Length")
            if content_length:
                try:
                    if int(content_length) > MAX_IMAGE_SIZE_BYTES:
                        raise HTTPException(
                            status_code=400,
                            detail=f"Image file too large: {int(content_length)} bytes (max: {MAX_IMAGE_SIZE_BYTES})",
                        )
                except ValueError:
                    pass

            chunks = []
            total_size = 0
            for chunk in response.iter_content(chunk_size=8192):
                if not chunk:
                    continue
                total_size += len(chunk)
                if total_size > MAX_IMAGE_SIZE_BYTES:
                    raise HTTPException(
                        status_code=400,
                        detail=f"Image file too large: {total_size} bytes (max: {MAX_IMAGE_SIZE_BYTES})",
                    )
                chunks.append(chunk)
            return b"".join(chunks)
        except http_requests.exceptions.Timeout:
            logger.error("Image download timeout for URL: %s", current_url[:100])
            raise HTTPException(status_code=504, detail="Image download timeout")
        except http_requests.exceptions.RequestException as exc:
            logger.error("Failed to download image: %s", str(exc))
            raise HTTPException(status_code=400, detail="Failed to download image")
        finally:
            if response is not None:
                response.close()

    logger.warning("SSRF blocked for %s: exceeded redirect limit", url)
    raise HTTPException(status_code=400, detail="Blocked remote URL")


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
    from PIL import Image

    start_time = time.time()

    try:
        if request.image_url:
            logger.info("Downloading image from: %s", request.image_url[:100])
            raw_bytes = _download_remote_image(request.image_url)
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
