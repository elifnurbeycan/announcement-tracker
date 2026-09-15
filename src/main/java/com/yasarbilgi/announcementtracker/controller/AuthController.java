package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.request.LoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;
import com.yasarbilgi.announcementtracker.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse servletResponse) {

        LoginResponseDto response = authService.login(request);

        // Set ADMIN_TOKEN cookie for browser page authentication
        Cookie cookie = new Cookie("ADMIN_TOKEN", response.getToken());
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        cookie.setHttpOnly(false); // Accessible by JS for Auth headers if needed
        servletResponse.addCookie(cookie);

        return ResponseEntity.ok(ApiResponseDto.ok("Giriş başarılı", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String tokenHeader,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String token = resolveToken(tokenHeader, servletRequest);
        authService.logout(token);

        // Clear ADMIN_TOKEN cookie
        Cookie cookie = new Cookie("ADMIN_TOKEN", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        servletResponse.addCookie(cookie);

        return ResponseEntity.ok(ApiResponseDto.ok("Oturum kapatıldı"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<AdminUserDto>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String tokenHeader,
            HttpServletRequest servletRequest) {

        String token = resolveToken(tokenHeader, servletRequest);
        AdminUserDto user = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponseDto.ok("Oturum geçerli", user));
    }

    private String resolveToken(String header, HttpServletRequest request) {
        if (header != null && !header.isBlank()) {
            return header;
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
