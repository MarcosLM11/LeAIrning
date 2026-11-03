package com.marcos.aiservice.quizz;

import java.util.List;
import java.util.UUID;

public record QuizzView(UUID id, QuizzStatus status, List<QuestionView> questions) {

    public record QuestionView(String text, List<String> options) {}

    static QuizzView of(Quizz quizz) {
        var questions = quizz.getQuestions().stream()
                .map(q -> new QuestionView(q.question(), q.options()))
                .toList();
        return new QuizzView(quizz.getId(), quizz.getStatus(), questions);
    }
}
