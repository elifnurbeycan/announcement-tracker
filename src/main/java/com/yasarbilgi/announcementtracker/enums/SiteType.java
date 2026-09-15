package com.yasarbilgi.announcementtracker.enums;

import lombok.Getter;

/**
 * Takip edilen duyuru kaynaklarının ve sitelerinin tanımlandığı enum.
 */
@Getter
public enum SiteType {
    EBELGE_GIB("e-Belge GİB", "https://ebelge.gib.gov.tr/duyurular.html"),
    KOSGEB("KOSGEB Duyuruları", "https://www.kosgeb.gov.tr/site/tr/genel/duyurular");

    private final String displayName;
    private final String baseUrl;

    SiteType(String displayName, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }
}
