package com.healthlens.api.service;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import com.healthlens.api.service.ocr.OcrCapability;
import com.healthlens.api.service.ocr.OcrJob;
import com.healthlens.api.service.ocr.OcrProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class PdfTextOcrProvider implements OcrProvider {

    @Override
    public String name() {
        return "pdfbox";
    }

    @Override
    public Set<OcrCapability> capabilities() {
        return Set.of(OcrCapability.PDF_TEXT);
    }

    @Override
    public OcrResult extract(OcrJob job) {
        if (!job.hasDocumentBytes()) {
            throw new OcrProcessingException("PDF text provider requires document bytes");
        }
        long started = System.nanoTime();
        try (PDDocument document = Loader.loadPDF(job.documentBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder combinedText = new StringBuilder();
            List<OcrResult.OcrPage> pages = new ArrayList<>();
            List<OcrResult.OcrSegment> lines = new ArrayList<>();
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();
                if (!pageText.isBlank()) {
                    if (!combinedText.isEmpty()) {
                        combinedText.append("\n\n");
                    }
                    combinedText.append(pageText);
                    lines.add(OcrResult.OcrSegment.builder()
                            .pageNumber(page)
                            .text(pageText)
                            .confidence(1.0f)
                            .kind("line")
                            .build());
                }
                pages.add(OcrResult.OcrPage.builder()
                        .pageNumber(page)
                        .text(pageText)
                        .confidence(pageText.isBlank() ? 0.0f : 1.0f)
                        .build());
            }
            String text = combinedText.toString();
            log.info("ocr_route_selected route=pdf-text-layer provider=pdfbox mimeType={} success={} pages={} totalLatencyMs={}",
                    job.mimeType(),
                    !text.isBlank(),
                    pageCount,
                    elapsedMs(started));
            return OcrResult.builder()
                    .provider(text.isBlank() ? "pdf-text-layer-empty" : "pdf-text-layer")
                    .modelVersion("pdfbox")
                    .mimeType(job.mimeType())
                    .retentionMode("transient")
                    .latencyMs(elapsedMs(started))
                    .text(text)
                    .confidence(text.isBlank() ? 0.0f : 1.0f)
                    .language("unknown")
                    .pages(pages)
                    .blocks(List.of())
                    .lines(lines)
                    .diagnostics(List.of())
                    .build();
        } catch (IOException ex) {
            throw new OcrProcessingException("PDF text extraction failed", ex);
        }
    }

    private long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
