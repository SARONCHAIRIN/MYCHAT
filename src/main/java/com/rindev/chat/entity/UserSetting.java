package com.rindev.chat.entity;

import com.rindev.chat.enums.PrivacyLevel;
import com.rindev.chat.enums.Theme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "user_settings",
        uniqueConstraints = @UniqueConstraint(name = "user_id", columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "bigint unsigned")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_settings_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme")
    private Theme theme = Theme.SYSTEM;

    @Column(name = "message_notifications")
    private Boolean messageNotifications = true;

    @Column(name = "group_notifications")
    private Boolean groupNotifications = true;

    @Column(name = "reaction_notifications")
    private Boolean reactionNotifications = true;

    @Column(name = "read_receipts")
    private Boolean readReceipts = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_seen_privacy")
    private PrivacyLevel lastSeenPrivacy = PrivacyLevel.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_photo_privacy")
    private PrivacyLevel profilePhotoPrivacy = PrivacyLevel.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_add_privacy")
    private PrivacyLevel groupAddPrivacy = PrivacyLevel.EVERYONE;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
