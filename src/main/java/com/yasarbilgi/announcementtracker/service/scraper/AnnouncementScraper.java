package com.yasarbilgi.announcementtracker.service.scraper;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;

import java.util.List;
import java.util.function.Predicate;

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
     * Executes full scraping logic to extract all announcements.
     * @return List of scraped announcement DTOs.
     */
    default List<ScrapedAnnouncementDto> scrape() {
        return scrape(hash -> false);
    }

    /**
     * Executes scraping logic with early exit when encountering existing content hash.
     * @param hashExistsPredicate Predicate returning true if hash already exists in DB.
     * @return List of scraped announcement DTOs.
     */
    List<ScrapedAnnouncementDto> scrape(Predicate<String> hashExistsPredicate);
}
