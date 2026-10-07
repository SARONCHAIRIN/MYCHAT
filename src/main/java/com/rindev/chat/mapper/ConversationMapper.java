package com.rindev.chat.mapper;

import com.rindev.chat.dto.response.ConversationMemberResponse;
import com.rindev.chat.dto.response.ConversationResponse;
import com.rindev.chat.entity.Conversation;
import com.rindev.chat.entity.ConversationMember;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationMemberResponse toMemberResponse(
            ConversationMember member) {
        return new ConversationMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getUsername(),
                member.getUser().getAvatarUrl(),
                member.getRole(),
                member.getJoinedAt());
    }

    public ConversationResponse toResponse(
            Conversation conversation,
            List<ConversationMember> members) {
        List<ConversationMemberResponse> memberResponses = members.stream()
                .map(this::toMemberResponse)
                .toList();

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getDescription(),
                conversation.getAvatarUrl(),
                conversation.getCreatedBy() != null
                        ? conversation.getCreatedBy().getId()
                        : null,
                conversation.getLastMessageAt(),
                memberResponses,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }
}