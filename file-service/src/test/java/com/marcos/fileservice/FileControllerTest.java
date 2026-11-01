package com.marcos.fileservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletionException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

class FileControllerTest {

    private final FileService service = mock(FileService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new FileController(service))
            .setControllerAdvice(new FileControllerAdvice())
            .build();

    @Test
    void returnsKeyOfUploadedFile() throws Exception {
        when(service.upload(any(HttpServletRequest.class))).thenReturn("abc");

        mvc.perform(post("/api/v1/files").contentType("text/plain").content("hey"))
                .andExpect(status().isOk())
                .andExpect(content().string("uploaded file: abc"));
    }

    @Test
    void downloadsFileBytes() throws Exception {
        when(service.download("abc")).thenReturn(new ByteArrayInputStream("hey".getBytes()));

        mvc.perform(get("/api/v1/files/abc")).andExpect(status().isOk()).andExpect(content().string("hey"));
    }

    @Test
    void downloadOfMissingKeyIs404() throws Exception {
        doThrow(new CompletionException(NoSuchKeyException.builder().build()))
                .when(service).assertExists("nope");

        mvc.perform(get("/api/v1/files/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("File not found"));
    }

    @Test
    void otherStorageFailureIs502() throws Exception {
        doThrow(new CompletionException(new RuntimeException("boom"))).when(service).assertExists("abc");
        mvc.perform(get("/api/v1/files/abc")).andExpect(status().isBadGateway());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/v1/files/abc")).andExpect(status().isNoContent());
        verify(service).delete("abc");
    }
}
