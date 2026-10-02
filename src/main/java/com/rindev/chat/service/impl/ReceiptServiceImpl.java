package com.rindev.chat.service.impl;

import com.rindev.chat.dto.response.ReceiptResponse;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.MessageReceipt;
import com.rindev.chat.entity.User;
import com.rindev.chat.entity.UserSetting;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.MessageReceiptRepository;
import com.rindev.chat.repository.MessageRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.repository.UserSettingRepository;
import com.rindev.chat.service.ReceiptService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptServiceImpl
        implements ReceiptService {

    private final MessageRepository messageRepository;
    private final MessageReceiptRepository receiptRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final UserSettingRepository settingRepository;

    public ReceiptServiceImpl(
            MessageRepository messageRepository,
            MessageReceiptRepository receiptRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository,
            UserSettingRepository settingRepository) {

        this.messageRepository = messageRepository;
        this.receiptRepository = receiptRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.settingRepository = settingRepository;
    }

    @Override
    @Transactional
    public ReceiptResponse markDelivered(
            Long userId,
            Long messageId) {

        Message message = requireAccessibleMessage(
                userId,
                messageId);

        MessageReceipt receipt = getOrCreate(
                message,
                userId);

        if (receipt.getDeliveredAt() == null) {
            receipt.setDeliveredAt(
                    LocalDateTime.now());
        }

        return map(
                receiptRepository.save(receipt));
    }

    @Override
    @Transactional
    public ReceiptResponse markRead(
            Long userId,
            Long messageId) {

        Message message = requireAccessibleMessage(
                userId,
                messageId);

        boolean readReceiptsEnabled = settingRepository
                .findByUserId(userId)
                .map(UserSetting::getReadReceipts)
                .orElse(true);

        if (!readReceiptsEnabled) {
            throw new ForbiddenException(
                    "Read receipts are disabled");
        }

        MessageReceipt receipt = getOrCreate(
                message,
                userId);

        LocalDateTime now = LocalDateTime.now();

        if (receipt.getDeliveredAt() == null) {
            receipt.setDeliveredAt(now);
        }

        if (receipt.getReadAt() == null) {
            receipt.setReadAt(now);
        }

        return map(
                receiptRepository.save(receipt));
    }

    private MessageReceipt getOrCreate(
            Message message,
            Long userId) {

        return receiptRepository
                .findByMessageIdAndUserId(
                        message.getId(),
                        userId)
                .orElseGet(() -> {

                    User user = userRepository
                            .findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "User not found"));

                    MessageReceipt receipt = new MessageReceipt();

                    receipt.setMessage(message);
                    receipt.setUser(user);

                    return receipt;
                });
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

    private ReceiptResponse map(
            MessageReceipt receipt) {

        return new ReceiptResponse(
                receipt.getId(),
                receipt.getMessage().getId(),
                receipt.getUser().getId(),
                receipt.getDeliveredAt(),
                receipt.getReadAt());
    }
}