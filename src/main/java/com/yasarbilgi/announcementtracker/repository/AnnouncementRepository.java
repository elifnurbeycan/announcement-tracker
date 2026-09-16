package com.yasarbilgi.announcementtracker.repository;

import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    boolean existsByContentHash(String contentHash);

    Optional<Announcement> findByContentHash(String contentHash);

    List<Announcement> findByIsNotifiedFalse();

    Page<Announcement> findBySourceSite(SiteType sourceSite, Pageable pageable);

    Page<Announcement> findBySourceSiteIn(java.util.Collection<SiteType> sourceSites, Pageable pageable);
}
