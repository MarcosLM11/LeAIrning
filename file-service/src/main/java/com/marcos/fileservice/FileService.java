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

    FileService(S3Service s3) {
        this.s3 = s3;
    }

    String upload(HttpServletRequest request) {
        //validate length > 0, virus scan, tipo válido
        var key = UUID.randomUUID().toString();
        var length = request.getContentLengthLong();
        var contentType = request.getContentType() != null
                ? request.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        try {
            s3.upload(key, request.getInputStream(), length, contentType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return key;
    }

    void assertExists(String key) {
        s3.assertExists(key);
    }

    InputStream download(String key) {
        return s3.download(key);
    }

    void delete(String key) {
        s3.delete(key);
    }
}
