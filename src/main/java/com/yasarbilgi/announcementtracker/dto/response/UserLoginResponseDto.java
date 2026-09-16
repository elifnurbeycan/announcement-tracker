package com.yasarbilgi.announcementtracker.dto.response;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponseDto {

    private String token;
    private Long id;
    private String email;
    private String fullName;
    private Set<SiteType> subscribedSites;
}
