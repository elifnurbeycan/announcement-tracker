package com.yasarbilgi.announcementtracker.dto.request;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberRequestDto {

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    private String email;

    private String fullName;

    private Set<SiteType> subscribedSites;
}
