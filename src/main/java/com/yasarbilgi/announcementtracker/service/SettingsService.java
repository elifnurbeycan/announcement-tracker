package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.response.ScrapeSettingsDto;

/**
 * Dinamik sistem ayarlarını yöneten servis arayüzü.
 */
public interface SettingsService {

    int getScrapeIntervalMinutes();

    boolean isSchedulerEnabled();

    ScrapeSettingsDto getScrapeSettings();

    ScrapeSettingsDto updateScrapeSettings(int intervalMinutes, Boolean enabled);
}
