package com.yasarbilgi.announcementtracker.scheduler;

import com.yasarbilgi.announcementtracker.service.AnnouncementService;
import com.yasarbilgi.announcementtracker.service.SettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    @DisplayName("İlk taramanın kısa başlangıç gecikmesiyle planlanması")
    void configureTasks_FirstRun_ShouldUseInitialDelay() {
        ReflectionTestUtils.setField(announcementScheduler, "initialDelaySeconds", 30L);
        ScheduledTaskRegistrar registrar = mock(ScheduledTaskRegistrar.class);
        org.mockito.ArgumentCaptor<Trigger> triggerCaptor = org.mockito.ArgumentCaptor.forClass(Trigger.class);
        TriggerContext context = mock(TriggerContext.class);
        when(context.lastCompletion()).thenReturn(null);

        announcementScheduler.configureTasks(registrar);
        verify(registrar).addTriggerTask(any(Runnable.class), triggerCaptor.capture());

        Instant before = Instant.now().plusSeconds(29);
        Instant nextExecution = triggerCaptor.getValue().nextExecution(context);
        Instant after = Instant.now().plusSeconds(31);

        assertThat(nextExecution).isBetween(before, after);
    }

    @Test
    @DisplayName("Sonraki taramanın güncel dinamik periyoda göre planlanması")
    void configureTasks_NextRun_ShouldUseCurrentInterval() {
        ScheduledTaskRegistrar registrar = mock(ScheduledTaskRegistrar.class);
        org.mockito.ArgumentCaptor<Trigger> triggerCaptor = org.mockito.ArgumentCaptor.forClass(Trigger.class);
        TriggerContext context = mock(TriggerContext.class);
        Instant lastCompletion = Instant.parse("2026-09-23T08:00:00Z");
        when(context.lastCompletion()).thenReturn(lastCompletion);
        when(settingsService.getScrapeIntervalMinutes()).thenReturn(15);

        announcementScheduler.configureTasks(registrar);
        verify(registrar).addTriggerTask(any(Runnable.class), triggerCaptor.capture());

        assertThat(triggerCaptor.getValue().nextExecution(context))
                .isEqualTo(lastCompletion.plusSeconds(15 * 60L));
    }

    @Test
    @DisplayName("Geçersiz periyodun 60 dakikaya normalleştirilmesi")
    void effectiveIntervalMinutes_InvalidValue_ShouldUseDefault() {
        assertThat(announcementScheduler.effectiveIntervalMinutes(0)).isEqualTo(60);
        assertThat(announcementScheduler.effectiveIntervalMinutes(-5)).isEqualTo(60);
        assertThat(announcementScheduler.effectiveIntervalMinutes(25)).isEqualTo(25);
    }

    @Test
    @DisplayName("Tarama hatasının zamanlayıcı iş parçacığını durdurmaması")
    void scheduleScraping_ServiceFailure_ShouldBeHandled() {
        when(settingsService.isSchedulerEnabled()).thenReturn(true);
        when(settingsService.getScrapeIntervalMinutes()).thenReturn(30);
        when(announcementService.triggerScrapeAll()).thenThrow(new RuntimeException("Bağlantı hatası"));

        assertThatCode(() -> announcementScheduler.scheduleScraping()).doesNotThrowAnyException();
        verify(announcementService).triggerScrapeAll();
    }
}
