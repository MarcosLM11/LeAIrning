package com.marcos.aiservice.quizz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Quizz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID fileId;

    @Column(nullable = false)
    private UUID userId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizzStatus status = QuizzStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // A question is identified by its position in this list.
    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<GeneratedQuestion> questions = new ArrayList<>();

    Quizz(UUID fileId, UUID userId) {
        this.fileId = fileId;
        this.userId = userId;
    }
}
