package com.application.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Swagger Aggregator for Spring Cloud Gateway (WebFlux).
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("/gateway").description("Default Gateway Server")
                ));
    }

    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {

        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        config.setDisplayRequestDuration(true);

        Set<SwaggerUrl> urls = new LinkedHashSet<>();

        urls.add(swaggerUrl("Auth Service",         "/gateway/auth/v3/api-docs"));
        urls.add(swaggerUrl("Timesheet Service",    "/gateway/timesheet/v3/api-docs"));
        urls.add(swaggerUrl("Leave Service",        "/gateway/leave/v3/api-docs"));
        urls.add(swaggerUrl("Admin Service",        "/gateway/admin/v3/api-docs"));

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
