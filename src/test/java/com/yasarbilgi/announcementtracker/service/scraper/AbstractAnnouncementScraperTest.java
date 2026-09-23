package com.yasarbilgi.announcementtracker.service.scraper;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractAnnouncementScraperTest {

    private TestScraper testScraper;

    private static class TestScraper extends AbstractAnnouncementScraper {
        @Override
        public SiteType getSiteType() {
            return SiteType.EBELGE_GIB;
        }

        @Override
        public List<ScrapedAnnouncementDto> scrape(Predicate<String> hashExistsPredicate) {
            return List.of();
        }

        public String testCalculateHash(String input) {
            return calculateHash(input);
        }

        public LocalDate testParseDate(String dateStr) {
            return parseDate(dateStr);
        }

        public String testAnnouncementHash(String url, String title, LocalDate date) {
            return calculateAnnouncementHash(getSiteType(), url, title, date);
        }

        public String testResolveAbsoluteUrl(String baseUrl, String relativeOrAbsoluteUrl) {
            return resolveAbsoluteUrl(baseUrl, relativeOrAbsoluteUrl);
        }
    }

    @BeforeEach
    void setUp() {
        testScraper = new TestScraper();
    }

    @Test
    @DisplayName("calculateHash - null veya boş veri gelirse boş metin dönmeli, geçerli metin için SHA-256 hash üretmeli")
    void calculateHash_Tests() {
        assertThat(testScraper.testCalculateHash(null)).isEqualTo("");
        assertThat(testScraper.testCalculateHash("   ")).isEqualTo("");

        String hash1 = testScraper.testCalculateHash("Duyuru Başlığı");
        String hash2 = testScraper.testCalculateHash("Duyuru Başlığı");
        String hash3 = testScraper.testCalculateHash("Farklı Başlık");

        assertThat(hash1).hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(hash3);
    }

    @Test
    @DisplayName("parseDate - Çeşitli tarih formatlarını ayrıştırmalı, geçersiz tarihte null vermeli")
    void parseDate_Tests() {
        assertThat(testScraper.testParseDate(null)).isNull();
        assertThat(testScraper.testParseDate("")).isNull();

        assertThat(testScraper.testParseDate("15.09.2026")).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(testScraper.testParseDate("5/9/2026")).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(testScraper.testParseDate("2026-09-15")).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(testScraper.testParseDate("Tarih: 15.09.2026")).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(testScraper.testParseDate("Tarih: 15.09.2026 saat 12:30"))
                .isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(testScraper.testParseDate("15-09-2026")).isEqualTo(LocalDate.of(2026, 9, 15));

        assertThat(testScraper.testParseDate("geçersiz_tarih")).isNull();
    }

    @Test
    void announcementHash_UsesStableUrlAndDoesNotDependOnFallbackDate() {
        String byUrlToday = testScraper.testAnnouncementHash(
                "https://example.com/announcements/42", "İlk başlık", LocalDate.now());
        String byUrlTomorrow = testScraper.testAnnouncementHash(
                "https://example.com/announcements/42", "Başlık değişmiş", LocalDate.now().plusDays(1));
        String withoutDate = testScraper.testAnnouncementHash(null, "  Aynı   Duyuru ", null);
        String normalizedTitle = testScraper.testAnnouncementHash(null, "aynı duyuru", null);

        assertThat(byUrlToday).isEqualTo(byUrlTomorrow);
        assertThat(withoutDate).isEqualTo(normalizedTitle);
    }

    @Test
    @DisplayName("resolveAbsoluteUrl - Göreli ve mutlak URL bileşimlerini doğru tam adrese dönüştürmeli")
    void resolveAbsoluteUrl_Tests() {
        assertThat(testScraper.testResolveAbsoluteUrl("https://example.com", null)).isNull();
        assertThat(testScraper.testResolveAbsoluteUrl("https://example.com", "   ")).isNull();

        assertThat(testScraper.testResolveAbsoluteUrl("https://example.com", "https://google.com/test"))
                .isEqualTo("https://google.com/test");

        assertThat(testScraper.testResolveAbsoluteUrl("https://example.com/", "/duyuru/1"))
                .isEqualTo("https://example.com/duyuru/1");

        assertThat(testScraper.testResolveAbsoluteUrl("https://example.com", "duyuru/1"))
                .isEqualTo("https://example.com/duyuru/1");

        assertThat(testScraper.testResolveAbsoluteUrl("https://example.com/", "duyuru/1"))
                .isEqualTo("https://example.com/duyuru/1");

        assertThat(testScraper.testResolveAbsoluteUrl(
                "https://example.com/path/page.html", "../dosya.pdf"))
                .isEqualTo("https://example.com/dosya.pdf");
        assertThat(testScraper.testResolveAbsoluteUrl("https://example.com", "//cdn.example.com/file.pdf"))
                .isEqualTo("https://cdn.example.com/file.pdf");
        assertThat(testScraper.testResolveAbsoluteUrl("https://example.com", "javascript:alert(1)"))
                .isNull();
    }
}
