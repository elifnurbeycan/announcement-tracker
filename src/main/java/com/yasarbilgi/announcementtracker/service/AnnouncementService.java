package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.response.AnnouncementResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AnnouncementService {

    /**
     * Triggers scraping for all registered site strategies.
     * @return List of newly added announcements.
     */
    List<AnnouncementResponseDto> triggerScrapeAll();

    /**
     * Triggers scraping for a specific site strategy.
     * @return List of newly added announcements for that site.
     */
    List<AnnouncementResponseDto> triggerScrapeSite(SiteType siteType);

    /**
     * Fetches stored announcements with pagination.
     */
    Page<AnnouncementResponseDto> getAllAnnouncements(SiteType siteType, Pageable pageable);

    /**
     * Retrieves announcement by ID.
     */
    AnnouncementResponseDto getAnnouncementById(Long id);

    /**
     * Dispatches pending un-notified announcements to subscribers.
     */
    int notifyPendingAnnouncements();

    /**
     * Sends a instant test email with the latest announcement to active subscribers.
     */
    void sendTestEmail();
}
