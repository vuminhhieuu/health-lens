package com.healthlens.api.events.ocr;

public interface OcrJobEventPublisher {

    void publish(OcrJobEvent event);

    void publishAfterCommit(OcrJobEvent event);
}
