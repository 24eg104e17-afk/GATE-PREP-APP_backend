package com.gateprep.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor auth;
    private final AdminInterceptor admin;
    private final String[] allowedOrigins;

    public WebConfig(AuthInterceptor auth, AdminInterceptor admin,
                     @Value("${app.cors.allowed-origins:http://localhost:5173}") String origins) {
        this.auth = auth;
        this.admin = admin;
        this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * The syllabus, question list and exam info stay public. Tests, history, progress and papers need a login.
     * The question bank editor (/api/admin) needs an administrator. Interceptors run in the order they are added.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auth).addPathPatterns(
                "/api/tests/**", "/api/attempts", "/api/attempts/**", "/api/progress", "/api/progress/**",
                "/api/papers", "/api/papers/**", "/api/admin/**");
        registry.addInterceptor(admin).addPathPatterns("/api/admin/**");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
