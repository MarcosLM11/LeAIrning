package com.marcos.aiservice.chat;

import com.marcos.aiservice.FileDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ChatFileDeletedListener {

    private final AnswerSourceRepository sources;

    @KafkaListener(topics = "file.deleted", groupId = "ai-service-chat-sources", properties = "spring.json.value.default.type=com.marcos.aiservice.FileDeletedEvent")
    void onFileDeleted(FileDeletedEvent event) {
        sources.deleteByFileId(event.fileId());
    }
}
