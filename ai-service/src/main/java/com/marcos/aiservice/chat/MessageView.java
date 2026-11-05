package com.marcos.aiservice.chat;

import java.time.Instant;
import java.util.List;

/** role is "user" or "assistant"; sources is only filled for answers grounded on documents. */
public record MessageView(
        String role,
        String text,
        Instant createdAt,
        List<Source> sources
) {

    public record Source(String filename, Integer page) {

        static Source of(AnswerSource s) {
            return new Source(s.getFilename(), s.getPage());
        }
    }
}
