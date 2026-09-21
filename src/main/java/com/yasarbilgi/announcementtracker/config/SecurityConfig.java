package com.yasarbilgi.announcementtracker.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OidcLoginSuccessHandler oidcLoginSuccessHandler;
    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;
    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(
            @Value("${app.security.session-cookie.secure:false}") boolean secure,
            @Value("${app.security.session-cookie.same-site:Lax}") String sameSite) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.secure(secure).sameSite(sameSite).path("/"));
        return repository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        http
            .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
            .cors(Customizer.withDefaults())
            .oauth2Login(oauth -> oauth
                    .successHandler(oidcLoginSuccessHandler)
                    .failureUrl("/user-login.html?ssoError=authentication"))
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                    .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)))
            .exceptionHandling(exceptions -> exceptions
                    .defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            PathPatternRequestMatcher.pathPattern("/api/**")))
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
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/api/v1/subscribers/unsubscribe",
                    "/api/v1/subscribers/unsubscribe/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/login",
                    "/api/v1/user/login",
                    "/api/v1/user/set-password"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/auth/mode",
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
                .requestMatchers("/api/v1/announcements/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
