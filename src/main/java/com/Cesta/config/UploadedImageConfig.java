package com.Cesta.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class UploadedImageConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path uploadFolder = Path.of("uploads")
                .toAbsolutePath()
                .normalize();

        String location = uploadFolder.toUri().toString();

        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}