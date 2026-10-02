package com.rindev.chat.service.impl;

import com.rindev.chat.dto.response.PinnedMessageResponse;
import com.rindev.chat.entity.ConversationMember;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.PinnedMessage;
import com.rindev.chat.entity.User;
import com.rindev.chat.enums.MemberRole;
import com.rindev.chat.exception.ConflictException;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.MessageRepository;
import com.rindev.chat.repository.PinnedMessageRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.PinnedMessageService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PinnedMessageServiceImpl
        implements PinnedMessageService {

    private final PinnedMessageRepository pinnedRepository;
    private final MessageRepository messageRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public PinnedMessageServiceImpl(
            PinnedMessageRepository pinnedRepository,
            MessageRepository messageRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository) {

        this.pinnedRepository = pinnedRepository;
        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public PinnedMessageResponse pin(
            Long userId,
            Long conversationId,
            Long messageId) {

        ConversationMember member = requireMember(conversationId, userId);

        requirePinPermission(member);

        Message message = requireMessageInConversation(
                conversationId,
                messageId);

        if (pinnedRepository
                .existsByConversationIdAndMessageId(
                        conversationId,
                        messageId)) {

            throw new ConflictException(
                    "Message is already pinned");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        PinnedMessage pinned = new PinnedMessage();

        pinned.setConversation(
                member.getConversation());

        pinned.setMessage(message);
        pinned.setPinnedBy(user);

        return map(
                pinnedRepository.save(pinned));
    }

    @Override
    @Transactional
    public void unpin(
            Long userId,
            Long conversationId,
            Long messageId) {

        ConversationMember member = requireMember(conversationId, userId);

        requirePinPermission(member);

        PinnedMessage pinned = pinnedRepository
                .findByConversationIdAndMessageId(
                        conversationId,
                        messageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pinned message not found"));

        pinnedRepository.delete(pinned);
    }

    @Override
    public List<PinnedMessageResponse> list(
            Long userId,
            Long conversationId) {

        requireMember(conversationId, userId);

        return pinnedRepository
                .findByConversationIdOrderByPinnedAtDesc(
                        conversationId)
                .stream()
                .map(this::map)
                .toList();
    }

    private ConversationMember requireMember(
            Long conversationId,
            Long userId) {

        return memberRepository
                .findByConversationIdAndUserIdAndLeftAtIsNull(
                        conversationId,
                        userId)
                .orElseThrow(() -> new ForbiddenException(
                        "You are not a member of this conversation"));
    }

    private void requirePinPermission(
            ConversationMember member) {

        if (member.getRole() != MemberRole.OWNER
                && member.getRole() != MemberRole.ADMIN) {

            throw new ForbiddenException(
                    "Only owners and admins can manage pins");
        }
    }

    private Message requireMessageInConversation(
            Long conversationId,
            Long messageId) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Message not found"));

        if (!message.getConversation()
                .getId()
                .equals(conversationId)) {

            throw new ForbiddenException(
                    "Message does not belong to this conversation");
        }

        return message;
    }

    private PinnedMessageResponse map(
            PinnedMessage pinned) {

        return new PinnedMessageResponse(
                pinned.getId(),
                pinned.getConversation().getId(),
                pinned.getMessage().getId(),
                pinned.getPinnedBy().getId(),
                pinned.getPinnedAt());
    }
}