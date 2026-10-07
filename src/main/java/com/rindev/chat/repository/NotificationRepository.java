package com.rindev.chat.repository;

import java.time.LocalDateTime;

import com.rindev.chat.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository
                extends JpaRepository<Notification, Long> {

        Page<Notification> findByUserIdOrderByCreatedAtDesc(
                        Long userId,
                        Pageable pageable);

        Optional<Notification> findByIdAndUserId(
                        Long id,
                        Long userId);

        @Modifying(

                        clearAutomatically = true,

                        flushAutomatically = true

        )

        @Query("""

                        UPDATE Notification n

                        SET n.isRead = true,

                            n.readAt = :readAt

                        WHERE n.user.id = :userId

                          AND (n.isRead = false OR n.isRead IS NULL)

                        """)

        int markAllUnreadAsRead(

                        @Param("userId") Long userId,

                        @Param("readAt") LocalDateTime readAt);

}