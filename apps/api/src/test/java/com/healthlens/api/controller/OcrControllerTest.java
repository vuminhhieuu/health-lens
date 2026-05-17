package com.healthlens.api.controller;

import com.healthlens.api.annotation.RequiresConsent;
import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import com.healthlens.api.service.OcrService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrControllerTest {

    @Mock
    private OcrService ocrService;

    private OcrController controller;

    @Test
    @DisplayName("OcrController.extractText has @RequiresConsent annotation")
    void extractText_hasRequiresConsentAnnotation() throws NoSuchMethodException {
        var method = OcrController.class.getMethod("extractText", Map.class);
        assertThat(method.isAnnotationPresent(RequiresConsent.class)).isTrue();
    }

    @Test
    @DisplayName("Valid https URL → returns 200 with result")
    void extractText_validHttpsUrl_returnsOk() {
        controller = new OcrController(ocrService);
        OcrResult expected = OcrResult.builder()
                .text("Glucose 5.4").confidence(0.92f).provider("easyocr").language("vi").latencyMs(3500)
                .build();
        when(ocrService.processImage("https://example.com/test.jpg")).thenReturn(expected);

        ResponseEntity<OcrResult> response = controller.extractText(Map.of("imageUrl", "https://example.com/test.jpg"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSource()).isEqualTo("easyocr");
    }

    @Test
    @DisplayName("Valid http URL → returns 200")
    void extractText_validHttpUrl_returnsOk() {
        controller = new OcrController(ocrService);
        when(ocrService.processImage("http://example.com/test.jpg")).thenReturn(
                OcrResult.builder().text("").confidence(0f).provider("easyocr").language("en").latencyMs(0).build()
        );

        ResponseEntity<OcrResult> response = controller.extractText(Map.of("imageUrl", "http://example.com/test.jpg"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
    }

    @Test
    @DisplayName("Null imageUrl → returns 400")
    void extractText_nullImageUrl_returnsBadRequest() {
        controller = new OcrController(ocrService);

        ResponseEntity<OcrResult> response = controller.extractText(
                java.util.Collections.singletonMap("imageUrl", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
    }

    @Test
    @DisplayName("Blank imageUrl → returns 400")
    void extractText_blankImageUrl_returnsBadRequest() {
        controller = new OcrController(ocrService);

        ResponseEntity<OcrResult> response = controller.extractText(Map.of("imageUrl", "   "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
    }

    @Test
    @DisplayName("ftp:// URL → returns 400")
    void extractText_ftpScheme_returnsBadRequest() {
        controller = new OcrController(ocrService);

        ResponseEntity<OcrResult> response = controller.extractText(Map.of("imageUrl", "ftp://example.com/test.jpg"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        verifyNoInteractions(ocrService);
    }

    @Test
    @DisplayName("file:// URL → returns 400")
    void extractText_fileScheme_returnsBadRequest() {
        controller = new OcrController(ocrService);

        ResponseEntity<OcrResult> response = controller.extractText(Map.of("imageUrl", "file:///etc/passwd"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        verifyNoInteractions(ocrService);
    }

    @Test
    @DisplayName("localhost URL → returns 400 (SSRF)")
    void extractText_localhostUrl_returnsBadRequest() {
        controller = new OcrController(ocrService);

        ResponseEntity<OcrResult> response = controller.extractText(Map.of("imageUrl", "http://localhost:8080/internal"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        verifyNoInteractions(ocrService);
    }

    @Test
    @DisplayName("10.x.x.x private IP → returns 400")
    void extractText_privateIpUrl_returnsBadRequest() {
        controller = new OcrController(ocrService);

        ResponseEntity<OcrResult> response = controller.extractText(Map.of("imageUrl", "http://10.0.0.1/internal"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        verifyNoInteractions(ocrService);
    }

    @Test
    @DisplayName("OCR throws OcrProcessingException → returns 500 with error result")
    void extractText_ocrException_returnsInternalServerError() {
        controller = new OcrController(ocrService);
        when(ocrService.processImage(anyString())).thenThrow(new OcrProcessingException("All providers failed"));

        ResponseEntity<OcrResult> response = controller.extractText(Map.of("imageUrl", "https://example.com/test.jpg"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(500));
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSource()).isEqualTo("error");
    }

    @Test
    @DisplayName("Missing imageUrl key → returns 400")
    void extractText_missingKey_returnsBadRequest() {
        controller = new OcrController(ocrService);

        ResponseEntity<OcrResult> response = controller.extractText(Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
    }
}
