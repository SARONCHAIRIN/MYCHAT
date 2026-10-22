package com.rindev.chat.storage;

import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
    private final Path root;

    public LocalFileStorageService(@Value("${app.upload.directory:uploads}") String directory) {
        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException("Upload directory must not be blank");
        }
        root = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize upload directory", e);
        }
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        String original = file.getOriginalFilename();
        String extension = StoredFileNames.extension(original);
        String storedName = UUID.randomUUID() + extension;
        Path destination = root.resolve(storedName).normalize();
        if (!destination.getParent().equals(root)) {
            throw new BadRequestException("Invalid file name");
        }
        try {
            file.transferTo(destination);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        return new StoredFile(original, storedName, "/uploads/" + storedName,
                file.getContentType(), file.getSize());
    }

    @Override
    public StoredObject read(String filename) {
        StoredFileNames.validate(filename);
        Path path = root.resolve(filename).normalize();
        if (!path.getParent().equals(root) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException("Stored file not found");
        }
        try {
            if (Files.size(path) > 25L * 1024 * 1024) {
                throw new IllegalStateException("Stored file exceeds 25 MB download limit");
            }
            String type = Files.probeContentType(path);
            return new StoredObject(Files.readAllBytes(path), type);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read stored file", e);
        }
    }

    @Override
    public long getFileSize(String filename) {
        StoredFileNames.validate(filename);
        Path path = root.resolve(filename).normalize();
        if (!path.getParent().equals(root) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException("Stored file not found");
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read stored file size", e);
        }
    }

    @Override
    public StoredObject readRange(String filename, long offset, long length) {
        StoredFileNames.validate(filename);
        Path path = root.resolve(filename).normalize();
        if (!path.getParent().equals(root) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException("Stored file not found");
        }
        try {
            long totalSize = Files.size(path);
            if (offset < 0 || length < 0 || offset + length > totalSize) {
                throw new IllegalArgumentException("Invalid range: offset " + offset + ", length " + length + ", total " + totalSize);
            }
            String type = Files.probeContentType(path);
            byte[] buffer = new byte[(int) length];
            try (var channel = java.nio.channels.FileChannel.open(path, java.nio.file.StandardOpenOption.READ)) {
                channel.position(offset);
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(buffer);
                while (bb.hasRemaining()) {
                    if (channel.read(bb) == -1) {
                        break;
                    }
                }
            }
            return new StoredObject(buffer, type);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read stored file range", e);
        }
    }

    @Override
    public void delete(String filename) {
        StoredFileNames.validate(filename);
        Path path = root.resolve(filename).normalize();
        if (!path.getParent().equals(root)) {
            throw new ResourceNotFoundException("Stored file not found");
        }
        try {
            // deleteIfExists deliberately makes cleanup after a failed or
            // repeated operation safe without accepting arbitrary paths.
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete stored file", e);
        }
    }
}
