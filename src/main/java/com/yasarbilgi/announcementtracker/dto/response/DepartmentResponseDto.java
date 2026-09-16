package com.yasarbilgi.announcementtracker.dto.response;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponseDto {

    private Long id;
    private String name;
    private String description;
    private Set<SiteType> sites;
    private int subscriberCount;
    private LocalDateTime createdAt;
}
