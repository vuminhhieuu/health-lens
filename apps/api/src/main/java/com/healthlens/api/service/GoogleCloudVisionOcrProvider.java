package com.healthlens.api.service;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import com.healthlens.api.service.ocr.OcrCapability;
import com.healthlens.api.service.ocr.OcrJob;
import com.healthlens.api.service.ocr.OcrProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GoogleCloudVisionOcrProvider implements OcrProvider {

    private final GoogleCloudVisionClient googleCloudVisionClient;

    public GoogleCloudVisionOcrProvider(GoogleCloudVisionClient googleCloudVisionClient) {
        this.googleCloudVisionClient = googleCloudVisionClient;
    }

    @Override
    public String name() {
        return "gcv";
    }

    @Override
    public Set<OcrCapability> capabilities() {
        return Set.of(OcrCapability.IMAGE_OCR, OcrCapability.DOCUMENT_LAYOUT);
    }

    @Override
    public OcrResult extract(OcrJob job) {
        if (job.hasBase64Image() || job.hasDocumentBytes()) {
            throw new OcrProcessingException("Google Cloud Vision provider requires document URL input");
        }
        try {
            return googleCloudVisionClient.extract(job.documentUrl());
        } catch (Exception ex) {
            throw new OcrProcessingException("Google Cloud Vision failed", ex);
        }
    }
}
