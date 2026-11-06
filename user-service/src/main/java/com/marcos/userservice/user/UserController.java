package com.marcos.userservice.user;

import java.net.URI;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest r) {
        var user = service.create(r);
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
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
    Page<UserResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
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
