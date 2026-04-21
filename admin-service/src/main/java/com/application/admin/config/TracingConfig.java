package com.application.admin.config;

import feign.Capability;
import feign.micrometer.MicrometerObservationCapability;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public Capability micrometerObservationCapability(ObservationRegistry registry) {
        return new MicrometerObservationCapability(registry);
    }
}
