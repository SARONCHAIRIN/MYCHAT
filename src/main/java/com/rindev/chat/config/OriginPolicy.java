package com.rindev.chat.config;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Shared origin parsing, with exact, non-wildcard origins required in
 * production.
 */
public final class OriginPolicy {
    private final List<String> http;
    private final List<String> websocket;

    public OriginPolicy(String http, String websocket, boolean production) {
        this.http = parse(http);
        this.websocket = parse(websocket);
        if (production) {
            this.http.forEach(OriginPolicy::validateProductionOrigin);
            this.websocket.forEach(OriginPolicy::validateProductionOrigin);
        }
    }

    public List<String> http() {
        return http;
    }

    public List<String> websocket() {
        return websocket;
    }

    private static List<String> parse(String origins) {
        return Arrays.stream(origins.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
    }

    private static void validateProductionOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            if (!origin.contains("*") && ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                    && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && uri.getPort() <= 65535) {
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // Do not echo configuration values in startup errors.
        }
        throw new IllegalArgumentException(
                "Production origins must be exact HTTP(S) origins without wildcards or paths");
    }
}
