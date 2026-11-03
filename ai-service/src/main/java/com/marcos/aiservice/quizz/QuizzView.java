package com.marcos.aiservice.quizz;

import java.util.List;
import java.util.UUID;

public record QuizzView(UUID id, QuizzStatus status, Strategy strategy, List<QuestionView> questions) {

    public record QuestionView(UUID id, String text, List<String> options) {}

    static QuizzView of(Quizz quizz) {
        var questions = quizz.getQuestions().stream()
                .map(q -> new QuestionView(q.getId(), q.getText(), q.getOptions()))
                .toList();
        return new QuizzView(quizz.getId(), quizz.getStatus(), quizz.getStrategy(), questions);
    }
}
