package com.rindev.chat.controller;

import com.rindev.chat.dto.request.CreateReportRequest;
import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.ReportResponse;
import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Report abusive users, conversations and messages")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(
            ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @Operation(summary = "Submit an abuse report")
    public ResponseEntity<ApiResponse<ReportResponse>> create(
            @AuthenticationPrincipal ChatUserDetails principal,
            @Valid @RequestBody CreateReportRequest request) {

        ReportResponse response = reportService.createReport(
                principal.getId(),
                request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }
}