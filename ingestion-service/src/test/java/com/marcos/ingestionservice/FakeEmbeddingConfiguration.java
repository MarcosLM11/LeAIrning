package com.marcos.ingestionservice;

import java.util.Random;
import java.util.stream.IntStream;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
class FakeEmbeddingConfiguration {

    @Bean
    @Primary
    EmbeddingModel fakeEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public @NonNull EmbeddingResponse call(@NonNull EmbeddingRequest request) {
                var texts = request.getInstructions();
                return new EmbeddingResponse(IntStream.range(0, texts.size())
                        .mapToObj(i -> new Embedding(vector(texts.get(i)), i)).toList());
            }

            @Override
            public float @NonNull [] embed(@NonNull Document document) {
                assert document.getText() != null;
                return vector(document.getText());
            }

            private float[] vector(String text) {
                var random = new Random(text.hashCode());
                var v = new float[1536];
                for (int i = 0; i < v.length; i++) v[i] = random.nextFloat();
                return v;
            }
        };
    }
}
