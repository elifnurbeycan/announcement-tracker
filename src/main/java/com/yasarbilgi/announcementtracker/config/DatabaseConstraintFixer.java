package com.yasarbilgi.announcementtracker.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL tarafında Enum eklemeleri sonrasında eski check constraint engellerini kaldıran yardımcı sınıf.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseConstraintFixer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void dropObsoleteEnumCheckConstraints() {
        try {
            jdbcTemplate.execute("ALTER TABLE announcements DROP CONSTRAINT IF EXISTS announcements_source_site_check");
            log.info("announcements_source_site_check kısıtlaması (check constraint) başarıyla kaldırıldı.");
        } catch (Exception e) {
            log.warn("Constraint kaldırılırken hata: {}", e.getMessage());
        }
    }
}
