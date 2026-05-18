package com.healthlens.api.service.ocr;

import com.healthlens.api.dto.OcrResult;

import java.util.Set;

public interface OcrProvider {
    String name();

    Set<OcrCapability> capabilities();

    OcrResult extract(OcrJob job);

    default boolean supports(OcrCapability capability) {
        return capabilities().contains(capability);
    }
}
