package com.marcos.ingestionservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class FileUploadedListener {

    static final String INGESTED_TOPIC = "file.ingested";
    private final IngestionService ingestion;
    private final KafkaTemplate<String, FileIngestedEvent> kafka;

    @KafkaListener(topics = "file.uploaded")
    void onFileUploaded(FileUploadedEvent event) {
        log.info("File uploaded event received: {}", event);
        boolean success;
        try {
            success = ingestion.ingest(event);
        } catch (Exception e) {
            log.error("Ingestion of {} failed", event.fileId(), e);
            success = false;
        }
        kafka.send(INGESTED_TOPIC, event.fileId().toString(), new FileIngestedEvent(event.fileId(), success));
    }
}
