package com.rindev.chat.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates only explicit Bearer access tokens; never reads cookies or query parameters. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final int MAX_TOKEN_LENGTH = 8192;
    private static final Pattern COMPACT_JWS = Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityErrorHandler errorHandler;
    private final AccountStatusUserDetailsChecker accountChecker = new AccountStatusUserDetailsChecker();

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService,
                                   SecurityErrorHandler errorHandler) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.errorHandler = errorHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var headers = request.getHeaders(HttpHeaders.AUTHORIZATION);
        if (headers == null || !headers.hasMoreElements()) {
            chain.doFilter(request, response);
            return;
        }

        String header = headers.nextElement();
        if (headers.hasMoreElements() || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            reject(request, response);
            return;
        }

        String token = header.substring(7).trim();
        if (token.length() > MAX_TOKEN_LENGTH || !COMPACT_JWS.matcher(token).matches()) {
            reject(request, response);
            return;
        }

        try {
            Long userId = jwtService.parseUserId(token);
            ChatUserDetails principal = userDetailsService.loadUserById(userId);
            accountChecker.check(principal);
            principal.eraseCredentials();

            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
            reject(request, response);
            return;
        }

        // Keep downstream application failures outside the authentication exception boundary.
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        errorHandler.commence(request, response, new BadCredentialsException("Invalid access token"));
    }
}
