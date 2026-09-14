package com.yasarbilgi.announcementtracker.service.scraper;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;

import java.util.List;

public interface AnnouncementScraper {

    /**
     * Identifies the site strategy supported by this scraper.
     */
    SiteType getSiteType();

    /**
     * Checks if this scraper supports the specified site.
     */
    default boolean supports(SiteType siteType) {
        return getSiteType() == siteType;
    }

    /**
     * Executes the scraping logic to extract announcements.
     * @return List of scraped announcement DTOs.
     */
    List<ScrapedAnnouncementDto> scrape();
}
