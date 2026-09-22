package com.yasarbilgi.announcementtracker.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class UnsubscribeTokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-09-22T00:00:00Z");

    @Test
    void generatedTokenIsValidAndNormalizesEmail() {
        UnsubscribeTokenService service = serviceAt(NOW);

        String token = service.generate(" USER@Example.COM ");

        assertThat(service.verifyAndExtractEmail(token)).contains("user@example.com");
    }

    @Test
    void modifiedTokenIsRejected() {
        UnsubscribeTokenService service = serviceAt(NOW);
        String token = service.generate("user@example.com");

        assertThat(service.verifyAndExtractEmail(token + "x")).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = serviceAt(NOW).generate("user@example.com");
        UnsubscribeTokenService afterExpiry = serviceAt(NOW.plus(Duration.ofDays(31)));

        assertThat(afterExpiry.verifyAndExtractEmail(token)).isEmpty();
    }

    private UnsubscribeTokenService serviceAt(Instant instant) {
        return new UnsubscribeTokenService(SECRET, Duration.ofDays(30), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
