package com.healthlens.api.service;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import com.healthlens.api.service.ocr.OcrCapability;
import com.healthlens.api.service.ocr.OcrJob;
import com.healthlens.api.service.ocr.OcrProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TextractOcrProvider implements OcrProvider {

    private final AwsTextractClient textractClient;

    public TextractOcrProvider(AwsTextractClient textractClient) {
        this.textractClient = textractClient;
    }

    @Override
    public String name() {
        return "textract";
    }

    @Override
    public Set<OcrCapability> capabilities() {
        return Set.of(OcrCapability.IMAGE_OCR, OcrCapability.PDF_SCAN, OcrCapability.DOCUMENT_LAYOUT);
    }

    @Override
    public OcrResult extract(OcrJob job) {
        if (job.hasBase64Image()) {
            throw new OcrProcessingException("Textract provider requires document URL input");
        }
        return textractClient.extract(job.documentUrl());
    }
}
