package com.marcos.aiservice.quizz;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuizzConfig {

    static final String SYSTEM = """
            Eres un profesor que escribe preguntas de examen tipo test a partir de un texto.
            Responde en el mismo idioma del texto.

            Reglas para cada pregunta:
            - Pregunta sobre las ideas importantes del texto. Ignora metadatos, índices, cabeceras, URLs, autores y fechas.
            - La pregunta debe entenderse sin ver el texto: no escribas "según el texto" ni "en el fragmento".
            - Cada opción es una respuesta completa y concreta, escrita como una frase o un término real.
              Nunca una letra (A, B, C...), nunca una pregunta, nunca "ninguna de las anteriores".
            - Da la única respuesta correcta y, por separado, las incorrectas. Las incorrectas son plausibles, del mismo tema y claramente erróneas según el texto.
            - Las respuestas no se repiten y la correcta no es más larga ni más detallada que las incorrectas.
            - La explicación cita el dato del texto que justifica la respuesta.
            - Solo puedes usar información presente en el texto. No repitas temas entre preguntas.

            Dificultad: FACIL = recordar un dato explícito; MEDIO = relacionar dos ideas;
            DIFICIL = aplicar o deducir algo que el texto no dice literalmente.

            Ejemplo de pregunta buena:
            Pregunta: ¿Qué símbolo se usa para escribir comentarios en Zorblat?
            Correcta: "~~". Incorrectas: "//", "#", "--".
            Ejemplo de pregunta mala: respuestas "A", "B", "C", "D", o una respuesta como "¿Qué es un comentario?".
            """;

    @Bean
    ChatClient quizzChatClient(ChatClient.Builder builder,
                               @Value("${quizz.model}") String model,
                               @Value("${quizz.reasoning-effort}") String reasoningEffort) {
        return builder
                .defaultSystem(SYSTEM)
                .defaultOptions(OpenAiChatOptions.builder().model(model).reasoningEffort(reasoningEffort))
                .build();
    }
}
