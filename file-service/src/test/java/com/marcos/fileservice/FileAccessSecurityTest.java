package com.marcos.fileservice;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import com.marcos.fileservice.storage.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FileAccessSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired FileMetadataRepository repository;
    @MockitoBean S3Service s3;

    private final UUID owner = UUID.randomUUID();

    @Test
    void ownerAndAdminCanDownloadOthersAreForbidden() throws Exception {
        var key = repository.save(new FileMetadata("a.txt", "text/plain", 3, owner)).getId();
        when(s3.download(key.toString())).thenAnswer(i -> new ByteArrayInputStream("hey".getBytes()));

        mvc.perform(get("/api/v1/files/" + key)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/files/" + key).with(user(owner, "ROLE_USER"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/files/" + key).with(user(UUID.randomUUID(), "ROLE_ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/files/" + key).with(user(UUID.randomUUID(), "ROLE_USER"))).andExpect(status().isForbidden());
    }

    @Test
    void foreignUserCannotDelete() throws Exception {
        var key = repository.save(new FileMetadata("a.txt", "text/plain", 3, owner)).getId();

        mvc.perform(delete("/api/v1/files/" + key).with(user(UUID.randomUUID(), "ROLE_USER")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/files/" + key).with(user(owner, "ROLE_USER")))
                .andExpect(status().isNoContent());
        verify(s3).delete(key.toString());
    }

    @Test
    void listReturnsOnlyOwnFiles() throws Exception {
        var mine = repository.save(new FileMetadata("mine.txt", "text/plain", 3, owner)).getId();
        repository.save(new FileMetadata("theirs.txt", "text/plain", 3, UUID.randomUUID()));

        mvc.perform(get("/api/v1/files")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/files").with(user(owner, "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mine.toString()))
                .andExpect(jsonPath("$.content[0].name").value("mine.txt"));
    }

    @Test
    void listFiltersByNameTypeAndPaginates() throws Exception {
        var me = UUID.randomUUID();
        repository.save(new FileMetadata("Biologia.pdf", "application/pdf", 1, me));
        repository.save(new FileMetadata("Notas_100%.md", "text/markdown", 1, me));
        repository.save(new FileMetadata("Fisica.pdf", "application/pdf", 1, me));

        mvc.perform(get("/api/v1/files").param("name", "bio").with(user(me, "ROLE_USER")))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Biologia.pdf"));
        mvc.perform(get("/api/v1/files").param("type", "PDF").with(user(me, "ROLE_USER")))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/api/v1/files").param("name", "%").with(user(me, "ROLE_USER")))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/api/v1/files").param("size", "2").param("page", "1").with(user(me, "ROLE_USER")))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    private static RequestPostProcessor user(UUID id, String role) {
        return jwt().jwt(j -> j.subject(id.toString())).authorities(new SimpleGrantedAuthority(role));
    }
}
