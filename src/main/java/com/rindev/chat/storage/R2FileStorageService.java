package com.rindev.chat.storage;

import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ResourceNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
public class R2FileStorageService implements FileStorageService, AutoCloseable {
    private final S3Client client;
    private final String bucket;

    public R2FileStorageService(
            @Value("${app.storage.r2.endpoint}") String endpoint,
            @Value("${app.storage.r2.bucket}") String bucket,
            @Value("${app.storage.r2.access-key-id}") String accessKeyId,
            @Value("${app.storage.r2.secret-access-key}") String secretAccessKey) {
        if (endpoint.isBlank() || bucket.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank()) {
            throw new IllegalArgumentException("R2 configuration must not be blank");
        }
        this.bucket = bucket;
        this.client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .forcePathStyle(true)
                .build();
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("File is required");
        if (file.getSize() > 25L * 1024 * 1024) throw new BadRequestException("File size exceeds 25 MB");
        String original = file.getOriginalFilename();
        String name = UUID.randomUUID() + StoredFileNames.extension(original);
        String type = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        try (var stream = file.getInputStream()) {
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(name)
                            .contentType(type)
                            .build(),
                    RequestBody.fromInputStream(stream, file.getSize()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read upload", e);
        }
        return new StoredFile(original, name, "/uploads/" + name, type, file.getSize());
    }

    @Override
    public StoredObject read(String filename) {
        StoredFileNames.validate(filename);
        try {
            ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(filename).build());
            if (object.asByteArray().length > 25L * 1024 * 1024) {
                throw new IllegalStateException("Stored file exceeds 25 MB download limit");
            }
            return new StoredObject(object.asByteArray(), object.response().contentType());
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Stored file not found");
        }
    }

    @Override
    public long getFileSize(String filename) {
        StoredFileNames.validate(filename);
        try {
            var head = client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(filename)
                    .build());
            return head.contentLength();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Stored file not found");
        }
    }

    @Override
    public StoredObject readRange(String filename, long offset, long length) {
        StoredFileNames.validate(filename);
        try {
            long end = offset + length - 1;
            String rangeHeader = "bytes=" + offset + "-" + end;
            ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(filename)
                            .range(rangeHeader)
                            .build());
            return new StoredObject(object.asByteArray(), object.response().contentType());
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Stored file not found");
        }
    }

    @Override
    public void delete(String filename) {
        StoredFileNames.validate(filename);
        // S3-compatible DeleteObject is idempotent: deleting an absent key is
        // a successful cleanup operation.
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(filename).build());
    }

    @Override
    public void close() {
        client.close();
    }
}
