package com.marcos.aiservice.quizz;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuizzController {

    private final QuizzService quizzService;

    @PostMapping("/quizz/{fileId}")
    public Quizz generate(@PathVariable UUID fileId,
                          @RequestParam(defaultValue = "MAP") Strategy strategy,
                          @RequestParam(defaultValue = "10") int questions,
                          @RequestParam(defaultValue = "4") int options,
                          @RequestParam(defaultValue = "MEDIO") Difficulty difficulty) {
        return quizzService.generate(fileId, strategy, questions, options, difficulty);
    }
}
