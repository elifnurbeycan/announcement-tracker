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
import java.util.List;

/**
 * Duyuru yönetimi, web kazıma koordinasyonu ve bildirim tetikleme iş mantığını yöneten servis.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final SubscriberRepository subscriberRepository;
    private final ScraperRegistry scraperRegistry;
    private final EmailService emailService;

    @Value("${announcement.tracker.email.default-recipient:admin@example.com}")
    private String defaultRecipient;

    /**
     * Sistemde kayıtlı tüm duyuru kaynaklarını (scrapers) tarar ve yeni duyuruları veritabanına kaydeder.
     */
    @Override
    @Transactional
    public List<AnnouncementResponseDto> triggerScrapeAll() {
        log.info("Kayıtlı tüm duyuru kaynakları için web kazıma başlatılıyor...");
        List<AnnouncementResponseDto> newAnnouncements = new ArrayList<>();

        for (AnnouncementScraper scraper : scraperRegistry.getAllScrapers()) {
            try {
                List<AnnouncementResponseDto> scrapedForSite = processScrapingForScraper(scraper);
                newAnnouncements.addAll(scrapedForSite);
            } catch (Exception e) {
                log.error("Site için kazıma işlemi başarısız oldu: {}. Neden: {}", scraper.getSiteType(), e.getMessage(), e);
            }
        }

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
        List<ScrapedAnnouncementDto> scrapedDtos = scraper.scrape();
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
    @Transactional(readOnly = true)
    public Page<AnnouncementResponseDto> getAllAnnouncements(SiteType siteType, Pageable pageable) {
        if (siteType != null) {
            return announcementRepository.findBySourceSite(siteType, pageable).map(this::mapToResponseDto);
        }
        return announcementRepository.findAll(pageable).map(this::mapToResponseDto);
    }

    /**
     * ID değerine göre duyuru detayını getirir.
     */
    @Override
    @Transactional(readOnly = true)
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

        List<String> recipientEmails = getRecipientEmails();
        log.info("Bekleyen {} duyurudan seçilen {} adet duyuru {} alıcıya bildiriliyor...", 
                pending.size(), toNotify.size(), recipientEmails.size());

        emailService.sendAnnouncementNotification(toNotify, recipientEmails);

        LocalDateTime now = LocalDateTime.now();
        // Tüm bekleyen duyuruları bildirildi olarak işaretler
        for (Announcement a : pending) {
            a.setNotified(true);
            a.setNotifiedAt(now);
        }
        announcementRepository.saveAll(pending);

        return toNotify.size();
    }


    @Override
    @Transactional(readOnly = true)
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
