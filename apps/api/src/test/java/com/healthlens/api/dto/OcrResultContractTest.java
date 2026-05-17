package com.healthlens.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OcrResultContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("serialize normalized OCR contract with compatibility aliases")
    void serializeContract_containsNormalizedAndCompatibilityFields() throws Exception {
        OcrResult result = OcrResult.builder()
                .provider("easyocr")
                .modelVersion("easyocr-default")
                .mimeType("image/jpeg")
                .text("Glucose 5.6")
                .confidence(0.91f)
                .latencyMs(1200)
                .retentionMode("transient")
                .pages(List.of(OcrResult.OcrPage.builder().pageNumber(1).text("Glucose 5.6").confidence(0.91f).build()))
                .lines(List.of(OcrResult.OcrSegment.builder().pageNumber(1).kind("line").text("Glucose 5.6").confidence(0.91f).build()))
                .build();

        String json = objectMapper.writeValueAsString(result);

        assertThat(json).contains("\"provider\":\"easyocr\"");
        assertThat(json).contains("\"modelVersion\":\"easyocr-default\"");
        assertThat(json).contains("\"mimeType\":\"image/jpeg\"");
        assertThat(json).contains("\"retentionMode\":\"transient\"");
        assertThat(json).contains("\"source\":\"easyocr\"");
        assertThat(json).contains("\"processingTimeMs\":1200");
    }

    @Test
    @DisplayName("ordered parser text uses line ordering when available")
    void getOrderedTextForParser_prefersLines() {
        OcrResult result = OcrResult.builder()
                .text("fallback text")
                .lines(List.of(
                        OcrResult.OcrSegment.builder().pageNumber(1).kind("line").text("Page1-Line1").build(),
                        OcrResult.OcrSegment.builder().pageNumber(2).kind("line").text("Page2-Line1").build()
                ))
                .build();

        assertThat(result.getOrderedTextForParser()).isEqualTo("Page1-Line1\nPage2-Line1");
    }

    @Test
    @DisplayName("multi-page contract preserves page and boundary structures")
    void multiPageContract_keepsPageAndBoundaryData() {
        OcrResult result = OcrResult.builder()
                .provider("gcv")
                .pages(List.of(
                        OcrResult.OcrPage.builder().pageNumber(1).build(),
                        OcrResult.OcrPage.builder().pageNumber(2).build()
                ))
                .blocks(List.of(
                        OcrResult.OcrSegment.builder().pageNumber(1).kind("block").text("Block 1").build(),
                        OcrResult.OcrSegment.builder().pageNumber(2).kind("block").text("Block 2").build()
                ))
                .lines(List.of(
                        OcrResult.OcrSegment.builder().pageNumber(1).kind("line").text("Line 1").build(),
                        OcrResult.OcrSegment.builder().pageNumber(2).kind("line").text("Line 2").build()
                ))
                .build();

        assertThat(result.getPages()).hasSize(2);
        assertThat(result.getBlocks()).hasSize(2);
        assertThat(result.getLines()).hasSize(2);
        assertThat(result.getLines().get(1).getPageNumber()).isEqualTo(2);
    }
}
