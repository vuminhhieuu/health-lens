package com.healthlens.api.service.ocr;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class OcrProviderRegistry {

    private static final String LOCAL_PDF_TEXT_PROVIDER = "pdfbox";

    private final Map<String, OcrProvider> providers;
    private final List<String> providerOrder;

    public OcrProviderRegistry(
            List<OcrProvider> providers,
            @Value("${app.ocr.providers.primary:easyocr}") String primaryProvider,
            @Value("${app.ocr.providers.fallback-order:textract}") String fallbackProviders
    ) {
        this.providers = new LinkedHashMap<>();
        for (OcrProvider provider : providers) {
            String name = normalizeProvider(provider.name());
            if (!name.isBlank()) {
                if (this.providers.containsKey(name)) {
                    throw new OcrProcessingException("Duplicate OCR provider name '" + name + "'");
                }
                this.providers.put(name, provider);
            }
        }
        this.providerOrder = resolveProviderOrder(primaryProvider, fallbackProviders);
    }

    public Route route(String mimeType) {
        return route(requiredCapabilityForMimeType(mimeType), mimeType);
    }

    public Route route(OcrCapability capability, String mimeType) {
        List<OcrProvider> selectedProviders = new ArrayList<>();
        List<OcrResult.OcrDiagnostic> diagnostics = new ArrayList<>();
        for (String providerName : providerOrder) {
            OcrProvider provider = providers.get(providerName);
            if (provider.supports(capability)) {
                selectedProviders.add(provider);
            } else {
                diagnostics.add(capabilityMismatch(provider, capability, mimeType));
            }
        }
        OcrProvider localPdfTextProvider = providers.get(LOCAL_PDF_TEXT_PROVIDER);
        if (capability == OcrCapability.PDF_TEXT
                && localPdfTextProvider != null
                && !providerOrder.contains(LOCAL_PDF_TEXT_PROVIDER)
                && localPdfTextProvider.supports(capability)) {
            selectedProviders.add(localPdfTextProvider);
        }
        return new Route(capability, selectedProviders, diagnostics);
    }

    public List<String> providerOrder() {
        return providerOrder;
    }

    public OcrProvider provider(String providerName) {
        String normalizedProvider = normalizeProvider(providerName);
        OcrProvider provider = providers.get(normalizedProvider);
        if (provider == null) {
            throw new OcrProcessingException("Unsupported OCR provider '" + normalizedProvider + "'");
        }
        return provider;
    }

    private List<String> resolveProviderOrder(String primaryProvider, String fallbackProviders) {
        List<String> order = new ArrayList<>();
        addConfiguredProvider(order, primaryProvider, "primary");
        if (fallbackProviders != null) {
            for (String fallbackProvider : fallbackProviders.split(",")) {
                addConfiguredProvider(order, fallbackProvider, "fallback");
            }
        }
        return List.copyOf(order);
    }

    private void addConfiguredProvider(List<String> order, String providerName, String source) {
        String normalizedProvider = normalizeProvider(providerName);
        if (normalizedProvider.isBlank()) {
            return;
        }
        if (!providers.containsKey(normalizedProvider)) {
            throw new OcrProcessingException("Unsupported OCR provider '" + normalizedProvider + "' configured as " + source);
        }
        if (!order.contains(normalizedProvider)) {
            order.add(normalizedProvider);
        }
    }

    private OcrCapability requiredCapabilityForMimeType(String mimeType) {
        String normalizedMimeType = normalizeMimeType(mimeType);
        if (normalizedMimeType.startsWith("image/")) {
            return OcrCapability.IMAGE_OCR;
        }
        if ("application/pdf".equals(normalizedMimeType)) {
            return OcrCapability.PDF_TEXT;
        }
        throw new OcrProcessingException("Unsupported OCR MIME type: " + normalizedMimeType);
    }

    private OcrResult.OcrDiagnostic capabilityMismatch(OcrProvider provider, OcrCapability capability, String mimeType) {
        return OcrResult.OcrDiagnostic.builder()
                .provider(provider.name())
                .category("provider_routing")
                .code("OCR_PROVIDER_CAPABILITY_MISMATCH")
                .message("Provider does not support required OCR capability")
                .context(Map.of(
                        "requiredCapability", capability.name(),
                        "mimeType", normalizeMimeType(mimeType)
                ))
                .build();
    }

    private String normalizeProvider(String provider) {
        if (provider == null) {
            return "";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        int parametersIndex = mimeType.indexOf(';');
        String baseType = parametersIndex >= 0 ? mimeType.substring(0, parametersIndex) : mimeType;
        return baseType.trim().toLowerCase(Locale.ROOT);
    }

    public record Route(
            OcrCapability capability,
            List<OcrProvider> providers,
            List<OcrResult.OcrDiagnostic> diagnostics
    ) {}
}
