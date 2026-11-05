package com.marcos.aiservice.quizz;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/quizz")
@RequiredArgsConstructor
public class QuizzController {

    private final QuizzService quizzService;

    @PostMapping("/{fileId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UUID start(@PathVariable UUID fileId,
                      @RequestParam(defaultValue = "10") int questions,
                      @RequestParam(defaultValue = "4") int options,
                      @RequestParam(defaultValue = "MEDIO") Difficulty difficulty,
                      @AuthenticationPrincipal Jwt jwt) {
        return quizzService.start(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("email"), fileId, questions, options, difficulty);
    }

    @GetMapping
    public List<QuizzSummary> list(Authentication auth) {
        return quizzService.list(userId(auth));
    }

    @GetMapping("/{id}")
    public QuizzView get(@PathVariable UUID id, Authentication auth) {
        return quizzService.get(userId(auth), id);
    }

    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void retry(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        quizzService.retry(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("email"), id);
    }

    @PostMapping("/{id}/attempts")
    public AttemptResult submit(@PathVariable UUID id, @RequestBody AttemptRequest request, Authentication auth) {
        return quizzService.submit(userId(auth), id, request.answers());
    }

    private static UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
