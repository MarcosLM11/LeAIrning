package com.marcos.aiservice.quizz;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record GeneratedQuizz(
        @JsonPropertyDescription("Preguntas del test, sin repetir temas")
        List<GeneratedQuestion> questions
) {}