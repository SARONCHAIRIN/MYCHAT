package com.rindev.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "pinned_messages", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pinned_message", columnNames = {"conversation_id", "message_id"})
}, indexes = {
        @Index(name = "fk_pinned_message", columnList = "message_id"),
        @Index(name = "fk_pinned_by", columnList = "pinned_by")
})
@Getter
@Setter
@NoArgsConstructor
public class PinnedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "bigint unsigned")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pinned_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pinned_message"))
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pinned_by", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pinned_by"))
    private User pinnedBy;

    @Generated(event = EventType.INSERT)
    @Column(name = "pinned_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime pinnedAt;
}
