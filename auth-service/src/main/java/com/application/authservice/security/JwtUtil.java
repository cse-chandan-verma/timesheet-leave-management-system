package com.application.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.application.authservice.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // ── Generate Token ────────────────────────────────────────
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                // 0.12.x → .subject()  instead of  .setSubject()
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .claim("fullName", user.getFullName())
                .issuedAt(new Date())
                // 0.12.x → .issuedAt()  instead of  .setIssuedAt()
                .expiration(new Date(System.currentTimeMillis() + expiration))
                // 0.12.x → .expiration()  instead of  .setExpiration()
                .signWith(getSigningKey())
                // 0.12.x → no need to pass SignatureAlgorithm separately
                .compact();
    }

    // ── Extract Email ─────────────────────────────────────────
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    // ── Extract Role ──────────────────────────────────────────
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    // ── Extract User ID ───────────────────────────────────────
    public Long extractUserId(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    // ── Validate Token ────────────────────────────────────────
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Extract Claims (private) ──────────────────────────────
    private Claims extractClaims(String token) {
        return Jwts.parser()
                   .verifyWith(getSigningKey())
                   // 0.12.x → .verifyWith()  instead of  .setSigningKey()
                   .build()
                   .parseSignedClaims(token)
                   // 0.12.x → .parseSignedClaims()  instead of  .parseClaimsJws()
                   .getPayload();
                   // 0.12.x → .getPayload()  instead of  .getBody()
    }

    // ── Get Signing Key ───────────────────────────────────────
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));
    }
}