package com.application.authservice.security;

import org.bouncycastle.jcajce.BCFKSLoadStoreParameter.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.application.authservice.model.User;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration}")
	private long expiration;

	public String generateToken(User user) {
		return Jwts.builder()

				.setSubject(user.getEmail()).claim("role", user.getRole().name()).claim("userId", user.getId())
				.claim("fullName", user.getFullName()).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
	}

	public String extractEmail(String token) {
		return extractClaims(token).getSubject();
	}

	public String extractRole(String token) {
		return extractClaims(token).get("role", String.class);
	}

	public Long extractUserId(String token) {
		return extractClaims(token).get("userId", Long.class);
	}

	public boolean validateToken(String token) {
		try {
			extractClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private Claims extractClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
	}

	private Key getSigningKey() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

		return Keys.hmacShaKeyFor(keyBytes);
	}
}