package com.rindev.chat.config;

import com.rindev.chat.controller.*;
import com.rindev.chat.dto.response.*;
import com.rindev.chat.entity.User;
import com.rindev.chat.enums.*;
import com.rindev.chat.exception.GlobalExceptionHandler;
import com.rindev.chat.exception.UnauthorizedException;
import com.rindev.chat.repository.UserRepository;
import com.rindev.chat.security.CustomUserDetailsService;
import com.rindev.chat.security.JwtService;
import com.rindev.chat.security.SecurityConfig;
import com.rindev.chat.security.SecurityErrorHandler;
import com.rindev.chat.service.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real MVC, security and Springdoc contracts, with no database, storage or push side effects. */
@SpringBootTest(classes = FlutterApiContractTest.ContractConfiguration.class,
        properties = { "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true" })
class FlutterApiContractTest {
    private static final List<Class<?>> SERVICES = List.of(AuthService.class, BlockService.class,
            ConversationService.class, ConversationMemberService.class, DeviceService.class,
            MessageService.class, NotificationService.class, PinnedMessageService.class,
            ReactionService.class, ReceiptService.class, ReportService.class, SettingsService.class,
            UploadService.class, UserService.class);
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    MockMvc mvc;

    @DynamicPropertySource
    static void signingKey(DynamicPropertyRegistry properties) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String key = Base64.getEncoder().encodeToString(bytes);
        properties.add("security.jwt.secret", () -> key);
    }

    @BeforeEach
    void setup() {
        SERVICES.forEach(type -> reset(context.getBean(type)));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash("unused-contract-fixture");
        when(users.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void generateOpenApiForReconciliation() throws Exception {
        String body = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode document = json.readTree(body);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/phase26-openapi.json"),
                json.writerWithDefaultPrettyPrinter().writeValueAsString(document));
        Set<String> actual = new HashSet<>();
        document.path("paths").properties().forEach(path -> path.getValue().properties()
                .forEach(operation -> actual.add(operation.getKey().toUpperCase() + " " + path.getKey())));
        assertThat(actual).containsExactlyInAnyOrderElementsOf(operations()
                .map(operation -> operation.method() + " " + operation.path()).toList());
        assertThat(actual).hasSize(45);
        assertThat(document.at("/components/securitySchemes/bearerAuth/type").stringValue()).isEqualTo("http");
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").stringValue()).isEqualTo("bearer");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    void successfulRequestsAndAuthenticationAgreeWithGeneratedOpenApi(Operation operation) throws Exception {
        JsonNode document = json.readTree(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode specification = document.path("paths").path(operation.path()).path(operation.method().toLowerCase());
        assertThat(specification.isMissingNode()).isFalse();
        assertThat(specification.path("security").size()).isEqualTo(operation.publicRoute() ? 0 : 1);
        if (!operation.publicRoute()) {
            mvc.perform(operation.request()).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(header().string("WWW-Authenticate", "Bearer"));
        }
        var result = mvc.perform(operation.request().header("Authorization", "Bearer " + jwt.generateAccessToken(1L)))
                .andExpect(status().is(operation.status())).andReturn().getResponse();
        JsonNode responseSchema = specification.path("responses").path(Integer.toString(operation.status()));
        assertThat(responseSchema.isMissingNode()).as(operation.toString()).isFalse();
        assertThat(specification.path("responses").properties().stream()
                .filter(entry -> entry.getKey().startsWith("2")).count()).isEqualTo(1);
        if (operation.status() == 204) {
            assertThat(result.getContentAsString()).isEmpty();
            assertThat(responseSchema.has("content")).isFalse();
            return;
        }
        assertThat(result.getContentType()).startsWith("application/json");
        JsonNode response = json.readTree(result.getContentAsString());
        assertThat(response.propertyNames()).containsExactlyInAnyOrder("success", "code", "message", "timestamp", "data");
        assertThat(response.path("success").asBoolean()).isTrue();
        assertThat(response.path("code").stringValue()).isEqualTo(operation.status() == 201 ? "CREATED" : "OK");
        assertThat(Instant.parse(response.path("timestamp").stringValue())).isBeforeOrEqualTo(Instant.now());
        JsonNode schema = responseSchema.path("content").properties().iterator().next().getValue().path("schema");
        assertThat(schema.isMissingNode()).isFalse();
        assertSchemaFieldsAndEnums(response, schema, document);
    }

    @Test
    void validationMalformedJsonAndInvalidEnumsHaveTheDocumentedErrorEnvelope() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.username").value("This field is required"))
                .andExpect(jsonPath("$.errors.password").value("This field is required"))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mvc.perform(post("/api/v1/conversations/10/messages")
                .header("Authorization", "Bearer " + jwt.generateAccessToken(1L))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"text\",\"content\":\"Hello\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void logoutInvalidRefreshSessionIsUnauthorizedAsDocumented() throws Exception {
        doThrow(new UnauthorizedException("Invalid refresh token"))
                .when(context.getBean(AuthService.class)).logout(any(), any());
        mvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + jwt.generateAccessToken(1L))
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"invalid\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void pagingNullFieldsAndTimestampFormatsMatchTheWireContract() throws Exception {
        var result = mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + jwt.generateAccessToken(1L)))
                .andExpect(status().isOk()).andReturn().getResponse();
        JsonNode page = json.readTree(result.getContentAsString()).path("data");
        assertThat(page.propertyNames()).containsExactlyInAnyOrder("content", "pageable", "last", "totalPages",
                "totalElements", "size", "number", "sort", "first", "numberOfElements", "empty");
        assertThat(page.path("pageable").propertyNames()).containsExactlyInAnyOrder("pageNumber", "pageSize", "sort",
                "offset", "paged", "unpaged");
        assertThat(page.path("content").get(0).path("avatarUrl").isNull()).isTrue();
        assertThat(page.path("content").get(0).path("lastSeenAt").stringValue()).isEqualTo("2026-10-07T09:00:00");
        assertThat(page.path("content").get(0).path("status").stringValue()).isEqualTo("OFFLINE");
        verify(context.getBean(UserService.class)).getUsers(0, 20);
        mvc.perform(get("/api/v1/conversations/10/messages?before=100&limit=2")
                .header("Authorization", "Bearer " + jwt.generateAccessToken(1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nextCursor").value(99))
                .andExpect(jsonPath("$.data.hasMore").value(true));
        verify(context.getBean(MessageService.class)).getMessages(1L, 10L, 100L, 2);
        Files.writeString(Path.of("target/phase26-user-page-example.json"),
                json.writerWithDefaultPrettyPrinter().writeValueAsString(json.readTree(result.getContentAsString())));
    }

    private static void assertSchemaFieldsAndEnums(JsonNode value, JsonNode schema, JsonNode document) {
        if (schema.has("$ref")) {
            schema = document.at(schema.path("$ref").stringValue().substring(1));
        }
        if (value.isNull()) return;
        if (value.isObject()) {
            JsonNode properties = schema.path("properties");
            assertThat(properties.isMissingNode()).as(value.toString()).isFalse();
            for (var field : value.properties()) {
                assertThat(properties.has(field.getKey())).as("documented field " + field.getKey()).isTrue();
                assertSchemaFieldsAndEnums(field.getValue(), properties.path(field.getKey()), document);
            }
        } else if (value.isArray()) {
            for (JsonNode item : value) assertSchemaFieldsAndEnums(item, schema.path("items"), document);
        } else if (schema.has("enum")) {
            assertThat(schema.path("enum").valueStream().map(JsonNode::asString).toList()).contains(value.asString());
        }
    }

    static Stream<Operation> operations() {
        return Stream.of(
                new Operation("POST", "/api/v1/auth/register", 201, "{\"name\":\"Alice\",\"username\":\"alice\",\"email\":\"alice@example.test\",\"password\":\"Example-pass-123\"}"),
                new Operation("POST", "/api/v1/auth/login", 200, "{\"username\":\"alice\",\"password\":\"Example-pass-123\"}"),
                new Operation("POST", "/api/v1/auth/refresh", 200, "{\"refreshToken\":\"example-refresh-token\"}"),
                new Operation("POST", "/api/v1/auth/logout", 200, "{\"refreshToken\":\"example-refresh-token\"}"),
                new Operation("GET", "/api/v1/auth/me", 200, null),
                new Operation("GET", "/api/v1/users", 200, null),
                new Operation("GET", "/api/v1/users/search", 200, null),
                new Operation("GET", "/api/v1/users/me", 200, null),
                new Operation("GET", "/api/v1/users/{id}", 200, null),
                new Operation("PATCH", "/api/v1/users/me", 200, "{\"bio\":\"Hello\"}"),
                new Operation("PATCH", "/api/v1/users/me/avatar", 200, "{\"avatarUrl\":\"https://example.test/avatar.png\"}"),
                new Operation("GET", "/api/v1/settings", 200, null),
                new Operation("PATCH", "/api/v1/settings", 200, "{\"theme\":\"DARK\"}"),
                new Operation("POST", "/api/v1/devices", 201, "{\"deviceName\":\"Test phone\",\"platform\":\"ANDROID\",\"fcmToken\":\"example-fcm-token\"}"),
                new Operation("DELETE", "/api/v1/devices/{id}", 204, null),
                new Operation("POST", "/api/v1/conversations", 201, "{\"type\":\"DIRECT\",\"memberId\":2}"),
                new Operation("GET", "/api/v1/conversations", 200, null),
                new Operation("GET", "/api/v1/conversations/{id}", 200, null),
                new Operation("PATCH", "/api/v1/conversations/{id}", 200, "{\"name\":\"Friends\"}"),
                new Operation("GET", "/api/v1/conversations/{conversationId}/members", 200, null),
                new Operation("POST", "/api/v1/conversations/{conversationId}/members", 201, "{\"userId\":2}"),
                new Operation("DELETE", "/api/v1/conversations/{conversationId}/members/{userId}", 204, null),
                new Operation("PATCH", "/api/v1/conversations/{conversationId}/members/{userId}/role", 200, "{\"role\":\"ADMIN\"}"),
                new Operation("GET", "/api/v1/conversations/{conversationId}/messages", 200, null),
                new Operation("POST", "/api/v1/conversations/{conversationId}/messages", 201, "{\"type\":\"TEXT\",\"content\":\"Hello\"}"),
                new Operation("GET", "/api/v1/messages/{messageId}", 200, null),
                new Operation("PATCH", "/api/v1/messages/{messageId}", 200, "{\"content\":\"Edited\"}"),
                new Operation("DELETE", "/api/v1/messages/{messageId}", 204, null),
                new Operation("GET", "/api/v1/messages/{messageId}/reactions", 200, null),
                new Operation("POST", "/api/v1/messages/{messageId}/reactions", 201, "{\"emoji\":\"👍\"}"),
                new Operation("DELETE", "/api/v1/messages/{messageId}/reactions/{emoji}", 204, null),
                new Operation("POST", "/api/v1/messages/{messageId}/delivered", 200, null),
                new Operation("POST", "/api/v1/messages/{messageId}/read", 200, null),
                new Operation("POST", "/api/v1/conversations/{conversationId}/pins/{messageId}", 201, null),
                new Operation("DELETE", "/api/v1/conversations/{conversationId}/pins/{messageId}", 204, null),
                new Operation("GET", "/api/v1/conversations/{conversationId}/pins", 200, null),
                new Operation("POST", "/api/v1/uploads", 201, null),
                new Operation("POST", "/api/v1/users/{id}/block", 201, null),
                new Operation("DELETE", "/api/v1/users/{id}/block", 204, null),
                new Operation("GET", "/api/v1/users/me/blocked", 200, null),
                new Operation("GET", "/api/v1/notifications", 200, null),
                new Operation("POST", "/api/v1/notifications/{id}/read", 200, null),
                new Operation("POST", "/api/v1/notifications/read-all", 204, null),
                new Operation("DELETE", "/api/v1/notifications/{id}", 204, null),
                new Operation("POST", "/api/v1/reports", 201, "{\"messageId\":100,\"reason\":\"SPAM\",\"description\":\"Repeated spam\"}"));
    }

    record Operation(String method, String path, int status, String body) {
        boolean publicRoute() {
            return Set.of("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").contains(path);
        }
        AbstractMockHttpServletRequestBuilder<?> request() {
            String url = path.replace("{id}", "10").replace("{conversationId}", "10")
                    .replace("{messageId}", "100").replace("{userId}", "2").replace("{emoji}", "👍");
            if (path.equals("/api/v1/uploads")) {
                return multipart(url).file(new MockMultipartFile("file", "sample.png", "image/png", new byte[] { 1, 2, 3 }))
                        .param("messageId", "100");
            }
            MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), url);
            if (path.endsWith("/search")) builder.param("q", "alice");
            if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(body);
            return builder;
        }
        @Override public String toString() { return method + " " + path; }
    }

    private static <T> T serviceMock(Class<T> type) {
        return mock(type, invocation -> {
            Class<?> returns = invocation.getMethod().getReturnType();
            if (returns == void.class) return null;
            if (returns == String.class) return type.getSimpleName();
            if (returns == List.class) {
                Class<?> element = Map.of(BlockService.class, BlockedUserResponse.class,
                        ConversationService.class, ConversationResponse.class,
                        ConversationMemberService.class, ConversationMemberResponse.class,
                        PinnedMessageService.class, PinnedMessageResponse.class,
                        ReactionService.class, ReactionResponse.class).get(type);
                return List.of(fixture(element));
            }
            return fixture(returns);
        });
    }

    private static Object fixture(Class<?> type) {
        LocalDateTime time = LocalDateTime.of(2026, 10, 7, 9, 0);
        if (type == UserResponse.class) return new UserResponse(1L, "Alice", "alice", "alice@example.test",
                null, null, "Hello", UserStatus.OFFLINE, time, time, time);
        if (type == UserPublicResponse.class) return new UserPublicResponse(1L, "Alice", "alice", null, "Hello", UserStatus.OFFLINE, time);
        if (type == AuthResponse.class) return new AuthResponse("example-access-token", "example-refresh-token", "Bearer", 900,
                Instant.parse("2026-11-06T09:00:00Z"), (UserResponse) fixture(UserResponse.class));
        if (type == UserSettingResponse.class) return new UserSettingResponse(1L, Theme.SYSTEM, true, true, true, true,
                PrivacyLevel.EVERYONE, PrivacyLevel.EVERYONE, PrivacyLevel.EVERYONE, time, time);
        if (type == ConversationMemberResponse.class) return new ConversationMemberResponse(20L, 1L, "Alice", "alice", null, MemberRole.OWNER, time);
        if (type == ConversationResponse.class) return new ConversationResponse(10L, ConversationType.GROUP, "Friends", null, null, 1L,
                time, List.of((ConversationMemberResponse) fixture(ConversationMemberResponse.class)), time, time);
        if (type == DeviceResponse.class) return new DeviceResponse(30L, "Test phone", DevicePlatform.ANDROID, time, time);
        if (type == MessageResponse.class) return new MessageResponse(100L, 10L, 1L, "Alice", "alice", MessageType.TEXT, "Hello",
                null, null, false, false, null, null, time, time);
        if (type == MessagePageResponse.class) return new MessagePageResponse(List.of((MessageResponse) fixture(MessageResponse.class)), 99L, true);
        if (type == NotificationResponse.class) return new NotificationResponse(40L, NotificationType.MESSAGE, "New message", "Hello", 10L, 100L, false, null, time);
        if (type == NotificationPageResponse.class) return new NotificationPageResponse(List.of((NotificationResponse) fixture(NotificationResponse.class)), 0, 20, 1, 1);
        if (type == PinnedMessageResponse.class) return new PinnedMessageResponse(50L, 10L, 100L, 1L, time);
        if (type == ReactionResponse.class) return new ReactionResponse(60L, 100L, 1L, "alice", "👍", time);
        if (type == ReceiptResponse.class) return new ReceiptResponse(70L, 100L, 1L, time, null);
        if (type == ReportResponse.class) return new ReportResponse(80L, 1L, null, null, 100L, ReportReason.SPAM, "Repeated spam", ReportStatus.PENDING, time);
        if (type == UploadResponse.class) return new UploadResponse(90L, 100L, AttachmentType.IMAGE, "sample.png", "/uploads/sample.png", "image/png", 3L);
        if (type == BlockedUserResponse.class) return new BlockedUserResponse(91L, 2L, "bob", time);
        if (type == Page.class) return new PageImpl<>(List.of((UserPublicResponse) fixture(UserPublicResponse.class)), PageRequest.of(0, 20, Sort.by("id")), 1);
        throw new AssertionError("Missing contract fixture: " + type);
    }

    @TestComponent
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration" })
    @Import({ OpenApiConfig.class, SecurityConfig.class, SecurityErrorHandler.class,
            CustomUserDetailsService.class, GlobalExceptionHandler.class,
            AuthController.class, BlockController.class, ConversationController.class,
            ConversationMemberController.class, DeviceController.class, MessageController.class,
            NotificationController.class, PinnedMessageController.class, ReactionController.class,
            ReceiptController.class, ReportController.class, SettingsController.class,
            UploadController.class, UserController.class })
    static class ContractConfiguration {
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
        @Bean AuthService authService() { return serviceMock(AuthService.class); }
        @Bean BlockService blockService() { return serviceMock(BlockService.class); }
        @Bean ConversationService conversationService() { return serviceMock(ConversationService.class); }
        @Bean ConversationMemberService conversationMemberService() { return serviceMock(ConversationMemberService.class); }
        @Bean DeviceService deviceService() { return serviceMock(DeviceService.class); }
        @Bean MessageService messageService() { return serviceMock(MessageService.class); }
        @Bean NotificationService notificationService() { return serviceMock(NotificationService.class); }
        @Bean PinnedMessageService pinnedMessageService() { return serviceMock(PinnedMessageService.class); }
        @Bean ReactionService reactionService() { return serviceMock(ReactionService.class); }
        @Bean ReceiptService receiptService() { return serviceMock(ReceiptService.class); }
        @Bean ReportService reportService() { return serviceMock(ReportService.class); }
        @Bean SettingsService settingsService() { return serviceMock(SettingsService.class); }
        @Bean UploadService uploadService() { return serviceMock(UploadService.class); }
        @Bean UserService userService() { return serviceMock(UserService.class); }
    }
}
