package com.rindev.chat.controller;

import com.rindev.chat.dto.request.AddConversationMemberRequest;
import com.rindev.chat.dto.request.UpdateMemberRoleRequest;
import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.ConversationMemberResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.ConversationMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/members")
@Tag(name = "Conversation Members", description = "Manage group conversation members and roles")
@SecurityRequirement(name = "bearerAuth")
public class ConversationMemberController {

    private final ConversationMemberService memberService;

    public ConversationMemberController(
            ConversationMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "List conversation members")
    public ResponseEntity<ApiResponse<List<ConversationMemberResponse>>> getMembers(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long conversationId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        memberService.getMembers(
                                principal.getId(),
                                conversationId)));
    }

    @PostMapping
    @Operation(summary = "Add a member to a group")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Resource created")
    public ResponseEntity<ApiResponse<ConversationMemberResponse>> addMember(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long conversationId,
            @Valid @RequestBody AddConversationMemberRequest request) {

        ConversationMemberResponse response = memberService.addMember(
                principal.getId(),
                conversationId,
                request.userId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove a member from a group")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content",
            content = @io.swagger.v3.oas.annotations.media.Content)
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long conversationId,
            @PathVariable Long userId) {

        memberService.removeMember(
                principal.getId(),
                conversationId,
                userId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "Change a group member role")
    public ResponseEntity<ApiResponse<ConversationMemberResponse>> updateRole(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long conversationId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        memberService.updateRole(
                                principal.getId(),
                                conversationId,
                                userId,
                                request.role())));
    }
}