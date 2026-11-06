package com.marcos.fileservice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID>, JpaSpecificationExecutor<FileMetadata> {
}
