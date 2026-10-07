package com.rindev.chat.security;

import com.rindev.chat.entity.User;
import com.rindev.chat.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(SecurityIntegrationTest.SecurityTestConfiguration.class)
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "app.origins.http=https://chat.example.com",
        "app.origins.websocket=https://chat.example.com",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
class ProductionSecurityIntegrationTest {
    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository users;
    @Autowired
    JwtService jwt;
    private MockMvc mvc;

    @DynamicPropertySource
    static void signingKey(DynamicPropertyRegistry properties) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String key = Base64.getEncoder().encodeToString(bytes);
        properties.add("security.jwt.secret", () -> key);
        properties.add("security.jwt.issuer", () -> "chat-backend");
        properties.add("security.jwt.access-token-ttl", () -> "15m");
    }

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        User user = new User();
        user.setId(42L);
        user.setUsername("production-check");
        user.setPasswordHash(UUID.randomUUID().toString());
        when(users.findById(42L)).thenReturn(Optional.of(user));
    }

    @Test
    void trustedBrowserCanPreflightBearerApiRequests() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://chat.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://chat.example.com"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void untrustedBrowserCannotPreflightOrCallTheApi() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(get("/api/v1/test/me")
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowedOriginStillRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/test/me").header(HttpHeaders.ORIGIN, "https://chat.example.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://chat.example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config",
            "/swagger-ui.html", "/swagger-ui/index.html", "/actuator", "/actuator/env",
            "/actuator/configprops", "/actuator/heapdump", "/actuator/health/db" })
    void sensitiveOrDisabledRoutesAreDeniedEvenWithAValidUserToken(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer())).andExpect(status().isForbidden());
    }

    private String bearer() {
        return "Bearer " + jwt.generateAccessToken(42L);
    }
}
