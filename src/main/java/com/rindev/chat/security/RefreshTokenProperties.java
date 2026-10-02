package com.rindev.chat.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.refresh-token")
public record RefreshTokenProperties(Duration ttl) {
    public RefreshTokenProperties {
        if (ttl == null || ttl.isNegative() || ttl.isZero() || ttl.getNano() != 0
                || ttl.compareTo(Duration.ofDays(365)) > 0) {
            throw new IllegalStateException("Refresh-token TTL must be whole seconds between 1 second and 365 days");
        }
    }
}
