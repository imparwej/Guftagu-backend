package com.guftagu.service;

import com.guftagu.model.MessageType;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    public String uploadBytes(byte[] bytes, String fileName, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .contentType(contentType)
                            .build()
            );

            String fullBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            if (fullBaseUrl.equals("http://localhost")) {
                fullBaseUrl += ":" + serverPort;
            }
            return fullBaseUrl + "/api/media/stream/" + fileName;

        } catch (Exception e) {
            log.error("Error uploading bytes to MinIO", e);
            throw new RuntimeException("Upload failed", e);
        }
    }

    public String uploadFile(MultipartFile file, String folder) {
        try {
            String extension = getExtension(file.getOriginalFilename());
            String fileName = folder + (folder.endsWith("/") ? "" : "/") + UUID.randomUUID().toString() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Return full streaming URL instead of direct MinIO URL
            // Format: http://localhost:8080/api/media/stream/{folder}/{filename}
            String fullBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            if (fullBaseUrl.equals("http://localhost")) {
                fullBaseUrl += ":" + serverPort;
            }
            return fullBaseUrl + "/api/media/stream/" + fileName;

        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Upload failed", e);
        }
    }

    public InputStream getObject(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching object from MinIO: {}", objectName, e);
            throw new RuntimeException("Fetch failed", e);
        }
    }

    public String getContentType(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return stat.contentType();
        } catch (Exception e) {
            log.error("Error fetching metadata from MinIO: {}", objectName, e);
            return "application/octet-stream";
        }
    }

    public String uploadMedia(MultipartFile file, MessageType type) {
        return uploadFile(file, getFolderByType(type));
    }

    private String getFolderByType(MessageType type) {
        return switch (type) {
            case IMAGE, GIF -> "images";
            case VIDEO -> "videos";
            case VOICE, AUDIO -> "voice";
            case DOCUMENT, FILE -> "documents";
            default -> "others";
        };
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // Set public read policy
            String policy = "{\n" +
                    "  \"Version\":\"2012-10-17\",\n" +
                    "  \"Statement\":[\n" +
                    "    {\n" +
                    "      \"Effect\":\"Allow\",\n" +
                    "      \"Principal\":\"*\",\n" +
                    "      \"Action\":[\"s3:GetObject\"],\n" +
                    "      \"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            minioClient.setBucketPolicy(
                    io.minio.SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
            log.info("MinIO bucket '{}' initialized with public read policy", bucketName);
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket policy", e);
        }
    }
}
