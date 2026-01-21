package com.telekom.orderPlacement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson ObjectMapper configuration.
 * 
 * Configures JSON serialization/deserialization for:
 * - API request/response handling
 * - Kafka event serialization
 * - Date/time formatting (ISO-8601)
 */
@Configuration
public class JacksonConfig {
    
    /**
     * Primary ObjectMapper bean used throughout the application.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());
        
        // Use ISO-8601 format for dates (not timestamps)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Pretty print disabled for performance
        mapper.disable(SerializationFeature.INDENT_OUTPUT);
        
        // Don't fail on unknown properties (forward compatibility)
        mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, 
                false
        );
        
        return mapper;
    }
}
