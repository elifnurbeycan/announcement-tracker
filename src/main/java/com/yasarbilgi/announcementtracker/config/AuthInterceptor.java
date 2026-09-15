package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Allow CORS pre-flight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // Public URLs
        if (uri.equals("/login.html") || uri.equals("/login") || uri.startsWith("/api/v1/auth/login") || uri.contains("/subscribers/unsubscribe")) {
            return true;
        }

        // Extract token from Header, Query, or Cookie
        String token = resolveToken(request);

        try {
            authService.validateToken(token);
            return true;
        } catch (Exception e) {
            log.warn("Unauthorized access attempt to {}: {}", uri, e.getMessage());

            if (uri.startsWith("/api/v1/")) {
                // API endpoints return 401 JSON
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Yetkisiz Erişim! Lütfen SüperAdmin olarak giriş yapın.\"}");
            } else {
                // HTML Page requests redirect to login.html
                response.sendRedirect("/login.html");
            }
            return false;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            return authHeader;
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("ADMIN_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return request.getParameter("token");
    }
}
