package com.rindev.chat.websocket;

import com.rindev.chat.entity.User;
import com.rindev.chat.enums.UserStatus;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.repository.UserSettingRepository;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rindev.chat.entity.UserSetting;
import com.rindev.chat.enums.PrivacyLevel;

@Service
public class PresenceService {

    private final UserRepository userRepository;
    private final ConversationMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserSettingRepository settingRepository;

    private final ConcurrentHashMap<Long, Set<String>> sessions = new ConcurrentHashMap<>();

    public PresenceService(
            UserRepository userRepository,
            ConversationMemberRepository memberRepository,
            SimpMessagingTemplate messagingTemplate,
            UserSettingRepository settingRepository) {

        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
        this.settingRepository = settingRepository;
    }

    @Transactional
    public boolean connected(
            Long userId,
            String sessionId) {

        Set<String> userSessions = sessions.computeIfAbsent(
                userId,
                ignored -> ConcurrentHashMap.newKeySet());

        boolean wasOffline = userSessions.isEmpty();

        userSessions.add(sessionId);

        if (wasOffline) {
            User user = requireUser(userId);
            user.setStatus(UserStatus.ONLINE);
            userRepository.save(user);

            return true;
        }

        return false;
    }

    @Transactional
    public PresenceEventPayload disconnected(
            Long userId,
            String sessionId) {

        Set<String> userSessions = sessions.get(userId);

        if (userSessions == null) {
            return null;
        }

        userSessions.remove(sessionId);

        // User still has another active WebSocket connection.
        if (!userSessions.isEmpty()) {
            return null;
        }

        sessions.remove(userId, userSessions);

        User user = requireUser(userId);

        LocalDateTime now = LocalDateTime.now();

        user.setStatus(UserStatus.OFFLINE);
        user.setLastSeenAt(now);

        userRepository.save(user);

        return new PresenceEventPayload(
                user.getId(),
                user.getUsername(),
                false,
                now);
    }

    public void broadcastPresence(
            Long userId,
            WebSocketEventType type,
            PresenceEventPayload payload) {

        PrivacyLevel privacy = settingRepository.findByUserId(userId)
                .map(UserSetting::getLastSeenPrivacy)
                .orElse(PrivacyLevel.EVERYONE);

        // User does not allow anyone to see presence/last seen
        if (privacy == PrivacyLevel.NOBODY) {
            return;
        }

        // Current project does not yet have a contact/friend relationship
        // Fail closed to avoid leaking presence information.
        if (privacy == PrivacyLevel.CONTACTS) {
            return;
        }

        // EVERYONE:
        // Broadcast only through conversations where the user
        // is currently an active member.
        memberRepository
                .findByUserIdAndLeftAtIsNull(userId)
                .forEach(member -> {

                    Long conversationId = member.getConversation().getId();

                    messagingTemplate.convertAndSend(
                            "/topic/conversations/" + conversationId,
                            WebSocketEvent.of(
                                    type,
                                    conversationId,
                                    payload));
                });
    }

    public boolean isOnline(Long userId) {
        Set<String> userSessions = sessions.get(userId);

        return userSessions != null
                && !userSessions.isEmpty();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found"));
    }
}