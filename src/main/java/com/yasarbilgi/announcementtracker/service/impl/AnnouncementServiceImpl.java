package com.yasarbilgi.announcementtracker.service.impl;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.dto.response.AnnouncementResponseDto;
import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.entity.Subscriber;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ResourceNotFoundException;
import com.yasarbilgi.announcementtracker.repository.AnnouncementRepository;
import com.yasarbilgi.announcementtracker.repository.SubscriberRepository;
import com.yasarbilgi.announcementtracker.service.AnnouncementService;
import com.yasarbilgi.announcementtracker.service.EmailService;
import com.yasarbilgi.announcementtracker.service.scraper.AnnouncementScraper;
import com.yasarbilgi.announcementtracker.service.scraper.ScraperRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Duyuru yönetimi, web kazıma koordinasyonu ve bildirim tetikleme iş mantığını yöneten servis.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final SubscriberRepository subscriberRepository;
    private final ScraperRegistry scraperRegistry;
    private final EmailService emailService;

    @Value("${announcement.tracker.email.default-recipient:admin@example.com}")
    private String defaultRecipient;

    /**
     * Sistemde kayıtlı tüm duyuru kaynaklarını (scrapers) paralel olarak tarar ve yeni duyuruları kaydeder.
     */
    @Override
    public List<AnnouncementResponseDto> triggerScrapeAll() {
        log.info("Kayıtlı tüm duyuru kaynakları için eş zamanlı (paralel) web kazıma başlatılıyor...");

        List<AnnouncementScraper> scrapers = scraperRegistry.getAllScrapers();
        if (scrapers.isEmpty()) {
            return List.of();
        }

        // Tüm kazıyıcılar için eş zamanlı CompletableFuture görevleri oluşturulur
        List<CompletableFuture<List<AnnouncementResponseDto>>> futures = scrapers.stream()
                .map(scraper -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return processScrapingForScraper(scraper);
                    } catch (Exception e) {
                        log.error("Site için kazıma işlemi başarısız oldu: {}. Neden: {}", scraper.getSiteType(), e.getMessage(), e);
                        return List.<AnnouncementResponseDto>of();
                    }
                }))
                .toList();

        // Tüm paralel görevlerin tamamlanması beklenir
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Tüm kazıyıcılardan gelen yeni duyurular tek bir listede birleştirilir
        List<AnnouncementResponseDto> newAnnouncements = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        // Yeni eklenen duyurular için abonelere e-posta bildirimi gönderilir
        if (!newAnnouncements.isEmpty()) {
            notifyPendingAnnouncements();
        }

        return newAnnouncements;
    }

    /**
     * Belirli bir duyuru kaynağını (site) tekil olarak tarar.
     */
    @Override
    @Transactional
    public List<AnnouncementResponseDto> triggerScrapeSite(SiteType siteType) {
        log.info("Belirtilen site için web kazıma başlatılıyor: {}", siteType);
        AnnouncementScraper scraper = scraperRegistry.getRequiredScraper(siteType);

        List<AnnouncementResponseDto> newAnnouncements = processScrapingForScraper(scraper);

        if (!newAnnouncements.isEmpty()) {
            notifyPendingAnnouncements();
        }

        return newAnnouncements;
    }

    /**
     * Kazıyıcı tarafından çekilen duyuruları veritabanındakilerle karşılaştırıp mükerrer olmayanları kaydeder.
     */
    private List<AnnouncementResponseDto> processScrapingForScraper(AnnouncementScraper scraper) {
        List<ScrapedAnnouncementDto> scrapedDtos = scraper.scrape(announcementRepository::existsByContentHash);
        List<Announcement> newEntities = new ArrayList<>();

        for (ScrapedAnnouncementDto dto : scrapedDtos) {
            if (!announcementRepository.existsByContentHash(dto.getContentHash())) {
                Announcement entity = Announcement.builder()
                        .title(dto.getTitle())
                        .content(dto.getContent())
                        .announcementDate(dto.getAnnouncementDate())
                        .sourceSite(dto.getSourceSite())
                        .sourceUrl(dto.getSourceUrl())
                        .attachmentUrl(dto.getAttachmentUrl())
                        .imageUrl(dto.getImageUrl())
                        .contentHash(dto.getContentHash())
                        .isNotified(false)
                        .build();

                newEntities.add(entity);
            }
        }

        if (!newEntities.isEmpty()) {
            List<Announcement> savedEntities = announcementRepository.saveAll(newEntities);
            log.info("{} kaynağı için {} yeni duyuru veritabanına kaydedildi.", scraper.getSiteType(), savedEntities.size());
            return savedEntities.stream().map(this::mapToResponseDto).toList();
        } else {
            log.info("{} kaynağında yeni duyuru bulunamadı.", scraper.getSiteType());
            return List.of();
        }
    }

    /**
     * Veritabanındaki duyuruları sayfalama (pagination) desteği ile listeler.
     */
    @Override
    public Page<AnnouncementResponseDto> getAllAnnouncements(SiteType siteType, Pageable pageable) {
        if (siteType != null) {
            return announcementRepository.findBySourceSite(siteType, pageable).map(this::mapToResponseDto);
        }
        return announcementRepository.findAll(pageable).map(this::mapToResponseDto);
    }

    @Override
    public Page<AnnouncementResponseDto> getAnnouncementsForSites(java.util.Set<SiteType> subscribedSites, SiteType siteFilter, Pageable pageable) {
        if (subscribedSites == null || subscribedSites.isEmpty()) {
            return Page.empty(pageable);
        }
        if (siteFilter != null) {
            if (!subscribedSites.contains(siteFilter)) {
                return Page.empty(pageable);
            }
            return announcementRepository.findBySourceSite(siteFilter, pageable).map(this::mapToResponseDto);
        }
        return announcementRepository.findBySourceSiteIn(subscribedSites, pageable).map(this::mapToResponseDto);
    }

    /**
     * ID değerine göre duyuru detayını getirir.
     */
    @Override
    public AnnouncementResponseDto getAnnouncementById(Long id) {
        Announcement entity = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Duyuru bulunamadı, ID: " + id));
        return mapToResponseDto(entity);
    }

    /**
     * Henüz bildirim gönderilmemiş duyuruları aktif abonelere e-posta olarak iletir.
     */
    @Override
    @Transactional
    public int notifyPendingAnnouncements() {
        List<Announcement> pending = announcementRepository.findByIsNotifiedFalse();
        if (pending.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        // Bugün yayımlanan duyuruları öncelikli olarak filtreler
        List<Announcement> todaysAnnouncements = pending.stream()
                .filter(a -> a.getAnnouncementDate() != null && (a.getAnnouncementDate().isEqual(today) || a.getAnnouncementDate().isAfter(today)))
                .toList();

        List<Announcement> toNotify = !todaysAnnouncements.isEmpty()
                ? todaysAnnouncements
                : pending.stream().limit(3).toList();

        List<Subscriber> activeSubscribers = subscriberRepository.findByActiveTrue();
        Set<SiteType> allAvailableSites = new HashSet<>(Arrays.asList(SiteType.values()));

        for (Announcement a : toNotify) {
            // Her duyuru için dinamik efektif kaynakları kapsayan abonelere mail gönderir
            List<String> recipients = activeSubscribers.stream()
                    .filter(sub -> sub.getEffectiveSites(allAvailableSites).contains(a.getSourceSite()))
                    .map(Subscriber::getEmail)
                    .distinct()
                    .toList();

            if (recipients.isEmpty() && activeSubscribers.isEmpty()) {
                recipients = List.of(defaultRecipient);
            }

            if (!recipients.isEmpty()) {
                emailService.sendSingleAnnouncementNotification(a, recipients);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        // Yalnızca bildirimi gönderilen duyuruları bildirildi olarak işaretler
        for (Announcement a : toNotify) {
            a.setNotified(true);
            a.setNotifiedAt(now);
        }
        announcementRepository.saveAll(toNotify);

        return toNotify.size();
    }


    @Override
    public void sendTestEmail() {
        List<String> recipients = getRecipientEmails();
        var latest = announcementRepository.findAll().stream().findFirst();
        if (latest.isPresent()) {
            log.info("Sending test notification to {} recipients...", recipients.size());
            emailService.sendSingleAnnouncementNotification(latest.get(), recipients);
        } else {
            log.warn("No announcements found in database to send test email.");
        }
    }

    private List<String> getRecipientEmails() {
        List<Subscriber> activeSubscribers = subscriberRepository.findByActiveTrue();
        if (activeSubscribers.isEmpty()) {
            return List.of(defaultRecipient);
        }
        return activeSubscribers.stream().map(Subscriber::getEmail).toList();
    }

    private AnnouncementResponseDto mapToResponseDto(Announcement entity) {
        return AnnouncementResponseDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .announcementDate(entity.getAnnouncementDate())
                .sourceSite(entity.getSourceSite())
                .sourceSiteDisplayName(entity.getSourceSite().getDisplayName())
                .sourceUrl(entity.getSourceUrl())
                .attachmentUrl(entity.getAttachmentUrl())
                .imageUrl(entity.getImageUrl())
                .createdAt(entity.getCreatedAt())
                .notifiedAt(entity.getNotifiedAt())
                .isNotified(entity.isNotified())
                .build();
    }
}
