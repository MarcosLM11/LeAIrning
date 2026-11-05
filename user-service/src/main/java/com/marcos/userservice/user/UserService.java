package com.marcos.userservice.user;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorage avatars;

    private static final Set<String> AVATAR_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long AVATAR_MAX_BYTES = 2 * 1024 * 1024;

    UserResponse create(CreateUserRequest r) {
        if (repository.existsByEmail(r.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (repository.existsByUsername(r.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        return UserResponse.from(repository.save(new User(r.email(), r.name(), r.username(), passwordEncoder.encode(r.password()))));
    }

    UserResponse get(UUID id) {
        return UserResponse.from(find(id));
    }

    List<UserResponse> list() {
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    UserResponse update(UUID id, UpdateUserRequest r) {
        var user = find(id);

        if (repository.existsByUsernameAndIdNot(r.username(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        user.update(r.name(), r.username());
        return UserResponse.from(user);
    }

    @Transactional
    UserResponse setAvatar(UUID id, MultipartFile file) {
        if (!AVATAR_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar must be PNG, JPEG or WebP");
        }

        if (file.getSize() > AVATAR_MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar must be at most 2 MB");
        }

        var user = find(id);

        try (var in = file.getInputStream()) {
            avatars.put(id, in, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        user.setAvatarContentType(file.getContentType());
        return UserResponse.from(user);
    }

    Avatar avatar(UUID id) {
        var type = find(id).getAvatarContentType();
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No avatar");
        }
        return new Avatar(type, avatars.get(id));
    }

    @Transactional
    void deleteAvatar(UUID id) {
        var user = find(id);
        if (user.getAvatarContentType() != null) {
            avatars.delete(id);
            user.setAvatarContentType(null);
        }
    }

    void delete(UUID id) {
        var user = find(id);
        if (user.getAvatarContentType() != null) {
            avatars.delete(id);
        }
        repository.delete(user);
    }

    private User find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
