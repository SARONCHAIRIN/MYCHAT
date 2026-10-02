package com.rindev.chat.exception;

import com.rindev.chat.dto.response.ApiResponse;
import com.rindev.chat.dto.response.UserResponse;
import com.rindev.chat.entity.User;
import com.rindev.chat.mapper.UserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private static final String SECRET = "rejected-password-must-never-be-returned";
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ContractController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void successfulResponseUsesSharedEnvelopeAndIsoTimestamp() throws Exception {
        var response = mvc.perform(get("/contract/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("Request successful"))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.timestamp").isString()).andReturn();
        assertThat(Instant.parse(json.readTree(response.getResponse().getContentAsString())
                .get("timestamp").stringValue())).isBeforeOrEqualTo(Instant.now());

    }

    @ParameterizedTest
    @CsvSource({ "bad-request,400,BAD_REQUEST", "not-found,404,NOT_FOUND", "conflict,409,CONFLICT",
            "unauthorized,401,UNAUTHORIZED", "forbidden,403,FORBIDDEN",
            "authentication,401,UNAUTHORIZED", "access-denied,403,FORBIDDEN",
            "integrity,409,CONFLICT", "unexpected,500,INTERNAL_SERVER_ERROR" })
    void exceptionsKeepStatusAndSafeContract(String kind, int expectedStatus, String code) throws Exception {
        var response = mvc.perform(get("/contract/fail/{kind}", kind))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist()).andReturn();
        assertThat(response.getResponse().getContentAsString()).doesNotContain(SECRET);
        if (expectedStatus == 401) {
            assertThat(response.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        }
    }

    @Test
    void invalidBodyProvidesFieldErrorsWithoutInterpolatedRejectedPasswords() throws Exception {
        String body = json.writeValueAsString(Map.of("username", "", "password", SECRET));
        var response = mvc.perform(post("/contract/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.username").value("This field is required"))
                .andExpect(jsonPath("$.errors.password").value("Length is outside the allowed range"))
                .andReturn();
        assertThat(response.getResponse().getContentAsString())
                .doesNotContain(SECRET, "rejectedValue", "validatedValue");
    }

    @ParameterizedTest
    @ValueSource(strings = { "{\"password\":\"rejected-password-must-never-be-returned\"", "[]", "", "null" })
    void malformedOrMissingBodiesHaveSafeBadRequestResponses(String body) throws Exception {
        var response = mvc.perform(post("/contract/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.success").value(false)).andReturn();
        assertThat(response.getResponse().getContentAsString()).doesNotContain(SECRET, "JsonParseException");
    }

    @Test
    void invalidNumericPathDoesNotExposeSubmittedValue() throws Exception {
        var response = mvc.perform(get("/contract/number/{id}", SECRET))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST")).andReturn();
        assertThat(response.getResponse().getContentAsString()).doesNotContain(SECRET, "NumberFormatException");
    }

    @Test
    void missingQueryParameterHasBadRequestContract() throws Exception {
        mvc.perform(get("/contract/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 101 })
    void methodParameterValidationUsesSafeFieldMessages(int size) throws Exception {
        mvc.perform(get("/contract/page").queryParam("size", Integer.toString(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.size").value("Value is outside the allowed range"));
    }

    @Test
    void unsupportedMethodPreservesAllowHeaderAndStatus() throws Exception {
        mvc.perform(post("/contract/success"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedMediaTypeHasConsistentErrorEnvelope() throws Exception {
        mvc.perform(post("/contract/validate").contentType(MediaType.TEXT_PLAIN).content(SECRET))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void ownerProfileExcludesPasswordAndVerificationDetails() {
        var user = new User();
        user.setId(42L);
        user.setUsername("alice");
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPhone("+85512345678");
        user.setPasswordHash(SECRET);
        user.setEmailVerifiedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        user.setPhoneVerifiedAt(LocalDateTime.of(2026, 1, 2, 0, 0));
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        UserResponse response = new UserMapper().toResponse(user);
        var document = json.readTree(json.writeValueAsString(ApiResponse.created(response)));
        assertThat(document.get("code").stringValue()).isEqualTo("CREATED");
        assertThat(document.get("data").get("email").stringValue()).isEqualTo("alice@example.com");
        assertThat(document.get("data").get("phone").stringValue()).isEqualTo("+85512345678");
        assertThat(document.toString()).doesNotContain(SECRET, "password", "emailVerifiedAt", "phoneVerifiedAt");
    }

    record ValidatedBody(@NotBlank String username,
            @NotBlank @Size(max = 8, message = "rejected ${validatedValue}") String password) {
    }

    @TestComponent
    @RestController
    static class ContractController {

        @GetMapping("/contract/success")
        ApiResponse<Map<String, Integer>> success() {
            return ApiResponse.ok(Map.of("id", 42));
        }

        @PostMapping(value = "/contract/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> validate(@Valid @RequestBody ValidatedBody body) {
            return ApiResponse.ok(null);
        }

        @GetMapping("/contract/number/{id}")
        ApiResponse<Long> number(@PathVariable Long id) {
            return ApiResponse.ok(id);
        }

        @GetMapping("/contract/search")
        ApiResponse<Void> search(@RequestParam String q) {
            return ApiResponse.ok(null);
        }

        @GetMapping("/contract/page")
        ApiResponse<Integer> page(@RequestParam @Min(1) @Max(100) int size) {
            return ApiResponse.ok(size);
        }

        @GetMapping("/contract/fail/{kind}")
        ApiResponse<Void> fail(@PathVariable String kind) {
            throw switch (kind) {
                case "bad-request" -> new BadRequestException("Invalid request");
                case "not-found" -> new ResourceNotFoundException("User not found");
                case "conflict" -> new ConflictException("Username is already in use");
                case "unauthorized" -> new UnauthorizedException();
                case "forbidden" -> new ForbiddenException();
                case "authentication" -> new BadCredentialsException(SECRET);
                case "access-denied" -> new AccessDeniedException(SECRET);
                case "integrity" -> new DataIntegrityViolationException("Duplicate database value " + SECRET);
                default -> new IllegalStateException(SECRET);
            };
        }
    }
}
