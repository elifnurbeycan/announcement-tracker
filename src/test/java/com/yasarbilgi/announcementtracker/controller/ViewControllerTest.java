package com.yasarbilgi.announcementtracker.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViewControllerTest {

    private ViewController viewController;

    @BeforeEach
    void setUp() {
        viewController = new ViewController();
    }

    @Test
    @DisplayName("Kök URL (/) isteğinin doğrudan Keycloak girişine yönlendirilmesi")
    void index_ShouldRedirectToLogin() {
        String redirect = viewController.index();
        assertThat(redirect).isEqualTo("redirect:/oauth2/authorization/keycloak");
    }

    @Test
    @DisplayName("Tüm giriş URL'leri doğrudan ortak Keycloak girişine yönlendirilmeli")
    void login_ShouldRedirectToKeycloak() {
        String redirect = viewController.login();
        assertThat(redirect).isEqualTo("redirect:/oauth2/authorization/keycloak");
    }

    @Test
    @DisplayName("/dashboard URL isteğinin /dashboard.html adresine yönlendirilmesi")
    void dashboard_ShouldForwardToDashboardHtml() {
        String forward = viewController.dashboard();
        assertThat(forward).isEqualTo("forward:/dashboard.html");
    }
}
