package com.rindev.chat.service;

import com.rindev.chat.entity.RefreshToken;
import com.rindev.chat.entity.User;
import com.rindev.chat.exception.UnauthorizedException;
import com.rindev.chat.repository.RefreshTokenRepository;
import com.rindev.chat.security.RefreshTokenProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @org.springframework.beans.factory.annotation.Autowired
    public RefreshTokenService(RefreshTokenRepository repository, RefreshTokenProperties properties) {
        this(repository, properties, Clock.systemUTC());
    }

    RefreshTokenService(RefreshTokenRepository repository, RefreshTokenProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        String raw = newToken();
        var session = new RefreshToken();
        session.setUser(user);
        session.setTokenHash(hash(raw));
        session.setCreatedAt(clock.instant());
        session.setExpiresAt(clock.instant().plus(properties.ttl()));
        repository.save(session);
        return new IssuedRefreshToken(raw, session.getExpiresAt());
    }

    @Transactional
    public RotatedRefreshToken rotate(String raw) {
        String digest = hashValidated(raw);
        var session = repository.findLockedByTokenHash(digest).orElseThrow(RefreshTokenService::invalid);
        if (!digest.equals(session.getTokenHash()) || session.getRevokedAt() != null
                || !session.getExpiresAt().isAfter(clock.instant())) {
            throw invalid();
        }
        String replacement = newToken();
        session.setTokenHash(hash(replacement));
        session.setExpiresAt(clock.instant().plus(properties.ttl()));
        repository.save(session);
        return new RotatedRefreshToken(session.getUser(), replacement, session.getExpiresAt());
    }

    @Transactional
    public void revoke(String raw, Long authenticatedUserId) {
        String digest = hashValidated(raw);
        var session = repository.findLockedByTokenHash(digest).orElseThrow(RefreshTokenService::invalid);
        if (!digest.equals(session.getTokenHash()) || !session.getUser().getId().equals(authenticatedUserId)) {
            throw invalid();
        }
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(clock.instant());
            repository.save(session);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashValidated(String raw) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) {
            throw invalid();
        }
        return hash(raw);
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable");
        }
    }

    private static UnauthorizedException invalid() {
        return new UnauthorizedException("Invalid refresh token");
    }

    public record IssuedRefreshToken(String token, Instant expiresAt) {
        @Override public String toString() { return "IssuedRefreshToken[token=[REDACTED]]"; }
    }

    public record RotatedRefreshToken(User user, String token, Instant expiresAt) {
        @Override public String toString() { return "RotatedRefreshToken[token=[REDACTED]]"; }
    }
}
