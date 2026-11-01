package com.marcos.fileservice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
}
