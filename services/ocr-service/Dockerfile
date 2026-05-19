# ============================================
# HealthLens OCR Service - FastAPI + EasyOCR
# ============================================
#
# Vietnamese/English text extraction for medical documents.
#
# Usage:
#   Development:  docker build --target builder -t healthlens-ocr:dev .
#   Production:   docker build -t healthlens-ocr:latest .
#
# Note:
#   EasyOCR models are downloaded at first run.
#   For faster startup, pre-download models and mount as volume.

# ============================================
# Base stage
# ============================================
FROM python:3.11-slim AS base

WORKDIR /app

# ============================================
# Dependencies stage
# ============================================
FROM base AS deps

# Install system dependencies first
RUN apt-get update && apt-get upgrade -y && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip wheel "jaraco.context>=6.1.0" \
    && pip install --no-cache-dir -r requirements.txt

# ============================================
# Builder stage (for dev)
# ============================================
FROM deps AS builder

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip wheel "jaraco.context>=6.1.0" \
    && pip install --no-cache-dir -r requirements.txt

# ============================================
# Runner stage (production)
# ============================================
FROM deps AS runner

# Security: Create non-root user
RUN groupadd -g 1001 ocrgroup && useradd -u 1001 -g ocrgroup -m -s /bin/sh ocruser

WORKDIR /app

# Copy application code
COPY --chown=ocruser:ocrgroup app.py .

# Switch to non-root user
USER ocruser

# Expose port
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/health', timeout=5).read()" || exit 1

# Run with uvicorn
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
