package com.marcos.aiservice.quizz;

import java.util.List;

public record Quizz(
        List<QuizzQuestion> questions
) {}