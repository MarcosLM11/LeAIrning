package com.marcos.aiservice.quizz;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * What the model writes. It does not place the correct answer: the code shuffles it.
 */
public record QuizzDraft(
        @JsonPropertyDescription("Preguntas del test, sin repetir temas")
        List<Question> questions
) {

    public record Question(
            @JsonPropertyDescription("Enunciado de la pregunta, comprensible sin ver el texto")
            String question,
            @JsonPropertyDescription("Texto completo de la única respuesta correcta, breve y directa, sin detalles de más")
            String correctAnswer,
            @JsonPropertyDescription("Texto completo de cada respuesta incorrecta pero plausible. Cada una es igual o más larga y detallada que la correcta, con la misma estructura de frase, y todas son distintas entre sí")
            List<String> wrongAnswers,
            @JsonPropertyDescription("Dato del texto que justifica la respuesta correcta")
            String explanation
    ) {}
}
