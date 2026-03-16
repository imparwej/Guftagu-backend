package com.guftagu.controller;

import com.guftagu.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
@RequestMapping("/api/media/stream")
@RequiredArgsConstructor
@Slf4j
public class MediaStreamingController {

    private final MinioService minioService;

    @GetMapping("/{folder}/{filename}")
    public ResponseEntity<InputStreamResource> streamMedia(
            @PathVariable String folder,
            @PathVariable String filename) {
        
        String objectName = folder + "/" + filename;
        log.info("Streaming media request: {}", objectName);

        try {
            InputStream inputStream = minioService.getObject(objectName);
            String contentType = minioService.getContentType(objectName);

            log.info("Streaming {} with content-type: {}", objectName, contentType);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            log.error("Failed to stream media: {}", objectName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
