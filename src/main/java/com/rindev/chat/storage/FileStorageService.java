package com.rindev.chat.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile store(MultipartFile file);
    StoredObject read(String storedFileName);
    StoredObject readRange(String storedFileName, long offset, long length);
    long getFileSize(String storedFileName);
    /**
     * Deletes a server-generated stored filename. Implementations must make a
     * repeated delete safe so callers can use it for rollback/cleanup.
     */
    void delete(String storedFileName);
}
