package com.rindev.chat.service.impl;

import com.rindev.chat.entity.Conversation;
import com.rindev.chat.entity.ConversationMember;
import com.rindev.chat.entity.Device;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.User;
import com.rindev.chat.entity.UserSetting;
import com.rindev.chat.enums.ConversationType;
import com.rindev.chat.notification.MessageNotificationEvent;
import com.rindev.chat.notification.PushNotificationProvider;
import com.rindev.chat.notification.PushNotificationRequest;
import com.rindev.chat.notification.PushNotificationResult;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.DeviceRepository;
import com.rindev.chat.repository.MessageRepository;
import com.rindev.chat.repository.UserSettingRepository;
import com.rindev.chat.service.PushNotificationService;
import com.rindev.chat.websocket.PresenceService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationServiceImpl
        implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(
            PushNotificationServiceImpl.class);

    private final MessageRepository messageRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserSettingRepository settingRepository;
    private final DeviceRepository deviceRepository;
    private final PresenceService presenceService;
    private final PushNotificationProvider provider;

    public PushNotificationServiceImpl(
            MessageRepository messageRepository,
            ConversationMemberRepository memberRepository,
            UserSettingRepository settingRepository,
            DeviceRepository deviceRepository,
            PresenceService presenceService,
            PushNotificationProvider provider) {

        this.messageRepository = messageRepository;
        this.memberRepository = memberRepository;
        this.settingRepository = settingRepository;
        this.deviceRepository = deviceRepository;
        this.presenceService = presenceService;
        this.provider = provider;
    }

    @Override
    @Transactional
    public void sendNewMessageNotifications(
            MessageNotificationEvent event) {

        Message message = messageRepository.findById(event.messageId())
                .orElse(null);

        if (message == null) {
            log.warn(
                    "Push skipped: message {} not found",
                    event.messageId());
            return;
        }

        Conversation conversation = message.getConversation();

        User sender = message.getSender();

        List<ConversationMember> members = memberRepository
                .findByConversationIdAndLeftAtIsNull(
                        event.conversationId());

        for (ConversationMember member : members) {

            User recipient = member.getUser();

            if (!shouldNotify(
                    sender,
                    recipient,
                    member,
                    conversation)) {
                continue;
            }

            sendToRecipient(
                    message,
                    conversation,
                    sender,
                    recipient);
        }
    }

    private boolean shouldNotify(
            User sender,
            User recipient,
            ConversationMember membership,
            Conversation conversation) {

        // Never push a user's own message back to them.
        if (recipient.getId().equals(sender.getId())) {
            return false;
        }

        // WebSocket-connected users already receive realtime events.
        if (presenceService.isOnline(recipient.getId())) {
            return false;
        }

        // Conversation mute.
        LocalDateTime mutedUntil = membership.getMutedUntil();

        if (mutedUntil != null
                && mutedUntil.isAfter(LocalDateTime.now())) {
            return false;
        }

        UserSetting settings = settingRepository
                .findByUserId(recipient.getId())
                .orElse(null);

        // No settings row = project defaults are enabled.
        if (settings == null) {
            return true;
        }

        if (Boolean.FALSE.equals(
                settings.getMessageNotifications())) {
            return false;
        }

        if (conversation.getType() == ConversationType.GROUP
                && Boolean.FALSE.equals(
                        settings.getGroupNotifications())) {
            return false;
        }

        return true;
    }

    private void sendToRecipient(
            Message message,
            Conversation conversation,
            User sender,
            User recipient) {

        List<Device> devices = deviceRepository
                .findByUserIdAndFcmTokenIsNotNull(
                        recipient.getId());

        for (Device device : devices) {

            String token = device.getFcmToken();

            if (token == null || token.isBlank()) {
                continue;
            }

            PushNotificationRequest request = new PushNotificationRequest(
                    token,
                    buildTitle(conversation, sender),
                    buildBody(message, sender),
                    Map.of(
                            "type", "message:new",
                            "messageId",
                            String.valueOf(message.getId()),
                            "conversationId",
                            String.valueOf(conversation.getId()),
                            "senderId",
                            String.valueOf(sender.getId())));

            PushNotificationResult result = provider.send(request);

            if (result.invalidToken()) {

                // Keep device record, clear only unusable token.
                device.setFcmToken(null);
                deviceRepository.save(device);

                log.info(
                        "Cleared invalid FCM token for device {}",
                        device.getId());
            }
        }
    }

    private String buildTitle(
            Conversation conversation,
            User sender) {

        if (conversation.getType() == ConversationType.GROUP) {

            String name = conversation.getName();

            if (name != null && !name.isBlank()) {
                return name;
            }
        }

        return sender.getName();
    }

    private String buildBody(
            Message message,
            User sender) {

        String content = message.getContent();

        if (content == null || content.isBlank()) {
            return sender.getName()
                    + " sent a message";
        }

        int maxLength = 120;

        if (content.length() <= maxLength) {
            return content;
        }

        return content.substring(0, maxLength)
                + "...";
    }
}