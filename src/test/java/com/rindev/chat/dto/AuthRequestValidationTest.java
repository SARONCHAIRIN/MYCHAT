package com.rindev.chat.dto;

import com.rindev.chat.dto.request.LoginRequest;
import com.rindev.chat.dto.request.RefreshTokenRequest;
import com.rindev.chat.dto.request.RegisterRequest;
import com.rindev.chat.dto.response.AuthResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void close() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidRegistrationAndOptionalEmail() {
        assertThat(validator.validate(registration("secret-password"))).isEmpty();
        assertThat(validator.validate(new RegisterRequest("Alice", "alice_123", null, "secret-password"))).isEmpty();
        assertThat(validator.validate(new RegisterRequest("Alice", "alice_123", "", "secret-password"))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", " space", "abc-def", "ééé", "a b", "abc@def"})
    void rejectsInvalidUsernames(String username) {
        assertThat(validator.validate(new RegisterRequest("Alice", username, null, "secret-password")))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("username"));
    }

    @Test
    void enforcesRegistrationMinimumAndByteLimitWithoutTruncatingUnicode() {
        assertThat(validator.validate(registration("short"))).isNotEmpty();
        assertThat(validator.validate(registration("a".repeat(72)))).isEmpty();
        assertThat(validator.validate(registration("a".repeat(73)))).isNotEmpty();
        assertThat(validator.validate(registration("界".repeat(24)))).isEmpty();
        assertThat(validator.validate(registration("界".repeat(25)))).isNotEmpty();
        assertThat(validator.validate(registration("🔐".repeat(18)))).isEmpty();
        assertThat(validator.validate(registration("🔐".repeat(19)))).isNotEmpty();
    }

    @Test
    void rejectsBlankAndOversizedProfileFields() {
        assertThat(validator.validate(new RegisterRequest(" ", "alice", null, "secret-password"))).isNotEmpty();
        assertThat(validator.validate(new RegisterRequest("a".repeat(101), "alice", null, "secret-password"))).isNotEmpty();
        assertThat(validator.validate(new RegisterRequest("Alice", "a".repeat(51), null, "secret-password"))).isNotEmpty();
        assertThat(validator.validate(new RegisterRequest("Alice", "alice", "not-an-email", "secret-password"))).isNotEmpty();
    }

    @Test
    void loginAllowsExistingShortPasswordsButRejectsEmptyAndOversizedValues() {
        assertThat(validator.validate(new LoginRequest("alice", "oldpwd"))).isEmpty();
        assertThat(validator.validate(new LoginRequest("alice", ""))).isNotEmpty();
        assertThat(validator.validate(new LoginRequest("alice", "界".repeat(25)))).isNotEmpty();
    }

    @Test
    void rejectsEmptyAndUnboundedRefreshTokens() {
        assertThat(validator.validate(new RefreshTokenRequest(""))).isNotEmpty();
        assertThat(validator.validate(new RefreshTokenRequest(" "))).isNotEmpty();
        assertThat(validator.validate(new RefreshTokenRequest("a".repeat(513)))).isNotEmpty();
    }

    @Test
    void requestsAndResponseDoNotLeakSecretsThroughToString() {
        assertThat(registration("secret-password").toString()).doesNotContain("secret-password", "alice@example.test");
        assertThat(new LoginRequest("alice", "secret-password").toString()).doesNotContain("secret-password");
        assertThat(new RefreshTokenRequest("secret-refresh-token").toString()).doesNotContain("secret-refresh-token");
        assertThat(new AuthResponse("access-secret", "refresh-secret", "Bearer", 900, Instant.now(), null).toString())
                .doesNotContain("access-secret", "refresh-secret");
    }

    @Test
    void requestSecretsAreWriteOnlyInJson() {
        var mapper = JsonMapper.builder().build();

        assertThat(mapper.writeValueAsString(registration("secret-password"))).doesNotContain("secret-password", "password");
        assertThat(mapper.writeValueAsString(new LoginRequest("alice", "secret-password"))).doesNotContain("secret-password", "password");
        assertThat(mapper.writeValueAsString(new RefreshTokenRequest("secret-refresh-token"))).doesNotContain("secret-refresh-token");
        assertThat(mapper.readValue("{\"username\":\"alice\",\"password\":\"secret-password\"}", LoginRequest.class).password())
                .isEqualTo("secret-password");
    }

    private static RegisterRequest registration(String password) {
        return new RegisterRequest("Alice", "alice", "alice@example.test", password);
    }
}
