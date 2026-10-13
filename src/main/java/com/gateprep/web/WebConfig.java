package com.gateprep.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor auth;
    private final AdminInterceptor admin;

    public WebConfig(AuthInterceptor auth, AdminInterceptor admin) {
        this.auth = auth;
        this.admin = admin;
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
