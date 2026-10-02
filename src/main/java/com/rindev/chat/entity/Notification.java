package com.rindev.chat.entity;

import com.rindev.chat.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_unread", columnList = "user_id,is_read"),
        @Index(name = "fk_notification_conversation", columnList = "conversation_id"),
        @Index(name = "fk_notification_message", columnList = "message_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "bigint unsigned")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false,
            columnDefinition = "enum('message','reaction','mention','group_invite','security')")
    private NotificationType type;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", foreignKey = @ForeignKey(name = "fk_notification_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", foreignKey = @ForeignKey(name = "fk_notification_message"))
    private Message message;

    @Column(name = "is_read", columnDefinition = "tinyint(1)")
    private Boolean isRead = false;

    @Column(name = "read_at", columnDefinition = "datetime")
    private LocalDateTime readAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
