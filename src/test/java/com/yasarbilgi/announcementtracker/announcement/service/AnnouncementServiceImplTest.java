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

        when(announcementRepository.findAllById(List.of(1L))).thenReturn(List.of(savedEntity));

        com.yasarbilgi.announcementtracker.entity.Department gibDept = com.yasarbilgi.announcementtracker.entity.Department.builder().id(10L).name("GIB").sites(Set.of(SiteType.EBELGE_GIB)).build();
        com.yasarbilgi.announcementtracker.entity.Department otherDept = com.yasarbilgi.announcementtracker.entity.Department.builder().id(20L).name("OTHER").sites(Set.of()).build();

        Subscriber sub1 = Subscriber.builder().email("sub1@gib.com").active(true).departments(Set.of(gibDept)).subscribedSites(Set.of(SiteType.EBELGE_GIB)).build();
        Subscriber sub2 = Subscriber.builder().email("sub2@other.com").active(true).departments(Set.of(otherDept)).subscribedSites(Set.of()).build();
        when(subscriberRepository.findByActiveTrue()).thenReturn(List.of(sub1, sub2));
        when(notificationOutboxService.enqueue(savedEntity, "sub1@gib.com")).thenReturn(true);

        List<AnnouncementResponseDto> results = announcementService.triggerScrapeAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Yeni GİB Duyurusu");

        verify(announcementPersistenceService).saveAndFlush(any(Announcement.class));
        verify(announcementRepository, never()).findByIsNotifiedFalse();
        verify(notificationOutboxService).enqueue(savedEntity, "sub1@gib.com");
        verify(notificationOutboxService, never()).enqueue(savedEntity, "sub2@other.com");
    }

    @Test
    @DisplayName("Kazıma sırasında bir scraper hata verirse diğer scraper'ların devam etmesi")
    void triggerScrapeAll_ScraperThrowsException_ShouldHandleGracefully() {
        AnnouncementScraper failingScraper = mock(AnnouncementScraper.class);
        when(failingScraper.getSiteType()).thenReturn(SiteType.EBELGE_GIB);
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
    @DisplayName("Tüm duyuruları sayfalama ile alma")
    void getAllAnnouncements_ShouldReturnPagedAnnouncements() {
        Announcement announcement = Announcement.builder()
                .id(1L)
                .title("Test Duyuru")
                .sourceSite(SiteType.EBELGE_GIB)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Announcement> page = new PageImpl<>(List.of(announcement), pageable, 1);

        when(announcementRepository.findAllFiltered(null, "", false, pageable)).thenReturn(page);

        Page<AnnouncementResponseDto> result = announcementService
                .getAllAnnouncements(null, null, false, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Duyuru");
    }

    @Test
    @DisplayName("Bulunamayan duyuru ID'si istendiğinde ResourceNotFoundException fırlatmalı")
    void getAnnouncementById_NotFound_ShouldThrowException() {
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.getAnnouncementById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Duyuru bulunamadı, ID: 999");
    }
}
