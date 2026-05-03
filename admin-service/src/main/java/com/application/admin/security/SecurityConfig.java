package com.application.admin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── Disable CSRF — stateless JWT, no session cookies ────────
            .csrf(csrf -> csrf.disable())

            // ── Disable CORS — gateway handles CORS with the browser ─────
            .cors(AbstractHttpConfigurer::disable)

            // ── Disable httpBasic — JWT only ─────────────────────────────
            .httpBasic(AbstractHttpConfigurer::disable)

            // ── Stateless session ─────────────────────────────────────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // ── Allow OPTIONS preflight forwarded by gateway ──────────
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Swagger public paths ──────────────────────────────────
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/admin/v3/api-docs",
                    "/admin/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
