package com.rindev.chat.controller;

import com.rindev.chat.entity.MessageAttachment;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.MessageAttachmentRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.storage.LocalFileStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secure image download controller.
 *
 * Features:
 * - JWT authentication required
 * - Swagger Bearer authentication
 * - Avatar image support
 * - Conversation membership verification
 * - Secure file retrieval
 * - Missing file handling
 */
@RestController
public class StoredImageController {

    private final LocalFileStorageService storage;
    private final MessageAttachmentRepository attachments;
    private final ConversationMemberRepository members;
    private final UserRepository users;

    public StoredImageController(
            LocalFileStorageService storage,
            MessageAttachmentRepository attachments,
            ConversationMemberRepository members,
            UserRepository users) {
        this.storage = storage;
        this.attachments = attachments;
        this.members = members;
        this.users = users;
    }

    /**
     * Download an image.
     *
     * Example:
     * GET /uploads/37118a16-b4f8-4d33-b374-d647d97437a0.jpg
     */
    @GetMapping("/uploads/{filename:.+}")
    @Operation(summary = "Download stored image", description = "Download an image using JWT authentication. "
            + "Message attachments require conversation membership.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<FileSystemResource> getImage(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable String filename) throws IOException {

        // 1. Require authentication
        if (principal == null) {
            throw new ForbiddenException("Authentication required");
        }

        // 2. Construct stored file URL
        String fileUrl = "/uploads/" + filename;

        // 3. Check whether the image is a user avatar
        boolean isAvatar = users.existsByAvatarUrl(fileUrl);

        // 4. For message attachments, verify conversation membership
        if (!isAvatar) {

            MessageAttachment attachment = attachments
                    .findByFileUrl(fileUrl)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Image attachment not found"));

            Long conversationId = attachment
                    .getMessage()
                    .getConversation()
                    .getId();

            boolean isMember = members
                    .existsByConversationIdAndUserIdAndLeftAtIsNull(
                            conversationId,
                            principal.getId());

            if (!isMember) {
                throw new ForbiddenException(
                        "You cannot access this image");
            }
        }

        // 5. Resolve stored file
        Path path = storage.findStoredFile(filename);

        // 6. Verify file exists and is readable
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException(
                    "Stored image file not found");
        }

        // 7. Determine MIME type
        String mimeType = Files.probeContentType(path);

        if (mimeType == null || !mimeType.startsWith("image/")) {
            mimeType = "application/octet-stream";
        }

        // 8. Return image bytes
        return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(mimeType))
                .contentLength(Files.size(path))
                .body(new FileSystemResource(path));
    }
}