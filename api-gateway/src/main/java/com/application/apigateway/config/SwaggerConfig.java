package com.application.apigateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
    	
        SwaggerUiConfigProperties config =
                new SwaggerUiConfigProperties();

        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl>
                urls = new HashSet<>();

        // ✅ Auth Service docs
        AbstractSwaggerUiConfigProperties.SwaggerUrl auth =
                new AbstractSwaggerUiConfigProperties.SwaggerUrl();
        auth.setName("Auth Service");
        auth.setUrl("/auth/v3/api-docs");
        urls.add(auth);

        // ✅ Timesheet Service docs
        AbstractSwaggerUiConfigProperties.SwaggerUrl timesheet =
                new AbstractSwaggerUiConfigProperties.SwaggerUrl();
        timesheet.setName("Timesheet Service");
        timesheet.setUrl("/timesheet/v3/api-docs");
        urls.add(timesheet);

        // ✅ Leave Service docs
        AbstractSwaggerUiConfigProperties.SwaggerUrl leave =
                new AbstractSwaggerUiConfigProperties.SwaggerUrl();
        leave.setName("Leave Service");
        leave.setUrl("/leave/v3/api-docs");
        urls.add(leave);

        // ✅ Admin Service docs
        AbstractSwaggerUiConfigProperties.SwaggerUrl admin =
                new AbstractSwaggerUiConfigProperties.SwaggerUrl();
        admin.setName("Admin Service");
        admin.setUrl("/admin/v3/api-docs");
        urls.add(admin);

        config.setUrls(urls);
        return config;
    }
}

