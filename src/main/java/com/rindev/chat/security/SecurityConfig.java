package com.rindev.chat.security;

import com.rindev.chat.config.OriginPolicy;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

        @Bean
        OriginPolicy originPolicy(
                        @Value("${app.origins.http:http://localhost:3000,http://localhost:5173}") String http,
                        @Value("${app.origins.websocket:*}") String websocket, Environment environment) {
                return new OriginPolicy(http, websocket, environment.acceptsProfiles(Profiles.of("prod")));
        }

        @Bean
        JwtService jwtService(JwtProperties properties) {
                return new JwtService(properties);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder) {
                var provider = new DaoAuthenticationProvider(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder);
                return provider;
        }

        @Bean
        AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
                return new ProviderManager(provider);
        }

        // Persistence-only application tests do not create HttpSecurity or servlet
        // filters.
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
        @EnableWebSecurity
        @EnableMethodSecurity
        static class WebSecurityConfiguration {

                @Bean
                CorsConfigurationSource corsConfigurationSource(OriginPolicy origins) {
                        var configuration = new CorsConfiguration();
                        configuration.setAllowedOrigins(origins.http());
                        configuration.setAllowedMethods(
                                        List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
                        configuration.setAllowCredentials(false);
                        configuration.setMaxAge(3600L);
                        var source = new UrlBasedCorsConfigurationSource();
                        source.registerCorsConfiguration("/api/**", configuration);
                        return source;
                }

                @Bean
                SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                CustomUserDetailsService userDetailsService,
                                SecurityErrorHandler errorHandler,
                                DaoAuthenticationProvider provider,
                                CorsConfigurationSource corsConfigurationSource,
                                @Value("${springdoc.api-docs.enabled:true}") boolean apiDocsEnabled,
                                @Value("${springdoc.swagger-ui.enabled:true}") boolean swaggerUiEnabled)
                                throws Exception {
                        // Construct here so Boot does not also register this filter with the servlet
                        // container.
                        var jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService, errorHandler);

                        return http
                                        .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                        .csrf(AbstractHttpConfigurer::disable)
                                        .formLogin(AbstractHttpConfigurer::disable)
                                        .httpBasic(AbstractHttpConfigurer::disable)
                                        .logout(AbstractHttpConfigurer::disable)

                                        .sessionManagement(session -> session
                                                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                        .securityContext(context -> context
                                                        .securityContextRepository(new NullSecurityContextRepository())
                                                        .requireExplicitSave(true))

                                        .requestCache(cache -> cache.requestCache(new NullRequestCache()))

                                        .authenticationProvider(provider)

                                        .exceptionHandling(errors -> errors
                                                        .authenticationEntryPoint(errorHandler)
                                                        .accessDeniedHandler(errorHandler))

                                        .authorizeHttpRequests(authorize -> authorize
                                                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                                                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                                                        .requestMatchers(HttpMethod.HEAD, "/actuator/health")
                                                        .permitAll()
                                                        .requestMatchers("/actuator", "/actuator/**").denyAll()

                                                        // Deny disabled documentation even to authenticated users.
                                                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**",
                                                                        "/v3/api-docs.yaml")
                                                        .access((authentication, context) -> new AuthorizationDecision(
                                                                        apiDocsEnabled))
                                                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html")
                                                        .access((authentication, context) -> new AuthorizationDecision(
                                                                        apiDocsEnabled && swaggerUiEnabled))
                                                        .requestMatchers("/ws", "/ws/**").permitAll()

                                                        // Authentication endpoints
                                                        .requestMatchers(
                                                                        HttpMethod.POST,
                                                                        "/api/v1/auth/register",
                                                                        "/api/v1/auth/login",
                                                                        "/api/v1/auth/refresh")
                                                        .permitAll()

                                                        .anyRequest().authenticated())

                                        .addFilterBefore(
                                                        jwtFilter,
                                                        UsernamePasswordAuthenticationFilter.class)

                                        .build();
                }
        }
}
