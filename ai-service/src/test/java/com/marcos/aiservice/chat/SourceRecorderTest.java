package com.marcos.aiservice.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;

class SourceRecorderTest {

    private final AnswerSourceRepository repository = mock(AnswerSourceRepository.class);
    private final SourceRecorder recorder = new SourceRecorder(repository);
    private final UUID fileId = UUID.randomUUID();

    private static Document chunk(UUID fileId, String name, Integer page) {
        var metadata = new HashMap<String, Object>(Map.of("fileId", fileId.toString(), "filename", name));
        if (page != null) metadata.put("page_number", page);
        return new Document("texto", metadata);
    }

    private static ChatClientResponse response(Map<String, Object> context) {
        return ChatClientResponse.builder().context(context).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void recordsDistinctSourcesUnderTheReturnedEventId() {
        var markdown = UUID.randomUUID();
        var documents = List.of(chunk(fileId, "kafka.pdf", 12), chunk(fileId, "kafka.pdf", 12), chunk(fileId, "kafka.pdf", 3), chunk(markdown, "notas.md", null));

        var eventId = recorder.generate(response(Map.of(
                RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, documents,
                SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, "u:1")), new AssistantMessage("ok"));

        var saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat((List<AnswerSource>) saved.getValue())
                .extracting(AnswerSource::getEventId, AnswerSource::getSessionId, AnswerSource::getFileId, AnswerSource::getFilename, AnswerSource::getPage)
                .containsExactly(tuple(eventId, "u:1", fileId, "kafka.pdf", 12), tuple(eventId, "u:1", fileId, "kafka.pdf", 3),
                        tuple(eventId, "u:1", markdown, "notas.md", null));
    }

    @Test
    void failingToRecordNeverBreaksStoringTheAnswer() {
        var broken = new Document("texto", Map.of("filename", "sin-id.pdf"));

        var eventId = recorder.generate(response(Map.of(
                RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, List.of(broken),
                SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, "u:1")), new AssistantMessage("ok"));

        assertThat(eventId).isNotBlank();
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void answersWithoutRetrievedDocumentsHaveNoSources() {
        var eventId = recorder.generate(response(Map.of(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, "u:1")), new AssistantMessage("ok"));

        assertThat(eventId).isNotBlank();
        verify(repository, never()).saveAll(anyList());
    }
}
