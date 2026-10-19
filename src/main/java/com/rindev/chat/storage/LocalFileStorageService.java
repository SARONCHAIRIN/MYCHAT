package com.rindev.chat.storage;

import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ResourceNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path root;

    public LocalFileStorageService(
            @Value("${app.upload.directory:uploads}") String directory) {

        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException(
                    "Upload directory must not be blank");
        }

        this.root = Paths.get(directory)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(root);

            log.info("Upload directory initialized: {}", root);

        } catch (IOException e) {

            log.error(
                    "Could not initialize upload directory: {}",
                    root,
                    e);

            throw new IllegalStateException(
                    "Could not initialize upload directory",
                    e);
        }
    }

    @Override
    public StoredFile store(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        String original = file.getOriginalFilename();

        String extension = "";

        if (original != null) {
            int index = original.lastIndexOf('.');

            if (index >= 0) {
                extension = original.substring(index);
            }
        }

        // Validate extension
        if (!extension.matches("\\.[A-Za-z0-9]{1,12}")) {
            throw new BadRequestException(
                    "Invalid file extension");
        }

        String storedName = UUID.randomUUID() + extension;

        Path destination = root.resolve(storedName)
                .normalize();

        if (!destination.getParent().equals(root)) {
            throw new BadRequestException(
                    "Invalid file name");
        }

        try {

            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING);

            log.info(
                    "File uploaded successfully: {}",
                    destination);

        } catch (IOException e) {

            log.error(
                    "Failed to store file: {}",
                    destination,
                    e);

            throw new BadRequestException(
                    "Failed to store file");
        }

        return new StoredFile(
                original,
                storedName,
                "/uploads/" + storedName,
                file.getContentType(),
                file.getSize());
    }

    /**
     * Resolve a stored file securely.
     */
    public Path findStoredFile(String filename) {

        log.info("Looking for stored image: {}", filename);

        // Validate UUID filename
        if (filename == null ||
                !filename.matches(
                        "[a-fA-F0-9-]{36}\\.[A-Za-z0-9]{1,12}")) {

            log.warn("Invalid stored filename: {}", filename);

            throw new ResourceNotFoundException(
                    "Image not found");
        }

        Path path = root.resolve(filename)
                .normalize();

        // Prevent path traversal
        if (!path.getParent().equals(root)) {

            log.warn("Invalid image path: {}", path);

            throw new ResourceNotFoundException(
                    "Image not found");
        }

        log.info("Resolved image path: {}", path);

        boolean exists = Files.exists(path);
        boolean regularFile = Files.isRegularFile(path);
        boolean readable = Files.isReadable(path);

        log.info("File exists: {}", exists);
        log.info("Regular file: {}", regularFile);
        log.info("Readable: {}", readable);

        if (!regularFile || !readable) {

            log.warn(
                    "Stored image missing or unreadable: {}",
                    path);

            throw new ResourceNotFoundException(
                    "Stored image file not found");
        }

        return path;
    }
}
