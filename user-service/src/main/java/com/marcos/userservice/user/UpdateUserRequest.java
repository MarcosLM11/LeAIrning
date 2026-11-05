package com.marcos.userservice.user;

import jakarta.validation.constraints.NotBlank;

record UpdateUserRequest(
        @NotBlank String name,
        @NotBlank String username
) {}