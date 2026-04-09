# HealthLens OCR Service
# ======================
# EasyOCR Python microservice for Vietnamese/English text extraction
# Story 1.9 — Option B+ Architecture

FROM python:3.11-slim

WORKDIR /app

# Install system dependencies required by OpenCV and image processing
# Note: python:3.11-slim does NOT include curl, use wget for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for Docker layer caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app.py .

# Health check using wget (curl not available in slim image)
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD wget --no-verbose --tries=1 -q -O- http://localhost:8001/health || exit 1

# Expose OCR service port
EXPOSE 8001

# Run with uvicorn
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
