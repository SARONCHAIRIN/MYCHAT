package com.rindev.chat.controller;

import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.BlockedUserResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.BlockService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Blocking", description = "Block and unblock users")
@SecurityRequirement(name = "bearerAuth")
public class BlockController {

        private final BlockService blockService;

        public BlockController(
                        BlockService blockService) {
                this.blockService = blockService;
        }

        @PostMapping("/{id}/block")
        public ResponseEntity<ApiResponse<BlockedUserResponse>> block(
                        @AuthenticationPrincipal ChatUserDetails principal,
                        @PathVariable Long id) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.created(
                                                blockService.block(
                                                                principal.getId(),
                                                                id)));
        }

        @DeleteMapping("/{id}/block")
        public ResponseEntity<Void> unblock(
                        @AuthenticationPrincipal ChatUserDetails principal,
                        @PathVariable Long id) {

                blockService.unblock(
                                principal.getId(),
                                id);

                return ResponseEntity.noContent().build();
        }

        @GetMapping("/me/blocked")
        public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> list(
                        @AuthenticationPrincipal ChatUserDetails principal) {

                return ResponseEntity.ok(
                                ApiResponse.ok(
                                                blockService.getBlockedUsers(
                                                                principal.getId())));
        }
}