package com.rindev.chat.service.impl;

import com.rindev.chat.dto.request.CreateMessageRequest;
import com.rindev.chat.dto.request.UpdateMessageRequest;
import com.rindev.chat.dto.response.MessagePageResponse;
import com.rindev.chat.dto.response.MessageResponse;
import com.rindev.chat.entity.Conversation;
import com.rindev.chat.entity.ConversationMember;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.User;
import com.rindev.chat.enums.ConversationType;
import com.rindev.chat.enums.MessageType;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.mapper.MessageMapper;
import com.rindev.chat.repository.BlockedUserRepository;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.ConversationRepository;
import com.rindev.chat.repository.MessageRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.MessageService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rindev.chat.websocket.ConversationEventPublisher;
import com.rindev.chat.websocket.WebSocketEventType;

@Service
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

        private static final int DEFAULT_LIMIT = 30;
        private static final int MAX_LIMIT = 100;

        private final MessageRepository messageRepository;
        private final ConversationRepository conversationRepository;
        private final ConversationMemberRepository memberRepository;
        private final UserRepository userRepository;
        private final ConversationEventPublisher eventPublisher;
        private final BlockedUserRepository blockedUserRepository;
        private final MessageMapper messageMapper;

        public MessageServiceImpl(
                        MessageRepository messageRepository,
                        ConversationRepository conversationRepository,
                        ConversationMemberRepository memberRepository,
                        UserRepository userRepository,
                        ConversationEventPublisher eventPublisher,
                        BlockedUserRepository blockedUserRepository,
                        MessageMapper messageMapper) {
                this.messageRepository = messageRepository;
                this.conversationRepository = conversationRepository;
                this.memberRepository = memberRepository;
                this.userRepository = userRepository;
                this.eventPublisher = eventPublisher;
                this.blockedUserRepository = blockedUserRepository;
                this.messageMapper = messageMapper;
        }

        @Override
        @Transactional
        public MessageResponse createMessage(
                        Long authenticatedUserId,
                        Long conversationId,
                        CreateMessageRequest request) {

                Conversation conversation = findConversation(conversationId);
                requireMembership(conversationId, authenticatedUserId);

                User sender = findUser(authenticatedUserId);

                validateMessage(request.type(), request.content());

                if (request.type() == MessageType.SYSTEM) {
                        throw new ForbiddenException(
                                        "SYSTEM messages cannot be created by users");
                }

                enforceBlockRules(conversation, authenticatedUserId);

                Message message = new Message();
                message.setConversation(conversation);
                message.setSender(sender);
                message.setType(request.type());
                message.setContent(normalize(request.content()));

                if (request.replyToId() != null) {
                        Message reply = findMessage(request.replyToId());

                        requireSameConversation(
                                        conversationId,
                                        reply,
                                        "Reply message");

                        message.setReplyTo(reply);
                }

                if (request.forwardedFromId() != null) {
                        Message forwarded = findMessage(request.forwardedFromId());

                        requireMembership(
                                        forwarded.getConversation().getId(),
                                        authenticatedUserId);

                        message.setForwardedFrom(forwarded);
                }

                // 1. Save message
                Message saved = messageRepository.save(message);

                // 2. Update conversation
                conversation.setLastMessageAt(LocalDateTime.now());
                conversationRepository.save(conversation);

                // 3. Build response once
                MessageResponse response = messageMapper.toResponse(saved);

                // 4. Publish internal event
                // WebSocket broadcast happens AFTER COMMIT
                eventPublisher.publish(
                                WebSocketEventType.MESSAGE_NEW,
                                conversationId,
                                response);

                // 5. Return normal REST response
                return response;
        }

        @Override
        public MessagePageResponse getMessages(
                        Long authenticatedUserId,
                        Long conversationId,
                        Long before,
                        Integer requestedLimit) {

                findConversation(conversationId);
                requireMembership(conversationId, authenticatedUserId);

                int limit = validateLimit(requestedLimit);

                if (before != null && before <= 0) {
                        throw new BadRequestException(
                                        "before cursor must be greater than zero");
                }

                PageRequest pageable = PageRequest.of(0, limit + 1);

                List<Message> fetched;

                if (before == null) {
                        fetched = messageRepository
                                        .findByConversationIdOrderByIdDesc(
                                                        conversationId,
                                                        pageable);
                } else {
                        fetched = messageRepository
                                        .findByConversationIdAndIdLessThanOrderByIdDesc(
                                                        conversationId,
                                                        before,
                                                        pageable);
                }

                boolean hasMore = fetched.size() > limit;

                List<Message> page = hasMore
                                ? fetched.subList(0, limit)
                                : fetched;

                List<MessageResponse> responses = page.stream()
                                .map(messageMapper::toResponse)
                                .toList();

                Long nextCursor = hasMore && !page.isEmpty()
                                ? page.get(page.size() - 1).getId()
                                : null;

                return new MessagePageResponse(
                                responses,
                                nextCursor,
                                hasMore);
        }

        @Override
        public MessageResponse getMessage(
                        Long authenticatedUserId,
                        Long messageId) {

                Message message = findMessage(messageId);

                requireMembership(
                                message.getConversation().getId(),
                                authenticatedUserId);

                return messageMapper.toResponse(message);
        }

        @Override
        @Transactional
        public MessageResponse updateMessage(
                        Long authenticatedUserId,
                        Long messageId,
                        UpdateMessageRequest request) {

                Message message = findMessage(messageId);

                Long conversationId = message.getConversation().getId();

                requireMembership(
                                conversationId,
                                authenticatedUserId);

                requireOwner(message, authenticatedUserId);

                if (message.getDeletedAt() != null) {
                        throw new BadRequestException(
                                        "Deleted messages cannot be edited");
                }

                if (message.getType() != MessageType.TEXT) {
                        throw new BadRequestException(
                                        "Only TEXT messages can be edited");
                }

                String content = normalize(request.content());

                if (content == null) {
                        throw new BadRequestException(
                                        "Message content is required");
                }

                message.setContent(content);
                message.setEditedAt(LocalDateTime.now());

                // 1. Save edited message
                Message saved = messageRepository.save(message);

                // 2. Build response
                MessageResponse response = messageMapper.toResponse(saved);

                // 3. Publish internal event
                // Actual WebSocket broadcast happens AFTER COMMIT
                eventPublisher.publish(
                                WebSocketEventType.MESSAGE_EDITED,
                                conversationId,
                                response);

                // 4. Normal REST response
                return response;
        }

        @Override
        @Transactional
        public void deleteMessage(
                        Long authenticatedUserId,
                        Long messageId) {

                Message message = findMessage(messageId);

                Long conversationId = message.getConversation().getId();

                requireMembership(
                                conversationId,
                                authenticatedUserId);

                requireOwner(message, authenticatedUserId);

                // Already deleted -> idempotent
                // Do not broadcast duplicate event
                if (message.getDeletedAt() != null) {
                        return;
                }

                message.setDeletedAt(LocalDateTime.now());

                // 1. Save soft delete
                Message saved = messageRepository.save(message);

                // 2. Build deleted representation
                MessageResponse response = messageMapper.toResponse(saved);

                // 3. Publish internal event
                // Actual broadcast happens AFTER COMMIT
                eventPublisher.publish(
                                WebSocketEventType.MESSAGE_DELETED,
                                conversationId,
                                response);
        }

        private int validateLimit(Integer requestedLimit) {
                int limit = requestedLimit == null
                                ? DEFAULT_LIMIT
                                : requestedLimit;

                if (limit < 1 || limit > MAX_LIMIT) {
                        throw new BadRequestException(
                                        "limit must be between 1 and 100");
                }

                return limit;
        }

        private void validateMessage(
                        MessageType type,
                        String content) {

                if (type == null) {
                        throw new BadRequestException(
                                        "Message type is required");
                }

                String normalized = normalize(content);

                switch (type) {
                        case TEXT, LOCATION -> {
                                if (normalized == null) {
                                        throw new BadRequestException(
                                                        "Content is required for " + type + " messages");
                                }
                        }

                        case IMAGE, VIDEO, AUDIO, FILE -> {
                                if (normalized == null) {
                                        throw new BadRequestException(
                                                        "Attachment reference is required for " + type
                                                                        + " messages");
                                }
                        }

                        case SYSTEM -> {
                                // rejected separately for normal authenticated users
                        }
                }
        }

        private void enforceBlockRules(
                        Conversation conversation,
                        Long senderId) {

                if (conversation.getType() != ConversationType.DIRECT) {
                        return;
                }

                List<ConversationMember> members = memberRepository
                                .findByConversationIdAndLeftAtIsNull(
                                                conversation.getId());

                for (ConversationMember member : members) {
                        Long otherUserId = member.getUser().getId();

                        if (otherUserId.equals(senderId)) {
                                continue;
                        }

                        boolean blocked = blockedUserRepository
                                        .existsByBlockerIdAndBlockedId(
                                                        senderId,
                                                        otherUserId)
                                        || blockedUserRepository
                                                        .existsByBlockerIdAndBlockedId(
                                                                        otherUserId,
                                                                        senderId);

                        if (blocked) {
                                throw new ForbiddenException(
                                                "Messaging is not allowed between these users");
                        }
                }
        }

        private void requireSameConversation(
                        Long conversationId,
                        Message message,
                        String resourceName) {

                if (!message.getConversation()
                                .getId()
                                .equals(conversationId)) {
                        throw new BadRequestException(
                                        resourceName
                                                        + " must belong to the same conversation");
                }
        }

        private void requireOwner(
                        Message message,
                        Long authenticatedUserId) {

                if (!message.getSender()
                                .getId()
                                .equals(authenticatedUserId)) {
                        throw new ForbiddenException(
                                        "You can only modify your own messages");
                }
        }

        private void requireMembership(
                        Long conversationId,
                        Long userId) {

                if (!memberRepository
                                .existsByConversationIdAndUserIdAndLeftAtIsNull(
                                                conversationId,
                                                userId)) {
                        throw new ForbiddenException(
                                        "You are not a member of this conversation");
                }
        }

        private Conversation findConversation(Long id) {
                return conversationRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Conversation not found with id: " + id));
        }

        private Message findMessage(Long id) {
                return messageRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Message not found with id: " + id));
        }

        private User findUser(Long id) {
                return userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User not found with id: " + id));
        }

        private String normalize(String value) {
                if (value == null) {
                        return null;
                }

                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }
}