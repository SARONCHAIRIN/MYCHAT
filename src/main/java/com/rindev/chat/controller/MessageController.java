package com.rindev.chat.controller;

import com.rindev.chat.dto.request.CreateMessageRequest;
import com.rindev.chat.dto.request.UpdateMessageRequest;
import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.MessagePageResponse;
import com.rindev.chat.dto.response.MessageResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Messages", description = "Send, retrieve, edit and delete chat messages")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/api/v1/conversations/{conversationId}/messages")
    @Operation(summary = "Send a message")
    public ResponseEntity<ApiResponse<MessageResponse>> createMessage(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long conversationId,
            @Valid @RequestBody CreateMessageRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        messageService.createMessage(
                                principal.getId(),
                                conversationId,
                                request)));
    }

    @GetMapping("/api/v1/conversations/{conversationId}/messages")
    @Operation(summary = "Get conversation message history")
    public ResponseEntity<ApiResponse<MessagePageResponse>> getMessages(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false, defaultValue = "30") Integer limit) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        messageService.getMessages(
                                principal.getId(),
                                conversationId,
                                before,
                                limit)));
    }

    @GetMapping("/api/v1/messages/{messageId}")
    @Operation(summary = "Get a message")
    public ResponseEntity<ApiResponse<MessageResponse>> getMessage(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long messageId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        messageService.getMessage(
                                principal.getId(),
                                messageId)));
    }

    @PatchMapping("/api/v1/messages/{messageId}")
    @Operation(summary = "Edit your message")
    public ResponseEntity<ApiResponse<MessageResponse>> updateMessage(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        messageService.updateMessage(
                                principal.getId(),
                                messageId,
                                request)));
    }

    @DeleteMapping("/api/v1/messages/{messageId}")
    @Operation(summary = "Soft delete your message")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long messageId) {

        messageService.deleteMessage(
                principal.getId(),
                messageId);

        return ResponseEntity.noContent().build();
    }
}