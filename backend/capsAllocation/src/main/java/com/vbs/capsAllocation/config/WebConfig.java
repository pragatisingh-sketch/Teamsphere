package com.vbs.capsAllocation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps /uploads/** to the physical directory on disk

        registry.addResourceHandler("/uploads/**")
//                .addResourceLocations("file:/home/vbs-tms/teamsphere/backend/capsAllocation/uploads/");
                .addResourceLocations("file:/home/piyush_mishra/teamsphere/backend/capsAllocation/uploads/");
    }
}
