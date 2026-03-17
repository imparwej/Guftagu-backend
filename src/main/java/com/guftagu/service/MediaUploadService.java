package com.guftagu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class MediaUploadService {

    private final MinioService minioService;

    public MediaUploadService(MinioService minioService) {
        this.minioService = minioService;
    }

    public Map<String, String> uploadMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
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

        // 1. Store original file in guftagu-media bucket (MinIO)
        // Note: MinioService currently uses minio.bucketName which is usually 'guftagu-media' or similar
        String originalUrl = minioService.uploadFile(file, folder);

        String thumbnailUrl = null;

        try {
            if ("IMAGE".equals(type)) {
                thumbnailUrl = generateImageThumbnail(file);
            } else if ("VIDEO".equals(type)) {
                thumbnailUrl = generateVideoThumbnail(file);
            }
        } catch (Exception e) {
            log.warn("Failed to generate thumbnail, falling back to original URL", e);
            thumbnailUrl = originalUrl;
        }

        Map<String, String> response = new HashMap<>();
        response.put("url", originalUrl);
        response.put("thumbnailUrl", thumbnailUrl != null ? thumbnailUrl : originalUrl);
        response.put("type", type);

        return response;
    }

    private String generateImageThumbnail(MultipartFile file) throws Exception {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        
        int targetWidth = 300;
        int targetHeight = 300;
        
        // Maintain aspect ratio
        double ratio = Math.min((double) targetWidth / originalImage.getWidth(), (double) targetHeight / originalImage.getHeight());
        int newWidth = (int) (originalImage.getWidth() * ratio);
        int newHeight = (int) (originalImage.getHeight() * ratio);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, type);
        java.awt.Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", os);

        byte[] thumbnailBytes = os.toByteArray();
        String fileName = "thumbnails/" + UUID.randomUUID().toString() + ".jpg";
        
        return minioService.uploadBytes(thumbnailBytes, fileName, "image/jpeg");
    }

    private String generateVideoThumbnail(MultipartFile file) throws Exception {
        // JCodec download blocked by environment-level Tag mismatch error.
        // Falling back to original video URL as thumbnail for now.
        return null; 
    }
}
