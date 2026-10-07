package com.rindev.chat.controller;

import com.rindev.chat.dto.request.CreateConversationRequest;
import com.rindev.chat.dto.request.UpdateConversationRequest;
import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.ConversationResponse;
import com.rindev.chat.dto.response.ErrorResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversations", description = "Create and manage direct and group conversations")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(
            ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    @Operation(summary = "Create a conversation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Resource created")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @AuthenticationPrincipal ChatUserDetails principal,

            @Valid @RequestBody CreateConversationRequest request) {

        ConversationResponse response = conversationService
                .createConversation(
                        principal.getId(),
                        request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping
    @Operation(summary = "List your conversations")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request successful")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal ChatUserDetails principal) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        conversationService
                                .getConversations(
                                        principal.getId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request successful")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @AuthenticationPrincipal ChatUserDetails principal,

            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        conversationService
                                .getConversation(
                                        principal.getId(),
                                        id)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a group conversation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request successful")
    public ResponseEntity<ApiResponse<ConversationResponse>> updateConversation(
            @AuthenticationPrincipal ChatUserDetails principal,

            @PathVariable Long id,

            @Valid @RequestBody UpdateConversationRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        conversationService
                                .updateConversation(
                                        principal.getId(),
                                        id,
                                        request)));
    }
}