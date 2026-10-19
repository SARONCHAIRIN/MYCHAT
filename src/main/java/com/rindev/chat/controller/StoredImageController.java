package com.rindev.chat.controller;

import com.rindev.chat.entity.MessageAttachment;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.MessageAttachmentRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.storage.LocalFileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Private image retrieval. Access requires a valid JWT and conversation
 * membership,
 * or an avatar URL belonging to a user. Never expose the uploads directory as
 * public static content.
 */
@RestController
public class StoredImageController {
    private final LocalFileStorageService storage;
    private final MessageAttachmentRepository attachments;
    private final ConversationMemberRepository members;
    private final UserRepository users;

    public StoredImageController(LocalFileStorageService storage,
            MessageAttachmentRepository attachments,
            ConversationMemberRepository members,
            UserRepository users) {
        this.storage = storage;
        this.attachments = attachments;
        this.members = members;
        this.users = users;
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<FileSystemResource> getImage(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable String filename) throws IOException {
        if (principal == null)
            throw new ForbiddenException("Authentication required");
        String url = "/uploads/" + filename;
        boolean isAvatar = users.existsByAvatarUrl(url);
        if (!isAvatar) {
            MessageAttachment attachment = attachments.findByFileUrl(url)
                    .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
            Long conversationId = attachment.getMessage().getConversation().getId();
            if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, principal.getId())) {
                throw new ForbiddenException("You cannot access this image");
            }
        }
        Path path = storage.findStoredFile(filename);
        String mime = Files.probeContentType(path);
        if (mime == null || !mime.startsWith("image/")) {
            mime = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(mime))
                .contentLength(Files.size(path))
                .body(new FileSystemResource(path));
    }
}
