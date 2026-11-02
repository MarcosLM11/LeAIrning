package com.marcos.ingestionservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class ExtractorStreamTests {

    private final DocumentExtractor extractor = new DocumentExtractor();

    private static FileUploadedEvent event(String name) {
        return new FileUploadedEvent(UUID.randomUUID(), "u", name, null, 1, Instant.now());
    }

    @Test
    void markdownFromStreamGetsMetadata() {
        var e = event("a.md");
        var docs = extractor.extract(FileType.MARKDOWN, new ByteArrayInputStream("# Title\n\nhello world\n".getBytes()), e);
        assertFalse(docs.isEmpty());
        assertEquals("a.md", docs.getFirst().getMetadata().get("filename"));
        assertEquals(e.fileId().toString(), docs.getFirst().getMetadata().get("fileId"));
    }

    @Test
    void pdfFromStreamIsReadable() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var pdf = new PDDocument()) {
            var page = new PDPage();
            pdf.addPage(page);
            try (var cs = new PDPageContentStream(pdf, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("hello pdf");
                cs.endText();
            }
            pdf.save(out);
        }
        var docs = extractor.extract(FileType.PDF, new ByteArrayInputStream(out.toByteArray()), event("a.pdf"));

        assertEquals("hello pdf", docs.getFirst().getText().replaceAll("\\s+", " ").trim());
        assertEquals("a.pdf", docs.getFirst().getMetadata().get("filename"));
    }
}
