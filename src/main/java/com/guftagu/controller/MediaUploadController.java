package com.guftagu.controller;

import com.guftagu.model.MessageType;
import com.guftagu.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaUploadController {

    private final MinioService minioService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadMedia(
            @RequestParam("file") MultipartFile file) {
        
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String contentType = file.getContentType();
        String folder = "others";
        String type = "FILE";

        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                folder = "images";
                type = "IMAGE";
            } else if (contentType.startsWith("video/")) {
                folder = "videos";
                type = "VIDEO";
            } else if (contentType.startsWith("audio/")) {
                folder = "voice";
                type = "VOICE";
            } else if (contentType.startsWith("application/")) {
                folder = "documents";
                type = "DOCUMENT";
            }
        }

        String url = minioService.uploadFile(file, folder);

        return ResponseEntity.ok(Map.of(
                "url", url,
                "type", type
        ));
    }
}
