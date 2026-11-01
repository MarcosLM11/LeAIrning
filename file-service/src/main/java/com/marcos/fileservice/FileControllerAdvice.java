package com.marcos.fileservice;

import java.util.concurrent.CompletionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@RestControllerAdvice
class FileControllerAdvice {

    @ExceptionHandler(CompletionException.class)
    ProblemDetail storageError(CompletionException e) {
        if (e.getCause() instanceof NoSuchKeyException) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "File not found");
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Storage error");
    }
}
