package com.healthlens.api.service.ocr;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OcrProviderRegistryTest {

    @Test
    @DisplayName("registry validates configured provider names fail-fast")
    void constructor_unsupportedPrimaryProvider_throwsValidationError() {
        List<OcrProvider> providers = List.of(new StubProvider("easyocr", Set.of(OcrCapability.IMAGE_OCR)));

        assertThatThrownBy(() -> new OcrProviderRegistry(providers, "missing-provider", "easyocr"))
                .isInstanceOf(OcrProcessingException.class)
                .hasMessageContaining("Unsupported OCR provider");
    }

    @Test
    @DisplayName("registry validates provider names are unique after normalization")
    void constructor_duplicateProviderName_throwsValidationError() {
        List<OcrProvider> providers = List.of(
                new StubProvider("easyocr", Set.of(OcrCapability.IMAGE_OCR)),
                new StubProvider(" EasyOCR ", Set.of(OcrCapability.PDF_SCAN))
        );

        assertThatThrownBy(() -> new OcrProviderRegistry(providers, "easyocr", ""))
                .isInstanceOf(OcrProcessingException.class)
                .hasMessageContaining("Duplicate OCR provider name 'easyocr'");
    }

    @Test
    @DisplayName("image routing uses fallback order and skips providers without IMAGE_OCR")
    void selectProviders_imageMime_skipsNonImageProvidersWithDiagnostics() {
        OcrProviderRegistry registry = new OcrProviderRegistry(
                List.of(
                        new StubProvider("pdfbox", Set.of(OcrCapability.PDF_TEXT)),
                        new StubProvider("easyocr", Set.of(OcrCapability.IMAGE_OCR)),
                        new StubProvider("textract", Set.of(OcrCapability.PDF_SCAN, OcrCapability.DOCUMENT_LAYOUT))
                ),
                "pdfbox",
                "easyocr,textract"
        );

        OcrProviderRegistry.Route route = registry.route("image/png");

        assertThat(route.capability()).isEqualTo(OcrCapability.IMAGE_OCR);
        assertThat(route.providers()).extracting(OcrProvider::name).containsExactly("easyocr");
        assertThat(route.diagnostics()).extracting(OcrResult.OcrDiagnostic::getCode)
                .containsExactly("OCR_PROVIDER_CAPABILITY_MISMATCH", "OCR_PROVIDER_CAPABILITY_MISMATCH");
        assertThat(route.diagnostics()).extracting(OcrResult.OcrDiagnostic::getProvider)
                .containsExactly("pdfbox", "textract");
    }

    @Test
    @DisplayName("image routing does not append unconfigured external providers")
    void selectProviders_imageMime_doesNotAppendUnconfiguredProviders() {
        OcrProviderRegistry registry = new OcrProviderRegistry(
                List.of(
                        new StubProvider("easyocr", Set.of(OcrCapability.IMAGE_OCR)),
                        new StubProvider("gcv", Set.of(OcrCapability.IMAGE_OCR))
                ),
                "easyocr",
                ""
        );

        OcrProviderRegistry.Route route = registry.route("image/png");

        assertThat(route.providers()).extracting(OcrProvider::name).containsExactly("easyocr");
    }

    @Test
    @DisplayName("PDF text routing includes local pdfbox provider even when provider order targets OCR fallbacks")
    void selectProviders_pdfText_includesLocalPdfboxProvider() {
        OcrProviderRegistry registry = new OcrProviderRegistry(
                List.of(
                        new StubProvider("pdfbox", Set.of(OcrCapability.PDF_TEXT)),
                        new StubProvider("easyocr", Set.of(OcrCapability.IMAGE_OCR)),
                        new StubProvider("textract", Set.of(OcrCapability.PDF_SCAN, OcrCapability.DOCUMENT_LAYOUT))
                ),
                "easyocr",
                "textract"
        );

        OcrProviderRegistry.Route route = registry.route("application/pdf");

        assertThat(route.capability()).isEqualTo(OcrCapability.PDF_TEXT);
        assertThat(route.providers()).extracting(OcrProvider::name).containsExactly("pdfbox");
        assertThat(route.diagnostics()).extracting(OcrResult.OcrDiagnostic::getProvider)
                .containsExactly("easyocr", "textract");
    }

    @Test
    @DisplayName("PDF scan routing uses declared PDF_SCAN capability after text layer stage")
    void selectProviders_pdfMime_routesPdfScanProviders() {
        OcrProviderRegistry registry = new OcrProviderRegistry(
                List.of(
                        new StubProvider("easyocr", Set.of(OcrCapability.IMAGE_OCR)),
                        new StubProvider("textract", Set.of(OcrCapability.PDF_SCAN, OcrCapability.DOCUMENT_LAYOUT))
                ),
                "easyocr",
                "textract"
        );

        OcrProviderRegistry.Route route = registry.route(OcrCapability.PDF_SCAN, "application/pdf");

        assertThat(route.providers()).extracting(OcrProvider::name).containsExactly("textract");
        assertThat(route.diagnostics()).extracting(OcrResult.OcrDiagnostic::getProvider).containsExactly("easyocr");
    }

    private record StubProvider(String name, Set<OcrCapability> capabilities) implements OcrProvider {
        @Override
        public OcrResult extract(OcrJob job) {
            return OcrResult.builder()
                    .provider(name)
                    .mimeType(job.mimeType())
                    .text(name + " result")
                    .confidence(1.0f)
                    .build();
        }
    }
}
