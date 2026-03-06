package com.socialmedia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow credentials (cookies, auth headers)
        config.setAllowCredentials(true);
        
        // Allow all origins (use specific origins in production for security)
        config.addAllowedOriginPattern("*");
        
        // Allow all headers
        config.addAllowedHeader("*");
        
        // Allow all HTTP methods
        config.addAllowedMethod("*");
        
        // Allow exposed headers
        config.addExposedHeader("*");
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

