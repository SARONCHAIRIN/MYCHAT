package com.rindev.chat.storage;

import com.rindev.chat.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path root;

    public LocalFileStorageService(@Value("${app.upload.directory:uploads}") String directory) {
        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException("Upload directory must not be blank");
        }
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not initialize upload directory", e);
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

        String storedName = UUID.randomUUID() + extension;
        Path destination = root.resolve(storedName).normalize();

        if (!destination.getParent().equals(root)) {
            throw new BadRequestException("Invalid file name");
        }

        try {
            file.transferTo(destination);
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file");
        }

        return new StoredFile(
                original,
                storedName,
                "/uploads/" + storedName,
                file.getContentType(),
                file.getSize());
    }
}
