package com.marcos.fileservice;

import java.time.Instant;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/files")
class FileController {

    private final FileService service;

    FileController(FileService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, Authentication auth) {
        var key = service.upload(file, userId(auth));
        return ResponseEntity.ok("uploaded file: " + key);
    }

    @GetMapping
    Page<FileResponse> list(
            Authentication auth,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Instant since,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.list(userId(auth), name, type, since, pageable);
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN') or @fileService.isOwner(#key, authentication.name)")
    ResponseEntity<Resource> download(@PathVariable UUID key) {
        service.assertExists(key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(service.download(key)));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN') or @fileService.isOwner(#key, authentication.name)")
    ResponseEntity<Void> delete(@PathVariable UUID key) {
        service.delete(key);
        return ResponseEntity.noContent().build();
    }

    private static UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
