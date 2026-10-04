package com.rindev.chat.service.impl;

import com.rindev.chat.dto.response.UploadResponse;
import com.rindev.chat.entity.Message;
import com.rindev.chat.entity.MessageAttachment;
import com.rindev.chat.enums.AttachmentType;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ForbiddenException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.ConversationMemberRepository;
import com.rindev.chat.repository.MessageAttachmentRepository;
import com.rindev.chat.repository.MessageRepository;
import com.rindev.chat.service.UploadService;
import com.rindev.chat.storage.FileStorageService;
import com.rindev.chat.storage.StoredFile;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadServiceImpl implements UploadService {

        private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;

        private final MessageRepository messageRepository;
        private final MessageAttachmentRepository attachmentRepository;
        private final ConversationMemberRepository memberRepository;
        private final FileStorageService storageService;

        public UploadServiceImpl(
                        MessageRepository messageRepository,
                        MessageAttachmentRepository attachmentRepository,
                        ConversationMemberRepository memberRepository,
                        FileStorageService storageService) {

                this.messageRepository = messageRepository;
                this.attachmentRepository = attachmentRepository;
                this.memberRepository = memberRepository;
                this.storageService = storageService;
        }

        @Override
        @Transactional
        public UploadResponse upload(
                        Long authenticatedUserId,
                        Long messageId,
                        MultipartFile file) {

                Message message = messageRepository.findById(messageId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Message not found"));

                Long conversationId = message.getConversation().getId();

                boolean member = memberRepository
                                .existsByConversationIdAndUserIdAndLeftAtIsNull(
                                                conversationId,
                                                authenticatedUserId);

                if (!member) {
                        throw new ForbiddenException(
                                        "You cannot access this conversation");
                }

                if (!message.getSender()
                                .getId()
                                .equals(authenticatedUserId)) {

                        throw new ForbiddenException(
                                        "You can only attach files to your own message");
                }

                if (message.getDeletedAt() != null) {
                        throw new BadRequestException(
                                        "Cannot attach files to a deleted message");
                }

                validateFile(file);

                AttachmentType attachmentType = resolveType(file.getContentType());

                StoredFile stored = storageService.store(file);

                MessageAttachment attachment = new MessageAttachment();

                attachment.setMessage(message);
                attachment.setType(attachmentType);
                attachment.setFileName(
                                stored.originalFileName());
                attachment.setFileUrl(
                                stored.fileUrl());
                attachment.setMimeType(
                                stored.contentType());
                attachment.setFileSize(
                                stored.size());

                MessageAttachment saved = attachmentRepository.save(attachment);

                return new UploadResponse(
                                saved.getId(),
                                messageId,
                                saved.getType(),
                                saved.getFileName(),
                                saved.getFileUrl(),
                                saved.getMimeType(),
                                saved.getFileSize());
        }

        private void validateFile(MultipartFile file) {

                if (file == null || file.isEmpty()) {
                        throw new BadRequestException(
                                        "File is required");
                }

                if (file.getSize() > MAX_FILE_SIZE) {
                        throw new BadRequestException(
                                        "File size exceeds 25 MB");
                }

                resolveType(file.getContentType());
        }

        private AttachmentType resolveType(
                        String contentType) {

                if (contentType == null) {
                        throw new BadRequestException(
                                        "File content type is required");
                }

                String value = contentType.toLowerCase(Locale.ROOT);

                if (value.startsWith("image/")) {
                        return AttachmentType.IMAGE;
                }

                if (value.startsWith("video/")) {
                        return AttachmentType.VIDEO;
                }

                if (value.startsWith("audio/")) {
                        return AttachmentType.AUDIO;
                }

                if (value.equals("application/pdf")
                                || value.equals("application/zip")
                                || value.equals("text/plain")
                                || value.contains("document")
                                || value.contains("spreadsheet")) {

                        return AttachmentType.FILE;
                }

                throw new BadRequestException(
                                "Unsupported file type: " + contentType);
        }
}