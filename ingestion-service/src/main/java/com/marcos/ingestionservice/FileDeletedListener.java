package com.marcos.ingestionservice;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FileDeletedListener {

    private final DocumentLoader loader;

    @KafkaListener(topics = "file.deleted", properties = "spring.json.value.default.type=com.marcos.ingestionservice.FileDeletedEvent")
    void onFileDeleted(FileDeletedEvent event) {
        loader.delete(event.fileId());
    }
}
