package com.rindev.chat.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

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
        SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                CustomUserDetailsService userDetailsService,
                SecurityErrorHandler errorHandler,
                DaoAuthenticationProvider provider) throws Exception {
            // Construct here so Boot does not also register this filter with the servlet
            // container.
            var jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService, errorHandler);

            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .logout(AbstractHttpConfigurer::disable)

                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

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

                            // Swagger / OpenAPI
                            .requestMatchers(
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/v3/api-docs/**")
                            .permitAll()

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
