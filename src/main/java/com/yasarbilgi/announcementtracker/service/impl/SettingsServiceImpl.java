package com.yasarbilgi.announcementtracker.service.impl;

import com.yasarbilgi.announcementtracker.dto.response.ScrapeSettingsDto;
import com.yasarbilgi.announcementtracker.entity.SystemSetting;
import com.yasarbilgi.announcementtracker.repository.SystemSettingRepository;
import com.yasarbilgi.announcementtracker.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dinamik sistem ayarlarının veritabanında saklanmasını ve okunmasını sağlayan servis uygulaması.
 * Dakika ve saat bazlı tarama zamanlamalarını destekler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsServiceImpl implements SettingsService {

    private final SystemSettingRepository systemSettingRepository;

    private static final String KEY_INTERVAL_MINUTES = "SCRAPE_INTERVAL_MINUTES";
    private static final String KEY_INTERVAL_HOURS = "SCRAPE_INTERVAL_HOURS";
    private static final String KEY_SCHEDULER_ENABLED = "SCRAPE_SCHEDULER_ENABLED";

    @Value("${announcement.tracker.interval-minutes:60}")
    private int defaultIntervalMinutes;

    @Value("${announcement.tracker.enabled:true}")
    private boolean defaultEnabled;

    @Override
    public int getScrapeIntervalMinutes() {
        return systemSettingRepository.findById(KEY_INTERVAL_MINUTES)
                .map(setting -> {
                    try {
                        int val = Integer.parseInt(setting.getSettingValue());
                        return val > 0 ? val : defaultIntervalMinutes;
                    } catch (NumberFormatException e) {
                        return defaultIntervalMinutes;
                    }
                })
                .orElseGet(() -> systemSettingRepository.findById(KEY_INTERVAL_HOURS)
                        .map(setting -> {
                            try {
                                int hours = Integer.parseInt(setting.getSettingValue());
                                return hours > 0 ? hours * 60 : defaultIntervalMinutes;
                            } catch (NumberFormatException e) {
                                return defaultIntervalMinutes;
                            }
                        })
                        .orElse(defaultIntervalMinutes));
    }

    @Override
    public int getScrapeIntervalHours() {
        return getScrapeIntervalMinutes() / 60;
    }

    @Override
    public boolean isSchedulerEnabled() {
        return systemSettingRepository.findById(KEY_SCHEDULER_ENABLED)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue()))
                .orElse(defaultEnabled);
    }

    @Override
    public ScrapeSettingsDto getScrapeSettings() {
        int minutes = getScrapeIntervalMinutes();
        return ScrapeSettingsDto.builder()
                .intervalMinutes(minutes)
                .intervalHours(minutes / 60)
                .enabled(isSchedulerEnabled())
                .build();
    }

    @Override
    @Transactional
    public ScrapeSettingsDto updateScrapeSettings(int intervalMinutes, Boolean enabled) {
        if (intervalMinutes < 1) {
            intervalMinutes = 1; // Minimum 1 dakika
        }

        saveSetting(KEY_INTERVAL_MINUTES, String.valueOf(intervalMinutes));
        saveSetting(KEY_INTERVAL_HOURS, String.valueOf(intervalMinutes / 60));

        boolean isEnabled = enabled != null ? enabled : isSchedulerEnabled();
        saveSetting(KEY_SCHEDULER_ENABLED, String.valueOf(isEnabled));

        log.info("Tarama zamanlama ayarları güncellendi: Periyot = {} dakika, Aktif = {}", intervalMinutes, isEnabled);

        return ScrapeSettingsDto.builder()
                .intervalMinutes(intervalMinutes)
                .intervalHours(intervalMinutes / 60)
                .enabled(isEnabled)
                .build();
    }

    private void saveSetting(String key, String value) {
        SystemSetting setting = systemSettingRepository.findById(key)
                .orElseGet(() -> SystemSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        systemSettingRepository.save(setting);
    }
}
