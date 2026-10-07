package com.rindev.chat.controller;

import com.rindev.chat.dto.request.UpdateAvatarRequest;
import com.rindev.chat.dto.request.UpdateUserRequest;
import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.ErrorResponse;
import com.rindev.chat.dto.response.UserPublicResponse;
import com.rindev.chat.dto.response.UserResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User discovery and profile management")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request successful")
    public ResponseEntity<ApiResponse<Page<UserPublicResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        userService.getUsers(page, size)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by name or username")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request successful")
    public ResponseEntity<ApiResponse<Page<UserPublicResponse>>> searchUsers(
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        userService.searchUsers(query, page, size)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get your profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request successful")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal ChatUserDetails principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        userService.getMe(principal.getId())));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update your profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request successful")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal ChatUserDetails principal,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        userService.updateMe(
                                principal.getId(),
                                request)));
    }

    @PatchMapping("/me/avatar")
    @Operation(summary = "Update your avatar")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request successful")
    public ResponseEntity<ApiResponse<UserResponse>> updateAvatar(
            @AuthenticationPrincipal ChatUserDetails principal,
            @Valid @RequestBody UpdateAvatarRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        userService.updateAvatar(
                                principal.getId(),
                                request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user's public profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request successful")
    public ResponseEntity<ApiResponse<UserPublicResponse>> getUserById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        userService.getUserById(id)));
    }
}