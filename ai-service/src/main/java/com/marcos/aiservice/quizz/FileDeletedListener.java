package com.marcos.aiservice.quizz;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FileDeletedListener {

    private final QuizzService service;

    @KafkaListener(topics = "file.deleted", properties = "spring.json.value.default.type=com.marcos.aiservice.quizz.FileDeletedEvent")
    void onFileDeleted(FileDeletedEvent event) {
        service.deleteByFile(event.fileId());
    }
}
