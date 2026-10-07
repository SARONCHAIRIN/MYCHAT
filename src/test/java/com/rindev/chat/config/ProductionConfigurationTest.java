package com.rindev.chat.config;

import com.rindev.chat.security.CustomUserDetailsService;
import com.rindev.chat.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Loads the shipping profiles without connecting to a database or Firebase. */
class ProductionConfigurationTest {

    private static final String RUNTIME_PASSWORD = UUID.randomUUID().toString();
    private static final String RUNTIME_SIGNING_KEY = Base64.getEncoder()
            .encodeToString(Jwts.SIG.HS256.key().build().getEncoded());

    @ParameterizedTest
    @CsvSource({
            "spring.datasource.url, DB_URL",
            "spring.datasource.username, DB_USERNAME",
            "spring.datasource.password, DB_PASSWORD",
            "security.jwt.secret, JWT_SECRET",
            "app.upload.directory, UPLOAD_DIR"
    })
    void productionRequiresExplicitRuntimeInputs(String property, String variable) {
        production().run(context -> {
            assertThat(context).hasNotFailed();
            assertThatThrownBy(() -> context.getEnvironment().getRequiredProperty(property))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(variable);
        });
    }

    @Test
    void suppliedRuntimeInputsResolveWithoutDevelopmentFallbacks() {
        withRuntimeInputs(production()).run(context -> {
            assertThat(context).hasNotFailed();
            var environment = context.getEnvironment();
            assertThat(environment.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:mysql://private-db:3306/chat_db");
            assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("runtime-user");
            assertThat(environment.getProperty("app.upload.directory")).isEqualTo("/runtime/uploads");
            // Compare secrets as booleans so assertion failures cannot print their values.
            assertThat(RUNTIME_PASSWORD.equals(environment.getProperty("spring.datasource.password")))
                    .as("database password comes from runtime configuration").isTrue();
            assertThat(RUNTIME_SIGNING_KEY.equals(environment.getProperty("security.jwt.secret")))
                    .as("signing key comes from runtime configuration").isTrue();
        });
    }

    @Test
    void productionValidatesExistingSchemaAndDisablesDatabaseRecreation() {
        production().run(context -> assertProperties(context.getEnvironment(), Map.of(
                "spring.jpa.hibernate.ddl-auto", "validate",
                "spring.jpa.show-sql", "false",
                "spring.flyway.enabled", "true",
                "spring.flyway.baseline-on-migrate", "false",
                "spring.flyway.baseline-version", "1",
                "spring.flyway.clean-disabled", "true",
                "spring.flyway.validate-on-migrate", "true",
                "spring.sql.init.mode", "never")));
    }

    @Test
    void productionExposesOnlyAggregateHealthAndSuppressesHttpErrorDetails() {
        production().run(context -> {
            assertProperties(context.getEnvironment(), Map.of(
                    "management.endpoints.access.default", "none",
                    "management.endpoints.web.exposure.include", "health",
                    "management.endpoints.web.discovery.enabled", "false",
                    "management.endpoints.jmx.exposure.exclude", "*",
                    "management.endpoint.health.access", "read-only",
                    "management.endpoint.health.probes.enabled", "false",
                    "management.endpoint.health.show-details", "never",
                    "management.endpoint.health.show-components", "never"));
            assertProperties(context.getEnvironment(), Map.of(
                    "spring.web.error.include-exception", "false",
                    "spring.web.error.include-message", "never",
                    "spring.web.error.include-binding-errors", "never",
                    "spring.web.error.include-stacktrace", "never",
                    "spring.mvc.log-request-details", "false"));
        });
    }

    @Test
    void productionDisablesBothDocumentationEndpointsByDefault() {
        production().run(context -> assertProperties(context.getEnvironment(), Map.of(
                "springdoc.api-docs.enabled", "false",
                "springdoc.swagger-ui.enabled", "false")));
    }

    @Test
    void productionCanExplicitlyEnableBothDocumentationEndpoints() {
        production().withPropertyValues("SWAGGER_ENABLED=true")
                .run(context -> assertProperties(context.getEnvironment(), Map.of(
                        "springdoc.api-docs.enabled", "true",
                        "springdoc.swagger-ui.enabled", "true")));
    }

    @Test
    void productionWithoutTrustedOriginsAllowsNoCrossOriginClients() {
        withSecurity(production()).run(context -> {
            assertThat(context).hasNotFailed();
            var origins = context.getBean(OriginPolicy.class);
            assertThat(origins.http()).isEmpty();
            assertThat(origins.websocket()).isEmpty();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "CORS_ALLOWED_ORIGINS", "WEBSOCKET_ALLOWED_ORIGINS" })
    void activeProductionProfileRejectsWildcardRuntimeOrigins(String property) {
        withSecurity(production()).withPropertyValues(property + "=*").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage(
                            "Production origins must be exact HTTP(S) origins without wildcards or paths");
        });
    }

