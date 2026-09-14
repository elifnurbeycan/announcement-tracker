package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.response.ScrapeSettingsDto;
import com.yasarbilgi.announcementtracker.entity.SystemSetting;
import com.yasarbilgi.announcementtracker.repository.SystemSettingRepository;
import com.yasarbilgi.announcementtracker.service.impl.SettingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @InjectMocks
    private SettingsServiceImpl settingsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(settingsService, "defaultIntervalMinutes", 60);
        ReflectionTestUtils.setField(settingsService, "defaultEnabled", true);
    }

    @Test
    @DisplayName("Veritabanında ayar yokken varsayılan 60 dakika döner")
    void getScrapeIntervalMinutes_DefaultValue() {
        when(systemSettingRepository.findById("SCRAPE_INTERVAL_MINUTES")).thenReturn(Optional.empty());

        int minutes = settingsService.getScrapeIntervalMinutes();

        assertEquals(60, minutes);
    }

    @Test
    @DisplayName("Veritabanında kayıtlı dakika ayarı başarıyla okunur")
    void getScrapeIntervalMinutes_CustomValue() {
        when(systemSettingRepository.findById("SCRAPE_INTERVAL_MINUTES"))
                .thenReturn(Optional.of(SystemSetting.builder().settingKey("SCRAPE_INTERVAL_MINUTES").settingValue("20").build()));

        int minutes = settingsService.getScrapeIntervalMinutes();

        assertEquals(20, minutes);
    }

    @Test
    @DisplayName("Tarama dakikasını güncelleme işlemi çalışır")
    void updateScrapeSettings_Success() {
        when(systemSettingRepository.findById(anyString())).thenReturn(Optional.empty());
        when(systemSettingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ScrapeSettingsDto updated = settingsService.updateScrapeSettings(20, true);

        assertEquals(20, updated.getIntervalMinutes());
        assertTrue(updated.isEnabled());
        verify(systemSettingRepository, times(3)).save(any(SystemSetting.class));
    }
}
