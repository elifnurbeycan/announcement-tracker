package com.yasarbilgi.announcementtracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/** Creates and validates signed, expiring unsubscribe links. */
@Service
public class UnsubscribeTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final Duration validity;
    private final Clock clock;

    @Autowired
    public UnsubscribeTokenService(
            @Value("${announcement.tracker.unsubscribe.secret}") String secret,
            @Value("${announcement.tracker.unsubscribe.validity:30d}") Duration validity) {
        this(secret, validity, Clock.systemUTC());
    }

    UnsubscribeTokenService(String secret, Duration validity, Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("UNSUBSCRIBE_TOKEN_SECRET en az 32 karakter olmalıdır.");
        }
        if (validity == null || validity.isZero() || validity.isNegative()) {
            throw new IllegalStateException("Abonelik iptal token süresi pozitif olmalıdır.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.validity = validity;
        this.clock = clock;
    }

    public String generate(String email) {
        String normalizedEmail = normalizeEmail(email);
        long expiresAt = clock.instant().plus(validity).getEpochSecond();
        String payload = normalizedEmail + "\n" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
        return encodedPayload + "." + encodedSignature;
    }

    public Optional<String> verifyAndExtractEmail(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) {
                return Optional.empty();
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(sign(payload), suppliedSignature)) {
                return Optional.empty();
            }

            int separator = payload.lastIndexOf('\n');
            if (separator <= 0) {
                return Optional.empty();
            }
            String email = normalizeEmail(payload.substring(0, separator));
            long expiresAt = Long.parseLong(payload.substring(separator + 1));
            if (expiresAt < clock.instant().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(email);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Abonelik iptal token'ı imzalanamadı.", exception);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-posta adresi boş olamaz.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
