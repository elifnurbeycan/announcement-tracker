package com.yasarbilgi.announcementtracker.dto.response;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponseDto {

    private Long id;
    private String title;
    private String content;
    private LocalDate announcementDate;
    private SiteType sourceSite;
    private String sourceSiteDisplayName;
    private String sourceUrl;
    private String attachmentUrl;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime notifiedAt;
    private boolean isNotified;
}
