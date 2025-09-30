package com.marcos.usersservice.controller;

import com.marcos.usersservice.dto.CreateUserRequestDto;
import com.marcos.usersservice.dto.UpdateUserRequestDto;
import com.marcos.usersservice.dto.UserResponseDto;
import com.marcos.usersservice.service.UsersService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    // CRUD user operations
    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public ResponseEntity<UserResponseDto> createUser(@RequestBody @Valid CreateUserRequestDto request) {
        UserResponseDto response = usersService.createUser(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        UserResponseDto response = usersService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable UUID id, @RequestBody @Valid UpdateUserRequestDto request) {
        UserResponseDto response = usersService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteUser() {
        return ResponseEntity.ok().build();
    }

    // ADMIN endpoints
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> response = usersService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<UserResponseDto>> getUsersPaginated(Pageable pageable) {
        Page<UserResponseDto> response = usersService.findAll(pageable);
        return ResponseEntity.ok(response);
    }
}
