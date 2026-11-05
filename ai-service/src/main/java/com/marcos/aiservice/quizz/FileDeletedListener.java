package com.marcos.aiservice.quizz;

import com.marcos.aiservice.FileDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FileDeletedListener {

    private final QuizzService service;

    @KafkaListener(topics = "file.deleted", properties = "spring.json.value.default.type=com.marcos.aiservice.FileDeletedEvent")
    void onFileDeleted(FileDeletedEvent event) {
        service.deleteByFile(event.fileId());
    }
}
