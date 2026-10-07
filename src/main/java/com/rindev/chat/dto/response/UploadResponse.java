package com.rindev.chat.dto.response;

import com.rindev.chat.enums.AttachmentType;

public record UploadResponse(
                Long id,
                Long messageId,
                AttachmentType type,
                String fileName,
                String fileUrl,
                String mimeType,
                Long fileSize) {
}