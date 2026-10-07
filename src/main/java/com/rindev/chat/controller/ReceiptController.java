package com.rindev.chat.controller;

import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.ReceiptResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.ReceiptService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages/{messageId}")
@Tag(name = "Message Receipts")
@SecurityRequirement(name = "bearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(
            ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping("/delivered")
    public ResponseEntity<ApiResponse<ReceiptResponse>> markDelivered(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long messageId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        receiptService.markDelivered(
                                principal.getId(),
                                messageId)));
    }

    @PostMapping("/read")
    public ResponseEntity<ApiResponse<ReceiptResponse>> markRead(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long messageId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        receiptService.markRead(
                                principal.getId(),
                                messageId)));
    }
}