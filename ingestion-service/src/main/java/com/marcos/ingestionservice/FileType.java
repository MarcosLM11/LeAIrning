package com.marcos.ingestionservice;

import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;

enum FileType {
    PDF,
    MARKDOWN;

    DocumentReader reader(Resource r) {
        return switch (this) {
            case PDF -> new PagePdfDocumentReader(r);
            case MARKDOWN -> new MarkdownDocumentReader(r, MarkdownDocumentReaderConfig.builder()
                            .withHorizontalRuleCreateDocument(true)
                            .build()
            );
        };
    }

    static FileType of(FileUploadedEvent e) {
        var name = e.name().toLowerCase();
        var byExtension = switch (name.substring(name.lastIndexOf('.') + 1)) {
            case "pdf" -> PDF;
            case "md", "markdown" -> MARKDOWN;
            default -> null;
        };
        if (byExtension != null || e.contentType() == null) return byExtension;
        return switch (e.contentType().split(";")[0].trim()) {
            case "application/pdf" -> PDF;
            case "text/markdown" -> MARKDOWN;
            default -> null;
        };
    }
}
