package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.config.SessionCookieService;
import com.yasarbilgi.announcementtracker.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionCookieService sessionCookieService;

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String token = sessionCookieService.resolveAdminSession(servletRequest);
        authService.logout(token);
        sessionCookieService.clearAdminSession(servletResponse);

        return ResponseEntity.ok(ApiResponseDto.ok("Oturum kapatıldı"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<AdminUserDto>> getProfile(
            @AuthenticationPrincipal AdminUserDto user) {
        return ResponseEntity.ok(ApiResponseDto.ok("Oturum geçerli", user));
    }

}
