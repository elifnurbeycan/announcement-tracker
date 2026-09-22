package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.config.SessionCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class SsoLogoutController {

    private final SessionCookieService sessionCookieService;
    private final String keycloakServerUrl;
    private final String realm;
    private final String clientId;
    private final String appBaseUrl;

    public SsoLogoutController(
            SessionCookieService sessionCookieService,
            @Value("${keycloak.auth-server-url:http://localhost:8180}") String keycloakServerUrl,
            @Value("${keycloak.realm:announcement-tracker-realm}") String realm,
            @Value("${keycloak.client-id:announcement-tracker-app}") String clientId,
            @Value("${announcement.tracker.app-base-url:http://localhost:8080}") String appBaseUrl) {
        this.sessionCookieService = sessionCookieService;
        this.keycloakServerUrl = removeTrailingSlash(keycloakServerUrl);
        this.realm = realm;
        this.clientId = clientId;
        this.appBaseUrl = removeTrailingSlash(appBaseUrl);
    }

    @GetMapping("/sso/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String idTokenHint = sessionCookieService.resolveOidcLogoutHint(request);
        sessionCookieService.clearAdminSession(response);
        sessionCookieService.clearUserSession(response);
        sessionCookieService.clearOidcLogoutHint(response);

        UriComponentsBuilder logoutUrlBuilder = UriComponentsBuilder.fromUriString(keycloakServerUrl)
                .pathSegment("realms", realm, "protocol", "openid-connect", "logout")
                .queryParam("client_id", clientId)
                .queryParam("post_logout_redirect_uri", appBaseUrl + "/login");
        if (idTokenHint != null && !idTokenHint.isBlank()) {
            logoutUrlBuilder.queryParam("id_token_hint", idTokenHint);
        }

        String keycloakLogoutUrl = logoutUrlBuilder
                .build()
                .encode()
                .toUriString();

        return "redirect:" + keycloakLogoutUrl;
    }

    private static String removeTrailingSlash(String value) {
        return value != null && value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }
}
