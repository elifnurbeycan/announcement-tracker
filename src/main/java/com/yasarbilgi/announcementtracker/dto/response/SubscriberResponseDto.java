package com.yasarbilgi.announcementtracker.dto.response;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberResponseDto {

    private Long id;
    private String email;
    private String fullName;
    private boolean active;
    private Set<SiteType> subscribedSites;
    private LocalDateTime createdAt;
}
