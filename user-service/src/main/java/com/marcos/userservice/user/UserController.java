package com.marcos.userservice.user;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
class UserController {

    private final UserService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(@Valid @RequestBody CreateUserRequest r) {
        return service.create(r);
    }

    @GetMapping("/me")
    UserResponse me(Authentication auth) {
        return service.get(UUID.fromString(auth.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    UserResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<UserResponse> list() {
        return service.list();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest r) {
        return service.update(id, r);
    }

    @PutMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    UserResponse setAvatar(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return service.setAvatar(id, file);
    }

    @GetMapping("/{id}/avatar")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    ResponseEntity<Resource> avatar(@PathVariable UUID id) {
        var a = service.avatar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.contentType()))
                .body(new InputStreamResource(a.body()));
    }

    @DeleteMapping("/{id}/avatar")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAvatar(@PathVariable UUID id) {
        service.deleteAvatar(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
