package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.request.LoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;
import com.yasarbilgi.announcementtracker.config.SessionCookieService;
import com.yasarbilgi.announcementtracker.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionCookieService sessionCookieService;

    @Value("${app.security.local-login-enabled:true}")
    private boolean localLoginEnabled = true;

    @Value("${app.security.sso-enabled:false}")
    private boolean ssoEnabled = false;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse servletResponse) {

        if (!localLoginEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Canlı ortamda yalnızca kurumsal SSO girişi kullanılabilir.");
        }

        LoginResponseDto response = authService.login(request);
        sessionCookieService.setAdminSession(servletResponse, response.getToken());

        return ResponseEntity.ok(ApiResponseDto.ok("Giriş başarılı", response));
    }

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

    @GetMapping("/mode")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getLoginMode() {
        return ResponseEntity.ok(ApiResponseDto.ok("Giriş modu", Map.of(
                "localLoginEnabled", localLoginEnabled,
                "ssoEnabled", ssoEnabled,
                "ssoLoginUrl", "/oauth2/authorization/keycloak"
        )));
    }
}
