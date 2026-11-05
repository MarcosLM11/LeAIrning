package com.marcos.aiservice.quizz;

import java.time.Instant;
import java.util.UUID;

public record QuizzSummary(
        UUID id,
        UUID fileId,
        QuizzStatus status,
        int questions,
        int options,
        Difficulty difficulty,
        Instant createdAt,
        Integer score
) {
    static QuizzSummary of(Quizz q, Integer score) {
        // The model may drop invalid questions, so once generated the real count wins over the requested one.
        var questions = q.getStatus() == QuizzStatus.READY ? q.getQuestions().size() : q.getRequestedQuestions();
        return new QuizzSummary(q.getId(), q.getFileId(), q.getStatus(), questions, q.getOptions(), q.getDifficulty(), q.getCreatedAt(), score);
    }
}
