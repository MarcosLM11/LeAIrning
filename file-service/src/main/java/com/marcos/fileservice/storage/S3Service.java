package com.marcos.fileservice.storage;

import java.io.InputStream;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Service
public class S3Service {

    private final S3AsyncClient s3;
    private final S3Properties props;

    S3Service(S3AsyncClient s3, S3Properties props) {
        this.s3 = s3;
        this.props = props;
    }

    public void upload(String key, InputStream in, long length, String contentType) {
        var body = AsyncRequestBody.forBlockingInputStream(length);
        var done = s3.putObject(r -> r.bucket(props.bucket()).key(key).contentType(contentType), body);
        try {
            body.writeInputStream(in);
        } catch (RuntimeException e) {
            body.cancel();
            throw e;
        }
        done.join();
    }

    public void assertExists(String key) {
        s3.headObject(r -> r.bucket(props.bucket()).key(key)).join();
    }

    public InputStream download(String key) {
        return s3.getObject(r -> r.bucket(props.bucket())
                        .key(key), AsyncResponseTransformer.toBlockingInputStream())
                .join();
    }

    public void delete(String key) {
        s3.deleteObject(r -> r.bucket(props.bucket()).key(key)).join();
    }
}
