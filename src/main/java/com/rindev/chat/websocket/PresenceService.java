package com.rindev.chat.websocket;

import com.rindev.chat.entity.User;
import com.rindev.chat.enums.UserStatus;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresenceService {

    private final UserRepository userRepository;
    private final ConversationMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<Long, Set<String>> sessions = new ConcurrentHashMap<>();

    public PresenceService(
            UserRepository userRepository,
            ConversationMemberRepository memberRepository,
            SimpMessagingTemplate messagingTemplate) {

        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.messagingTemplate = messagingTemplate;
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