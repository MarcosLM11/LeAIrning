package com.marcos.aiservice.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A document fragment an assistant message was grounded on. page is null for files without pages (Markdown). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AnswerSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private UUID fileId;

    @Column(nullable = false)
    private String filename;

    private Integer page;

    AnswerSource(String eventId, String sessionId, UUID fileId, String filename, Integer page) {
        this.eventId = eventId;
        this.sessionId = sessionId;
        this.fileId = fileId;
        this.filename = filename;
        this.page = page;
    }
}
