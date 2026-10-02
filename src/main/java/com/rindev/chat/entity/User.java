package com.rindev.chat.entity;

import com.rindev.chat.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "username", columnNames = "username"),
        @UniqueConstraint(name = "email", columnNames = "email"),
        @UniqueConstraint(name = "phone", columnNames = "phone")
})
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigint unsigned")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "bio", length = 255)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enum('online','offline')")
    private UserStatus status = UserStatus.OFFLINE;

    @Column(name = "last_seen_at", columnDefinition = "datetime")
    private LocalDateTime lastSeenAt;

    @Column(name = "email_verified_at", columnDefinition = "datetime")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "phone_verified_at", columnDefinition = "datetime")
    private LocalDateTime phoneVerifiedAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", columnDefinition = "timestamp", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
