package com.rindev.chat.controller;

import com.rindev.chat.dto.request.AddReactionRequest;
import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.ReactionResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.ReactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages/{messageId}/reactions")
@Tag(name = "Reactions")
@SecurityRequirement(name = "bearerAuth")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(
            ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReactionResponse>> add(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long messageId,
            @Valid @RequestBody AddReactionRequest request) {

        ReactionResponse response = reactionService.addReaction(
                principal.getId(),
                messageId,
                request.emoji());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReactionResponse>>> list(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long messageId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        reactionService.getReactions(
                                principal.getId(),
                                messageId)));
    }

    @DeleteMapping("/{emoji}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long messageId,
            @PathVariable String emoji) {

        reactionService.removeReaction(
                principal.getId(),
                messageId,
                emoji);

        return ResponseEntity.noContent().build();
    }
}