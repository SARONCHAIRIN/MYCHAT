package com.rindev.chat.entity;

import com.rindev.chat.enums.MessageType;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_conversation", columnList = "conversation_id,created_at"),
        @Index(name = "idx_message_sender", columnList = "sender_id"),
        @Index(name = "fk_message_reply", columnList = "reply_to_id"),
        @Index(name = "fk_message_forward", columnList = "forwarded_from_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigint unsigned")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, foreignKey = @ForeignKey(name = "fk_message_sender"))
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "enum('text','image','video','audio','file','location','system')")
    private MessageType type = MessageType.TEXT;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id", foreignKey = @ForeignKey(name = "fk_message_reply"))
    private Message replyTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_from_id", foreignKey = @ForeignKey(name = "fk_message_forward"))
    private Message forwardedFrom;

    @Column(name = "edited_at", columnDefinition = "datetime")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at", columnDefinition = "datetime")
    private LocalDateTime deletedAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
