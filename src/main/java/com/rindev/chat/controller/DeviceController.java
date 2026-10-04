package com.rindev.chat.controller;

import com.rindev.chat.dto.request.RegisterDeviceRequest;
import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.DeviceResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.DeviceService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Register and manage user devices")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeviceResponse>> register(
            @AuthenticationPrincipal ChatUserDetails principal,
            @Valid @RequestBody RegisterDeviceRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        deviceService.register(
                                principal.getId(),
                                request,
                                httpRequest.getRemoteAddr())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal ChatUserDetails principal,
            @PathVariable Long id) {

        deviceService.delete(
                principal.getId(),
                id);

        return ResponseEntity.noContent().build();
    }
}