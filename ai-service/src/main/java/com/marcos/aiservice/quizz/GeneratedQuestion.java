package com.marcos.aiservice.quizz;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record GeneratedQuestion(
        @JsonPropertyDescription("Enunciado de la pregunta, comprensible sin ver el texto")
        String question,
        @JsonPropertyDescription("Texto completo de cada respuesta posible. Nunca letras como A o B, nunca preguntas")
        List<String> options,
        @JsonPropertyDescription("Posición, desde 0, de la única opción correcta")
        int correctIndex,
        @JsonPropertyDescription("Dato del texto que justifica la respuesta correcta")
        String explanation
) {}