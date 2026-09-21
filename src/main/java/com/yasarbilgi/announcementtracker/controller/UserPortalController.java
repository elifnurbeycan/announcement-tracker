package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.request.SetPasswordRequestDto;
import com.yasarbilgi.announcementtracker.dto.request.UserLoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.AnnouncementResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.UserLoginResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.AnnouncementService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.yasarbilgi.announcementtracker.config.SessionCookieService;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserPortalController {

    private final SubscriberService subscriberService;
    private final AnnouncementService announcementService;
    private final SessionCookieService sessionCookieService;

    @Value("${app.security.local-login-enabled:true}")
    private boolean localLoginEnabled = true;

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> setPassword(@Valid @RequestBody SetPasswordRequestDto dto) {
        SubscriberResponseDto updated = subscriberService.setPasswordWithToken(dto);
        return ResponseEntity.ok(ApiResponseDto.ok("Şifreniz başarıyla oluşturuldu. Artık giriş yapabilirsiniz.", updated));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<UserLoginResponseDto>> login(
            @Valid @RequestBody UserLoginRequestDto dto,
            HttpServletResponse servletResponse) {
        if (!localLoginEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Canlı ortamda yalnızca kurumsal SSO girişi kullanılabilir.");
        }
        UserLoginResponseDto response = subscriberService.loginUser(dto);
        sessionCookieService.setUserSession(servletResponse, response.getToken());
        return ResponseEntity.ok(ApiResponseDto.ok("Giriş başarılı.", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        subscriberService.logoutUser(sessionCookieService.resolveUserSession(request));
        sessionCookieService.clearUserSession(response);
        return ResponseEntity.ok(ApiResponseDto.ok("Oturum kapatıldı."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> getProfile(
            @AuthenticationPrincipal SubscriberResponseDto user) {
        return ResponseEntity.ok(ApiResponseDto.ok("Kullanıcı profili getirildi.", user));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> updatePreferences(
            @AuthenticationPrincipal SubscriberResponseDto user,
            @RequestBody Set<SiteType> siteTypes) {
        SubscriberResponseDto updated = subscriberService.updateSitePreferences(user.getId(), siteTypes);
        return ResponseEntity.ok(ApiResponseDto.ok("Takip tercihleri güncellendi.", updated));
    }

    @GetMapping("/me/announcements")
    public ResponseEntity<ApiResponseDto<Page<AnnouncementResponseDto>>> getMyAnnouncements(
            @AuthenticationPrincipal SubscriberResponseDto user,
            @RequestParam(required = false) SiteType siteType,
            @org.springframework.data.web.PageableDefault(size = 10, sort = {"announcementDate", "id"}, direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<AnnouncementResponseDto> announcements = announcementService.getAnnouncementsForSites(user.getEffectiveSites(), siteType, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok("Duyurular listelendi.", announcements));
    }
}
