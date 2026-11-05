package com.marcos.fileservice;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FileIngestedListener {

    private final FileService service;

    @KafkaListener(topics = "file.ingested")
    void onFileIngested(FileIngestedEvent event) {
        service.markIngested(event.fileId(), event.success());
    }
}
