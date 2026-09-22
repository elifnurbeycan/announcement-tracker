package com.yasarbilgi.announcementtracker.repository;

import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    boolean existsByContentHash(String contentHash);

    Optional<Announcement> findByContentHash(String contentHash);

    List<Announcement> findByIsNotifiedFalse();

    @Query("""
            SELECT a FROM Announcement a
            WHERE (:siteType IS NULL OR a.sourceSite = :siteType)
              AND (:search = ''
                   OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(a.content, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:hasAttachment = false
                   OR (a.attachmentUrl IS NOT NULL AND TRIM(a.attachmentUrl) <> ''))
            """)
    Page<Announcement> findAllFiltered(
            @Param("siteType") SiteType siteType,
            @Param("search") String search,
            @Param("hasAttachment") boolean hasAttachment,
            Pageable pageable);

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.sourceSite IN :sourceSites
              AND (:search = ''
                   OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(a.content, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:hasAttachment = false
                   OR (a.attachmentUrl IS NOT NULL AND TRIM(a.attachmentUrl) <> ''))
            """)
    Page<Announcement> findForSitesFiltered(
            @Param("sourceSites") Collection<SiteType> sourceSites,
            @Param("search") String search,
            @Param("hasAttachment") boolean hasAttachment,
            Pageable pageable);

    @Query("""
            SELECT a.sourceSite AS siteType, COUNT(a) AS announcementCount
            FROM Announcement a
            GROUP BY a.sourceSite
            """)
    List<SiteAnnouncementCount> countBySourceSite();

    interface SiteAnnouncementCount {
        SiteType getSiteType();

        long getAnnouncementCount();
    }
}
