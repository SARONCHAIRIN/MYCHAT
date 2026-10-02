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
    private final UserRepository userRepository;

    public ReactionServiceImpl(
            MessageRepository messageRepository,
            MessageReactionRepository reactionRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository) {

        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
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

        return map(
                reactionRepository.save(reaction));
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

        requireAccessibleMessage(
                userId,
                messageId);

        String normalized = normalizeEmoji(emoji);

        MessageReaction reaction = reactionRepository
                .findByMessageIdAndUserIdAndEmoji(
                        messageId,
                        userId,
                        normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reaction not found"));

        reactionRepository.delete(reaction);
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