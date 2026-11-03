package com.marcos.aiservice.quizz;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuizzController {

    private final QuizzService quizzService;

    @PostMapping("/quizz/{fileId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UUID start(@PathVariable UUID fileId,
                      @RequestParam(defaultValue = "10") int questions,
                      @RequestParam(defaultValue = "4") int options,
                      @RequestParam(defaultValue = "MEDIO") Difficulty difficulty,
                      Authentication auth) {
        return quizzService.start(userId(auth), fileId, questions, options, difficulty);
    }

    @GetMapping("/quizz/{id}")
    public QuizzView get(@PathVariable UUID id, Authentication auth) {
        return quizzService.get(userId(auth), id);
    }

    private static UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
