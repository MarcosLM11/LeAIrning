package com.marcos.ingestionservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class FileUploadedListener {

    @KafkaListener(topics = "file.uploaded")
    void onFileUploaded(FileUploadedEvent event) {
        log.info("File uploaded event received: {}", event);
    }
}
