package com.rindev.chat.dto.response;

import com.rindev.chat.enums.AttachmentType;

public record UploadResponse(
                Long id,
                Long messageId,
                AttachmentType type,
                String fileName,
                String fileUrl,
                String mimeType,
                Long fileSize,
                Long durationSeconds) {

        /**
         * Keeps existing upload callers source-compatible while duration remains
         * unavailable for non-audio attachments.
         */
        public UploadResponse(
                        Long id,
                        Long messageId,
                        AttachmentType type,
                        String fileName,
                        String fileUrl,
                        String mimeType,
                        Long fileSize) {
                this(id, messageId, type, fileName, fileUrl, mimeType, fileSize, null);
        }
}
