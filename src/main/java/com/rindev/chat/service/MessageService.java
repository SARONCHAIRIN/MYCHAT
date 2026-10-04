package com.rindev.chat.service;

import com.rindev.chat.dto.request.CreateMessageRequest;
import com.rindev.chat.dto.request.UpdateMessageRequest;
import com.rindev.chat.dto.response.MessagePageResponse;
import com.rindev.chat.dto.response.MessageResponse;

public interface MessageService {

    MessageResponse createMessage(
            Long authenticatedUserId,
            Long conversationId,
            CreateMessageRequest request);

    MessagePageResponse getMessages(
            Long authenticatedUserId,
            Long conversationId,
            Long before,
            Integer limit);

    MessageResponse getMessage(
            Long authenticatedUserId,
            Long messageId);

    MessageResponse updateMessage(
            Long authenticatedUserId,
            Long messageId,
            UpdateMessageRequest request);

    void deleteMessage(
            Long authenticatedUserId,
            Long messageId);
}