package com.yasarbilgi.announcementtracker.service.scraper;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.scraper.impl.EBelgeGibScraper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EBelgeGibScraperTest {

    @Test
    void testScraperStrategyRegistration() {
        EBelgeGibScraper scraper = new EBelgeGibScraper();
        assertEquals(SiteType.EBELGE_GIB, scraper.getSiteType());
        assertTrue(scraper.supports(SiteType.EBELGE_GIB));

        ScraperRegistry registry = new ScraperRegistry(List.of(scraper));
        assertTrue(registry.getScraper(SiteType.EBELGE_GIB).isPresent());
        assertEquals(scraper, registry.getRequiredScraper(SiteType.EBELGE_GIB));
    }

    @Test
    void testScrapeEBelgeWebsite() {
        EBelgeGibScraper scraper = new EBelgeGibScraper();
        List<ScrapedAnnouncementDto> scrapedList = scraper.scrape();

        assertNotNull(scrapedList, "Scraped list should not be null");
        assertFalse(scrapedList.isEmpty(), "Scraped list should contain items from ebelge.gib.gov.tr");

        ScrapedAnnouncementDto first = scrapedList.get(0);
        assertNotNull(first.getTitle(), "Title should not be null");
        assertNotNull(first.getContent(), "Content should not be null");
        assertNotNull(first.getContentHash(), "Hash should be calculated");
        assertEquals(SiteType.EBELGE_GIB, first.getSourceSite());
    }
}
