package com.application.authservice.security;

import com.application.authservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil — Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "my-very-long-and-super-secure-secret-key-12345";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
    }

    @Test
    @DisplayName("generateToken() and extraction — success")
    void tokenOperations_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setFullName("John Doe");
        user.setRole(User.Role.EMPLOYEE);

        String token = jwtUtil.generateToken(user);
        assertThat(token).isNotBlank();

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@test.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("EMPLOYEE");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }
}
