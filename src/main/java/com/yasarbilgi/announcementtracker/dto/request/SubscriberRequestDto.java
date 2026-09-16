package com.yasarbilgi.announcementtracker.dto.request;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Email adresi zorunludur")
    @Email(message = "Geçersiz e-posta biçimi")
    @Size(max = 255, message = "E-posta adresi en fazla 255 karakter olabilir")
    private String email;

    @Size(max = 100, message = "Ad Soyad en fazla 100 karakter olabilir")
    private String fullName;

    private Set<SiteType> subscribedSites;
}
