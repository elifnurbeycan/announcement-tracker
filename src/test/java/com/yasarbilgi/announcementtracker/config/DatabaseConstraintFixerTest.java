package com.yasarbilgi.announcementtracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseConstraintFixerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseConstraintFixer databaseConstraintFixer;

    @Test
    @DisplayName("Uygulama başladığında gereksiz SQL check constraint kaldırılmalı")
    void dropObsoleteEnumCheckConstraints_ShouldExecuteSql() {
        databaseConstraintFixer.dropObsoleteEnumCheckConstraints();

        verify(jdbcTemplate, times(1))
                .execute("ALTER TABLE announcements DROP CONSTRAINT IF EXISTS announcements_source_site_check");
    }

    @Test
    @DisplayName("SQL çalıştırılırken hata alınırsa hatayı yakalamalı ve uygulama çökmemeli")
    void dropObsoleteEnumCheckConstraints_OnException_ShouldLogAndNotThrow() {
        doThrow(new RuntimeException("DB Connection error"))
                .when(jdbcTemplate).execute(anyString());

        // Exception caught internally
        databaseConstraintFixer.dropObsoleteEnumCheckConstraints();

        verify(jdbcTemplate).execute(anyString());
    }
}
