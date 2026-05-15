package com.healthlens.api.service;

import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.dto.ReferenceRangeDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HealthRecordPdfService {

    private static final String DISCLAIMER =
            "Thông tin chỉ mang tính tham khảo, không thay thế tư vấn y tế chuyên môn.";
    private static final float MARGIN = 48f;
    private static final float LEADING = 15f;
    private static final DateTimeFormatter GENERATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public byte[] generatePdf(HealthRecordPdfContext context) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont regularFont = loadVietnameseFont(document);
            PDFont boldFont = loadVietnameseBoldFont(document, regularFont);
            PdfWriter writer = new PdfWriter(document, regularFont, boldFont);

            writer.heading("HealthLens - Kết quả khám");
            writer.text("Ngày tạo PDF: " + GENERATED_AT_FORMATTER.format(context.generatedAt()));
            writer.blank();
            writer.subheading("Thông tin hồ sơ");
            writer.text("Tên hồ sơ: " + safe(context.profile().getDisplayName(), "Chưa cập nhật"));
            writer.text("Ngày khám: " + safe(context.record().getExamDate(), "Chưa có ngày khám"));
            writer.text("Loại phiếu: " + safe(context.record().getRecordType(), "Phiếu khám bệnh"));
            writer.text("Cơ sở y tế: " + safe(context.record().getHospitalName(), "Chưa cập nhật"));
            if (hasText(context.record().getDiagnosis())) {
                writer.text("Chẩn đoán/ghi chú: " + context.record().getDiagnosis());
            }

            writer.blank();
            writer.subheading("Tóm tắt trạng thái");
            writer.text(resolveOverallSummary(context.metrics()));

            writer.blank();
            writer.subheading("Bảng chỉ số");
            if (context.metrics().isEmpty()) {
                writer.text("Chưa có chỉ số được lưu cho kết quả khám này.");
            } else {
                writer.table(
                        List.of("Chỉ số", "Giá trị", "Ngưỡng tham chiếu", "Trạng thái"),
                        context.metrics().stream()
                                .map(metric -> List.of(
                                        metricName(metric),
                                        metricValue(metric),
                                        rangeText(metric.getReferenceRange()),
                                        statusLabel(metric.getStatus())
                                ))
                                .toList()
                );
            }

            writer.space(18f);
            writer.subheading("Giải thích");
            if (context.explanations().isEmpty()) {
                writer.text("Chưa có giải thích tại thời điểm tải xuống.");
            } else {
                for (Map.Entry<String, String> entry : context.explanations().entrySet()) {
                    writer.text(metricLabel(entry.getKey()) + ": " + safe(entry.getValue(), "Chưa có giải thích tại thời điểm tải xuống."));
                }
            }

            writer.blank();
            writer.subheading("Khuyến nghị");
            List<String> recommendations = context.recommendations();
            if (recommendations == null || recommendations.isEmpty()) {
                writer.text("Chưa có khuyến nghị tại thời điểm tải xuống.");
            } else {
                for (String recommendation : recommendations) {
                    writer.text("• " + recommendation);
                }
            }

            writer.blank();
            writer.subheading("Lưu ý y tế");
            writer.text(firstText(context.recommendationsDisclaimer(), DISCLAIMER));
            writer.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể tạo PDF kết quả khám", ex);
        }
    }

    private PDFont loadVietnameseFont(PDDocument document) throws IOException {
        InputStream bundledFont = HealthRecordPdfService.class.getClassLoader()
                .getResourceAsStream("fonts/Arial.ttf");
        if (bundledFont != null) {
            try (InputStream stream = bundledFont) {
                return PDType0Font.load(document, stream);
            }
        }

        bundledFont = HealthRecordPdfService.class.getClassLoader()
                .getResourceAsStream("fonts/NotoSans-Regular.ttf");
        if (bundledFont != null) {
            try (InputStream stream = bundledFont) {
                return PDType0Font.load(document, stream);
            }
        }

        for (String path : List.of(
                "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                "/Library/Fonts/Arial Unicode.ttf"
        )) {
            Path fontPath = Path.of(path);
            if (Files.exists(fontPath)) {
                return PDType0Font.load(document, fontPath.toFile());
            }
        }

        throw new IllegalStateException(
                "Khong tim thay font Unicode de tao PDF tieng Viet. " +
                "Hay bundle fonts/Arial.ttf hoac fonts/NotoSans-Regular.ttf, " +
                "hoac cai dat NotoSans/Arial Unicode tren host."
        );
    }

    private PDFont loadVietnameseBoldFont(PDDocument document, PDFont fallbackFont) throws IOException {
        for (String fontPath : List.of(
                "fonts/Arial-Bold.ttf",
                "fonts/NotoSans-Bold.ttf",
                "fonts/DejaVuSans-Bold.ttf"
        )) {
            try (InputStream stream = HealthRecordPdfService.class.getClassLoader().getResourceAsStream(fontPath)) {
                if (stream != null) {
                    return PDType0Font.load(document, stream);
                }
            }
        }

        for (String path : List.of(
                "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
                "/Library/Fonts/Arial Bold.ttf"
        )) {
            Path fontPath = Path.of(path);
            if (Files.exists(fontPath)) {
                return PDType0Font.load(document, fontPath.toFile());
            }
        }

        return fallbackFont;
    }

    private String resolveOverallSummary(List<MetricDto> metrics) {
        boolean abnormal = metrics.stream().anyMatch(metric -> "abnormal".equalsIgnoreCase(metric.getStatus()));
        boolean attention = metrics.stream().anyMatch(metric ->
                "attention".equalsIgnoreCase(metric.getStatus()) || "warning".equalsIgnoreCase(metric.getStatus()));
        if (abnormal) {
            return "Tổng quan: Bất thường - có chỉ số cần theo dõi.";
        }
        if (attention) {
            return "Tổng quan: Cần chú ý - có chỉ số lệch ngưỡng nhẹ.";
        }
        return "Tổng quan: Bình thường.";
    }

    private String metricName(MetricDto metric) {
        return firstText(metric.getDisplayNameVi(), metric.getName(), metric.getRawName(), "Chỉ số");
    }

    private String rangeText(ReferenceRangeDto range) {
        if (range == null) {
            return "Không có";
        }
        return numberText(range.min()) + " - " + numberText(range.max()) + " " + safe(range.unit(), "");
    }

    private String metricValue(MetricDto metric) {
        String value = safe(firstText(metric.getNormalizedValue(), metric.getValue()), "N/A");
        String unit = firstText(metric.getNormalizedUnit(), metric.getUnit());
        return unit.isBlank() ? value : value + " " + unit;
    }

    private String numberText(BigDecimal value) {
        return value == null ? "?" : value.stripTrailingZeros().toPlainString();
    }

    private String statusLabel(String status) {
        if (status == null) {
            return "Chưa phân loại";
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "normal" -> "Bình thường";
            case "attention", "warning" -> "Cần chú ý";
            case "abnormal" -> "Bất thường";
            case "no_data" -> "Chưa có dữ liệu";
            default -> status;
        };
    }

    private String metricLabel(String value) {
        return hasText(value) ? value : "Chỉ số";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static final class PdfWriter {
        private final PDDocument document;
        private final PDFont regularFont;
        private final PDFont boldFont;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        private PdfWriter(PDDocument document, PDFont regularFont, PDFont boldFont) throws IOException {
            this.document = document;
            this.regularFont = regularFont;
            this.boldFont = boldFont;
            newPage();
        }

        private void heading(String text) throws IOException {
            writeWrapped(text, boldFont, 18f, 18f);
        }

        private void subheading(String text) throws IOException {
            writeWrapped(text, boldFont, 13f, 16f);
        }

        private void text(String text) throws IOException {
            writeWrapped(text, regularFont, 10.5f, LEADING);
        }

        private void table(List<String> headers, List<List<String>> rows) throws IOException {
            float tableWidth = page.getMediaBox().getWidth() - (MARGIN * 2);
            float[] columnRatios = new float[]{0.28f, 0.20f, 0.28f, 0.24f};
            float[] columnWidths = new float[columnRatios.length];
            for (int i = 0; i < columnRatios.length; i++) {
                columnWidths[i] = tableWidth * columnRatios[i];
            }

            drawTableRow(headers, columnWidths, true);
            for (List<String> row : rows) {
                drawTableRow(row, columnWidths, false);
            }
        }

        private void blank() throws IOException {
            y -= 8f;
            ensureSpace(LEADING);
        }

        private void space(float height) throws IOException {
            y -= height;
            ensureSpace(LEADING);
        }

        private void close() throws IOException {
            if (content != null) {
                content.close();
            }
        }

        private void newPage() throws IOException {
            if (content != null) {
                content.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void writeWrapped(String text, PDFont font, float fontSize, float leading) throws IOException {
            for (String line : wrap(text, font, fontSize, page.getMediaBox().getWidth() - (MARGIN * 2))) {
                ensureSpace(leading);
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(MARGIN, y);
                content.showText(line);
                content.endText();
                y -= leading;
            }
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < MARGIN) {
                newPage();
            }
        }

        private void drawTableRow(List<String> cells, float[] columnWidths, boolean header) throws IOException {
            float fontSize = header ? 10f : 9.5f;
            float lineHeight = header ? 14f : 13f;
            float cellPaddingX = 6f;
            float cellPaddingY = 5f;
            PDFont font = header ? boldFont : regularFont;
            List<List<String>> wrappedCells = new ArrayList<>();
            int maxLines = 1;

            for (int i = 0; i < columnWidths.length; i++) {
                String cellText = i < cells.size() ? safe(cells.get(i), "") : "";
                List<String> wrapped = wrap(cellText, font, fontSize, columnWidths[i] - (cellPaddingX * 2));
                wrappedCells.add(wrapped);
                maxLines = Math.max(maxLines, wrapped.size());
            }

            float rowHeight = (maxLines * lineHeight) + (cellPaddingY * 2);
            ensureSpace(rowHeight);

            float x = MARGIN;
            float rowTop = y;
            float rowBottom = y - rowHeight;

            for (int i = 0; i < columnWidths.length; i++) {
                content.addRect(x, rowBottom, columnWidths[i], rowHeight);
                content.stroke();

                float textY = rowTop - cellPaddingY - fontSize;
                for (String line : wrappedCells.get(i)) {
                    content.beginText();
                    content.setFont(font, fontSize);
                    content.newLineAtOffset(x + cellPaddingX, textY);
                    content.showText(line);
                    content.endText();
                    textY -= lineHeight;
                }
                x += columnWidths[i];
            }

            y -= rowHeight;
        }

        private List<String> wrap(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String remaining = text == null ? "" : text.replace('\n', ' ').trim();
            if (remaining.isEmpty()) {
                lines.add("");
                return lines;
            }
            StringBuilder line = new StringBuilder();
            for (String word : remaining.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.getStringWidth(candidate) / 1000f * fontSize <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                    }
                    line = new StringBuilder(word);
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            return lines;
        }
    }
}
