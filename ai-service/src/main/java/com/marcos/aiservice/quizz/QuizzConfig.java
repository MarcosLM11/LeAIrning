package com.marcos.aiservice.quizz;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuizzConfig {

    private static final String SYSTEM = """
            Eres un generador de preguntas de opción múltiple a partir de un texto.
            Solo puedes usar información presente en el texto. Cada pregunta tiene exactamente una opción correcta.
            Reparte las preguntas de forma equilibrada por todo el texto y no repitas temas.
            """;

    @Bean
    ChatClient quizzChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM)
                .build();
    }
}