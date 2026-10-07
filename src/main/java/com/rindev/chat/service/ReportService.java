package com.rindev.chat.service;

import com.rindev.chat.dto.request.CreateReportRequest;
import com.rindev.chat.dto.response.ReportResponse;

public interface ReportService {

    ReportResponse createReport(
            Long reporterId,
            CreateReportRequest request);
}