package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists one scraped announcement in an isolated transaction so a concurrent duplicate
 * cannot roll back the rest of the scrape batch.
 */
@Service
@RequiredArgsConstructor
public class AnnouncementPersistenceService {

    private final AnnouncementRepository announcementRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Announcement saveAndFlush(Announcement announcement) {
        return announcementRepository.saveAndFlush(announcement);
    }
}
