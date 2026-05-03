package com.application.authservice.config;

import com.application.authservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.application.authservice.repository.UserRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter  jwtAuthFilter;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            // ── Disable CSRF — stateless JWT, no session cookies ────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Disable CORS in auth-service — gateway handles CORS ──────
            // auth-service is ONLY accessed via the API Gateway (internal).
            // CORS negotiation happens at the gateway level with the browser.
            // Enabling Spring Security CORS here causes 403 on forwarded reqs.
            .cors(AbstractHttpConfigurer::disable)

            // ── Disable httpBasic — we use JWT only ──────────────────────
            .httpBasic(AbstractHttpConfigurer::disable)

            // ── Stateless session — no HttpSession ever created ──────────
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // ── Allow OPTIONS preflight (forwarded by gateway) ───────
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Public endpoints — no token needed ──────────────────
                .requestMatchers(
                    "/auth/login",
                    "/auth/register",
                    "/auth/forgot-password"
                ).permitAll()

                // ── Internal service-to-service endpoint — no JWT ────────
                .requestMatchers("/auth/internal/**").permitAll()

                // ── Swagger UI — public for development ─────────────────
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/auth/v3/api-docs/**",
                    "/auth/v3/api-docs",
                    "/webjars/**"
                ).permitAll()

                // ── Actuator health ──────────────────────────────────────
                .requestMatchers("/actuator/health").permitAll()

                // ── Everything else requires valid JWT ───────────────────
                // Fine-grained role checks are done by @PreAuthorize
                // on each method in the controller
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository
                .findByEmail(username)
                .map(user -> org.springframework.security.core
                        .userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}