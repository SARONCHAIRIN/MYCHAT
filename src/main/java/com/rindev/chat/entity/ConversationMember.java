package com.rindev.chat.entity;

import com.rindev.chat.enums.MemberRole;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "conversation_members",
        uniqueConstraints = @UniqueConstraint(name = "uq_conversation_user",
                columnNames = {"conversation_id", "user_id"}),
        indexes = @Index(name = "idx_member_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class ConversationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "bigint unsigned")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_member_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private MemberRole role = MemberRole.MEMBER;

    @Generated(event = EventType.INSERT)
    @Column(name = "joined_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
