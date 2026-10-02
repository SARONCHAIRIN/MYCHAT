package com.rindev.chat.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings supplied through deployment configuration; never print the signing secret. */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {

    @Override
    public String toString() {
        return "JwtProperties[secret=[REDACTED], issuer=" + issuer
                + ", accessTokenTtl=" + accessTokenTtl + "]";
    }
}
