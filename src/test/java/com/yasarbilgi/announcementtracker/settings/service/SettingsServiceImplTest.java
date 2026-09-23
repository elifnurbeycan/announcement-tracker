package com.yasarbilgi.announcementtracker.settings.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceImplTest {

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
    @DisplayName("Varsayılan tarama aralığı dakika değerini okuma")
    void getScrapeIntervalMinutes_DefaultValue_ShouldReturn60() {
        when(systemSettingRepository.findById("SCRAPE_INTERVAL_MINUTES")).thenReturn(Optional.empty());

        int minutes = settingsService.getScrapeIntervalMinutes();

        assertThat(minutes).isEqualTo(60);
    }

    @Test
    @DisplayName("Veritabanından özel tarama aralığı dakika değerini okuma")
    void getScrapeIntervalMinutes_CustomValue_ShouldReturnDbValue() {
        SystemSetting setting = SystemSetting.builder()
                .settingKey("SCRAPE_INTERVAL_MINUTES")
                .settingValue("30")
                .build();
        when(systemSettingRepository.findById("SCRAPE_INTERVAL_MINUTES")).thenReturn(Optional.of(setting));

        int minutes = settingsService.getScrapeIntervalMinutes();

        assertThat(minutes).isEqualTo(30);
    }

    @Test
    @DisplayName("Tarama ayarlarını güncelleme ve veritabanına kaydetme")
    void updateScrapeSettings_ShouldSaveToDb() {
        when(systemSettingRepository.findById(anyString())).thenReturn(Optional.empty());

        ScrapeSettingsDto dto = settingsService.updateScrapeSettings(20, true);

        assertThat(dto.getIntervalMinutes()).isEqualTo(20);
        assertThat(dto.isEnabled()).isTrue();
        verify(systemSettingRepository, atLeastOnce()).save(any(SystemSetting.class));
    }

    @Test
    @DisplayName("Bir dakikadan küçük tarama aralığı reddedilmeli")
    void updateScrapeSettings_InvalidInterval_ShouldThrowException() {
        assertThatThrownBy(() -> settingsService.updateScrapeSettings(0, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("en az 1 dakika");
        verifyNoInteractions(systemSettingRepository);
    }

    @Test
    @DisplayName("Geçersiz aktiflik değeri varsayılan ayara dönmeli")
    void isSchedulerEnabled_InvalidValue_ShouldUseDefault() {
        SystemSetting setting = SystemSetting.builder()
                .settingKey("SCRAPE_SCHEDULER_ENABLED")
                .settingValue("bozuk-deger")
                .build();
        when(systemSettingRepository.findById("SCRAPE_SCHEDULER_ENABLED"))
                .thenReturn(Optional.of(setting));

        assertThat(settingsService.isSchedulerEnabled()).isTrue();
    }
}
