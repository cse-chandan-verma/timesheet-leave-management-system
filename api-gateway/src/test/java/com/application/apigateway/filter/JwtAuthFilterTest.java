package com.application.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@DisplayName("JwtAuthFilter — Unit Tests")
class JwtAuthFilterTest {

    private JwtAuthFilter filter;

    // Must be ≥32 chars for HS256
    private static final String SECRET =
            "my-super-secret-key-for-testing-only-32chars!";

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
    }

    // ──────────────────────────────────────────────
    // Helper: build a valid signed JWT
    // ──────────────────────────────────────────────
    private String buildToken(long expiryMs) {
        SecretKey key = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user@example.com")
                .claim("role", "EMPLOYEE")
                .claim("userId", 42)
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key)
                .compact();
    }

    private String buildExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user@example.com")
                .expiration(new Date(System.currentTimeMillis() - 10_000))
                .signWith(key)
                .compact();
    }

    // ──────────────────────────────────────────────
    // 1. Public paths — no token needed
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("Public path /auth/login — passes through without token")
    void publicPath_login_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/login").build());

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Public path /auth/register — passes through without token")
    void publicPath_register_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/register").build());

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Public path /timesheet/swagger-ui — passes through")
    void publicPath_swagger_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/timesheet/swagger-ui/index.html").build());

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Public path /auth/validate — passes through")
    void publicPath_validate_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/validate").build());

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();
    }

    // ──────────────────────────────────────────────
    // 2. Missing Authorization header
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("Protected path — missing Authorization header → 401")
    void protectedPath_noHeader_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/timesheet/add").build());

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();

        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
        assert exchange.getResponse().getHeaders()
                .containsKey("X-Auth-Error");
    }

    // ──────────────────────────────────────────────
    // 3. Malformed Authorization header
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("Protected path — header without 'Bearer ' prefix → 401")
    void protectedPath_noBearerPrefix_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/timesheet/add")
                        .header(HttpHeaders.AUTHORIZATION, "Basic abc123")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();

        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
    }

    // ──────────────────────────────────────────────
    // 4. Valid JWT — happy path
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("Valid JWT — request passes with injected headers")
    void validJwt_passesAndInjectsHeaders() {
        String token = buildToken(60_000);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/timesheet/add")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        var capturedRequest =
                new org.springframework.http.server.reactive.ServerHttpRequest[1];

        StepVerifier.create(filter.filter(exchange, ex -> {   // ✅ ex = mutated exchange
            capturedRequest[0] = ex.getRequest();
            return Mono.empty();
        })).verifyComplete();

        assert "user@example.com"
                .equals(capturedRequest[0].getHeaders().getFirst("X-User-Email"));
        assert "EMPLOYEE"
                .equals(capturedRequest[0].getHeaders().getFirst("X-User-Role"));
        assert "42"
                .equals(capturedRequest[0].getHeaders().getFirst("X-User-Id"));
    }

    @Test
    @DisplayName("Expired JWT → 401 with expiry message")
    void expiredJwt_returns401() {
        String token = buildExpiredToken();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/timesheet/add")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();

        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
        String errorHeader = exchange.getResponse().getHeaders()
                .getFirst("X-Auth-Error");
        assert errorHeader != null && errorHeader.contains("expired");
    }

    @Test
    @DisplayName("Tampered JWT → 401 with invalid message")
    void invalidJwt_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/timesheet/add")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer this.is.not.a.valid.jwt")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty()))
                .verifyComplete();

        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
    }
    
    @Test
    @DisplayName("getOrder() returns -1 (highest priority)")
    void getOrder_returnsMinusOne() {
        assert filter.getOrder() == -1;
    }
}