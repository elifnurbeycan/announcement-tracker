package com.yasarbilgi.announcementtracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Otomatik tarama zamanlama ayarlarını taşıyan DTO sınıfı.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeSettingsDto {

    private int intervalMinutes;
    private boolean enabled;
}
