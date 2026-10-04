package com.rindev.chat.service.impl;

import com.rindev.chat.dto.response.NotificationPageResponse;
import com.rindev.chat.dto.response.NotificationResponse;
import com.rindev.chat.entity.Notification;
import com.rindev.chat.exception.BadRequestException;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.repository.NotificationRepository;
import com.rindev.chat.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl
        implements NotificationService {

    private final NotificationRepository repository;

    public NotificationServiceImpl(
            NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationPageResponse getNotifications(
            Long userId,
            int page,
            int size) {

        if (page < 0) {
            throw new BadRequestException(
                    "Page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new BadRequestException(
                    "Size must be between 1 and 100");
        }

        Page<Notification> result = repository
                .findByUserIdOrderByCreatedAtDesc(
                        userId,
                        PageRequest.of(page, size));

        List<NotificationResponse> notifications = result.getContent()
                .stream()
                .map(this::map)
                .toList();

        return new NotificationPageResponse(
                notifications,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    @Transactional
    public NotificationResponse markRead(
            Long userId,
            Long notificationId) {

        Notification notification = requireOwned(
                userId,
                notificationId);

        if (!Boolean.TRUE.equals(
                notification.getIsRead())) {

            notification.setIsRead(true);
            notification.setReadAt(
                    LocalDateTime.now());
        }

        return map(
                repository.save(notification));
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {

        Page<Notification> page;

        int pageNumber = 0;

        do {
            page = repository
                    .findByUserIdOrderByCreatedAtDesc(
                            userId,
                            PageRequest.of(pageNumber, 100));

            for (Notification notification : page.getContent()) {

                if (!Boolean.TRUE.equals(
                        notification.getIsRead())) {

                    notification.setIsRead(true);

                    if (notification.getReadAt() == null) {
                        notification.setReadAt(
                                LocalDateTime.now());
                    }
                }
            }

            repository.saveAll(page.getContent());

            pageNumber++;

        } while (pageNumber < page.getTotalPages());
    }

    @Override
    @Transactional
    public void delete(
            Long userId,
            Long notificationId) {

        Notification notification = requireOwned(
                userId,
                notificationId);

        repository.delete(notification);
    }

    private Notification requireOwned(
            Long userId,
            Long notificationId) {

        return repository
                .findByIdAndUserId(
                        notificationId,
                        userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found"));
    }

    private NotificationResponse map(
            Notification n) {

        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),

                n.getConversation() == null
                        ? null
                        : n.getConversation().getId(),

                n.getMessage() == null
                        ? null
                        : n.getMessage().getId(),

                Boolean.TRUE.equals(n.getIsRead()),
                n.getReadAt(),
                n.getCreatedAt());
    }
}