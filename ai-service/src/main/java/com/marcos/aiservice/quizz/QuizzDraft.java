package com.marcos.aiservice.quizz;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * What the model writes. It does not place the correct answer: the code shuffles it, because asked for a
 * position the model marked the wrong option in ~8% of the questions and favoured the first slots.
 */
public record QuizzDraft(
        @JsonPropertyDescription("Preguntas del test, sin repetir temas")
        List<Question> questions
) {

    public record Question(
            @JsonPropertyDescription("Enunciado de la pregunta, comprensible sin ver el texto")
            String question,
            @JsonPropertyDescription("Texto completo de la única respuesta correcta")
            String correctAnswer,
            @JsonPropertyDescription("Texto completo de cada respuesta incorrecta pero plausible, todas distintas entre sí")
            List<String> wrongAnswers,
            @JsonPropertyDescription("Dato del texto que justifica la respuesta correcta")
            String explanation
    ) {}
}
