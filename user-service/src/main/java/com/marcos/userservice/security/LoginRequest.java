package com.marcos.userservice.security;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {}