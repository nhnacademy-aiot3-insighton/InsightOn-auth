package com.nhnacademy.insightonauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/api/v1/auth/login").setViewName("forward:/login.html");
        registry.addViewController("/api/v1/auth/signup").setViewName("forward:/signup.html");
    }
}
