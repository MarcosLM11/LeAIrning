package com.marcos.userservice.security;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("api/v1/auth")
class AuthController {

    private static final String COOKIE = "refreshToken";
    private final AuthService service;
    private final boolean cookieSecure;

    AuthController(AuthService service, @Value("${auth.cookie-secure:false}") boolean cookieSecure) {
        this.service = service;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest r) {
        return respond(service.login(r));
    }

    @PostMapping("/refresh")
    ResponseEntity<AccessTokenResponse> refresh(@CookieValue(value = COOKIE, required = false) String token) {
        if (token == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        return respond(service.refresh(token));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(value = COOKIE, required = false) String token) {
        if (token != null) service.logout(token);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0).toString()).build();
    }

    private ResponseEntity<AccessTokenResponse> respond(Tokens t) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(t.refreshToken(), AuthService.REFRESH_TTL.toSeconds()).toString())
                .body(new AccessTokenResponse(t.accessToken()));
    }

    private ResponseCookie cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
