package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.OcrResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleCloudVisionClientTest {

    @Mock
    private RestTemplate restTemplate;

    private GoogleCloudVisionClient client;

    @BeforeEach
    void setUp() {
        client = new GoogleCloudVisionClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(client, "gcvEnabled", true);
        ReflectionTestUtils.setField(client, "gcvApiKey", "test-key");
        ReflectionTestUtils.setField(client, "gcvEndpoint", "https://vision.example.test/v1/images:annotate");
        ReflectionTestUtils.setField(client, "gcvProjectId", "test-project");
    }

    @Test
    @DisplayName("GCV mapping preserves page-level confidence")
    void extract_preservesPageConfidence() {
        String responseBody = """
                {
                  "responses": [
                    {
                      "fullTextAnnotation": {
                        "text": "Glucose",
                        "pages": [
                          {
                            "confidence": 0.77,
                            "property": {"detectedLanguages": [{"languageCode": "vi"}]},
                            "blocks": [
                              {
                                "confidence": 0.80,
                                "boundingBox": {"vertices": [{"x": 1, "y": 2}, {"x": 11, "y": 2}, {"x": 11, "y": 12}, {"x": 1, "y": 12}]},
                                "paragraphs": [
                                  {
                                    "confidence": 0.79,
                                    "boundingBox": {"vertices": [{"x": 1, "y": 2}, {"x": 11, "y": 2}, {"x": 11, "y": 12}, {"x": 1, "y": 12}]},
                                    "words": [{"symbols": [{"text": "G"}, {"text": "l"}, {"text": "u"}]}]
                                  }
                                ]
                              }
                            ]
                          }
                        ]
                      }
                    }
                  ]
                }
                """;
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        OcrResult result = client.extract("https://example.com/report.jpg");

        assertThat(result.getPages()).hasSize(1);
        assertThat(result.getPages().get(0).getConfidence()).isEqualTo(0.77f);
        assertThat(result.getBlocks().get(0).getConfidence()).isEqualTo(0.80f);
        assertThat(result.getLines().get(0).getConfidence()).isEqualTo(0.79f);
    }
}
