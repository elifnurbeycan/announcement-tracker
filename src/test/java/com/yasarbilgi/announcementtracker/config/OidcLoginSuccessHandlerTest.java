package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.UserLoginResponseDto;
import com.yasarbilgi.announcementtracker.service.AuthService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
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

    @Test
    void adminRole_CreatesOpaqueAdminSessionAndRedirects() throws Exception {
        OidcLoginSuccessHandler handler = new OidcLoginSuccessHandler(authService, subscriberService, sessionCookieService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getClaimAsMap("realm_access")).thenReturn(Map.of("roles", List.of("ROLE_ADMIN")));
        when(oidcUser.getClaimAsString("preferred_username")).thenReturn("admin");
        when(authService.createOidcSession("admin")).thenReturn(LoginResponseDto.builder().token("opaque-admin-session").build());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(sessionCookieService).setAdminSession(response, "opaque-admin-session");
        assertThat(response.getRedirectedUrl()).isEqualTo("/dashboard.html");
    }

    @Test
    void employeeRole_CreatesOpaqueUserSessionAndRedirects() throws Exception {
        OidcLoginSuccessHandler handler = new OidcLoginSuccessHandler(authService, subscriberService, sessionCookieService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getClaimAsMap("realm_access")).thenReturn(Map.of("roles", List.of("ROLE_USER")));
        when(oidcUser.getEmail()).thenReturn("employee@example.com");
        when(subscriberService.createOidcSession("employee@example.com"))
                .thenReturn(UserLoginResponseDto.builder().token("opaque-user-session").build());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(sessionCookieService).setUserSession(response, "opaque-user-session");
        assertThat(response.getRedirectedUrl()).isEqualTo("/user-dashboard.html");
    }
}
