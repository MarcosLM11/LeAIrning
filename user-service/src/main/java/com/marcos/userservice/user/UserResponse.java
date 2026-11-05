package com.marcos.userservice.user;

import java.time.Instant;
import java.util.UUID;

record UserResponse(
        UUID id,
        String email,
        String username,
        String name,
        Instant createdAt,
        boolean hasAvatar
) {

    static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getUsername(), u.getName(), u.getCreatedAt(), u.getAvatarContentType() != null);
    }
}
