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
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT AUTH FILTER — Compatible with JJWT 0.11.x AND 0.12.x
 *
 * Uses GlobalFilter with a public path whitelist.
 * No JwtAuthFilterFactory needed.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Public paths — no token required
    private static final List<String> PUBLIC_PATHS = List.of(
            "/gateway/auth/login",
            "/gateway/auth/register",
            "/gateway/auth/validate",
            "/gateway/auth/users",

            // Swagger UI paths for all services
            "/gateway/timesheet/v3/api-docs",
            "/gateway/timesheet/swagger-ui/**",
            "/gateway/leave/v3/api-docs",
            "/gateway/leave/swagger-ui/**",
            "/gateway/admin/v3/api-docs",
            "/gateway/admin/swagger-ui/**"
    );

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        String requestPath = exchange.getRequest()
                                     .getURI()
                                     .getPath();

        // Skip JWT check for public paths
        boolean isPublic = PUBLIC_PATHS.stream()
                .anyMatch(requestPath::startsWith);

        if (isPublic) {
            return chain.filter(exchange);
        }

        // Protected path — check token
        ServerHttpRequest request = exchange.getRequest();

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return rejectRequest(exchange, "Authorization header is missing");
        }

        String authHeader = request.getHeaders()
                                   .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return rejectRequest(exchange,
                    "Authorization header must start with Bearer");
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
                    exchange.mutate().request(modifiedRequest).build());

        } catch (ExpiredJwtException e) {
            return rejectRequest(exchange, "JWT token has expired");
        } catch (JwtException e) {
            return rejectRequest(exchange, "JWT token is invalid");
        } catch (Exception e) {
            return rejectRequest(exchange, "JWT token validation failed");
        }
    }

    private Claims extractClaims(String token) {

        SecretKey signingKey = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8));

        // ── JJWT 0.12.x API ──────────────────────────────────
        return Jwts.parser()
                   .verifyWith(signingKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();

        // ── JJWT 0.11.x API (old — use this if you downgrade) ─
        // return Jwts.parserBuilder()
        //            .setSigningKey(signingKey)
        //            .build()
        //            .parseClaimsJws(token)
        //            .getBody();
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange,
                                      String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("X-Auth-Error", reason);
        return response.setComplete();
    }
}