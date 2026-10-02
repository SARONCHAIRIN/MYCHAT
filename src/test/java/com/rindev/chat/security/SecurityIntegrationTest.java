package com.rindev.chat.security;

import com.rindev.chat.entity.User;
import com.rindev.chat.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.DispatcherType;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the production filter chain with real JWTs and no database or
 * business endpoints.
 */
@SpringJUnitWebConfig(SecurityIntegrationTest.SecurityTestConfiguration.class)
class SecurityIntegrationTest {

    private static final Long USER_ID = 42L;
    private static final String USERNAME = "database-user";
    private static final String ISSUER = "chat-backend";
    private static final SecretKey SIGNING_KEY = Jwts.SIG.HS256.key().build();
    private static final String SECRET = Base64.getEncoder().encodeToString(SIGNING_KEY.getEncoded());
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder(12).encode(PASSWORD);

    private final WebApplicationContext context;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;

    private MockMvc mvc;

    @Autowired
    SecurityIntegrationTest(WebApplicationContext context, UserRepository userRepository,
            JwtService jwtService, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
        this.context = context;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.objectMapper = objectMapper;
    }

    @DynamicPropertySource
    static void jwtConfiguration(DynamicPropertyRegistry properties) {
        properties.add("security.jwt.secret", () -> SECRET);
        properties.add("security.jwt.issuer", () -> ISSUER);
        properties.add("security.jwt.access-token-ttl", () -> "15m");
    }

