package com.yasarbilgi.announcementtracker.announcement.service;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.dto.response.AnnouncementResponseDto;
import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.entity.Subscriber;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ResourceNotFoundException;
import com.yasarbilgi.announcementtracker.repository.AnnouncementRepository;
import com.yasarbilgi.announcementtracker.repository.SubscriberRepository;
import com.yasarbilgi.announcementtracker.service.EmailService;
import com.yasarbilgi.announcementtracker.service.AnnouncementPersistenceService;
import com.yasarbilgi.announcementtracker.service.NotificationOutboxService;
import com.yasarbilgi.announcementtracker.service.impl.AnnouncementServiceImpl;
import com.yasarbilgi.announcementtracker.service.scraper.AnnouncementScraper;
import com.yasarbilgi.announcementtracker.service.scraper.ScraperRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceImplTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private ScraperRegistry scraperRegistry;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationOutboxService notificationOutboxService;

    @Mock
    private AnnouncementPersistenceService announcementPersistenceService;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private AnnouncementScraper scraperMock;
    private ScrapedAnnouncementDto scrapedDto;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(announcementService, "defaultRecipient", "admin@example.com");
        scraperMock = mock(AnnouncementScraper.class);
        lenient().when(scraperMock.getSiteType()).thenReturn(SiteType.EBELGE_GIB);

        scrapedDto = ScrapedAnnouncementDto.builder()
                .title("Yeni GİB Duyurusu")
                .content("GİB Duyuru İçeriği")
                .contentHash("hash123")
                .sourceUrl("https://ebelge.gib.gov.tr/duyuru/1")
                .sourceSite(SiteType.EBELGE_GIB)
                .announcementDate(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("Tüm scraper'lar tetiklendiğinde yeni duyuruların kaydedilmesi ve maillerin filtrelenerek gönderilmesi")
    void triggerScrapeAll_ShouldScrapeAndNotifySubscribersOfMatchingSite() {
        when(scraperRegistry.getAllScrapers()).thenReturn(List.of(scraperMock));
        when(scraperMock.scrape(any())).thenReturn(List.of(scrapedDto));
        when(announcementRepository.existsByContentHash("hash123")).thenReturn(false);

        when(announcementPersistenceService.saveAndFlush(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            announcement.setId(1L);
            return announcement;
        });

        Announcement savedEntity = Announcement.builder()
                .id(1L)
                .title(scrapedDto.getTitle())
                .content(scrapedDto.getContent())
                .contentHash(scrapedDto.getContentHash())
                .sourceSite(SiteType.EBELGE_GIB)
                .announcementDate(LocalDate.now())
                .isNotified(false)
                .build();

        when(announcementRepository.findByIsNotifiedFalse()).thenReturn(List.of(savedEntity));

        com.yasarbilgi.announcementtracker.entity.Department gibDept = com.yasarbilgi.announcementtracker.entity.Department.builder().id(10L).name("GIB").sites(Set.of(SiteType.EBELGE_GIB)).build();
        com.yasarbilgi.announcementtracker.entity.Department kosgebDept = com.yasarbilgi.announcementtracker.entity.Department.builder().id(20L).name("KOSGEB").sites(Set.of(SiteType.KOSGEB)).build();

        Subscriber sub1 = Subscriber.builder().email("sub1@gib.com").active(true).departments(Set.of(gibDept)).subscribedSites(Set.of(SiteType.EBELGE_GIB)).build();
        Subscriber sub2 = Subscriber.builder().email("sub2@kosgeb.com").active(true).departments(Set.of(kosgebDept)).subscribedSites(Set.of(SiteType.KOSGEB)).build();
        when(subscriberRepository.findByActiveTrue()).thenReturn(List.of(sub1, sub2));
        when(notificationOutboxService.enqueue(savedEntity, "sub1@gib.com")).thenReturn(true);

        List<AnnouncementResponseDto> results = announcementService.triggerScrapeAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Yeni GİB Duyurusu");

        verify(announcementPersistenceService).saveAndFlush(any(Announcement.class));
        verify(notificationOutboxService).enqueue(savedEntity, "sub1@gib.com");
        verify(notificationOutboxService, never()).enqueue(savedEntity, "sub2@kosgeb.com");
    }

    @Test
    @DisplayName("Kazıma sırasında bir scraper hata verirse diğer scraper'ların devam etmesi")
    void triggerScrapeAll_ScraperThrowsException_ShouldHandleGracefully() {
        AnnouncementScraper failingScraper = mock(AnnouncementScraper.class);
        when(failingScraper.getSiteType()).thenReturn(SiteType.KOSGEB);
        when(failingScraper.scrape(any())).thenThrow(new RuntimeException("Network timeout"));

        when(scraperRegistry.getAllScrapers()).thenReturn(List.of(failingScraper));

        List<AnnouncementResponseDto> results = announcementService.triggerScrapeAll();

        assertThat(results).isEmpty();
        verify(announcementPersistenceService, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Mükerrer duyuru tespit edildiğinde veritabanına tekrar kaydedilmemeli")
    void triggerScrapeSite_DuplicateAnnouncement_ShouldSkipSave() {
        when(scraperRegistry.getRequiredScraper(SiteType.EBELGE_GIB)).thenReturn(scraperMock);
        when(scraperMock.scrape(any())).thenReturn(List.of(scrapedDto));
        when(announcementRepository.existsByContentHash("hash123")).thenReturn(true);

        List<AnnouncementResponseDto> results = announcementService.triggerScrapeSite(SiteType.EBELGE_GIB);

        assertThat(results).isEmpty();
        verify(announcementPersistenceService, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Sayfalı duyuru listesi alma")
    void getAllAnnouncements_ShouldReturnPagedResponse() {
        Announcement entity = Announcement.builder().id(1L).title("Duyuru").sourceSite(SiteType.EBELGE_GIB).build();
        Page<Announcement> pagedEntities = new PageImpl<>(List.of(entity));

        Pageable pageable = PageRequest.of(0, 10);
        when(announcementRepository.findAll(pageable)).thenReturn(pagedEntities);

        Page<AnnouncementResponseDto> result = announcementService.getAllAnnouncements(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Duyuru");
    }

    @Test
    @DisplayName("Site bazlı sayfalı duyuru listesi alma")
    void getAnnouncementsBySite_ShouldFilterBySiteType() {
        Announcement entity = Announcement.builder().id(1L).title("KOSGEB Duyuru").sourceSite(SiteType.KOSGEB).build();
        Page<Announcement> pagedEntities = new PageImpl<>(List.of(entity));

        Pageable pageable = PageRequest.of(0, 10);
        when(announcementRepository.findBySourceSite(SiteType.KOSGEB, pageable)).thenReturn(pagedEntities);

        Page<AnnouncementResponseDto> result = announcementService.getAllAnnouncements(SiteType.KOSGEB, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSourceSite()).isEqualTo(SiteType.KOSGEB);
    }

    @Test
    @DisplayName("ID ile duyuru detayını getirme - Bulundu")
    void getAnnouncementById_Success() {
        Announcement entity = Announcement.builder().id(5L).title("Test Duyuru").sourceSite(SiteType.EBELGE_GIB).build();
        when(announcementRepository.findById(5L)).thenReturn(Optional.of(entity));

        AnnouncementResponseDto dto = announcementService.getAnnouncementById(5L);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getTitle()).isEqualTo("Test Duyuru");
    }

    @Test
    @DisplayName("ID ile duyuru detayını getirme - Bulunamadı, Exception fırlatmalı")
    void getAnnouncementById_NotFound_ShouldThrowException() {
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.getAnnouncementById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Duyuru bulunamadı, ID: 99");
    }

    @Test
    @DisplayName("Bekleyen bildirim yoksa notifyPendingAnnouncements 0 dönmeli")
    void notifyPendingAnnouncements_NoPending_ShouldReturnZero() {
        when(announcementRepository.findByIsNotifiedFalse()).thenReturn(List.of());

        int count = announcementService.notifyPendingAnnouncements();

        assertThat(count).isEqualTo(0);
        verify(emailService, never()).sendSingleAnnouncementNotification(any(), any());
    }

    @Test
    @DisplayName("Test e-postası gönderme - Duyuru var")
    void sendTestEmail_WithAnnouncement_ShouldSendNotification() {
        Announcement announcement = Announcement.builder().id(1L).title("Test").sourceSite(SiteType.EBELGE_GIB).build();
        when(announcementRepository.findAll()).thenReturn(List.of(announcement));

        Subscriber sub = Subscriber.builder().email("admin@test.com").active(true).build();
        when(subscriberRepository.findByActiveTrue()).thenReturn(List.of(sub));

        announcementService.sendTestEmail();

        verify(emailService).sendSingleAnnouncementNotification(announcement, List.of("admin@test.com"));
    }

    @Test
    @DisplayName("Test e-postası gönderme - Duyuru yoksa bildirim göndermemeli")
    void sendTestEmail_NoAnnouncement_ShouldSkip() {
        when(subscriberRepository.findByActiveTrue()).thenReturn(List.of());
        when(announcementRepository.findAll()).thenReturn(List.of());

        announcementService.sendTestEmail();

        verify(emailService, never()).sendSingleAnnouncementNotification(any(), any());
    }
}
