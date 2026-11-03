package com.marcos.aiservice.quizz;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record Concepts(
        @JsonPropertyDescription("Cada concepto importante con su explicación breve, en una sola cadena")
        List<String> concepts
) {}
