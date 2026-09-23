package com.yasarbilgi.announcementtracker.service.scraper;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.scraper.impl.EBelgeGibScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EBelgeGibScraperTest {

    @Test
    void parsesAnnouncementsAndResolvesAttachmentLinks() {
        StubEBelgeGibScraper scraper = new StubEBelgeGibScraper(announcementFixture());

        List<ScrapedAnnouncementDto> announcements = scraper.scrape();

        assertThat(announcements).hasSize(4);
        assertThat(announcements).allSatisfy(announcement -> {
            assertThat(announcement.getSourceSite()).isEqualTo(SiteType.EBELGE_GIB);
            assertThat(announcement.getContentHash()).isNotBlank();
            assertThat(announcement.getTitle()).isNotBlank();
        });
        assertThat(announcements.get(0).getAttachmentUrl())
                .isEqualTo("https://ebelge.gib.gov.tr/dosyalar/ilk-duyuru.PDF?v=2");
    }

    @Test
    void continuesScanningAfterSeveralExistingAnnouncements() {
        StubEBelgeGibScraper scraper = new StubEBelgeGibScraper(announcementFixture());
        List<ScrapedAnnouncementDto> firstScan = scraper.scrape();
        List<String> existingHashes = firstScan.stream()
                .limit(3)
                .map(ScrapedAnnouncementDto::getContentHash)
                .toList();

        List<ScrapedAnnouncementDto> newAnnouncements = scraper.scrape(existingHashes::contains);

        assertThat(newAnnouncements)
                .extracting(ScrapedAnnouncementDto::getContentHash)
                .containsExactly(firstScan.get(3).getContentHash());
    }

    @Test
    void appliesExistingHashCheckToAlternativePageStructure() {
        Document document = Jsoup.parse("""
                <html><body>
                  <a href="duyurular.html"><strong>15.09.2026</strong></a>
                  <p><a href="/dosyalar/alternatif.pdf">Alternatif yapıdaki duyuru metni yeterince uzundur.</a></p>
                </body></html>
                """);
        StubEBelgeGibScraper scraper = new StubEBelgeGibScraper(document);
        String existingHash = scraper.scrape().getFirst().getContentHash();

        List<ScrapedAnnouncementDto> announcements = scraper.scrape(existingHash::equals);

        assertThat(announcements).isEmpty();
    }

    private static Document announcementFixture() {
        return Jsoup.parse("""
                <html><body>
                  <div class="nspArt"><p>20.09.2026 <a href="/dosyalar/ilk-duyuru.PDF?v=2">Birinci duyuru metni yeterince uzundur.</a></p></div>
                  <div class="nspArt"><p>19.09.2026 <a href="/duyurular/ikinci">İkinci duyuru metni yeterince uzundur.</a></p></div>
                  <div class="nspArt"><p>18.09.2026 <a href="/duyurular/ucuncu">Üçüncü duyuru metni yeterince uzundur.</a></p></div>
                  <div class="nspArt"><p>17.09.2026 <a href="/duyurular/dorduncu">Dördüncü duyuru metni yeterince uzundur.</a></p></div>
                </body></html>
                """);
    }

    private static final class StubEBelgeGibScraper extends EBelgeGibScraper {

        private final Document document;

        private StubEBelgeGibScraper(Document document) {
            this.document = document;
        }

        @Override
        protected Document fetchDocument(String url) {
            return document.clone();
        }
    }
}
