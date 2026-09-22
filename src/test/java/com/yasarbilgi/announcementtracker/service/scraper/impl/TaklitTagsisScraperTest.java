package com.yasarbilgi.announcementtracker.service.scraper.impl;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaklitTagsisScraperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void scrapesEveryPageAndDeduplicatesRecordsSharedByBothLists() throws Exception {
        Map<String, JsonNode> pages = new HashMap<>();
        pages.put("304:0", page(2, record("Firma A", "Marka A", "Ürün A", "Neden A", "P-1")));
        pages.put("304:1", page(2, record("Firma B", "Marka B", "Ürün B", "Neden B", "P-2")));
        pages.put("305:0", page(2,
                record("Firma A", "Marka A", "Ürün A", "Neden A", "P-1"),
                record("Firma C", "Marka C", "Ürün C", "Neden C", "P-3")));

        StubTaklitTagsisScraper scraper = new StubTaklitTagsisScraper(objectMapper, pages);

        List<ScrapedAnnouncementDto> announcements = scraper.scrape(hash -> false);

        assertEquals(3, announcements.size());
        assertEquals(List.of("304:0", "304:1", "305:0"), scraper.getRequests());
        assertTrue(announcements.stream().allMatch(item -> item.getSourceSite() == SiteType.TAKLIT_TAGSIS));
        assertTrue(announcements.stream().allMatch(item -> LocalDate.of(1970, 1, 1).equals(item.getAnnouncementDate())));
        assertTrue(announcements.stream().allMatch(item -> item.getContent().contains("Ürün Grubu: Süt Ürünleri")));
    }

    @Test
    void skipsHashesThatAlreadyExist() throws Exception {
        JsonNode existingRecord = record("Firma A", "Marka A", "Ürün A", "Neden A", "P-1");
        StubTaklitTagsisScraper initialScraper = new StubTaklitTagsisScraper(objectMapper, Map.of(
                "304:0", page(1, existingRecord),
                "305:0", page(0)));
        String existingHash = initialScraper.scrape(hash -> false).getFirst().getContentHash();

        StubTaklitTagsisScraper scraper = new StubTaklitTagsisScraper(objectMapper, Map.of(
                "304:0", page(1, existingRecord),
                "305:0", page(0)));

        assertTrue(scraper.scrape(existingHash::equals).isEmpty());
    }

    @Test
    void rejectsRemoteErrorPayloadInsteadOfReturningAnEmptySuccessfulResult() throws Exception {
        StubTaklitTagsisScraper scraper = new StubTaklitTagsisScraper(objectMapper, Map.of(
                "304:0", objectMapper.readTree("""
                        {"Durum":0,"Mesaj":"Sunucu hatası","Data":null}
                        """)));

        ScrapingException exception = assertThrows(ScrapingException.class, scraper::scrape);

        assertTrue(exception.getMessage().contains("Sunucu hatası"));
    }

    private JsonNode page(int total, JsonNode... records) throws Exception {
        var data = objectMapper.createArrayNode();
        for (JsonNode record : records) {
            data.add(record);
        }
        var root = objectMapper.createObjectNode();
        root.put("recordsTotal", total);
        root.put("recordsFiltered", total);
        root.set("data", data);
        return root;
    }

    private JsonNode record(String firm, String brand, String product, String reason, String batchNo) {
        var record = objectMapper.createObjectNode();
        record.put("DuyuruTarihi", "/Date(0)/");
        record.put("FirmaAdi", firm);
        record.put("Marka", brand);
        record.put("UrunAdi", product);
        record.put("Uygunsuzluk", reason);
        record.put("PartiSeriNo", batchNo);
        record.put("FirmaIl", "ANKARA");
        record.put("FirmaIlce", "ÇANKAYA");
        record.put("UrunGrupAdi", "Süt Ürünleri");
        return record;
    }

    private static final class StubTaklitTagsisScraper extends TaklitTagsisScraper {

        private final Map<String, JsonNode> pages;
        private final List<String> requests = new ArrayList<>();

        private StubTaklitTagsisScraper(ObjectMapper objectMapper, Map<String, JsonNode> pages) {
            super(objectMapper);
            this.pages = pages;
        }

        @Override
        protected JsonNode fetchPage(String listTypeId, int start, int length, int draw) {
            String key = listTypeId + ":" + start;
            requests.add(key);
            JsonNode page = pages.get(key);
            if (page == null) {
                throw new AssertionError("Beklenmeyen sayfa isteği: " + key);
            }
            return page;
        }

        private List<String> getRequests() {
            return requests;
        }
    }
}
