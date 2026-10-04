package com.rindev.chat.service.impl;

import com.rindev.chat.dto.response.ReactionResponse;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.MessageReaction;
import com.rindev.chat.entity.User;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ConflictException;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.MessageReactionRepository;
import com.rindev.chat.repository.MessageRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.ReactionService;
import com.rindev.chat.websocket.ConversationEventPublisher;
import com.rindev.chat.websocket.WebSocketEventType;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReactionServiceImpl
                implements ReactionService {

        private final MessageRepository messageRepository;
        private final MessageReactionRepository reactionRepository;
        private final ConversationMemberRepository memberRepository;
        private final ConversationEventPublisher eventPublisher;
        private final UserRepository userRepository;

        public ReactionServiceImpl(
                        MessageRepository messageRepository,
                        MessageReactionRepository reactionRepository,
                        ConversationMemberRepository memberRepository,
                        ConversationEventPublisher eventPublisher,
                        UserRepository userRepository) {

                this.messageRepository = messageRepository;
                this.reactionRepository = reactionRepository;
                this.memberRepository = memberRepository;
                this.userRepository = userRepository;
                this.eventPublisher = eventPublisher;

        }

        @Override
        @Transactional
        public ReactionResponse addReaction(
                        Long userId,
                        Long messageId,
                        String emoji) {

                Message message = requireAccessibleMessage(
                                userId,
                                messageId);

                Long conversationId = message.getConversation().getId();

                String normalized = normalizeEmoji(emoji);

                boolean exists = reactionRepository
                                .existsByMessageIdAndUserIdAndEmoji(
                                                messageId,
                                                userId,
                                                normalized);

                if (exists) {
                        throw new ConflictException(
                                        "Reaction already exists");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User not found"));

                MessageReaction reaction = new MessageReaction();

                reaction.setMessage(message);
                reaction.setUser(user);
                reaction.setEmoji(normalized);

                // 1. Save
                MessageReaction saved = reactionRepository.save(reaction);

                // 2. Build response
                ReactionResponse response = map(saved);

                // 3. Publish internal event
                // WebSocket broadcast occurs AFTER COMMIT
                eventPublisher.publish(
                                WebSocketEventType.REACTION_ADDED,
                                conversationId,
                                response);

                // 4. Normal REST response
                return response;
        }

        @Override
        public List<ReactionResponse> getReactions(
                        Long userId,
                        Long messageId) {

                requireAccessibleMessage(
                                userId,
                                messageId);

                return reactionRepository
                                .findByMessageIdOrderByCreatedAtAsc(
                                                messageId)
                                .stream()
                                .map(this::map)
                                .toList();
        }

        @Override
        @Transactional
        public void removeReaction(
                        Long userId,
                        Long messageId,
                        String emoji) {

                Message message = requireAccessibleMessage(
                                userId,
                                messageId);

                Long conversationId = message.getConversation().getId();

                String normalized = normalizeEmoji(emoji);

                MessageReaction reaction = reactionRepository
                                .findByMessageIdAndUserIdAndEmoji(
                                                messageId,
                                                userId,
                                                normalized)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Reaction not found"));

                // Build response BEFORE delete
                ReactionResponse response = map(reaction);

                // Delete reaction
                reactionRepository.delete(reaction);

                // Publish internal event.
                // Listener sends it only AFTER transaction commit.
                eventPublisher.publish(
                                WebSocketEventType.REACTION_REMOVED,
                                conversationId,
                                response);
        }

        private Message requireAccessibleMessage(
                        Long userId,
                        Long messageId) {

                Message message = messageRepository
                                .findById(messageId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Message not found"));

                Long conversationId = message.getConversation().getId();

                boolean member = memberRepository
                                .existsByConversationIdAndUserIdAndLeftAtIsNull(
                                                conversationId,
                                                userId);

                if (!member) {
                        throw new ForbiddenException(
                                        "You cannot access this message");
                }

                return message;
        }

        private String normalizeEmoji(String emoji) {

                if (emoji == null || emoji.isBlank()) {
                        throw new BadRequestException(
                                        "Emoji is required");
                }

                String value = emoji.trim();

                if (value.length() > 20) {
                        throw new BadRequestException(
                                        "Emoji is too long");
                }

                return value;
        }

        private ReactionResponse map(
                        MessageReaction reaction) {

                return new ReactionResponse(
                                reaction.getId(),
                                reaction.getMessage().getId(),
                                reaction.getUser().getId(),
                                reaction.getUser().getUsername(),
                                reaction.getEmoji(),
                                reaction.getCreatedAt());
        }
}