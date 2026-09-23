package com.yasarbilgi.announcementtracker.scheduler;

import com.yasarbilgi.announcementtracker.service.AnnouncementService;
import com.yasarbilgi.announcementtracker.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Belirli zaman aralıklarıyla duyuru sitelerini otomatik olarak tarayan zamanlayıcı sınıf.
 * Tarama periyodu veritabanındaki dinamik ayarlardan dakika cinsinden anlık olarak okunur.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementScheduler implements SchedulingConfigurer {

    private static final int DEFAULT_INTERVAL_MINUTES = 60;

    private final AnnouncementService announcementService;
    private final SettingsService settingsService;

    @Value("${announcement.tracker.scheduler.initial-delay-seconds:30}")
    private long initialDelaySeconds;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                this::scheduleScraping,
                triggerContext -> {
                    Instant lastCompletion = triggerContext.lastCompletion();
                    if (lastCompletion == null) {
                        long effectiveInitialDelay = Math.max(0, initialDelaySeconds);
                        Instant firstRun = Instant.now().plusSeconds(effectiveInitialDelay);
                        log.info("İlk otomatik duyuru taraması {} saniye sonra çalışacak: {}",
                                effectiveInitialDelay, firstRun);
                        return firstRun;
                    }
                    int intervalMinutes = effectiveIntervalMinutes(settingsService.getScrapeIntervalMinutes());
                    Instant nextRun = lastCompletion.plus(Duration.ofMinutes(intervalMinutes));
                    log.info("Sonraki otomatik duyuru taraması {} dakika sonra çalışacak: {}",
                            intervalMinutes, nextRun);
                    return nextRun;
                }
        );
    }

    public void scheduleScraping() {
        if (!settingsService.isSchedulerEnabled()) {
            log.info("Duyuru takip zamanlayıcısı ayarlar üzerinden devredışı bırakılmış.");
            return;
        }

        int intervalMinutes = effectiveIntervalMinutes(settingsService.getScrapeIntervalMinutes());
        log.info("Zamanlanmış duyuru tarama görevi başlatılıyor (Periyot: {} dakikada bir)...", intervalMinutes);
        try {
            var newItems = announcementService.triggerScrapeAll();
            log.info("Zamanlanmış tarama tamamlandı. İşlenen yeni duyuru sayısı: {}", newItems.size());
        } catch (Exception e) {
            log.error("Zamanlanmış tarama işlemi sırasında hata oluştu: {}", e.getMessage(), e);
        }
    }

    int effectiveIntervalMinutes(int configuredIntervalMinutes) {
        return configuredIntervalMinutes > 0 ? configuredIntervalMinutes : DEFAULT_INTERVAL_MINUTES;
    }
}
