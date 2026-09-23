package com.yasarbilgi.announcementtracker.service.scraper;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ScraperRegistry {

    private final Map<SiteType, AnnouncementScraper> scrapers = new EnumMap<>(SiteType.class);

    public ScraperRegistry(List<AnnouncementScraper> scraperList) {
        for (AnnouncementScraper scraper : scraperList) {
            SiteType siteType = scraper.getSiteType();
            AnnouncementScraper existing = scrapers.putIfAbsent(siteType, scraper);
            if (existing != null) {
                throw new IllegalStateException(
                        "Aynı kaynak türü için birden fazla scraper tanımlandı: " + siteType
                                + " (" + existing.getClass().getSimpleName()
                                + ", " + scraper.getClass().getSimpleName() + ")");
            }
            log.info("Registered Scraper strategy: {} for SiteType: {}", scraper.getClass().getSimpleName(), scraper.getSiteType());
        }
    }

    public Optional<AnnouncementScraper> getScraper(SiteType siteType) {
        return Optional.ofNullable(scrapers.get(siteType));
    }

    public AnnouncementScraper getRequiredScraper(SiteType siteType) {
        return getScraper(siteType)
                .orElseThrow(() -> new ScrapingException("No scraper strategy registered for site type: " + siteType));
    }

    public List<AnnouncementScraper> getAllScrapers() {
        return List.copyOf(scrapers.values());
    }
}
