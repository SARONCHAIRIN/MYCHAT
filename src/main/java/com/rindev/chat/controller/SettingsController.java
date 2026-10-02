package com.rindev.chat.controller;

import com.rindev.chat.dto.request.UpdateSettingsRequest;
import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.ErrorResponse;
import com.rindev.chat.dto.response.UserSettingResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@Tag(name = "User Settings", description = "Manage authenticated user preferences and privacy settings")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @Operation(summary = "Get your settings")
    public ResponseEntity<ApiResponse<UserSettingResponse>> getSettings(
            @AuthenticationPrincipal ChatUserDetails principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        settingsService.getSettings(
                                principal.getId())));
    }

    @PatchMapping
    @Operation(summary = "Update your settings")
    public ResponseEntity<ApiResponse<UserSettingResponse>> updateSettings(
            @AuthenticationPrincipal ChatUserDetails principal,
            @Valid @RequestBody UpdateSettingsRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        settingsService.updateSettings(
                                principal.getId(),
                                request)));
    }
}