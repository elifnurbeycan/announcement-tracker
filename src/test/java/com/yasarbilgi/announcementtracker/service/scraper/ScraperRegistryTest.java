package com.yasarbilgi.announcementtracker.service.scraper;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScraperRegistryTest {

    private ScraperRegistry registry;
    private AnnouncementScraper gibScraper;

    @BeforeEach
    void setUp() {
        gibScraper = mock(AnnouncementScraper.class);
        when(gibScraper.getSiteType()).thenReturn(SiteType.EBELGE_GIB);

        registry = new ScraperRegistry(List.of(gibScraper));
    }

    @Test
    @DisplayName("Kayıtlı scraper'ı getirme")
    void getScraper_ValidSiteType_ShouldReturnScraper() {
        assertThat(registry.getScraper(SiteType.EBELGE_GIB)).isPresent();
        assertThat(registry.getRequiredScraper(SiteType.EBELGE_GIB)).isEqualTo(gibScraper);
    }

    @Test
    @DisplayName("Kayıtlı olmayan site tipi istendiğinde ScrapingException fırlatmalı")
    void getRequiredScraper_UnregisteredSiteType_ShouldThrowException() {
        assertThatThrownBy(() -> registry.getRequiredScraper(SiteType.KOSGEB))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("No scraper strategy registered for site type: KOSGEB");
    }

    @Test
    @DisplayName("Tüm scraper stratejilerini alma")
    void getAllScrapers_ShouldReturnAllRegisteredStrategies() {
        assertThat(registry.getAllScrapers()).hasSize(1);
    }
}
