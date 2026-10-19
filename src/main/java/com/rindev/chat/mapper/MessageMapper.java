package com.rindev.chat.mapper;

import com.rindev.chat.dto.response.MessageResponse;
import com.rindev.chat.dto.response.UploadResponse;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.MessageAttachment;
import com.rindev.chat.repository.MessageAttachmentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

        private final MessageAttachmentRepository attachmentRepository;

        public MessageMapper(MessageAttachmentRepository attachmentRepository) {
                this.attachmentRepository = attachmentRepository;
        }

        public MessageResponse toResponse(Message message) {
                List<UploadResponse> attachments = message.getDeletedAt() == null
                                ? attachmentRepository.findByMessageId(message.getId()).stream()
                                                .map(this::toUploadResponse).toList()
                                : List.of();
                return toResponse(message, attachments);
        }

        // Batch fetch avoids one attachment query per message in paginated history.
        public List<MessageResponse> toResponses(List<Message> messages) {
                if (messages.isEmpty()) {
                        return List.of();
                }
                List<Long> visibleIds = messages.stream()
                                .filter(message -> message.getDeletedAt() == null)
                                .map(Message::getId).toList();
                Map<Long, List<UploadResponse>> byMessage = visibleIds.isEmpty()
                                ? Map.of()
                                : attachmentRepository.findByMessageIdIn(visibleIds).stream()
                                                .collect(Collectors.groupingBy(
                                                                attachment -> attachment.getMessage().getId(),
                                                                Collectors.mapping(this::toUploadResponse,
                                                                                Collectors.toList())));
                return messages.stream()
                                .map(message -> toResponse(message,
                                                byMessage.getOrDefault(message.getId(), List.of())))
                                .toList();
        }

        private UploadResponse toUploadResponse(MessageAttachment attachment) {
                return new UploadResponse(
                                attachment.getId(), attachment.getMessage().getId(),
                                attachment.getType(), attachment.getFileName(),
                                attachment.getFileUrl(), attachment.getMimeType(),
                                attachment.getFileSize());
        }

        private MessageResponse toResponse(Message message, List<UploadResponse> attachments) {
                return new MessageResponse(
                                message.getId(),
                                message.getConversation().getId(),
                                message.getSender().getId(),
                                message.getSender().getName(),
                                message.getSender().getUsername(),
                                message.getType(),
                                message.getDeletedAt() != null ? null : message.getContent(),
                                message.getReplyTo() != null ? message.getReplyTo().getId() : null,
                                message.getForwardedFrom() != null ? message.getForwardedFrom().getId() : null,
                                message.getEditedAt() != null,
                                message.getDeletedAt() != null,
                                message.getEditedAt(),
                                message.getDeletedAt(),
                                message.getCreatedAt(),
                                message.getUpdatedAt(),
                                attachments);
        }
}
