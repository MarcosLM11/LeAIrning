package com.marcos.aiservice.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

class ChatServiceRagFilterTest {

    @Test
    void ragSearchIsFilteredByRequestingUser() {
        var userId = UUID.randomUUID();

        assertThat(searchFilter(userId, List.of()))
                .isEqualTo(new FilterExpressionBuilder().eq("userId", userId.toString()).build());
    }

    @Test
    void ragSearchIsNarrowedToTheSelectedFiles() {
        var userId = UUID.randomUUID();
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        var builder = new FilterExpressionBuilder();

        assertThat(searchFilter(userId, List.of(a, b))).isEqualTo(
                builder.and(builder.eq("userId", userId.toString()), builder.in("fileId", a.toString(), b.toString())).build());
    }

    private Filter.Expression searchFilter(UUID userId, List<UUID> fileIds) {
        var model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(ChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
        var vectorStore = mock(VectorStore.class);
        var client = ChatClient.builder(model)
                .defaultAdvisors(RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
                        .build())
                .build();

        new ChatService(client, mock(SessionService.class), mock(AnswerSourceRepository.class)).ask("hola", "1", userId, fileIds);

        var request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        return request.getValue().getFilterExpression();
    }
}
