package com.marcos.fileservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

class FileControllerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Authentication AUTH = new TestingAuthenticationToken(USER.toString(), null, "ROLE_USER");
    private final FileService service = mock(FileService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new FileController(service))
            .setControllerAdvice(new FileControllerAdvice())
            .build();

    @Test
    void returnsKeyOfUploadedFile() throws Exception {
        when(service.upload(any(MultipartFile.class), eq(USER))).thenReturn(ID);

        mvc.perform(multipart("/api/v1/files").file(new MockMultipartFile("file", "a.txt", "text/plain", "hey".getBytes())).principal(AUTH))
                .andExpect(status().isOk())
                .andExpect(content().string("uploaded file: " + ID));
    }

    @Test
    void downloadsFileBytes() throws Exception {
        when(service.download(ID)).thenReturn(new ByteArrayInputStream("hey".getBytes()));

        mvc.perform(get("/api/v1/files/" + ID).principal(AUTH)).andExpect(status().isOk()).andExpect(content().string("hey"));
    }

    @Test
    void downloadOfMissingKeyIs404() throws Exception {
        doThrow(new CompletionException(NoSuchKeyException.builder().build()))
                .when(service).assertExists(ID);

        mvc.perform(get("/api/v1/files/" + ID).principal(AUTH))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("File not found"));
    }

    @Test
    void otherStorageFailureIs502() throws Exception {
        doThrow(new CompletionException(new RuntimeException("boom"))).when(service).assertExists(ID);
        mvc.perform(get("/api/v1/files/" + ID).principal(AUTH)).andExpect(status().isBadGateway());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/v1/files/" + ID).principal(AUTH)).andExpect(status().isNoContent());
        verify(service).delete(ID);
    }
}
