package com.application.timesheet.security;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {
        http
            // ── Disable CSRF — stateless JWT, no session cookies ────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Disable CORS — gateway handles CORS with the browser ─────
            .cors(AbstractHttpConfigurer::disable)

            // ── Disable httpBasic — JWT only ─────────────────────────────
            .httpBasic(AbstractHttpConfigurer::disable)

            // ── Stateless session ─────────────────────────────────────────
            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // ── Allow OPTIONS preflight forwarded by gateway ──────────
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Swagger / Actuator public paths ───────────────────────
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html/**",
                    "/v3/api-docs/**",
                    "/timesheet/v3/api-docs",
                    "/timesheet/v3/api-docs/**",
                    "/actuator/**",
                    "/webjars/**"
                ).permitAll()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
