package com.marcos.userservice;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
class UserService {

    private final UserRepository repository;

    UserResponse create(CreateUserRequest r) {
        if (repository.existsByEmail(r.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        return UserResponse.from(repository.save(new User(r.email(), r.name())));
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
        user.update(r.name());
        return UserResponse.from(user);
    }

    void delete(UUID id) {
        repository.delete(find(id));
    }

    private User find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
