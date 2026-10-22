package com.rindev.chat.service.impl;

import com.rindev.chat.dto.response.AudioStreamResponse;
import com.rindev.chat.dto.response.MessageResponse;
import com.rindev.chat.entity.Conversation;
import com.rindev.chat.entity.ConversationMember;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.MessageAttachment;
import com.rindev.chat.entity.User;
import com.rindev.chat.enums.AttachmentType;
import com.rindev.chat.enums.ConversationType;
import com.rindev.chat.enums.MessageType;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.RangeNotSatisfiableException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.mapper.MessageMapper;
import com.rindev.chat.notification.MessageNotificationPublisher;
import com.rindev.chat.repository.BlockedUserRepository;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.ConversationRepository;
import com.rindev.chat.repository.MessageAttachmentRepository;
import com.rindev.chat.repository.MessageRepository;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.service.VoiceMessageService;
import com.rindev.chat.storage.FileStorageService;
import com.rindev.chat.storage.StoredFile;
import com.rindev.chat.storage.StoredFileNames;
import com.rindev.chat.storage.StoredObject;
import com.rindev.chat.websocket.ConversationEventPublisher;
import com.rindev.chat.websocket.WebSocketEventType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VoiceMessageServiceImpl implements VoiceMessageService {

    private static final Logger log = LoggerFactory.getLogger(VoiceMessageServiceImpl.class);

    private static final long MAX_AUDIO_SIZE = 15L * 1024 * 1024; // 15 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/m4a",
            "audio/x-m4a",
            "audio/mp4",
            "audio/aac",
            "audio/x-aac",
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/wave",
            "audio/x-wav",
            "audio/webm",
            "audio/ogg",
            "audio/3gpp",
            "audio/amr");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".m4a",
            ".mp3",
            ".wav",
            ".aac",
            ".webm",
            ".ogg",
            ".mp4",
            ".3gp",
            ".amr");

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final FileStorageService storageService;
    private final MessageMapper messageMapper;
    private final ConversationEventPublisher eventPublisher;
    private final MessageNotificationPublisher notificationPublisher;

    public VoiceMessageServiceImpl(
            MessageRepository messageRepository,
            MessageAttachmentRepository attachmentRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository,
            BlockedUserRepository blockedUserRepository,
            FileStorageService storageService,
            MessageMapper messageMapper,
            ConversationEventPublisher eventPublisher,
            MessageNotificationPublisher notificationPublisher) {
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.storageService = storageService;
        this.messageMapper = messageMapper;
        this.eventPublisher = eventPublisher;
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    @Transactional
    public MessageResponse sendVoiceMessage(
            Long authenticatedUserId,
            Long conversationId,
            MultipartFile audio,
            Integer durationSeconds) {

        if (conversationId == null) {
            throw new BadRequestException("Conversation ID is required");
        }

        validateAudioFile(audio);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found with id: " + conversationId));

        if (!memberRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(
                conversationId, authenticatedUserId)) {
            throw new ForbiddenException("You are not a member of this conversation");
        }

        enforceBlockRules(conversation, authenticatedUserId);

        User sender = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + authenticatedUserId));

        StoredFile stored = storageService.store(audio);

        try {
            Message message = new Message();
            message.setConversation(conversation);
            message.setSender(sender);
            message.setType(MessageType.AUDIO);
            message.setContent(null);

            Message savedMessage = messageRepository.save(message);

            MessageAttachment attachment = new MessageAttachment();
            attachment.setMessage(savedMessage);
            attachment.setType(AttachmentType.AUDIO);
            attachment.setFileName(stored.originalFileName() != null ? stored.originalFileName() : stored.storedFileName());
            attachment.setFileUrl(stored.fileUrl());
            attachment.setMimeType(stored.contentType());
            attachment.setFileSize(stored.size());
            attachment.setDurationSeconds(durationSeconds != null && durationSeconds > 0
                    ? durationSeconds.longValue()
                    : null);

            attachmentRepository.save(attachment);

            conversation.setLastMessageAt(LocalDateTime.now());
            conversationRepository.save(conversation);

            MessageResponse response = messageMapper.toResponse(savedMessage);

            // Broadcast real-time event AFTER COMMIT
            eventPublisher.publish(
                    WebSocketEventType.MESSAGE_NEW,
                    conversationId,
                    response);

            // Push notification
            notificationPublisher.publish(
                    savedMessage.getId(),
                    conversationId,
                    authenticatedUserId);

            return response;
        } catch (Exception e) {
            // Clean up uploaded file if database persistence fails
            try {
                storageService.delete(stored.storedFileName());
            } catch (Exception cleanupEx) {
                log.warn("Failed to delete stored file during rollback: {}", stored.storedFileName(), cleanupEx);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AudioStreamResponse streamVoiceMessage(
            Long authenticatedUserId,
            Long messageId,
            String rangeHeader) {

        if (messageId == null) {
            throw new BadRequestException("Message ID is required");
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Voice message not found with id: " + messageId));

        if (message.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Voice message not found or has been deleted");
        }

        if (message.getType() != MessageType.AUDIO) {
            throw new ResourceNotFoundException("Message is not an audio message");
        }

        Long conversationId = message.getConversation().getId();
        if (!memberRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(
                conversationId, authenticatedUserId)) {
            throw new ForbiddenException("You cannot access this conversation");
        }

        List<MessageAttachment> attachments = attachmentRepository.findByMessageId(messageId);
        MessageAttachment attachment = attachments.stream()
                .filter(a -> a.getType() == AttachmentType.AUDIO)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Audio attachment not found for message " + messageId));

        String fileUrl = attachment.getFileUrl();
        String filename = fileUrl.startsWith("/uploads/") ? fileUrl.substring("/uploads/".length()) : fileUrl;
        StoredFileNames.validate(filename);

        long totalLength = storageService.getFileSize(filename);
        String contentType = attachment.getMimeType() != null && !attachment.getMimeType().isBlank()
                ? attachment.getMimeType()
                : "audio/mp4";

        if (rangeHeader != null && !rangeHeader.isBlank()) {
            List<HttpRange> ranges;
            try {
                ranges = HttpRange.parseRanges(rangeHeader);
            } catch (IllegalArgumentException e) {
                throw new RangeNotSatisfiableException(totalLength);
            }

            if (ranges.isEmpty()) {
                throw new RangeNotSatisfiableException(totalLength);
            }

            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(totalLength);
            long end = range.getRangeEnd(totalLength);

            if (start < 0 || end >= totalLength || start > end) {
                throw new RangeNotSatisfiableException(totalLength);
            }

            long length = end - start + 1;
            StoredObject chunk = storageService.readRange(filename, start, length);
            return new AudioStreamResponse(chunk.bytes(), contentType, totalLength, start, end, true);
        }

        StoredObject full = storageService.read(filename);
        return new AudioStreamResponse(full.bytes(), contentType, totalLength, 0L, totalLength - 1, false);
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Audio file is required");
        }

        if (file.getSize() > MAX_AUDIO_SIZE) {
            throw new BadRequestException("Audio file size exceeds maximum limit of 15 MB");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = StoredFileNames.extension(originalFilename).toLowerCase(Locale.ROOT);

        boolean validMime = contentType != null && ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        boolean validExtension = ALLOWED_EXTENSIONS.contains(extension);

        if (!validMime && !validExtension) {
            throw new BadRequestException("Unsupported audio format: " + (contentType != null ? contentType : extension));
        }
    }

    private void enforceBlockRules(Conversation conversation, Long senderId) {
        if (conversation.getType() != ConversationType.DIRECT) {
            return;
        }

        List<ConversationMember> members = memberRepository
                .findByConversationIdAndLeftAtIsNull(conversation.getId());

        for (ConversationMember member : members) {
            Long otherUserId = member.getUser().getId();
            if (otherUserId.equals(senderId)) {
                continue;
            }

            boolean blocked = blockedUserRepository.existsByBlockerIdAndBlockedId(senderId, otherUserId)
                    || blockedUserRepository.existsByBlockerIdAndBlockedId(otherUserId, senderId);

            if (blocked) {
                throw new ForbiddenException("Messaging is not allowed between these users");
            }
        }
    }
}
