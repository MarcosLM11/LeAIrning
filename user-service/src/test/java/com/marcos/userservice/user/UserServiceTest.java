package com.marcos.userservice.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserService service;

    @Test
    void createRejectsDuplicateEmail() {
        when(repository.existsByEmail("a@a.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserRequest("a@a.com", "Ana", "ana", "password1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(repository, never()).save(any());
    }

    @Test
    void updateChangesName() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new User("a@a.com", "Ana", "ana", "hash")));

        assertThat(service.update(id, new UpdateUserRequest("Ana María")).name()).isEqualTo("Ana María");
    }

    @Test
    void getUnknownIdIsNotFound() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
