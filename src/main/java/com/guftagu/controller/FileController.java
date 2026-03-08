package com.guftagu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileController {

    private final String UPLOAD_DIR = "uploads/";

    @PostMapping("/{type}")
    public ResponseEntity<?> uploadFile(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            String subDir = switch (type) {
                case "image" -> "images/";
                case "audio" -> "audio/";
                case "document" -> "documents/";
                default -> "misc/";
            };

            Path path = Paths.get(UPLOAD_DIR + subDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = path.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/uploads/" + subDir + fileName;
            
            return ResponseEntity.ok(Map.of("url", fileUrl));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Could not upload file: " + e.getMessage());
        }
    }
}
