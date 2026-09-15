package com.yasarbilgi.announcementtracker.entity;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcement_hash", columnList = "content_hash", unique = true),
        @Index(name = "idx_announcement_site", columnList = "source_site"),
        @Index(name = "idx_announcement_notified", columnList = "is_notified"),
        @Index(name = "idx_announcement_date", columnList = "announcement_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDate announcementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_site", nullable = false)
    private SiteType sourceSite;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 1000)
    private String attachmentUrl;

    @Column(length = 1000)
    private String imageUrl;

    @Column(name = "content_hash", nullable = false, unique = true, length = 64)
    private String contentHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime notifiedAt;

    @Builder.Default
    private boolean isNotified = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
