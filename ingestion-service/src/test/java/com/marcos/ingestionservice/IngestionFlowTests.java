package com.marcos.ingestionservice;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import com.marcos.ingestionservice.storage.S3Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

class IngestionFlowTests {

    private static FileUploadedEvent event(String name) {
        return new FileUploadedEvent(UUID.randomUUID(), "u", name, "application/octet-stream", 1, Instant.now());
    }

    @Test
    void loaderReplacesPreviousChunksOfTheFile() {
        var store = mock(VectorStore.class);
        var e = event("a.md");
        var chunks = List.of(new Document("x"));
        new DocumentLoader(store).load(e, chunks);
        var order = inOrder(store);
        order.verify(store).delete("fileId == '" + e.fileId() + "'");
        order.verify(store).add(chunks);
    }

    @Test
    void unsupportedFileIsSkippedWithoutTouchingS3() throws Exception {
        var s3 = mock(S3Service.class);
        new IngestionService(s3, new DocumentExtractor(), new DocumentChunker(), mock(DocumentLoader.class))
                .ingest(event("a.exe"));
        verifyNoInteractions(s3);
    }
}
