package com.rindev.chat.service;

import com.rindev.chat.dto.response.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    UploadResponse upload(
            Long authenticatedUserId,
            Long messageId,
            MultipartFile file);
}