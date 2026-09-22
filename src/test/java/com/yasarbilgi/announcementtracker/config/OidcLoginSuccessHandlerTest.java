package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.dto.session.SessionToken;
import com.yasarbilgi.announcementtracker.service.AuthService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcLoginSuccessHandlerTest {

    @Mock private AuthService authService;
    @Mock private SubscriberService subscriberService;
    @Mock private SessionCookieService sessionCookieService;
    @Mock private Authentication authentication;
    @Mock private OidcUser oidcUser;
    @Mock private OidcIdToken oidcIdToken;

    @Test
    void adminRole_CreatesOpaqueAdminSessionAndRedirects() throws Exception {
        OidcLoginSuccessHandler handler = new OidcLoginSuccessHandler(authService, subscriberService, sessionCookieService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getIdToken()).thenReturn(oidcIdToken);
        when(oidcIdToken.getTokenValue()).thenReturn("header.payload.signature");
        when(oidcUser.getClaimAsMap("realm_access")).thenReturn(Map.of("roles", List.of("ROLE_ADMIN")));
        when(oidcUser.getSubject()).thenReturn("keycloak-admin-subject");
        when(oidcUser.getClaimAsString("preferred_username")).thenReturn("admin");
        when(authService.createOidcSession("keycloak-admin-subject", "admin")).thenReturn(new SessionToken("opaque-admin-session"));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(sessionCookieService).setOidcLogoutHint(response, "header.payload.signature");
        verify(sessionCookieService).setAdminSession(response, "opaque-admin-session");
        assertThat(response.getRedirectedUrl()).isEqualTo("/dashboard.html");
    }

    @Test
    void employeeRole_CreatesOpaqueUserSessionAndRedirects() throws Exception {
        OidcLoginSuccessHandler handler = new OidcLoginSuccessHandler(authService, subscriberService, sessionCookieService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getIdToken()).thenReturn(oidcIdToken);
        when(oidcIdToken.getTokenValue()).thenReturn("header.payload.signature");
        when(oidcUser.getClaimAsMap("realm_access")).thenReturn(Map.of("roles", List.of("ROLE_USER")));
        when(oidcUser.getSubject()).thenReturn("keycloak-user-subject");
        when(oidcUser.getEmail()).thenReturn("employee@example.com");
        when(subscriberService.createOidcSession("keycloak-user-subject", "employee@example.com"))
                .thenReturn(new SessionToken("opaque-user-session"));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(sessionCookieService).setOidcLogoutHint(response, "header.payload.signature");
        verify(sessionCookieService).setUserSession(response, "opaque-user-session");
        assertThat(response.getRedirectedUrl()).isEqualTo("/user-dashboard.html");
    }
}
