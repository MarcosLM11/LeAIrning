package com.marcos.userservice.user;

import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@Component
class AvatarStorage {

    private final S3Client s3;
    private final S3Properties props;

    AvatarStorage(S3Client s3, S3Properties props) {
        this.s3 = s3;
        this.props = props;
    }

    void put(UUID userId, InputStream in, long length, String contentType) {
        s3.putObject(r -> r.bucket(props.bucket()).key(key(userId)).contentType(contentType),
                RequestBody.fromInputStream(in, length));
    }

    InputStream get(UUID userId) {
        return s3.getObject(r -> r.bucket(props.bucket()).key(key(userId)));
    }

    void delete(UUID userId) {
        s3.deleteObject(r -> r.bucket(props.bucket()).key(key(userId)));
    }

    private static String key(UUID userId) {
        return "avatars/" + userId;
    }
}
