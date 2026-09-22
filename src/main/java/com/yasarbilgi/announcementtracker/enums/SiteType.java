package com.yasarbilgi.announcementtracker.enums;

import lombok.Getter;

/**
 * Takip edilen duyuru kaynaklarının ve sitelerinin tanımlandığı enum.
 */
@Getter
public enum SiteType {
    EBELGE_GIB("e-Belge GİB", "https://ebelge.gib.gov.tr/duyurular.html"),
    TAKLIT_TAGSIS("Taklit/Tağşiş Duyuruları", "https://guvenilirgida.tarimorman.gov.tr/GuvenilirGida/gkd/TaklitVeyaTagsis");

    private final String displayName;
    private final String baseUrl;

    SiteType(String displayName, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }
}
