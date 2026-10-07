package com.rindev.chat.controller;

import com.rindev.chat.dto.request.LoginRequest;
import com.rindev.chat.dto.request.RefreshTokenRequest;
import com.rindev.chat.dto.request.RegisterRequest;
import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.AuthResponse;
import com.rindev.chat.dto.response.ErrorResponse;
import com.rindev.chat.dto.response.UserResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, sign in, rotate refresh tokens, and access your account")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register an account", description = "Creates the account and default settings, then returns access and refresh tokens.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.created(authService.register(request)));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Sign in with a username and password")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Signed in")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renew authentication tokens",
            description = "Exchanges a refresh token once. Store the returned replacement; the previous refresh token stops working.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tokens renewed")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(authService.refresh(request)));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke one refresh session",
            description = "Requires your access token and a refresh token owned by your account. Access JWTs remain valid until their expiry.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refresh session revoked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid refresh token, including a token owned by another account",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal ChatUserDetails principal,
                                                   @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(principal.getId(), request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get your current account")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current account")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal ChatUserDetails principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(authService.me(principal.getId())));
    }
}
