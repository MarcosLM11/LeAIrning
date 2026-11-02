package com.marcos.ingestionservice.storage;

import java.io.InputStream;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

@Service
public class S3Service {

    private final S3Client s3;
    private final S3Properties props;

    S3Service(S3Client s3, S3Properties props) {
        this.s3 = s3;
        this.props = props;
    }

    /** Streams the object; the caller must close it. */
    public InputStream open(String key) {
        return s3.getObject(r -> r.bucket(props.bucket()).key(key));
    }
}
