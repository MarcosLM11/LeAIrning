package com.marcos.userservice;

import java.time.Instant;
import java.util.UUID;

record UserResponse(
        UUID id,
        String email,
        String name,
        Instant createdAt
) {

    static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getCreatedAt());
    }
}
