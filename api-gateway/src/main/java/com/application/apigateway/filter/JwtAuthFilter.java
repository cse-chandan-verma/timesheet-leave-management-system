package com.application.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/forgot-password",
            "/auth/validate",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/auth/v3/api-docs",
            "/timesheet/v3/api-docs",
            "/leave/v3/api-docs",
            "/admin/v3/api-docs");

    @Override
    public int getOrder() {
        return -2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
            GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        // ── Step 1: Allow OPTIONS preflight requests ──────────────────────
        // Browser sends OPTIONS before every real request to check CORS.
        // If we block it here, CORS headers never reach the browser.
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        // ── Step 2: Allow public paths without token ──────────────────────
        String pathToCheck = path;
        if (pathToCheck.startsWith("/gateway")) {
            pathToCheck = pathToCheck.substring(8);
        }

        boolean isPublic = PUBLIC_PATHS.stream()
                .anyMatch(pathToCheck::startsWith);

        if (isPublic) {
            return chain.filter(exchange);
        }

        // ── Step 3: Validate JWT for protected paths ──────────────────────
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("X-Auth-Error",
                    "Authorization header is missing");
            return response.setComplete();
        }

        String authHeader = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("X-Auth-Error",
                    "Authorization header must start with Bearer");
            return response.setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = extractClaims(token);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Email", claims.getSubject())
                    .header("X-User-Role",
                            claims.get("role", String.class))
                    .header("X-User-Id",
                            String.valueOf(claims.get("userId")))
                    .build();

            return chain.filter(
                    exchange.mutate()
                            .request(modifiedRequest)
                            .build());

        } catch (ExpiredJwtException e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("X-Auth-Error", "Token expired");
            return response.setComplete();
        } catch (JwtException e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("X-Auth-Error", "Token invalid");
            return response.setComplete();
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("X-Auth-Error",
                    "Token validation failed");
            return response.setComplete();
        }
    }

    private Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}