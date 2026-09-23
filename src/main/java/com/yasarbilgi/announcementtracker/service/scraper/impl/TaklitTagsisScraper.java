package com.yasarbilgi.announcementtracker.service.scraper.impl;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.service.scraper.AbstractAnnouncementScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaklitTagsisScraper extends AbstractAnnouncementScraper {

    private static final String DATATABLES_URL =
            "https://guvenilirgida.tarimorman.gov.tr/GuvenilirGida/GKD/DataTablesList";
    private static final String PAGE_URL = SiteType.TAKLIT_TAGSIS.getBaseUrl();
    private static final Pattern DATE_MS_PATTERN = Pattern.compile("/Date\\((\\d+)\\)/");
    private static final ZoneId SOURCE_TIME_ZONE = ZoneId.of("Europe/Istanbul");
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES_PER_LIST = 1000;
    private static final int MAX_RECORDS_PER_LIST = PAGE_SIZE * MAX_PAGES_PER_LIST;
    private static final List<String> COLUMN_NAMES = List.of(
            "DuyuruTarihi",
            "FirmaAdi",
            "Marka",
            "UrunAdi",
            "Uygunsuzluk",
            "PartiSeriNo",
            "FirmaIlce",
            "FirmaIl",
            "UrunGrupAdi"
    );
    private static final List<String> LIST_TYPE_IDS = List.of("304", "305");

    private final ObjectMapper objectMapper;

    @Override
    public SiteType getSiteType() {
        return SiteType.TAKLIT_TAGSIS;
    }

    @Override
    public List<ScrapedAnnouncementDto> scrape(Predicate<String> hashExistsPredicate) {
        List<ScrapedAnnouncementDto> results = new ArrayList<>();
        Set<String> hashesSeenInThisRun = new HashSet<>();

        log.info("Taklit/Tağşiş kamuoyu duyuruları DataTables API üzerinden çekiliyor...");

        try {
            for (String listTypeId : LIST_TYPE_IDS) {
                scrapeList(listTypeId, hashExistsPredicate, hashesSeenInThisRun, results);
            }
        } catch (ScrapingException e) {
            throw e;
        } catch (Exception e) {
            throw new ScrapingException("Taklit/Tağşiş duyuruları çekilemedi: " + e.getMessage(), e);
        }

        log.info("Taklit/Tağşiş kaynağından toplam {} adet yeni duyuru ayıklandı.", results.size());
        return results;
    }

    private void scrapeList(
            String listTypeId,
            Predicate<String> hashExistsPredicate,
            Set<String> hashesSeenInThisRun,
            List<ScrapedAnnouncementDto> results) {
        int start = 0;
        int draw = 1;
        int pagesFetched = 0;

        while (true) {
            if (pagesFetched++ >= MAX_PAGES_PER_LIST) {
                throw new ScrapingException(
                        "Taklit/Tağşiş listesi güvenli sayfa sınırını aştı. ListeTurId=" + listTypeId);
            }
            JsonNode root = fetchPage(listTypeId, start, PAGE_SIZE, draw++);
            JsonNode data = root.path("data");

            if (!data.isArray()) {
                JsonNode messageNode = root.path("Mesaj");
                String remoteMessage = messageNode.isMissingNode() || messageNode.isNull()
                        ? "Beklenmeyen cevap biçimi"
                        : messageNode.asString();
                throw new ScrapingException(
                        "Taklit/Tağşiş listesi alınamadı. ListeTurId=" + listTypeId + ", hata=" + remoteMessage);
            }

            int totalRecords = root.path("recordsFiltered")
                    .asInt(root.path("recordsTotal").asInt(data.size()));
            if (totalRecords > MAX_RECORDS_PER_LIST) {
                throw new ScrapingException(
                        "Taklit/Tağşiş listesi güvenli kayıt sınırını aştı. ListeTurId="
                                + listTypeId + ", kayıt=" + totalRecords);
            }

            for (JsonNode item : data) {
                ScrapedAnnouncementDto announcement = toAnnouncement(item, listTypeId);
                if (announcement == null) {
                    continue;
                }

                String contentHash = announcement.getContentHash();
                if (!hashesSeenInThisRun.add(contentHash) || hashExistsPredicate.test(contentHash)) {
                    log.debug("Duyuru zaten mevcut veya bu taramada daha önce görüldü: {}", announcement.getTitle());
                    continue;
                }

                results.add(announcement);
            }

            if (data.isEmpty()) {
                break;
            }

            start += data.size();
            if (start >= totalRecords) {
                break;
            }
        }
    }

    protected JsonNode fetchPage(String listTypeId, int start, int length, int draw) {
        try {
            Connection connection = Jsoup.connect(DATATABLES_URL)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .referrer(PAGE_URL)
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .data("draw", Integer.toString(draw))
                    .data("start", Integer.toString(start))
                    .data("length", Integer.toString(length))
                    .data("search[value]", "")
                    .data("search[regex]", "false")
                    .data("order[0][column]", "0")
                    .data("order[0][dir]", "desc")
                    .data("KamuoyuDuyuruAra.HaricTut", "")
                    .data("KamuoyuDuyuruAra.ListeTurId", listTypeId)
                    .data("KamuoyuDuyuruAra.IdariYaptirimYasalDayanakIdler", "")
                    .data("KamuoyuDuyuruAra.IdariYaptirimYasalDayanakId", "0")
                    .data("KamuoyuDuyuruAra.DuyuruTarihi", "")
                    .data("KamuoyuDuyuruAra.UrunGrupId", "")
                    .data("SiteYayinDurumu", "True")
                    .method(Connection.Method.POST)
                    .ignoreContentType(true);

            for (int index = 0; index < COLUMN_NAMES.size(); index++) {
                String columnName = COLUMN_NAMES.get(index);
                String prefix = "columns[" + index + "]";
                connection
                        .data(prefix + "[data]", columnName)
                        .data(prefix + "[name]", columnName)
                        .data(prefix + "[searchable]", "true")
                        .data(prefix + "[orderable]", "true")
                        .data(prefix + "[search][value]", "")
                        .data(prefix + "[search][regex]", "false");
            }

            Connection.Response response = connection.execute();
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new ScrapingException(
                    "Taklit/Tağşiş sayfası alınamadı. ListeTurId=" + listTypeId + ", start=" + start,
                    e);
        }
    }

    private ScrapedAnnouncementDto toAnnouncement(JsonNode item, String listTypeId) {
        String firmName = text(item, "FirmaAdi");
        String brand = text(item, "Marka");
        String product = text(item, "UrunAdi");
        String reason = text(item, "Uygunsuzluk");
        String batchNo = text(item, "PartiSeriNo");
        String city = text(item, "FirmaIl");
        String district = text(item, "FirmaIlce");
        String productGroup = text(item, "UrunGrupAdi");
        LocalDate announcementDate = parseJsonDate(text(item, "DuyuruTarihi"));

        if (firmName.isBlank() && product.isBlank()) {
            return null;
        }

        String listLabel = "304".equals(listTypeId) ? "Taklit/Tağşiş-1" : "Taklit/Tağşiş-2";
        String title = buildTitle(listLabel, firmName, product, brand);
        String content = String.format(
                "Liste: %s | Firma: %s | İl/İlçe: %s/%s | Ürün Grubu: %s | Marka: %s | Ürün: %s | Uygunsuzluk: %s | Parti/Seri No: %s",
                listLabel, firmName, city, district, productGroup, brand, product, reason, batchNo);

        String identity = String.join("|",
                normalize(announcementDate != null ? announcementDate.toString() : ""),
                normalize(firmName),
                normalize(brand),
                normalize(product),
                normalize(reason),
                normalize(batchNo),
                normalize(city),
                normalize(district),
                normalize(productGroup));
        String contentHash = calculateHash(getSiteType().name() + ":" + identity);

        return ScrapedAnnouncementDto.builder()
                .title(title)
                .content(content)
                .announcementDate(announcementDate)
                .sourceSite(getSiteType())
                .sourceUrl(PAGE_URL)
                .contentHash(contentHash)
                .build();
    }

    private String buildTitle(String listLabel, String firmName, String product, String brand) {
        StringBuilder title = new StringBuilder("[").append(listLabel).append("] ").append(firmName);
        if (!product.isBlank()) {
            title.append(" - ").append(product);
        }
        if (!brand.isBlank()) {
            title.append(" (").append(brand).append(')');
        }
        return title.toString().trim();
    }

    private String text(JsonNode node, String fieldName) {
        return node.path(fieldName).asString("").trim();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.forLanguageTag("tr-TR"));
    }

    private LocalDate parseJsonDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        Matcher matcher = DATE_MS_PATTERN.matcher(rawDate);
        if (matcher.find()) {
            try {
                long epochMillis = Long.parseLong(matcher.group(1));
                return Instant.ofEpochMilli(epochMillis).atZone(SOURCE_TIME_ZONE).toLocalDate();
            } catch (NumberFormatException e) {
                log.warn("Taklit/Tağşiş tarihi epoch millis olarak çözümlenemedi: {}", rawDate);
            }
        }
        return parseDate(rawDate);
    }
}
