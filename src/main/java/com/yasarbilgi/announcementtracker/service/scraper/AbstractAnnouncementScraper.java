package com.yasarbilgi.announcementtracker.service.scraper;

import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import com.yasarbilgi.announcementtracker.enums.SiteType;

@Slf4j
public abstract class AbstractAnnouncementScraper implements AnnouncementScraper {

    protected static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    protected static final int TIMEOUT_MS = 15000;

    @Override
    public List<com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto> scrape() {
        return scrape(hash -> false);
    }

    @Override
    public List<com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto> scrape(Predicate<String> hashExistsPredicate) {
        return Collections.emptyList();
    }

    /**
     * Fetch document with custom user agent and timeout settings.
     */
    protected Document fetchDocument(String url) {
        try {
            log.info("Fetching webpage content from: {}", url);
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .referrer("https://www.google.com")
                    .get();
        } catch (IOException e) {
            log.error("Failed to fetch content from URL: {}", url, e);
            throw new ScrapingException("Error connecting to site: " + url + " - " + e.getMessage(), e);
        }
    }

    /**
     * Calculates SHA-256 hash for deduplication.
     */
    protected String calculateHash(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new ScrapingException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Produces a stable announcement identity. A permanent detail/attachment URL is preferred;
     * otherwise a normalized title and an optional, actually parsed date are used. The current
     * date is deliberately never injected because that would create a new hash on every day.
     */
    protected String calculateAnnouncementHash(
            SiteType siteType, String stableUrl, String title, LocalDate parsedDate) {
        String identity;
        if (stableUrl != null && !stableUrl.isBlank()) {
            identity = "url:" + stableUrl.trim();
        } else {
            String normalizedTitle = title == null
                    ? ""
                    : title.trim().replaceAll("\\s+", " ").toLowerCase(Locale.forLanguageTag("tr-TR"));
            identity = "title:" + normalizedTitle;
            if (parsedDate != null) {
                identity += ":date:" + parsedDate;
            }
        }
        return calculateHash(siteType.name() + ":" + identity);
    }

    /**
     * Helper to safely parse Turkish date format DD.MM.YYYY
     */
    protected LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String cleanStr = dateStr.trim().replaceAll("[^0-9./-]", "");
        for (String pattern : new String[]{"dd.MM.yyyy", "d.M.yyyy", "dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd"}) {
            try {
                return LocalDate.parse(cleanStr, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
            }
        }
        log.warn("Could not parse date string: {}. Announcement date will be left empty.", dateStr);
        return null;
    }

    /**
     * Helper to resolve relative URL to absolute URL.
     */
    protected String resolveAbsoluteUrl(String baseUrl, String relativeOrAbsoluteUrl) {
        if (relativeOrAbsoluteUrl == null || relativeOrAbsoluteUrl.isBlank()) {
            return null;
        }
        if (relativeOrAbsoluteUrl.startsWith("http://") || relativeOrAbsoluteUrl.startsWith("https://")) {
            return relativeOrAbsoluteUrl;
        }
        if (baseUrl.endsWith("/") && relativeOrAbsoluteUrl.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + relativeOrAbsoluteUrl;
        }
        if (!baseUrl.endsWith("/") && !relativeOrAbsoluteUrl.startsWith("/")) {
            return baseUrl + "/" + relativeOrAbsoluteUrl;
        }
        return baseUrl + relativeOrAbsoluteUrl;
    }
}
