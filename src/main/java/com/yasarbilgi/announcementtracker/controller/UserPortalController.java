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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> setPassword(@Valid @RequestBody SetPasswordRequestDto dto) {
        SubscriberResponseDto updated = subscriberService.setPasswordWithToken(dto);
        return ResponseEntity.ok(ApiResponseDto.ok("Şifreniz başarıyla oluşturuldu. Artık giriş yapabilirsiniz.", updated));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<UserLoginResponseDto>> login(@Valid @RequestBody UserLoginRequestDto dto) {
        UserLoginResponseDto response = subscriberService.loginUser(dto);
        return ResponseEntity.ok(ApiResponseDto.ok("Giriş başarılı.", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> getProfile(@RequestHeader("Authorization") String token) {
        SubscriberResponseDto user = subscriberService.validateUserToken(token);
        return ResponseEntity.ok(ApiResponseDto.ok("Kullanıcı profili getirildi.", user));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> updatePreferences(
            @RequestHeader("Authorization") String token,
            @RequestBody Set<SiteType> siteTypes) {
        SubscriberResponseDto user = subscriberService.validateUserToken(token);
        SubscriberResponseDto updated = subscriberService.updateSitePreferences(user.getId(), siteTypes);
        return ResponseEntity.ok(ApiResponseDto.ok("Takip tercihleri güncellendi.", updated));
    }

    @GetMapping("/me/announcements")
    public ResponseEntity<ApiResponseDto<Page<AnnouncementResponseDto>>> getMyAnnouncements(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) SiteType siteType,
            @org.springframework.data.web.PageableDefault(size = 10, sort = {"announcementDate", "id"}, direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        SubscriberResponseDto user = subscriberService.validateUserToken(token);
        Page<AnnouncementResponseDto> announcements = announcementService.getAnnouncementsForSites(user.getSubscribedSites(), siteType, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok("Duyurular listelendi.", announcements));
    }
}
