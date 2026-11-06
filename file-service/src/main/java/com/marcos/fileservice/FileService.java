package com.marcos.fileservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.UUID;
import com.marcos.fileservice.storage.S3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private final S3Service s3;
    private final FileMetadataRepository repository;
    private final KafkaTemplate<String, Object> kafka;
    static final String FILE_UPLOADED_TOPIC = "file.uploaded";
    static final String FILE_DELETED_TOPIC = "file.deleted";

    FileService(S3Service s3, FileMetadataRepository repository, KafkaTemplate<String, Object> kafka) {
        this.s3 = s3;
        this.repository = repository;
        this.kafka = kafka;
    }

    UUID upload(MultipartFile upload, UUID ownerId) {
        //validate length > 0, virus scan, tipo válido
        var length = upload.getSize();
        var contentType = upload.getContentType() != null
                ? upload.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        var name = upload.getOriginalFilename() != null ? upload.getOriginalFilename() : "unnamed";
        var file = repository.save(new FileMetadata(name, contentType, length, ownerId));
        var key = file.getId();

        try {
            s3.upload(key.toString(), upload.getInputStream(), length, contentType);
        } catch (IOException e) {
            markFailed(file);
            throw new UncheckedIOException(e);
        } catch (RuntimeException e) {
            markFailed(file);
            throw e;
        }
        file.setStatus(FileStatus.UPLOADED);
        repository.save(file);
        kafka.send(FILE_UPLOADED_TOPIC, key.toString(), FileUploadedEvent.builder()
                .fileId(key)
                .userId(ownerId.toString())
                .name(file.getName())
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadedAt(file.getUploadedAt())
                .build());
        return key;
    }

    private void markFailed(FileMetadata file) {
        file.setStatus(FileStatus.FAILED);
        repository.save(file);
    }

    Page<FileResponse> list(UUID ownerId, String name, String type, Instant since, Pageable pageable) {
        Specification<FileMetadata> spec = (f, _, cb) -> cb.equal(f.get("ownerId"), ownerId);
        if (name != null && !name.isBlank()) {
            spec = spec.and((f, _, cb) -> cb.like(cb.lower(f.get("name")), "%" + like(name) + "%", '\\'));
        }
        if (type != null && !type.isBlank()) {
            spec = spec.and((f, _, cb) -> cb.like(cb.lower(f.get("name")), "%." + like(type), '\\'));
        }
        if (since != null) {
            spec = spec.and((f, _, cb) -> cb.greaterThanOrEqualTo(f.<Instant>get("uploadedAt"), since));
        }
        return repository.findAll(spec, pageable).map(FileResponse::from);
    }

    private static String like(String s) {
        return s.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public boolean isOwner(UUID key, String userId) {
        return repository.findById(key)
                .map(f -> f.getOwnerId() != null && f.getOwnerId().toString().equals(userId))
                .orElse(false);
    }

    void assertExists(UUID key) {
        s3.assertExists(key.toString());
    }

    InputStream download(UUID key) {
        return s3.download(key.toString());
    }

    void delete(UUID key) {
        s3.delete(key.toString());
        repository.deleteById(key);
        kafka.send(FILE_DELETED_TOPIC, key.toString(), new FileDeletedEvent(key));
    }

    void markIngested(UUID key, boolean success) {
        repository.findById(key).ifPresent(f -> {
            f.setStatus(success ? FileStatus.READY : FileStatus.FAILED);
            repository.save(f);
        });
    }
}
