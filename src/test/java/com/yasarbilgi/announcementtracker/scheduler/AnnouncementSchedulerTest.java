package com.yasarbilgi.announcementtracker.scheduler;

import com.yasarbilgi.announcementtracker.service.AnnouncementService;
import com.yasarbilgi.announcementtracker.service.SettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementSchedulerTest {

    @Mock
    private AnnouncementService announcementService;

    @Mock
    private SettingsService settingsService;

    @InjectMocks
    private AnnouncementScheduler announcementScheduler;

    @Test
    @DisplayName("Zamanlayıcı aktif olduğunda tarama görevinin yürütülmesi")
    void scheduleScraping_Enabled_ShouldTriggerScrapeAll() {
        when(settingsService.isSchedulerEnabled()).thenReturn(true);
        when(settingsService.getScrapeIntervalMinutes()).thenReturn(30);
        when(announcementService.triggerScrapeAll()).thenReturn(List.of());

        announcementScheduler.scheduleScraping();

        verify(announcementService, times(1)).triggerScrapeAll();
    }

    @Test
    @DisplayName("Zamanlayıcı pasif olduğunda tarama görevinin atlanması")
    void scheduleScraping_Disabled_ShouldSkipScrape() {
        when(settingsService.isSchedulerEnabled()).thenReturn(false);

        announcementScheduler.scheduleScraping();

        verify(announcementService, never()).triggerScrapeAll();
    }

    @Test
    @DisplayName("Görevin TaskRegistrar'a tanımlanabilmesi")
    void configureTasks_ShouldRegisterTask() {
        ScheduledTaskRegistrar registrar = mock(ScheduledTaskRegistrar.class);

        announcementScheduler.configureTasks(registrar);

        verify(registrar, times(1)).addTriggerTask(any(Runnable.class), any());
    }
}
