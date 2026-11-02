package com.marcos.ingestionservice;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
class DocumentLoader {

    private final VectorStore vectorStore;

    DocumentLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    void load(FileUploadedEvent event, List<Document> chunks) {
        // Kafka is at-least-once: drop previous chunks so a redelivery doesn't duplicate them.
        vectorStore.delete("fileId == '" + event.fileId() + "'");
        vectorStore.add(chunks);
    }
}
