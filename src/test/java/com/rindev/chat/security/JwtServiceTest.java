package com.rindev.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String ISSUER = "chat-backend-test";
    private static final SecretKey KEY = Jwts.SIG.HS256.key().build();
    private static final String SECRET = Base64.getEncoder().encodeToString(KEY.getEncoded());

    private final JwtProperties properties = new JwtProperties(SECRET, ISSUER, TTL);
    private final JwtService service = new JwtService(properties, CLOCK);

    @Test
    void issuesHs256TokensWithOnlyRequiredIdentityAndLifetimeClaims() {
        String token = service.generateAccessToken(42L);

        assertThat(service.parseUserId(token)).isEqualTo(42L);
        var parsed = Jwts.parser().verifyWith(KEY).clock(() -> Date.from(NOW))
                .build().parseSignedClaims(token);
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("HS256");
        Claims claims = parsed.getPayload();
        assertThat(claims).containsOnlyKeys("sub", "iss", "iat", "exp", "token_type");
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plus(TTL));
        assertThat(claims.get("token_type")).isEqualTo("access");
    }

    @Test
    void supportsTheExistingPositiveLongIdRange() {
        assertThat(service.parseUserId(service.generateAccessToken(Long.MAX_VALUE)))
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void alignsIssuedTimestampsWithJwtWholeSecondPrecision() {
        JwtService subsecondService = new JwtService(properties,
                Clock.fixed(NOW.plusMillis(999), ZoneOffset.UTC));

        assertThat(subsecondService.parseUserId(subsecondService.generateAccessToken(42L)))
                .isEqualTo(42L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void doesNotIssueTokensForInvalidUserIds(Long userId) {
        assertThatThrownBy(() -> service.generateAccessToken(userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not-a-token", "one.two.three", "Bearer token"})
    void rejectsMalformedTokensWithoutRetainingParserDetails(String token) {
        assertInvalid(token);
    }

    @Test
    void rejectsTamperedPayloads() {
        String token = service.generateAccessToken(42L);
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]),
                java.nio.charset.StandardCharsets.UTF_8).replace("\"42\"", "\"99\"");
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + "." + parts[2];

        assertInvalid(tampered);
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        assertInvalid(claims().signWith(Jwts.SIG.HS256.key().build(), Jwts.SIG.HS256).compact());
    }

    @Test
    void rejectsUnsignedTokens() {
        assertInvalid(claims().compact());
    }

    @Test
    void rejectsOtherSigningAlgorithmsEvenWithMatchingKeyMaterial() {
        SecretKey strongerKey = Jwts.SIG.HS512.key().build();
        String encoded = Base64.getEncoder().encodeToString(strongerKey.getEncoded());
        JwtService sameKeyService = new JwtService(new JwtProperties(encoded, ISSUER, TTL), CLOCK);

        assertThat(sameKeyService.parseUserId(sameKeyService.generateAccessToken(42L)))
                .isEqualTo(42L);
        assertThatThrownBy(() -> sameKeyService.parseUserId(
                claims().signWith(strongerKey, Jwts.SIG.HS512).compact()))
                .isInstanceOf(JwtException.class).hasMessage("Invalid access token").hasNoCause();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sub", "iss", "iat", "exp", "token_type"})
    void rejectsMissingRequiredClaims(String claim) {
        assertInvalid(signed(claims().claim(claim, null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "+1", "01", "1.0", "1e1", "user", " 42", "42 ",
            "9223372036854775808"})
    void rejectsNoncanonicalOrOutOfRangeSubjects(String subject) {
        assertInvalid(signed(claims().subject(subject)));
    }

    @Test
    void rejectsWrongIssuerAndRefreshTokens() {
        assertInvalid(signed(claims().issuer("other-service")));
        assertInvalid(signed(claims().claim("token_type", "refresh")));
    }

    @Test
    void rejectsExpiredTokensIncludingTheExactExpiryBoundary() {
        String token = service.generateAccessToken(42L);
        JwtService atExpiry = new JwtService(properties, Clock.fixed(NOW.plus(TTL), ZoneOffset.UTC));
        JwtService afterExpiry = new JwtService(properties,
                Clock.fixed(NOW.plus(TTL).plusSeconds(1), ZoneOffset.UTC));

        assertThatThrownBy(() -> atExpiry.parseUserId(token))
                .isInstanceOf(JwtException.class).hasMessage("Invalid access token").hasNoCause();
        assertThatThrownBy(() -> afterExpiry.parseUserId(token))
                .isInstanceOf(JwtException.class).hasMessage("Invalid access token").hasNoCause();
    }

    @Test
    void rejectsFutureIssueAndNotBeforeTimes() {
        assertInvalid(signed(claims().issuedAt(Date.from(NOW.plusSeconds(1)))));
        assertInvalid(signed(claims().notBefore(Date.from(NOW.plusSeconds(1)))));
    }

    @Test
    void rejectsExcessiveOrInvalidTokenLifetimes() {
        assertInvalid(signed(claims().expiration(Date.from(NOW.plus(TTL).plusSeconds(1)))));
        assertInvalid(signed(claims().expiration(Date.from(NOW))));
        assertInvalid(signed(claims().expiration(Date.from(NOW.minusSeconds(1)))));
    }

    @Test
    void rejectsSignedContentThatIsNotAClaimsObject() {
        assertInvalid(Jwts.builder().content("private-internal-content")
                .signWith(KEY, Jwts.SIG.HS256).compact());
    }

    @Test
    void rejectsMissingConfiguration() {
        assertThatThrownBy(() -> new JwtService(null, CLOCK))
                .isInstanceOf(IllegalStateException.class).hasNoCause();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not-valid-base64!", "c2hvcnQ="})
    void rejectsMissingMalformedAndWeakSecrets(String secret) {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(secret, ISSUER, TTL), CLOCK))
                .isInstanceOf(IllegalStateException.class).hasNoCause()
                .satisfies(exception -> {
                    if (secret != null && !secret.isBlank()) {
                        assertThat(exception.getMessage()).doesNotContain(secret);
                    }
                });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " issuer", "issuer "})
    void rejectsMissingOrAmbiguousIssuer(String issuer) {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(SECRET, issuer, TTL), CLOCK))
                .isInstanceOf(IllegalStateException.class).hasNoCause()
                .hasMessageNotContaining(SECRET);
    }

    @ParameterizedTest
    @MethodSource("invalidTtls")
    void rejectsInvalidTokenTtls(Duration ttl) {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(SECRET, ISSUER, ttl), CLOCK))
                .isInstanceOf(IllegalStateException.class).hasNoCause()
                .hasMessageNotContaining(SECRET);
    }

    @Test
    void redactsTheSigningSecretFromConfigurationToString() {
        assertThat(properties.toString()).contains("secret=[REDACTED]").doesNotContain(SECRET);
    }

    private static Stream<Duration> invalidTtls() {
        return Stream.of(null, Duration.ZERO, Duration.ofSeconds(-1), Duration.ofMillis(500),
                Duration.ofMillis(1500), Duration.ofSeconds(Long.MAX_VALUE));
    }

    private static JwtBuilder claims() {
        return Jwts.builder().subject("42").issuer(ISSUER).issuedAt(Date.from(NOW))
                .expiration(Date.from(NOW.plus(TTL))).claim("token_type", "access");
    }

    private static String signed(JwtBuilder builder) {
        return builder.signWith(KEY, Jwts.SIG.HS256).compact();
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> service.parseUserId(token))
                .isInstanceOf(JwtException.class).hasMessage("Invalid access token").hasNoCause();
    }
}
