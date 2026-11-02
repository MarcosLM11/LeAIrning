package com.marcos.aiservice.quizz;

import java.util.List;

public record GeneratedQuestion(
        String question,
        List<String> options,
        int correctIndex,
        String explanation
) {}