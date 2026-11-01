package com.marcos.fileservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import com.marcos.fileservice.storage.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
class FileService {

    private final S3Service s3;
    private final FileMetadataRepository repository;

    FileService(S3Service s3, FileMetadataRepository repository) {
        this.s3 = s3;
        this.repository = repository;
    }

    UUID upload(HttpServletRequest request, String name) {
        //validate length > 0, virus scan, tipo válido
        var length = request.getContentLengthLong();
        var contentType = request.getContentType() != null
                ? request.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        var file = repository.save(new FileMetadata(name != null ? name : "unnamed", contentType, length));
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
        return key;
    }

    private void markFailed(FileMetadata file) {
        file.setStatus(FileStatus.FAILED);
        repository.save(file);
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
