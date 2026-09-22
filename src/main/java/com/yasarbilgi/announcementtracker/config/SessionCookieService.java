package com.yasarbilgi.announcementtracker.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
public class SessionCookieService {

    public static final String ADMIN_COOKIE = "ADMIN_SESSION";
    public static final String USER_COOKIE = "USER_SESSION";
    private static final String OIDC_LOGOUT_HINT_COOKIE = "OIDC_LOGOUT_HINT";

    private final boolean secure;
    private final String sameSite;

    public SessionCookieService(
            @Value("${app.security.session-cookie.secure:false}") boolean secure,
            @Value("${app.security.session-cookie.same-site:Lax}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void setAdminSession(HttpServletResponse response, String token) {
        addSessionCookie(response, ADMIN_COOKIE, token, Duration.ofHours(24));
        clearCookie(response, USER_COOKIE);
    }

    public void setUserSession(HttpServletResponse response, String token) {
        addSessionCookie(response, USER_COOKIE, token, Duration.ofDays(7));
        clearCookie(response, ADMIN_COOKIE);
    }

    public void clearAdminSession(HttpServletResponse response) {
        clearCookie(response, ADMIN_COOKIE);
    }

    public void clearUserSession(HttpServletResponse response) {
        clearCookie(response, USER_COOKIE);
    }

    public void setOidcLogoutHint(HttpServletResponse response, String idToken) {
        addSessionCookie(response, OIDC_LOGOUT_HINT_COOKIE, idToken, Duration.ofHours(12));
    }

    public String resolveOidcLogoutHint(HttpServletRequest request) {
        return resolveCookie(request, OIDC_LOGOUT_HINT_COOKIE);
    }

    public void clearOidcLogoutHint(HttpServletResponse response) {
        clearCookie(response, OIDC_LOGOUT_HINT_COOKIE);
    }

    public String resolveAdminSession(HttpServletRequest request) {
        return resolveCookie(request, ADMIN_COOKIE);
    }

    public String resolveUserSession(HttpServletRequest request) {
        return resolveCookie(request, USER_COOKIE);
    }

    private void addSessionCookie(HttpServletResponse response, String name, String token, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String resolveCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
