package com.marcos.fileservice;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/files")
class FileController {

    private final FileService service;

    FileController(FileService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<String> upload(HttpServletRequest request, @RequestParam(required = false) String name) {
        var key = service.upload(request, name);
        return ResponseEntity.ok("uploaded file: " + key);
    }

    @GetMapping("/{key}")
    ResponseEntity<Resource> download(@PathVariable UUID key) {
        service.assertExists(key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(service.download(key)));
    }

    @DeleteMapping("/{key}")
    ResponseEntity<Void> delete(@PathVariable UUID key) {
        service.delete(key);
        return ResponseEntity.noContent().build();
    }
}
