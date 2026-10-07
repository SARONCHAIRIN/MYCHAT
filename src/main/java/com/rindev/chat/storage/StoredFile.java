package com.rindev.chat.storage;

public record StoredFile(
                String originalFileName,
                String storedFileName,
                String fileUrl,
                String contentType,
                long size) {
}