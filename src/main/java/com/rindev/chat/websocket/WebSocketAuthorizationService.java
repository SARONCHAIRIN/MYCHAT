package com.rindev.chat.websocket;

import com.rindev.chat.repository.ConversationMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class WebSocketAuthorizationService {

    private final ConversationMemberRepository memberRepository;

    public WebSocketAuthorizationService(
            ConversationMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public boolean canAccessConversation(
            Long userId,
            Long conversationId) {

        return memberRepository
                .existsByConversationIdAndUserIdAndLeftAtIsNull(
                        conversationId,
                        userId);
    }
}