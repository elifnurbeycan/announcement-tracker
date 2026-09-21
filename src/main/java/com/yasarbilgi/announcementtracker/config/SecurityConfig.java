package com.yasarbilgi.announcementtracker.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomTokenAuthenticationFilter customTokenAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/login",
                    "/login.html",
                    "/admin-login",
                    "/admin-login.html",
                    "/user-login",
                    "/user-login.html",
                    "/set-password.html",
                    "/dashboard",
                    "/dashboard.html",
                    "/user-dashboard",
                    "/user-dashboard.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/api/v1/subscribers/unsubscribe"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/login",
                    "/api/v1/user/login",
                    "/api/v1/user/set-password",
                    "/api/v1/subscribers/register"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/announcements",
                    "/api/v1/announcements/**",
                    "/api/v1/sites"
                ).permitAll()
                .requestMatchers("/api/v1/user/**").hasRole("USER")
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/admin/**",
                    "/api/v1/settings/**",
                    "/api/v1/subscribers/**",
                    "/api/v1/departments/**"
                ).hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/announcements/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(customTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
