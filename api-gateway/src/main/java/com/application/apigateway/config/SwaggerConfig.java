package com.application.apigateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Swagger Aggregator for Spring Cloud Gateway (WebFlux).
 *
 * Each downstream service exposes its own /v3/api-docs at a prefixed path.
 * The gateway proxies those paths through its routes, so Swagger UI can
 * fetch them all from the single gateway origin (localhost:8080).
 */
@Configuration
public class SwaggerConfig {

    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {

        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        config.setDisplayRequestDuration(true);
        config.setDisplayOperationId(false);

        Set<SwaggerUrl> urls = new LinkedHashSet<>();

        urls.add(swaggerUrl("Auth Service",         "/auth/v3/api-docs"));
        urls.add(swaggerUrl("Timesheet Service",    "/timesheet/v3/api-docs"));
        urls.add(swaggerUrl("Leave Service",        "/leave/v3/api-docs"));
        urls.add(swaggerUrl("Admin Service",        "/admin/v3/api-docs"));

        config.setUrls(urls);
        return config;
    }

    private SwaggerUrl swaggerUrl(String name, String url) {
        SwaggerUrl swaggerUrl = new SwaggerUrl();
        swaggerUrl.setName(name);
        swaggerUrl.setUrl(url);
        return swaggerUrl;
    }
}
