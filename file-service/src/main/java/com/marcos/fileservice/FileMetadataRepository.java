package com.marcos.fileservice;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    List<FileMetadata> findByOwnerId(UUID ownerId);
}
