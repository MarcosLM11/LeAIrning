package com.marcos.userservice.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AvatarStorage avatars;
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
    void updateChangesNameAndUsername() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new User("a@a.com", "Ana", "ana", "hash")));

        var r = service.update(id, new UpdateUserRequest("Ana María", "anam"));

        assertThat(r.name()).isEqualTo("Ana María");
        assertThat(r.username()).isEqualTo("anam");
    }

    @Test
    void updateRejectsUsernameTakenByAnotherUser() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new User("a@a.com", "Ana", "ana", "hash")));
        when(repository.existsByUsernameAndIdNot("bea", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, new UpdateUserRequest("Ana", "bea")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void setAvatarRejectsNonImage() {
        var id = UUID.randomUUID();
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[10]);

        assertThatThrownBy(() -> service.setAvatar(id, file))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        verify(avatars, never()).put(any(), any(), anyLong(), any());
    }

    @Test
    void setAvatarRejectsTooBig() {
        var id = UUID.randomUUID();
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[2 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.setAvatar(id, file))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }

    @Test
    void setAvatarStoresImageAndMarksUser() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new User("a@a.com", "Ana", "ana", "hash")));

        var r = service.setAvatar(id, new MockMultipartFile("file", "a.png", "image/png", new byte[10]));

        assertThat(r.hasAvatar()).isTrue();
        verify(avatars).put(any(), any(), eq(10L), eq("image/png"));
    }

    @Test
    void avatarWithoutOneIsNotFound() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new User("a@a.com", "Ana", "ana", "hash")));

        assertThatThrownBy(() -> service.avatar(id))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
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
