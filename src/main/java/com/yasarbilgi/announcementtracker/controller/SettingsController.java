package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.ScrapeSettingsDto;
import com.yasarbilgi.announcementtracker.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Dinamik sistem ve tarama ayarları için REST denetleyici sınıfı.
 * Dakika bazlı periyot güncellemelerini destekler.
 */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/scrape")
    public ResponseEntity<ApiResponseDto<ScrapeSettingsDto>> getScrapeSettings() {
        ScrapeSettingsDto settings = settingsService.getScrapeSettings();
        return ResponseEntity.ok(ApiResponseDto.ok("Tarama ayarları başarıyla getirildi", settings));
    }

    @PostMapping("/scrape")
    public ResponseEntity<ApiResponseDto<ScrapeSettingsDto>> updateScrapeSettings(
            @RequestParam(required = false) Integer intervalMinutes,
            @RequestParam(required = false) Integer intervalHours,
            @RequestParam(required = false) Boolean enabled) {

        int minutes = 60;
        if (intervalMinutes != null && intervalMinutes > 0) {
            minutes = intervalMinutes;
        } else if (intervalHours != null && intervalHours > 0) {
            minutes = intervalHours * 60;
        }

        ScrapeSettingsDto updated = settingsService.updateScrapeSettings(minutes, enabled);
        return ResponseEntity.ok(ApiResponseDto.ok(
                "Tarama aralığı başarıyla " + updated.getIntervalMinutes() + " dakika olarak güncellendi.",
                updated
        ));
    }
}
