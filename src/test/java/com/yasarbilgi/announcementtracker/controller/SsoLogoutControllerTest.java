package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.config.SessionCookieService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SsoLogoutControllerTest {

    @Test
    void logout_ShouldClearApplicationCookiesAndRedirectToKeycloakLogout() {
        SessionCookieService cookieService = mock(SessionCookieService.class);
        SsoLogoutController controller = new SsoLogoutController(
                cookieService,
                "http://localhost:8180/",
                "announcement-tracker-realm",
                "announcement-tracker-app",
                "http://localhost:8080/");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cookieService.resolveOidcLogoutHint(request)).thenReturn("header.payload.signature");

        String redirect = controller.logout(request, response);

        verify(cookieService).resolveOidcLogoutHint(request);
        verify(cookieService).clearAdminSession(response);
        verify(cookieService).clearUserSession(response);
        verify(cookieService).clearOidcLogoutHint(response);
        assertThat(redirect)
                .startsWith("redirect:http://localhost:8180/realms/announcement-tracker-realm/protocol/openid-connect/logout?")
                .contains("client_id=announcement-tracker-app")
                .contains("post_logout_redirect_uri=http://localhost:8080/login")
                .contains("id_token_hint=header.payload.signature");
    }
}
