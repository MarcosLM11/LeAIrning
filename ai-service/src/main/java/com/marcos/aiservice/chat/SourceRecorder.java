package com.marcos.aiservice.chat;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.session.advisor.SessionEventResponseIdGenerator;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.stereotype.Component;

/**
 * Names each stored assistant message and, at that moment, records the documents it was grounded on.
 * It is the only place that knows both the message id and the retrieved documents; the memory advisor
 * does not persist them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SourceRecorder implements SessionEventResponseIdGenerator {

    private final AnswerSourceRepository repository;

    @Override
    public String generate(ChatClientResponse response, Message message) {
        var eventId = UUID.randomUUID().toString();
        var documents = (List<Document>) response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        if (documents != null && !documents.isEmpty()) {
            var sessionId = (String) response.context().get(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY);
            try {
                // Several chunks of the same page count once.
                var distinct = documents.stream()
                        .map(d -> new Key((String) d.getMetadata().get("fileId"), (String) d.getMetadata().get("filename"), (Integer) d.getMetadata().get("page_number")))
                        .distinct()
                        .toList();
                repository.saveAll(distinct.stream()
                        .map(k -> new AnswerSource(eventId, sessionId, UUID.fromString(k.fileId()), k.filename(), k.page()))
                        .toList());
            } catch (RuntimeException e) {
                // Sources are optional: failing here must never lose the answer being stored.
                log.warn("Could not record the sources of message {}", eventId, e);
            }
        }
        return eventId;
    }

    private record Key(String fileId, String filename, Integer page) {}
}
