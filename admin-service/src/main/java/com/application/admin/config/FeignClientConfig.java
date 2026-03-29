package com.application.admin.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Global Feign interceptor that propagates all auth-related headers
 * from the incoming admin-service request to every outbound Feign call.
 *
 * Downstream services (timesheet-service, leave-service) authenticate via
 * either the X-User-* headers (gateway-style, Case 1 in their JwtAuthFilter)
 * or the Authorization header (Case 2). We forward BOTH so that either path works.
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;

            HttpServletRequest request = attributes.getRequest();

            // Forward Authorization header (Bearer JWT) for Case 2 path
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                requestTemplate.header("Authorization", authHeader);
            }

            // Forward X-User-* headers (injected by the gateway) for Case 1 path.
            // Downstream JwtAuthFilters prioritise these headers, so this is the
            // most reliable way to propagate identity through Feign calls.
            String userEmail = request.getHeader("X-User-Email");
            String userRole  = request.getHeader("X-User-Role");
            String userId    = request.getHeader("X-User-Id");

            if (userEmail != null) requestTemplate.header("X-User-Email", userEmail);
            if (userRole  != null) requestTemplate.header("X-User-Role",  userRole);
            if (userId    != null) requestTemplate.header("X-User-Id",    userId);
        };
    }
}