    @Test
    void runtimeTrustedOriginsAreTrimmedDeduplicatedAndKeptSeparate() {
        withSecurity(production()).withPropertyValues(
                "CORS_ALLOWED_ORIGINS= https://chat.example.com,https://chat.example.com, https://admin.example.com ",
                "WEBSOCKET_ALLOWED_ORIGINS=https://chat.example.com:8443")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var origins = context.getBean(OriginPolicy.class);
                    assertThat(origins.http()).containsExactly("https://chat.example.com", "https://admin.example.com");
                    assertThat(origins.websocket()).containsExactly("https://chat.example.com:8443");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "*", "https://*.example.com", "https://chat.example.com/", "https://chat.example.com/path",
            "https://user@chat.example.com", "https://chat.example.com?query=value",
            "https://chat.example.com#fragment", "https://chat.example.com:65536",
            "not an origin", "null", "ftp://chat.example.com"
    })
    void productionRejectsMalformedOriginsForHttpAndWebSocket(String origin) {
        assertThatThrownBy(() -> new OriginPolicy(origin, "", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OriginPolicy("", origin, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void localDevelopmentKeepsSwaggerAndWebSocketWildcard() {
        withSecurity(configuration()).run(context -> {
            assertThat(context).hasNotFailed();
            assertProperties(context.getEnvironment(), Map.of(
                    "springdoc.api-docs.enabled", "true",
                    "springdoc.swagger-ui.enabled", "true",
                    "spring.flyway.enabled", "false",
                    "spring.jpa.hibernate.ddl-auto", "validate"));
            assertThat(context.getBean(OriginPolicy.class).websocket()).containsExactly("*");
            assertThat(context.getBean(OriginPolicy.class).http())
                    .containsExactly("http://localhost:3000", "http://localhost:5173");
        });
    }

    private static ApplicationContextRunner configuration() {
        return new ApplicationContextRunner().withInitializer(context -> {
            // Host credentials/profile choices must never influence these tests.
            var sources = context.getEnvironment().getPropertySources();
            sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            new ConfigDataApplicationContextInitializer().initialize(context);
        });
    }

    private static ApplicationContextRunner production() {
        return configuration().withPropertyValues("spring.profiles.active=prod");
    }

    private static ApplicationContextRunner withRuntimeInputs(ApplicationContextRunner runner) {
        return runner.withPropertyValues(
                "DB_URL=jdbc:mysql://private-db:3306/chat_db",
                "DB_USERNAME=runtime-user",
                "DB_PASSWORD=" + RUNTIME_PASSWORD,
                "JWT_SECRET=" + RUNTIME_SIGNING_KEY,
                "UPLOAD_DIR=/runtime/uploads");
    }

    private static ApplicationContextRunner withSecurity(ApplicationContextRunner runner) {
        return withRuntimeInputs(runner).withUserConfiguration(SecurityConfig.class)
                .withBean(CustomUserDetailsService.class, () -> mock(CustomUserDetailsService.class));
    }

    private static void assertProperties(Environment environment, Map<String, String> properties) {
        properties.forEach((name, expected) -> assertThat(environment.getProperty(name)).as(name).isEqualTo(expected));
    }
}
