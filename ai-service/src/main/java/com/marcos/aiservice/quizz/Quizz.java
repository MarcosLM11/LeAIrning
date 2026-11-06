package com.marcos.aiservice.quizz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Quizz {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private UUID fileId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int requestedQuestions;

    @Column(nullable = false)
    private int options;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizzStatus status = QuizzStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<GeneratedQuestion> questions = new ArrayList<>();

    Quizz(UUID fileId, UUID userId, int requestedQuestions, int options, Difficulty difficulty) {
        this.fileId = fileId;
        this.userId = userId;
        this.requestedQuestions = requestedQuestions;
        this.options = options;
        this.difficulty = difficulty;
    }
}
