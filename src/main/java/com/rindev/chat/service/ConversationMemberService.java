package com.rindev.chat.service;

import com.rindev.chat.dto.response.ConversationMemberResponse;
import com.rindev.chat.enums.MemberRole;
import java.util.List;

public interface ConversationMemberService {

    List<ConversationMemberResponse> getMembers(
            Long authenticatedUserId,
            Long conversationId);

    ConversationMemberResponse addMember(
            Long authenticatedUserId,
            Long conversationId,
            Long userId);

    void removeMember(
            Long authenticatedUserId,
            Long conversationId,
            Long userId);

    ConversationMemberResponse updateRole(
            Long authenticatedUserId,
            Long conversationId,
            Long userId,
            MemberRole role);
}