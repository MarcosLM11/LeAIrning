package com.marcos.fileservice;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String contentType;

    private long size;

    // nullable: files uploaded before ownership existed have no owner (only ADMIN can reach them)
    @Column(updatable = false)
    private UUID ownerId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileStatus status = FileStatus.UPLOADING;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    FileMetadata(String name, String contentType, long size, UUID ownerId) {
        this.name = name;
        this.contentType = contentType;
        this.size = size;
        this.ownerId = ownerId;
    }
}
