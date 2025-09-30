package com.marcos.usersservice.controller;

import com.marcos.usersservice.dto.CreateUserRequestDto;
import com.marcos.usersservice.dto.UpdateUserRequestDto;
import com.marcos.usersservice.dto.UserResponseDto;
import com.marcos.usersservice.service.UsersService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@RestController
@RequestMapping("/api/v1/users")
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
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
        return ResponseEntity.created(URI.create("api/v1/users/" + response.id())).body(response);
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
        return ResponseEntity.accepted().body(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        usersService.deleteUser(id);
        return ResponseEntity.noContent().build();
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
