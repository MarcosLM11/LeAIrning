package com.marcos.fileservice;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FileMetadataRepositoryTest {

    @Autowired
    FileMetadataRepository repository;

    @Test
    void fillsIdAndUploadDateOnSave() {
        var saved = repository.save(new FileMetadata("a.txt", "text/plain", 3, UUID.randomUUID()));
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUploadedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(FileStatus.UPLOADING);
    }
}
