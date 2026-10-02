package com.rindev.chat.service;

import com.rindev.chat.entity.RefreshToken;
import com.rindev.chat.entity.User;
import com.rindev.chat.exception.UnauthorizedException;
import com.rindev.chat.repository.RefreshTokenRepository;
import com.rindev.chat.security.RefreshTokenProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final Instant now = Instant.parse("2026-10-02T00:00:00Z");
    private final RefreshTokenService service = new RefreshTokenService(repository,
            new RefreshTokenProperties(Duration.ofDays(30)), Clock.fixed(now, ZoneOffset.UTC));
    private final User user = new User();
    private RefreshToken session;
    private String raw;

    @BeforeEach
    void setup() {
        user.setId(12L);
        raw = service.issue(user).token();
        var capture = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(capture.capture());
        session = capture.getValue();
        when(repository.findLockedByTokenHash(session.getTokenHash())).thenReturn(Optional.of(session));
        clearInvocations(repository);
    }

    @Test
    void storesOnlyDigestAndSetsExpiry() {
        assertThat(raw).matches("[A-Za-z0-9_-]{43}");
        assertThat(session.getTokenHash()).matches("[a-f0-9]{64}").doesNotContain(raw);
        assertThat(session.getExpiresAt()).isEqualTo(now.plus(Duration.ofDays(30)));
        assertThat(session.getUser()).isSameAs(user);
        assertThat(service.issue(user).token()).isNotEqualTo(raw);
    }

    @Test
    void rotatesSameSessionAndRejectsOldDigest() {
        var rotated = service.rotate(raw);
        assertThat(rotated.user()).isSameAs(user);
        assertThat(rotated.token()).isNotEqualTo(raw);
        verify(repository).save(session);
        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsExpiryAtCurrentTime() {
        session.setExpiresAt(now);
        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(UnauthorizedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsRevokedToken() {
        session.setRevokedAt(now.minusSeconds(1));
        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(UnauthorizedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnknownToken() {
        when(repository.findLockedByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logoutIsOwnedAndIdempotent() {
        assertThatThrownBy(() -> service.revoke(raw, 13L)).isInstanceOf(UnauthorizedException.class);
        assertThat(session.getRevokedAt()).isNull();
        service.revoke(raw, user.getId());
        service.revoke(raw, user.getId());
        assertThat(session.getRevokedAt()).isEqualTo(now);
        verify(repository, times(1)).save(session);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "   ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa=", "éaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void malformedTokensNeverReachStorage(String token) {
        assertThatThrownBy(() -> service.rotate(token)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.revoke(token, 12L)).isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void tokenResultsRedactSecrets() {
        var issued = service.issue(user);
        assertThat(issued.toString()).doesNotContain(issued.token());
        var rotated = service.rotate(raw);
        assertThat(rotated.toString()).doesNotContain(rotated.token());
    }

    @Test
    void validatesRefreshLifetime() {
        assertThatThrownBy(() -> new RefreshTokenProperties(null)).isInstanceOf(IllegalStateException.class);
        for (Duration ttl : new Duration[] {Duration.ZERO, Duration.ofSeconds(-1), Duration.ofNanos(1), Duration.ofDays(366)}) {
            assertThatThrownBy(() -> new RefreshTokenProperties(ttl)).isInstanceOf(IllegalStateException.class);
        }
    }
}
