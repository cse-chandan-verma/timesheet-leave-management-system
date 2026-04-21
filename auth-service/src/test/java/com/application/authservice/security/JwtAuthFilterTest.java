package com.application.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter — Unit Tests")
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @DisplayName("doFilterInternal() — no header skips filter")
    void doFilter_NoHeader_Success() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal() — valid token sets authentication")
    void doFilter_ValidToken_Success() throws ServletException, IOException {
        String token = "valid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractEmail(token)).thenReturn("user@test.com");
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_EMPLOYEE");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        verify(filterChain).doFilter(request, response);
    }
}
