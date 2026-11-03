package com.marcos.aiservice.quizz;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/quizz/{userId}/{fileId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UUID start(@PathVariable UUID userId,
                      @PathVariable UUID fileId,
                      @RequestParam(defaultValue = "MAP") Strategy strategy,
                      @RequestParam(defaultValue = "10") int questions,
                      @RequestParam(defaultValue = "4") int options,
                      @RequestParam(defaultValue = "MEDIO") Difficulty difficulty) {
        return quizzService.start(userId, fileId, strategy, questions, options, difficulty);
    }

    @GetMapping("/quizz/{userId}/{id}")
    public QuizzView get(@PathVariable UUID userId, @PathVariable UUID id) {
        return quizzService.get(userId, id);
    }
}
