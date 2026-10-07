package com.rindev.chat.mapper;

import com.rindev.chat.dto.response.MessageResponse;
import com.rindev.chat.entity.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getSender().getUsername(),
                message.getType(),
                message.getDeletedAt() != null ? null : message.getContent(),
                message.getReplyTo() != null
                        ? message.getReplyTo().getId()
                        : null,
                message.getForwardedFrom() != null
                        ? message.getForwardedFrom().getId()
                        : null,
                message.getEditedAt() != null,
                message.getDeletedAt() != null,
                message.getEditedAt(),
                message.getDeletedAt(),
                message.getCreatedAt(),
                message.getUpdatedAt());
    }
}