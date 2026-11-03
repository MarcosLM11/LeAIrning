package com.marcos.fileservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileServiceAccessTest {

    private final FileMetadataRepository repository = mock(FileMetadataRepository.class);
    private final FileService service = new FileService(null, repository, null);
    private final UUID owner = UUID.randomUUID();
    private final UUID key = UUID.randomUUID();

    @Test
    void onlyTheOwnerIsOwner() {
        when(repository.findById(key)).thenReturn(Optional.of(new FileMetadata("a", "text/plain", 1, owner)));

        assertThat(service.isOwner(key, owner.toString())).isTrue();
        assertThat(service.isOwner(key, UUID.randomUUID().toString())).isFalse();
    }

    @Test
    void legacyFileWithoutOwnerAndUnknownFileHaveNoOwner() {
        when(repository.findById(key)).thenReturn(Optional.of(new FileMetadata("a", "text/plain", 1, null)));

        assertThat(service.isOwner(key, owner.toString())).isFalse();
        assertThat(service.isOwner(UUID.randomUUID(), owner.toString())).isFalse();
    }
}
