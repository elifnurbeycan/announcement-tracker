package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.response.AnnouncementResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.AnnouncementService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.yasarbilgi.announcementtracker.config.SessionCookieService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserPortalController {

    private final SubscriberService subscriberService;
    private final AnnouncementService announcementService;
    private final SessionCookieService sessionCookieService;

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
