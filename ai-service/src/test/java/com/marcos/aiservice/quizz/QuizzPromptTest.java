package com.marcos.aiservice.quizz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

class QuizzPromptTest {

    @Test
    void schemaTellsTheModelWhatOptionsAre() {
        var schema = new BeanOutputConverter<>(GeneratedQuizz.class).getJsonSchema();

        assertThat(schema).contains("Texto completo de cada respuesta posible");
    }

    @Test
    void systemPromptHasNoTemplateBraces() {
        assertThat(QuizzConfig.SYSTEM).doesNotContain("{").doesNotContain("}");
    }
}
