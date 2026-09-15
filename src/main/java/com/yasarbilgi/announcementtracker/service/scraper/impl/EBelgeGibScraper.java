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

/**
 * Gelir İdaresi Başkanlığı e-Belge (ebelge.gib.gov.tr) portalı duyurularını ayrıştıran kazıyıcı sınıf.
 */
@Slf4j
@Component
public class EBelgeGibScraper extends AbstractAnnouncementScraper {

    private static final String TARGET_URL = SiteType.EBELGE_GIB.getBaseUrl();
    private static final String DOMAIN_BASE = "https://ebelge.gib.gov.tr";

    @Override
    public SiteType getSiteType() {
        return SiteType.EBELGE_GIB;
    }

    /**
     * e-Belge duyurular sayfasını bağlanan HTML belgesinden ayrıştırır ve DTO listesi olarak döndürür.
     */
    @Override
    public List<ScrapedAnnouncementDto> scrape() {
        List<ScrapedAnnouncementDto> results = new ArrayList<>();
        log.info("Site için duyuru tarama işlemi başlatılıyor: {}", getSiteType());

        Document doc = fetchDocument(TARGET_URL);

        // e-Belge GİB duyuruları NSP veya içerik paragrafları içerisinde yer alır
        Elements paragraphs = doc.select("p.nspText, div.nspArt p, div.gkIsContent p");

        if (paragraphs.isEmpty()) {
            // Alternatif paragraf seçicisi
            paragraphs = doc.select("p");
        }

        log.info("Hedef adreste ({}) {} potansiyel duyuru öğesi bulundu.", TARGET_URL, paragraphs.size());

        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() < 10) {
                continue; // Kısa metin öğelerini atla
            }

            // Metin veya önceki bağlamdan duyuru tarihini ayrıştırır
            String dateStr = extractDateFromElementOrContext(p);
            LocalDate date = parseDate(dateStr);

            // Ek dosya ve gömülü görsel bağlantılarını tespit eder
            Element linkElement = p.selectFirst("a[href]");
            String attachmentUrl = null;
            if (linkElement != null) {
                String href = linkElement.attr("href");
                if (href.contains("dosyalar") || href.endsWith(".pdf") || href.endsWith(".zip") || href.endsWith(".rar")) {
                    attachmentUrl = resolveAbsoluteUrl(DOMAIN_BASE, href);
                }
            }

            Element imgElement = p.selectFirst("img[src]");
            String imageUrl = null;
            if (imgElement != null) {
                imageUrl = resolveAbsoluteUrl(DOMAIN_BASE, imgElement.attr("src"));
            }

            // Metinden başlık ve benzersiz içerik karması (hash) türetir
            String title = extractTitleFromText(text);
            String contentHash = calculateHash(getSiteType().name() + ":" + date.toString() + ":" + title);

            boolean existsInList = results.stream().anyMatch(r -> r.getContentHash().equals(contentHash));
            if (!existsInList) {
                ScrapedAnnouncementDto dto = ScrapedAnnouncementDto.builder()
                        .title(title)
                        .content(text)
                        .announcementDate(date)
                        .sourceSite(getSiteType())
                        .sourceUrl(TARGET_URL)
                        .attachmentUrl(attachmentUrl)
                        .imageUrl(imageUrl)
                        .contentHash(contentHash)
                        .build();

                results.add(dto);
            }
        }

        // Tarih başlıklarıyla eşleşen alternatif duyuru yapılarını ayrıştırır
        Elements dateAnchors = doc.select("a[href*='duyurular.html'] strong, a[href*='duyurular'] strong");
        for (Element strong : dateAnchors) {
            String dateStr = strong.text().trim();
            Element parentA = strong.parent();
            if (parentA == null) continue;

            Element nextElement = parentA.nextElementSibling();
            if (nextElement != null && nextElement.tagName().equals("p")) {
                String text = nextElement.text().trim();
                if (text.isBlank()) continue;

                LocalDate date = parseDate(dateStr);
                String title = extractTitleFromText(text);
                String contentHash = calculateHash(getSiteType().name() + ":" + date.toString() + ":" + title);

                // Mükerrer kayıt oluşmasını engeller
                boolean exists = results.stream().anyMatch(r -> r.getContentHash().equals(contentHash));
                if (!exists) {
                    Element linkElement = nextElement.selectFirst("a[href]");
                    String attachmentUrl = null;
                    if (linkElement != null) {
                        String href = linkElement.attr("href");
                        attachmentUrl = resolveAbsoluteUrl(DOMAIN_BASE, href);
                    }

                    ScrapedAnnouncementDto dto = ScrapedAnnouncementDto.builder()
                            .title(title)
                            .content(text)
                            .announcementDate(date)
                            .sourceSite(getSiteType())
                            .sourceUrl(TARGET_URL)
                            .attachmentUrl(attachmentUrl)
                            .contentHash(contentHash)
                            .build();

                    results.add(dto);
                }
            }
        }

        log.info("{} kaynağından {} duyuru başarıyla ayrıştırıldı.", getSiteType().getDisplayName(), results.size());
        return results;
    }

    /**
     * Paragraf içi veya bitişik öğelerdeki tarih bilgisini GG.AA.YYYY formatında yakalar.
     */
    private String extractDateFromElementOrContext(Element p) {
        Element prev = p.previousElementSibling();
        if (prev != null) {
            Element strong = prev.selectFirst("strong");
            if (strong != null && strong.text().matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                return strong.text().trim();
            }
        }

        String text = p.text();
        var matcher = java.util.regex.Pattern.compile("\\b(\\d{2}\\.\\d{2}\\.\\d{4})\\b").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return LocalDate.now().toString();
    }

    /**
     * Duyuru metninin ilk cümlesinden veya belirtilen uzunluğundan kurumsal başlık üretir.
     */
    private String extractTitleFromText(String text) {
        if (text == null || text.isBlank()) {
            return "e-Belge Duyurusu";
        }
        int dotIdx = text.indexOf('.');
        if (dotIdx > 15 && dotIdx < 300) {
            return text.substring(0, dotIdx + 1).trim();
        }
        if (text.length() <= 200) {
            return text;
        }
        int lastSpace = text.substring(0, 200).lastIndexOf(' ');
        if (lastSpace > 50) {
            return text.substring(0, lastSpace).trim();
        }
        return text.substring(0, 200).trim();
    }
}

