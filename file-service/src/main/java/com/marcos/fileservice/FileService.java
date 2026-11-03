package com.marcos.fileservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import com.marcos.fileservice.storage.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FileService {

    private final S3Service s3;
    private final FileMetadataRepository repository;
    private final KafkaTemplate<String, FileUploadedEvent> kafka;
    static final String FILE_UPLOADED_TOPIC = "file.uploaded";

    FileService(S3Service s3, FileMetadataRepository repository, KafkaTemplate<String, FileUploadedEvent> kafka) {
        this.s3 = s3;
        this.repository = repository;
        this.kafka = kafka;
    }

    UUID upload(HttpServletRequest request, String name, UUID ownerId) {
        //validate length > 0, virus scan, tipo válido
        var length = request.getContentLengthLong();
        var contentType = request.getContentType() != null
                ? request.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        var file = repository.save(new FileMetadata(name != null ? name : "unnamed", contentType, length, ownerId));
        var key = file.getId();

        try {
            s3.upload(key.toString(), request.getInputStream(), length, contentType);
        } catch (IOException e) {
            markFailed(file);
            throw new RuntimeException(e);
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

    List<FileResponse> list(UUID ownerId) {
        return repository.findByOwnerId(ownerId).stream().map(FileResponse::from).toList();
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
    }
}
