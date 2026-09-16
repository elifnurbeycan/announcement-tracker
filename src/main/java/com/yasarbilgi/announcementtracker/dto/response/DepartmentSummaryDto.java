package com.yasarbilgi.announcementtracker.dto.response;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentSummaryDto {

    private Long id;
    private String name;
    private Set<SiteType> sites;
}
