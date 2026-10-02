package com.rindev.chat.controller;

import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.NotificationPageResponse;
import com.rindev.chat.dto.response.NotificationResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Manage user notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

        private final NotificationService service;

        public NotificationController(
                        NotificationService service) {
                this.service = service;
        }

        @GetMapping
        public ResponseEntity<ApiResponse<NotificationPageResponse>> list(
                        @AuthenticationPrincipal ChatUserDetails principal,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {

                return ResponseEntity.ok(
                                ApiResponse.ok(
                                                service.getNotifications(
                                                                principal.getId(),
                                                                page,
                                                                size)));
        }

        @PostMapping("/{id}/read")
        public ResponseEntity<ApiResponse<NotificationResponse>> read(
                        @AuthenticationPrincipal ChatUserDetails principal,
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                ApiResponse.ok(
                                                service.markRead(
                                                                principal.getId(),
                                                                id)));
        }

        @PostMapping("/read-all")
        public ResponseEntity<Void> readAll(
                        @AuthenticationPrincipal ChatUserDetails principal) {

                service.markAllRead(
                                principal.getId());

                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(
                        @AuthenticationPrincipal ChatUserDetails principal,
                        @PathVariable Long id) {

                service.delete(
                                principal.getId(),
                                id);

                return ResponseEntity.noContent().build();
        }
}