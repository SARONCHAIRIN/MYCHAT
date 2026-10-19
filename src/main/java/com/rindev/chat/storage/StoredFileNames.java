package com.rindev.chat.storage;

import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ResourceNotFoundException;

/** Flat UUID filenames only. Never accept object keys or arbitrary paths from clients. */
public final class StoredFileNames {
    private StoredFileNames() {}

    public static String extension(String original) {
        if (original == null) return "";
        int dot = original.lastIndexOf('.');
        String extension = dot < 0 ? "" : original.substring(dot);
        if (!extension.isEmpty() && !extension.matches("\\.[A-Za-z0-9]{1,12}")) {
            throw new BadRequestException("Invalid file name");
        }
        return extension;
    }

    public static void validate(String filename) {
        if (filename == null || !filename.matches(
                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}(\\.[A-Za-z0-9]{1,12})?")) {
            throw new ResourceNotFoundException("Stored file not found");
        }
    }
}
