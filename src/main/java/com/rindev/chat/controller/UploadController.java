package com.rindev.chat.controller;

import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.UploadResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
@Tag(name = "Uploads", description = "Upload message attachments")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

        private final UploadService uploadService;

        public UploadController(
                        UploadService uploadService) {
                this.uploadService = uploadService;
        }

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Upload a message attachment")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Resource created")
        public ResponseEntity<ApiResponse<UploadResponse>> upload(
                        @AuthenticationPrincipal ChatUserDetails principal,
                        @RequestParam Long messageId,
                        @RequestPart("file") MultipartFile file) {

                UploadResponse response = uploadService.upload(
                                principal.getId(),
                                messageId,
                                file);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.created(response));
        }
}