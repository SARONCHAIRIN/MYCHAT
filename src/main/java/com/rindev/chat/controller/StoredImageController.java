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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoredImageController {

    private static final Logger log = LoggerFactory.getLogger(StoredImageController.class);

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

    @GetMapping("/uploads/{filename:.+}")
    @Operation(summary = "Download stored image", description = "Download an image using JWT authentication. "
            + "Message attachments require conversation membership.", security = @SecurityRequirement(name = "bearerAuth"))
    @Transactional(readOnly = true)
    public ResponseEntity<FileSystemResource> getImage(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable String filename) throws IOException {

        log.info("========== IMAGE DOWNLOAD START ==========");
        log.info("Requested filename: {}", filename);

        // 1. Authentication
        if (principal == null) {
            log.warn("Image download rejected: unauthenticated");
            throw new ForbiddenException("Authentication required");
        }

        log.info("Authenticated user ID: {}", principal.getId());

        // 2. Construct file URL
        String fileUrl = "/uploads/" + filename;

        log.info("File URL: {}", fileUrl);

        // 3. Check avatar
        boolean isAvatar = users.existsByAvatarUrl(fileUrl);

        log.info("Is avatar: {}", isAvatar);

        // 4. Check attachment and conversation membership
        if (!isAvatar) {

            log.info("Searching attachment...");

            MessageAttachment attachment = attachments
                    .findByFileUrl(fileUrl)
                    .orElseThrow(() -> {
                        log.warn("Attachment not found: {}", fileUrl);

                        return new ResourceNotFoundException(
                                "Image attachment not found");
                    });

            log.info("Attachment found, ID: {}", attachment.getId());

            log.info("Resolving message relationship...");

            Long conversationId = attachment
                    .getMessage()
                    .getConversation()
                    .getId();

            log.info("Conversation ID: {}", conversationId);

            boolean isMember = members
                    .existsByConversationIdAndUserIdAndLeftAtIsNull(
                            conversationId,
                            principal.getId());

            log.info("Conversation membership: {}", isMember);

            if (!isMember) {
                log.warn(
                        "User {} denied access to conversation {}",
                        principal.getId(),
                        conversationId);

                throw new ForbiddenException(
                        "You cannot access this image");
            }
        }

        // 5. Resolve physical file
        log.info("Resolving physical file...");

        Path path = storage.findStoredFile(filename);

        log.info("Resolved path: {}", path);

        // 6. Validate physical file
        boolean exists = Files.exists(path);
        boolean regularFile = Files.isRegularFile(path);
        boolean readable = Files.isReadable(path);

        log.info("File exists: {}", exists);
        log.info("Regular file: {}", regularFile);
        log.info("Readable: {}", readable);

        if (!regularFile || !readable) {
            log.warn("Image file missing or unreadable: {}", path);

            throw new ResourceNotFoundException(
                    "Stored image file not found");
        }

        // 7. Determine MIME type
        String mimeType = Files.probeContentType(path);

        if (mimeType == null || !mimeType.startsWith("image/")) {
            mimeType = "application/octet-stream";
        }

        log.info("MIME type: {}", mimeType);

        // 8. Determine file size
        long fileSize = Files.size(path);

        log.info("File size: {} bytes", fileSize);
        log.info("========== IMAGE DOWNLOAD SUCCESS ==========");

        // 9. Return image
        return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(mimeType))
                .contentLength(fileSize)
                .body(new FileSystemResource(path));
    }
}