package com.rindev.chat.controller;

import com.rindev.chat.entity.MessageAttachment;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.MessageAttachmentRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.storage.FileStorageService;
import com.rindev.chat.storage.StoredObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** All stored objects are private; R2 bucket has no public URL access. */
@RestController
public class StoredImageController {
    private final FileStorageService storage;
    private final MessageAttachmentRepository attachments;
    private final ConversationMemberRepository members;
    private final UserRepository users;

    public StoredImageController(FileStorageService storage,
                                 MessageAttachmentRepository attachments,
                                 ConversationMemberRepository members,
                                 UserRepository users) {
        this.storage = storage;
        this.attachments = attachments;
        this.members = members;
        this.users = users;
    }

    @GetMapping("/uploads/{filename:.+}")
    @Operation(summary = "Download stored attachment", security = @SecurityRequirement(name = "bearerAuth"))
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getImage(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable String filename) {
        if (principal == null) throw new ForbiddenException("Authentication required");
        String fileUrl = "/uploads/" + filename;
        boolean avatar = users.existsByAvatarUrl(fileUrl);
        if (!avatar) {
            MessageAttachment attachment = attachments.findByFileUrl(fileUrl)
                    .orElseThrow(() -> new ResourceNotFoundException("Image attachment not found"));
            Long conversationId = attachment.getMessage().getConversation().getId();
            if (!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, principal.getId())) {
                throw new ForbiddenException("You cannot access this image");
            }
        }
        StoredObject object = storage.read(filename);
        String type = object.contentType();
        boolean safeImage = "image/jpeg".equalsIgnoreCase(type)
                || "image/png".equalsIgnoreCase(type)
                || "image/gif".equalsIgnoreCase(type)
                || "image/webp".equalsIgnoreCase(type);
        MediaType responseType = safeImage ? MediaType.parseMediaType(type) : MediaType.APPLICATION_OCTET_STREAM;
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentType(responseType)
                .contentLength(object.bytes().length);
        if (!safeImage) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(filename).build().toString());
        }
        return builder.body(object.bytes());
    }
}
