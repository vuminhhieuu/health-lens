package com.healthlens.api.service;

import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.Profile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HealthRecordPdfServiceTest {

    private final HealthRecordPdfService service = new HealthRecordPdfService();

    @Test
    @DisplayName("generatePdf render nội dung health record, metrics, explanation, recommendation và disclaimer")
    void generatePdf_containsExpectedContent() throws Exception {
        UUID recordId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setId(profileId);
        profile.setDisplayName("Nguyễn Văn A");
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setProfileId(profileId);
        record.setExamDate(LocalDate.of(2026, 5, 15));
        record.setRecordType("Xét nghiệm máu");
        record.setHospitalName("Bệnh viện Đại học Y");
        record.setDiagnosis("Theo dõi đường huyết");

        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .displayNameVi("Đường huyết")
                .normalizedValue("5.4")
                .normalizedUnit("mmol/L")
                .status("normal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        null,
                        null,
                        "mmol/L"
                ))
                .build();

        byte[] pdf = service.generatePdf(new HealthRecordPdfContext(
                record,
                profile,
                List.of(metric),
                Map.of("Đường huyết", "Glucose phản ánh lượng đường trong máu."),
                List.of("Duy trì vận động đều đặn."),
                "Thông tin chỉ mang tính tham khảo, không thay thế tư vấn y tế chuyên môn.",
                Instant.parse("2026-05-15T06:00:00Z")
        ));

        assertThat(pdf).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("HealthLens - Kết quả khám");
            assertThat(text).contains("Nguyễn Văn A");
            assertThat(text).contains("Xét nghiệm máu");
            assertThat(text).contains("Bệnh viện Đại học Y");
            assertThat(text).contains("Chỉ số");
            assertThat(text).contains("Giá trị");
            assertThat(text).contains("Ngưỡng tham chiếu");
            assertThat(text).contains("Trạng thái");
            assertThat(text).contains("Đường huyết");
            assertThat(text).contains("5.4");
            assertThat(text).contains("Bình thường");
            assertThat(text).contains("Glucose phản ánh lượng đường trong máu.");
            assertThat(text).contains("Duy trì vận động đều đặn.");
            assertThat(text).contains("Thông tin chỉ mang tính tham khảo");
        }
    }
}
