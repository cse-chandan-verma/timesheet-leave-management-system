package com.application.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;

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

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {  

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final List<String> PUBLIC_ROUTES = List.of(
            "/gateway/auth/login",
            "/gateway/auth/signup",
            "/gateway/admin/config/public",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        boolean isPublicRoute = PUBLIC_ROUTES.stream()
                .anyMatch(path::startsWith);

        if (isPublicRoute) {
            return chain.filter(exchange);   
        }

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return rejectRequest(exchange, "Authorization header is missing");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return rejectRequest(exchange, "Authorization header must start with Bearer");
        }
        String token = authHeader.substring(7);

        try {
            Claims claims = extractClaims(token);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Email", claims.getSubject())
                    .header("X-User-Role",  claims.get("role", String.class))
                    .header("X-User-Id",    String.valueOf(
                            claims.get("userId", Integer.class) != null
                            ? claims.get("userId", Integer.class) : ""))
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return rejectRequest(exchange, "JWT token has expired");
        } catch (SecurityException e) {
            return rejectRequest(exchange, "JWT signature is invalid");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            return rejectRequest(exchange, "JWT token is malformed");
        } catch (Exception e) {
            return rejectRequest(exchange, "JWT token validation failed");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Claims extractClaims(String token) {
        SecretKey signingKey = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("X-Auth-Error", reason);
        return response.setComplete();
    }
}