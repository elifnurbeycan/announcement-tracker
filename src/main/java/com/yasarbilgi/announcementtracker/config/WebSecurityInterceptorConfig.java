package com.yasarbilgi.announcementtracker.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebSecurityInterceptorConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/dashboard",
                        "/dashboard.html",
                        "/api/v1/settings/**"
                )
                .excludePathPatterns(
                        "/login",
                        "/login.html",
                        "/admin-login",
                        "/admin-login.html",
                        "/user-login.html",
                        "/set-password.html",
                        "/user-dashboard.html",
                        "/index.html",
                        "/",
                        "/api/v1/auth/login",
                        "/api/v1/subscribers/unsubscribe",
                        "/api/v1/user/**"
                );
    }
}
