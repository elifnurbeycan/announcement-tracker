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
    @DisplayName("Kök URL (/) isteğinin /dashboard.html adresine yönlendirilmesi")
    void index_ShouldForwardToDashboardHtml() {
        String forward = viewController.index();
        assertThat(forward).isEqualTo("forward:/dashboard.html");
    }

    @Test
    @DisplayName("/login URL isteğinin /login.html adresine yönlendirilmesi")
    void login_ShouldForwardToLoginHtml() {
        String forward = viewController.login();
        assertThat(forward).isEqualTo("forward:/login.html");
    }

    @Test
    @DisplayName("/dashboard URL isteğinin /dashboard.html adresine yönlendirilmesi")
    void dashboard_ShouldForwardToDashboardHtml() {
        String forward = viewController.dashboard();
        assertThat(forward).isEqualTo("forward:/dashboard.html");
    }
}
