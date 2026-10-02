package com.rindev.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One renewable session; only the SHA-256 digest of its current token is stored. */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "bigint unsigned")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamp(6)")
    private Instant expiresAt;

    @Column(name = "revoked_at", columnDefinition = "timestamp(6)")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp(6)")
    private Instant createdAt;
}
