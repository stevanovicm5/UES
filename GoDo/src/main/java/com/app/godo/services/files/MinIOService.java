package com.app.godo.services.files;

import com.app.godo.configurations.MinioClientProperties;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinIOService {

    private final MinioClient minioClient;
    private final MinioClientProperties minioProperties;

    @PostConstruct
    public void initBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .build()
                );
                log.info("Created MinIO bucket: {}", minioProperties.getBucket());
            } else {
                log.info("MinIO bucket '{}' already exists", minioProperties.getBucket());
            }

            // Set bucket policy to public so browser can load images directly
            String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(minioProperties.getBucket());

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .config(policy)
                            .build()
            );
            log.info("Set public read policy on bucket: {}", minioProperties.getBucket());
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(uniqueFilename)
                            .stream(inputStream, file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Uploaded file '{}' to MinIO bucket '{}'", uniqueFilename, minioProperties.getBucket());
            return uniqueFilename;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO: " + originalFilename, e);
        }
    }

    // Upload from raw bytes (used when we already read the file into memory)
    public String uploadFileFromBytes(byte[] fileBytes, String originalFilename, String contentType) {
        String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

        try (InputStream inputStream = new java.io.ByteArrayInputStream(fileBytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(uniqueFilename)
                            .stream(inputStream, (long) fileBytes.length, -1L)
                            .contentType(contentType)
                            .build()
            );

            log.info("Uploaded file '{}' to MinIO bucket '{}'", uniqueFilename, minioProperties.getBucket());
            return uniqueFilename;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO: " + originalFilename, e);
        }
    }

    public byte[] downloadFile(String filename) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(filename)
                        .build()
        )) {
            return stream.readAllBytes();

        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " + filename, e);
        }
    }

    public InputStream getFileStream(String filename) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(filename)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file stream from MinIO: " + filename, e);
        }
    }

    public void deleteFile(String filename) {
        if (filename == null || filename.isBlank()) return;

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(filename)
                            .build()
            );
            log.info("Deleted file '{}' from MinIO", filename);

        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", filename, e);
        }
    }


    // Generates a URL to access the file.
    // This URL is what the frontend uses to display images
    // or to download PDFs.
    //
    // Format: http://localhost:9000/godo-files/a1b2c3d4_photo.jpg
    public String getFileUrl(String filename) {
        if (filename == null || filename.isBlank()) return null;

        return minioProperties.getPublicUrl() + "/"
                + minioProperties.getBucket() + "/"
                + filename;
    }

    public boolean fileExists(String filename) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(filename)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
