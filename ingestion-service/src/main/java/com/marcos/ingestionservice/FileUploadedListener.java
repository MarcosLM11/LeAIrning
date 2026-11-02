package com.marcos.ingestionservice;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class FileUploadedListener {

    private final IngestionService ingestion;

    @KafkaListener(topics = "file.uploaded")
    void onFileUploaded(FileUploadedEvent event) throws IOException {
        log.info("File uploaded event received: {}", event);
        ingestion.ingest(event);
    }
}
