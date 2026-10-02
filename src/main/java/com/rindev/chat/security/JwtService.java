package com.rindev.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/** Issues and verifies access tokens. User existence and status are checked separately. */
public final class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(JwtProperties properties, Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock is required");
        if (properties == null) {
            throw new IllegalStateException("JWT configuration is required");
        }
        this.signingKey = signingKey(properties.secret());
        if (properties.issuer() == null || properties.issuer().isBlank()
                || !properties.issuer().equals(properties.issuer().strip())) {
            throw new IllegalStateException("JWT issuer must be nonblank without surrounding whitespace");
        }
        this.issuer = properties.issuer();
        this.accessTokenTtl = validTtl(properties.accessTokenTtl(), clock);
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .sig().clear().add(Jwts.SIG.HS256).and()
                .requireIssuer(issuer)
                .require("token_type", "access")
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public String generateAccessToken(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("A positive user ID is required");
        }
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(accessTokenTtl)))
                .claim("token_type", "access")
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Long parseUserId(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            Date issuedAt = claims.getIssuedAt();
            Date expiresAt = claims.getExpiration();
            Instant now = clock.instant();
            if (issuedAt == null || expiresAt == null
                    || issuedAt.toInstant().isAfter(now)
                    || !expiresAt.toInstant().isAfter(now)
                    || !expiresAt.after(issuedAt)
                    || Duration.between(issuedAt.toInstant(), expiresAt.toInstant())
                            .compareTo(accessTokenTtl) > 0) {
                throw new JwtException("Invalid access token");
            }
            String subject = claims.getSubject();
            if (subject == null) {
                throw new JwtException("Invalid access token");
            }
            Long userId = Long.valueOf(subject);
            if (userId <= 0 || !userId.toString().equals(subject)) {
                throw new JwtException("Invalid access token");
            }
            return userId;
        } catch (JwtException | IllegalArgumentException exception) {
            // Library exceptions may contain parsed claims. Do not retain their message or cause.
            throw new JwtException("Invalid access token");
        }
    }

    private static SecretKey signingKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is required and must contain a Base64 signing key");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET must contain a valid Base64 signing key");
        }
        try {
            if (decoded.length < 32) {
                throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
            }
            return new SecretKeySpec(decoded, "HmacSHA256");
        } finally {
            Arrays.fill(decoded, (byte) 0);
        }
    }

    private static Duration validTtl(Duration ttl, Clock clock) {
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.getNano() != 0) {
            throw new IllegalStateException("JWT access-token TTL must be a positive whole number of seconds");
        }
        try {
            Date.from(clock.instant().plus(ttl));
        } catch (DateTimeException | IllegalArgumentException | ArithmeticException exception) {
            throw new IllegalStateException("JWT access-token TTL is outside the supported range");
        }
        return ttl;
    }
}
