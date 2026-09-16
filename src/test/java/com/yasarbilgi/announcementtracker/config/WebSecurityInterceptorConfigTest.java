package com.yasarbilgi.announcementtracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSecurityInterceptorConfigTest {

    @Mock
    private AuthInterceptor authInterceptor;

    @Mock
    private InterceptorRegistry registry;

    @Mock
    private InterceptorRegistration registration;

    @InjectMocks
    private WebSecurityInterceptorConfig webSecurityInterceptorConfig;

    @Test
    @DisplayName("Interceptor registry'ye AuthInterceptor başarıyla eklenmeli")
    void addInterceptors_ShouldRegisterAuthInterceptor() {
        when(registry.addInterceptor(authInterceptor)).thenReturn(registration);
        when(registration.addPathPatterns(any(String[].class))).thenReturn(registration);
        when(registration.excludePathPatterns(any(String[].class))).thenReturn(registration);

        webSecurityInterceptorConfig.addInterceptors(registry);

        verify(registry).addInterceptor(authInterceptor);
        verify(registration).addPathPatterns("/dashboard", "/dashboard.html", "/api/v1/settings/**");
        verify(registration).excludePathPatterns(
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
