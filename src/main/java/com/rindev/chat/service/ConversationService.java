package com.rindev.chat.service;

import com.rindev.chat.dto.request.CreateConversationRequest;
import com.rindev.chat.dto.request.UpdateConversationRequest;
import com.rindev.chat.dto.response.ConversationResponse;
import java.util.List;

public interface ConversationService {

    ConversationResponse createConversation(
            Long authenticatedUserId,
            CreateConversationRequest request);

    List<ConversationResponse> getConversations(
            Long authenticatedUserId);

    ConversationResponse getConversation(
            Long authenticatedUserId,
            Long conversationId);

    ConversationResponse updateConversation(
            Long authenticatedUserId,
            Long conversationId,
            UpdateConversationRequest request);
}