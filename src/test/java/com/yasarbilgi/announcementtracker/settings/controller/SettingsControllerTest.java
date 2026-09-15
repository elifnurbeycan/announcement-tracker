package com.yasarbilgi.announcementtracker.settings.controller;

import com.yasarbilgi.announcementtracker.controller.SettingsController;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.ScrapeSettingsDto;
import com.yasarbilgi.announcementtracker.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @Mock
    private SettingsService settingsService;

    @InjectMocks
    private SettingsController settingsController;

    private ScrapeSettingsDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = ScrapeSettingsDto.builder()
                .intervalMinutes(30)
                .intervalHours(0)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/settings/scrape - Tarama ayarlarını okuma HTTP 200")
    void getScrapeSettings_ShouldReturnSettings() {
        when(settingsService.getScrapeSettings()).thenReturn(sampleDto);

        ResponseEntity<ApiResponseDto<ScrapeSettingsDto>> response = settingsController.getScrapeSettings();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getIntervalMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("POST /api/v1/settings/scrape - Tarama periyodunu güncelleme HTTP 200")
    void updateScrapeSettings_ShouldUpdateAndReturnNewSettings() {
        when(settingsService.updateScrapeSettings(anyInt(), anyBoolean())).thenReturn(sampleDto);

        ResponseEntity<ApiResponseDto<ScrapeSettingsDto>> response = 
                settingsController.updateScrapeSettings(30, null, true);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
