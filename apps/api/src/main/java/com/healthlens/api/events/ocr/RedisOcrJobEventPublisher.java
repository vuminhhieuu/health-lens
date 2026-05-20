package com.healthlens.api.events.ocr;

import com.healthlens.api.events.ApplicationStreamNames;
import com.healthlens.api.events.ApplicationStreamPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RedisOcrJobEventPublisher implements OcrJobEventPublisher {

    private final ApplicationStreamPublisher streamPublisher;
    private final String ocrStreamName;

    public RedisOcrJobEventPublisher(
            ApplicationStreamPublisher streamPublisher,
            @Value("${app.stream.ocr-events:" + ApplicationStreamNames.OCR_EVENTS + "}") String ocrStreamName
    ) {
        this.streamPublisher = streamPublisher;
        this.ocrStreamName = ocrStreamName;
    }

    @Override
    public void publish(OcrJobEvent event) {
        streamPublisher.publish(ocrStreamName, event.toStreamMap());
    }

    @Override
    public void publishAfterCommit(OcrJobEvent event) {
        streamPublisher.publishAfterCommit(ocrStreamName, event.toStreamMap());
    }
}
