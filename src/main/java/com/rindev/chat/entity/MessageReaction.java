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
@Table(name = "message_reactions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_message_reaction", columnNames = {"message_id", "user_id", "emoji"})
}, indexes = {
        @Index(name = "fk_reaction_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "bigint unsigned")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reaction_message"))
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reaction_user"))
    private User user;

    @Column(name = "emoji", nullable = false, length = 20)
    private String emoji;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
