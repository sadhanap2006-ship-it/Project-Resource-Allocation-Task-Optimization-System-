package com.sist.rms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Two small pieces of configuration:
 *
 * 1. CORS - so the HTML pages can call the API even when they are opened
 *    from a different port (for example the VS Code Live Server on 5500).
 *
 * 2. Static files - so that http://localhost:8080/ serves the pages in the
 *    frontend/ folder directly. Both paths are registered because the app
 *    may be started from the repository root or from the backend/ folder.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "file:./frontend/",        // started from the repository root
                        "file:../frontend/",       // started from the backend/ folder
                        "classpath:/static/");
    }
}
