package com.rindev.chat.service;

import com.rindev.chat.dto.response.AudioStreamResponse;
import com.rindev.chat.dto.response.MessageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VoiceMessageService {

    MessageResponse sendVoiceMessage(
            Long authenticatedUserId,
            Long conversationId,
            MultipartFile audio,
            Integer durationSeconds);

    AudioStreamResponse streamVoiceMessage(
            Long authenticatedUserId,
            Long messageId,
            String rangeHeader);
}
