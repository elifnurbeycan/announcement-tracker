package com.yasarbilgi.announcementtracker.dto;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapedAnnouncementDto {

    private String title;
    private String content;
    private LocalDate announcementDate;
    private SiteType sourceSite;
    private String sourceUrl;
    private String attachmentUrl;
    private String imageUrl;
    private String contentHash;
}
