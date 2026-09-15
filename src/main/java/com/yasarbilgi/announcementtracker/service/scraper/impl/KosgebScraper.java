package com.yasarbilgi.announcementtracker.service.scraper.impl;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.scraper.AbstractAnnouncementScraper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KOSGEB (kosgeb.gov.tr) portalı duyurularını ayrıştıran kazıyıcı sınıf.
 */
@Slf4j
@Component
public class KosgebScraper extends AbstractAnnouncementScraper {

    private static final String TARGET_URL = SiteType.KOSGEB.getBaseUrl();
    private static final String DOMAIN_BASE = "https://www.kosgeb.gov.tr";

    private static final Map<String, String> TURKISH_MONTHS = Map.ofEntries(
            Map.entry("ocak", "01"),
            Map.entry("şubat", "02"),
            Map.entry("mart", "03"),
            Map.entry("nisan", "04"),
            Map.entry("mayıs", "05"),
            Map.entry("haziran", "06"),
            Map.entry("temmuz", "07"),
            Map.entry("ağustos", "08"),
            Map.entry("eylül", "09"),
            Map.entry("ekim", "10"),
            Map.entry("kasım", "11"),
            Map.entry("aralık", "12")
    );

    @Override
    public SiteType getSiteType() {
        return SiteType.KOSGEB;
    }

    @Override
    public List<ScrapedAnnouncementDto> scrape() {
        List<ScrapedAnnouncementDto> results = new ArrayList<>();
        log.info("Site için duyuru tarama işlemi başlatılıyor: {}", getSiteType());

        Document doc = fetchDocument(TARGET_URL);
        Elements items = doc.select(".duyuru-item, div.duyuru-item");

        log.info("Hedef adreste ({}) {} potansiyel KOSGEB duyuru öğesi bulundu.", TARGET_URL, items.size());

        for (Element item : items) {
            Element titleLink = item.selectFirst("h5.title a, h5 a, .title a");
            if (titleLink == null) {
                continue;
            }

            String title = titleLink.text().trim();
            if (title.isBlank()) {
                continue;
            }

            String href = titleLink.attr("href");
            String detailUrl = resolveAbsoluteUrl(DOMAIN_BASE, href);

            // Tarih ayrıştırma (Örn: "27 Ağustos 2026")
            Element dateElem = item.selectFirst(".tarih");
            String dateText = dateElem != null ? dateElem.text().trim() : "";
            LocalDate announcementDate = parseKosgebDate(dateText);

            String content = title; // KOSGEB başlığı içerik özeti olarak kullanılır
            String contentHash = calculateHash(getSiteType().name() + ":" + title + ":" + announcementDate.toString());

            ScrapedAnnouncementDto dto = ScrapedAnnouncementDto.builder()
                    .title(title)
                    .content(content)
                    .announcementDate(announcementDate)
                    .sourceSite(getSiteType())
                    .sourceUrl(detailUrl != null ? detailUrl : TARGET_URL)
                    .contentHash(contentHash)
                    .build();

            results.add(dto);
        }

        log.info("{} kaynağından {} duyuru başarıyla ayrıştırıldı.", getSiteType().getDisplayName(), results.size());
        return results;
    }

    private LocalDate parseKosgebDate(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return LocalDate.now();
        }
        try {
            String cleaned = rawText.replaceAll("\\s+", " ").trim();
            String[] parts = cleaned.split(" ");
            if (parts.length >= 3) {
                String day = String.format("%02d", Integer.parseInt(parts[0]));
                String monthName = parts[1].toLowerCase(java.util.Locale.forLanguageTag("tr-TR"));
                String month = TURKISH_MONTHS.getOrDefault(monthName, "01");
                String year = parts[2];
                return LocalDate.parse(year + "-" + month + "-" + day);
            }
        } catch (Exception e) {
            log.warn("KOSGEB tarih ayrıştırma hatası [{}]: {}", rawText, e.getMessage());
        }
        return LocalDate.now();
    }
}
