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
    @DisplayName("Tüm scraper stratejilerini alma")
    void getAllScrapers_ShouldReturnAllRegisteredStrategies() {
        assertThat(registry.getAllScrapers()).hasSize(1);
    }

    @Test
    @DisplayName("Kayıtlı olmayan kaynak için açık hata verme")
    void getRequiredScraper_MissingSite_ShouldThrowException() {
        assertThatThrownBy(() -> registry.getRequiredScraper(SiteType.TAKLIT_TAGSIS))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("TAKLIT_TAGSIS");
    }

    @Test
    @DisplayName("Aynı kaynak için birden fazla scraper kaydını reddetme")
    void constructor_DuplicateSiteType_ShouldFailFast() {
        AnnouncementScraper duplicate = mock(AnnouncementScraper.class);
        when(duplicate.getSiteType()).thenReturn(SiteType.EBELGE_GIB);

        assertThatThrownBy(() -> new ScraperRegistry(List.of(gibScraper, duplicate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EBELGE_GIB");
    }
}
