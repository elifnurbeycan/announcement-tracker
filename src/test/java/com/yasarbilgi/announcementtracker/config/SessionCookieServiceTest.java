package com.yasarbilgi.announcementtracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCookieServiceTest {

    @Test
    @DisplayName("Üretim oturum cookie'si HttpOnly, Secure ve SameSite olmalı")
    void adminSession_UsesProductionCookieFlags() {
        SessionCookieService service = new SessionCookieService(true, "Lax");
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.setAdminSession(response, "secret-session-id");

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).anySatisfy(cookie -> assertThat(cookie)
                .contains("ADMIN_SESSION=secret-session-id")
                .contains("Path=/")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax"));
        assertThat(cookies).noneMatch(cookie -> cookie.contains("ADMIN_TOKEN="));
    }

    @Test
    @DisplayName("Oturum sadece beklenen cookie adından okunmalı")
    void resolveUserSession_ReadsOnlyUserCookie() {
        SessionCookieService service = new SessionCookieService(false, "Lax");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new jakarta.servlet.http.Cookie("OTHER", "ignored"),
                new jakarta.servlet.http.Cookie(SessionCookieService.USER_COOKIE, "user-session-id"));

        assertThat(service.resolveUserSession(request)).isEqualTo("user-session-id");
        assertThat(service.resolveAdminSession(request)).isNull();
    }
}