    @BeforeEach
    void setUp() {
        reset(userRepository);
        var user = new User();
        user.setId(USER_ID);
        user.setUsername(USERNAME);
        user.setPasswordHash(PASSWORD_HASH);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void missingTokenReturnsJsonWithoutRedirectOrSession() throws Exception {
        assertUnauthorized(get("/api/v1/test/me"))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
        verifyNoInteractions(userRepository);
    }

    @Test
    void validTokenResolvesIdentityFromDatabaseAndErasesCredentials() throws Exception {
        var result = mvc.perform(get("/api/v1/test/me")
                .queryParam("userId", "999")
                .queryParam("username", "forged-user")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        verify(userRepository).findById(USER_ID);
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void bearerAuthenticatedPostDoesNotRequireCsrfToken() throws Exception {
        mvc.perform(post("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID));
    }

    @Test
    void authenticationDoesNotCarryOverToNextRequest() throws Exception {
        mvc.perform(get("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());

        assertUnauthorized(get("/api/v1/test/me"));
        verify(userRepository).findById(USER_ID);
    }

    @Test
    void deletedOrUnknownUserCannotAuthenticate() throws Exception {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertUnauthorized(get("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, bearer()));
    }

    @Test
    void bearerSchemeIsCaseInsensitive() throws Exception {
        mvc.perform(get("/api/v1/test/me")
                .header(HttpHeaders.AUTHORIZATION, "bEaReR " + jwtService.generateAccessToken(USER_ID)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "Bearer", "Bearer ", "Bearer not-a-jwt", "Basic invalid", "Token invalid" })
    void malformedCredentialsCannotAuthenticate(String authorization) throws Exception {
        assertUnauthorized(get("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, authorization));
        verifyNoInteractions(userRepository);
    }

    @Test
    void tokenInQueryStringCannotAuthenticate() throws Exception {
        assertUnauthorized(get("/api/v1/test/me")
                .queryParam("access_token", jwtService.generateAccessToken(USER_ID)));
        verifyNoInteractions(userRepository);
    }

    @Test
    void tamperedSignatureCannotAuthenticate() throws Exception {
        var token = jwtService.generateAccessToken(USER_ID);
        int signatureStart = token.lastIndexOf('.') + 1;
        char changed = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        var tampered = token.substring(0, signatureStart) + changed + token.substring(signatureStart + 1);

        assertUnauthorized(get("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered));
        verifyNoInteractions(userRepository);
    }

    @Test
    void expiredTokenCannotAuthenticate() throws Exception {
        var now = Instant.now();
        var expired = Jwts.builder()
                .issuer(ISSUER)
                .subject(USER_ID.toString())
                .issuedAt(Date.from(now.minusSeconds(600)))
                .expiration(Date.from(now.minusSeconds(60)))
                .claim("token_type", "access")
                .signWith(SIGNING_KEY, Jwts.SIG.HS256)
                .compact();

        assertUnauthorized(get("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired));
        verifyNoInteractions(userRepository);
    }

    @Test
    void duplicateAuthorizationHeadersAreRejected() throws Exception {
        var authorization = bearer();
        assertUnauthorized(get("/api/v1/test/me")
                .header(HttpHeaders.AUTHORIZATION, authorization, authorization));
        verifyNoInteractions(userRepository);
    }

    @Test
    void multipleCredentialsInOneHeaderAreRejected() throws Exception {
        var authorization = bearer();
        assertUnauthorized(get("/api/v1/test/me")
                .header(HttpHeaders.AUTHORIZATION, authorization + ", " + authorization));
        verifyNoInteractions(userRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = { "register", "login", "refresh" })
    void onlyRequiredAuthenticationPostRoutesArePublic(String route) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/" + route))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        verifyNoInteractions(userRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = { "register", "login", "refresh", "me", "logout", "unlisted" })
    void authenticationGetRoutesRemainProtected(String route) throws Exception {
        assertUnauthorized(get("/api/v1/auth/" + route));
    }

    @ParameterizedTest
    @ValueSource(strings = { "me", "logout", "unlisted" })
    void otherAuthenticationPostRoutesRemainProtected(String route) throws Exception {
        assertUnauthorized(post("/api/v1/auth/" + route));
    }

    @Test
    void invalidBearerIsRejectedEvenOnPublicAuthenticationRoute() throws Exception {
        assertUnauthorized(post("/api/v1/auth/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void authenticatedAccessDenialReturnsSafeJson403() throws Exception {
        mvc.perform(get("/api/v1/test/forbidden").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void passwordEncoderUsesBcryptAndVerifiesCredentials() {
        var password = UUID.randomUUID().toString();
        var hash = passwordEncoder.encode(password);

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(hash).isNotEqualTo(password).startsWith("$2");
        assertThat(passwordEncoder.matches(password, hash)).isTrue();
        assertThat(passwordEncoder.matches(UUID.randomUUID().toString(), hash)).isFalse();
    }

    @Test
    void authenticationManagerVerifiesDatabasePasswordAndErasesCredentials() {
        var result = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(USERNAME, PASSWORD));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getCredentials()).isNull();
        assertThat(result.getPrincipal()).isInstanceOf(ChatUserDetails.class);
        var principal = (ChatUserDetails) result.getPrincipal();
        assertThat(principal.getId()).isEqualTo(USER_ID);
        assertThat(principal.getUsername()).isEqualTo(USERNAME);
        assertThat(principal.getPassword()).isNull();
        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    void authenticationManagerRejectsIncorrectPassword() {
        assertThatThrownBy(() -> authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(USERNAME, UUID.randomUUID().toString())))
                .isInstanceOf(BadCredentialsException.class);
        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    void containerErrorDispatchIsNotReplacedWithAuthenticationFailure() throws Exception {
        mvc.perform(get("/error").with(request -> {
            request.setDispatcherType(DispatcherType.ERROR);
            return request;
        }))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("TEST_ERROR"));

        assertUnauthorized(get("/error"));
    }

    @Test
    void downstreamApplicationExceptionsAreNotReclassifiedAsInvalidTokens() {
        assertThatThrownBy(() -> mvc.perform(get("/api/v1/test/failure")
                .header(HttpHeaders.AUTHORIZATION, bearer())))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    private String bearer() {
        return "Bearer " + jwtService.generateAccessToken(USER_ID);
    }

    private ResultActions assertUnauthorized(MockHttpServletRequestBuilder request) throws Exception {
        var result = mvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));

        var response = result.andReturn();
        var json = objectMapper.readTree(response.getResponse().getContentAsString());
        assertThat(Instant.parse(json.get("timestamp").stringValue())).isBeforeOrEqualTo(Instant.now());
        assertThat(response.getRequest().getSession(false)).isNull();
        assertThat(response.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        return result;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({ SecurityConfig.class, SecurityErrorHandler.class, CustomUserDetailsService.class,
            TestController.class })
    static class SecurityTestConfiguration {

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }
    }

    @TestComponent
    @RestController
    static class TestController {

        @PostMapping({ "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh" })
        public Map<String, Boolean> authenticationStub() {
            return Map.of("success", true);
        }

        @GetMapping("/api/v1/test/me")
        public Map<String, Object> identity(Authentication authentication) {
            var principal = (ChatUserDetails) authentication.getPrincipal();
            assertThat(authentication.getCredentials()).isNull();
            assertThat(principal.getPassword()).isNull();
            return Map.of("id", principal.getId(), "username", principal.getUsername());
        }

        @PostMapping("/api/v1/test/me")
        public Map<String, Object> authenticatedPost(Authentication authentication) {
            return identity(authentication);
        }

        @GetMapping("/api/v1/test/forbidden")
        @PreAuthorize("denyAll()")
        public Map<String, Boolean> forbidden() {
            return Map.of("success", true);
        }

        @GetMapping("/api/v1/test/failure")
        public void applicationFailure() {
            throw new IllegalArgumentException("Test application failure");
        }

        @GetMapping("/error")
        public ResponseEntity<Map<String, String>> errorDispatch() {
            return ResponseEntity.internalServerError().body(Map.of("code", "TEST_ERROR"));
        }
    }
}
