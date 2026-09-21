package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.service.AuthService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionCookieService sessionCookieService;
    private final AuthService authService;
    private final SubscriberService subscriberService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String adminToken = sessionCookieService.resolveAdminSession(request);
            if (adminToken != null && !adminToken.isBlank()) {
                try {
                    AdminUserDto adminDto = authService.validateToken(adminToken);
                    var authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
                    );
                    var auth = new UsernamePasswordAuthenticationToken(adminDto, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    log.debug("Invalid admin session token: {}", e.getMessage());
                }
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String userToken = sessionCookieService.resolveUserSession(request);
                if (userToken != null && !userToken.isBlank()) {
                    try {
                        SubscriberResponseDto userDto = subscriberService.validateUserToken(userToken);
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                        var auth = new UsernamePasswordAuthenticationToken(userDto, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch (Exception e) {
                        log.debug("Invalid user session token: {}", e.getMessage());
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
