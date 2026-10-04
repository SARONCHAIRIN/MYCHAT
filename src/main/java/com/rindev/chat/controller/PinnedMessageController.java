package com.rindev.chat.controller;

import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.PinnedMessageResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.PinnedMessageService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/pins")
@Tag(name = "Pinned Messages", description = "Pin and manage conversation messages")
@SecurityRequirement(name = "bearerAuth")
public class PinnedMessageController {

        private final PinnedMessageService service;

        public PinnedMessageController(
                        PinnedMessageService service) {
                this.service = service;
        }

        @PostMapping("/{messageId}")
        public ResponseEntity<ApiResponse<PinnedMessageResponse>> pin(
                        @AuthenticationPrincipal ChatUserDetails principal,
                        @PathVariable Long conversationId,
                        @PathVariable Long messageId) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.created(
                                                service.pin(
                                                                principal.getId(),
                                                                conversationId,
                                                                messageId)));
        }

        @GetMapping
        public ResponseEntity<ApiResponse<List<PinnedMessageResponse>>> list(
                        @AuthenticationPrincipal ChatUserDetails principal,
                        @PathVariable Long conversationId) {

                return ResponseEntity.ok(
                                ApiResponse.ok(
                                                service.list(
                                                                principal.getId(),
                                                                conversationId)));
        }

        @DeleteMapping("/{messageId}")
        public ResponseEntity<Void> unpin(
                        @AuthenticationPrincipal ChatUserDetails principal,
                        @PathVariable Long conversationId,
                        @PathVariable Long messageId) {

                service.unpin(
                                principal.getId(),
                                conversationId,
                                messageId);

                return ResponseEntity.noContent().build();
        }
}