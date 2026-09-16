package com.yasarbilgi.announcementtracker.service.scraper;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.scraper.impl.KosgebScraper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KosgebScraperTest {

    private final KosgebScraper kosgebScraper = new KosgebScraper();

    @Test
    @DisplayName("KOSGEB scraper site tipi KOSGEB olmalıdır")
    void getSiteType_ReturnsKosgeb() {
        assertEquals(SiteType.KOSGEB, kosgebScraper.getSiteType());
    }

    @Test
    @DisplayName("KOSGEB resmi duyurular sayfasından canlı içerik çeker")
    void scrape_FetchesAnnouncementsSuccessfully() {
        List<ScrapedAnnouncementDto> results = kosgebScraper.scrape();

        assertNotNull(results, "Sonuç listesi null olmamalıdır");
        assertFalse(results.isEmpty(), "En az bir KOSGEB duyurusu ayrıştırılmalıdır");

        ScrapedAnnouncementDto first = results.get(0);
        assertNotNull(first.getTitle(), "Başlık alanı boş olmamalıdır");
        assertNotNull(first.getAnnouncementDate(), "Tarih alanı null olmamalıdır");
        assertEquals(SiteType.KOSGEB, first.getSourceSite());
    }
}
