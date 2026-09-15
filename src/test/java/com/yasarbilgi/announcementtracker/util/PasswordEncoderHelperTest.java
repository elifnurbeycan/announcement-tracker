package com.yasarbilgi.announcementtracker.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderHelperTest {

    private PasswordEncoderHelper passwordEncoderHelper;

    @BeforeEach
    void setUp() {
        passwordEncoderHelper = new PasswordEncoderHelper();
    }

    @Test
    @DisplayName("Şifre hash'leme testi")
    void encode_ShouldReturnHashedPassword() {
        String rawPassword = "mySecretPassword123";
        String encoded = passwordEncoderHelper.encode(rawPassword);

        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoded).hasSize(64);
    }

    @Test
    @DisplayName("Doğru şifre eşleşme testi")
    void matches_CorrectPassword_ShouldReturnTrue() {
        String rawPassword = "mySecretPassword123";
        String encoded = passwordEncoderHelper.encode(rawPassword);

        boolean matches = passwordEncoderHelper.matches(rawPassword, encoded);

        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Yanlış şifre eşleşme testi")
    void matches_WrongPassword_ShouldReturnFalse() {
        String rawPassword = "mySecretPassword123";
        String encoded = passwordEncoderHelper.encode(rawPassword);

        boolean matches = passwordEncoderHelper.matches("wrongPassword", encoded);

        assertThat(matches).isFalse();
    }
}
