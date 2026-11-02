package com.marcos.ingestionservice;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

@Component
class DocumentChunker {

    // shortcut: default splitter (800 tokens), tune chunk size/overlap once retrieval quality is measured.
    private final TokenTextSplitter splitter = TokenTextSplitter
            .builder()
            .build();

    List<Document> chunk(List<Document> docs) {
        return splitter.split(docs);
    }
}
