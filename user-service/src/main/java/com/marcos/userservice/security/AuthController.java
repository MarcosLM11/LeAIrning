package com.marcos.userservice.security;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService service;

    @PostMapping("/login")
    Map<String, String> login(@Valid @RequestBody LoginRequest r) {
        return Map.of("accessToken", service.login(r));
    }
}
