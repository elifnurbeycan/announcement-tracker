package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.UserLoginResponseDto;
import com.yasarbilgi.announcementtracker.service.AuthService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OidcLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final SubscriberService subscriberService;
    private final SessionCookieService sessionCookieService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        Collection<String> roles = extractRealmRoles(oidcUser);
        boolean adminLogin = hasAdminRole(roles);
        boolean subscriberLogin = hasSubscriberRole(roles);

        if (!adminLogin && !subscriberLogin) {
            clearTemporaryOauthSession(request);
            response.sendRedirect("/user-login.html?ssoError=insufficient-roles");
            return;
        }

        try {
            if (adminLogin) {
                String username = firstNonBlank(
                        oidcUser.getClaimAsString("preferred_username"),
                        oidcUser.getEmail());
                LoginResponseDto session = authService.createOidcSession(username);
                sessionCookieService.setAdminSession(response, session.getToken());
                clearTemporaryOauthSession(request);
                response.sendRedirect("/dashboard.html");
                return;
            }

            UserLoginResponseDto session = subscriberService.createOidcSession(oidcUser.getEmail());
            sessionCookieService.setUserSession(response, session.getToken());
            clearTemporaryOauthSession(request);
            response.sendRedirect("/user-dashboard.html");
        } catch (RuntimeException exception) {
            clearTemporaryOauthSession(request);
            response.sendRedirect((adminLogin ? "/admin-login.html" : "/user-login.html") + "?ssoError=account-mapping");
        }
    }

    private Collection<String> extractRealmRoles(OidcUser user) {
        Map<String, Object> realmAccess = user.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> rawRoles)) {
            return java.util.List.of();
        }
        return rawRoles.stream().map(String::valueOf).toList();
    }

    private boolean hasAdminRole(Collection<String> roles) {
        return roles.stream()
                .map(String::toUpperCase)
                .anyMatch(role -> role.equals("ADMIN") || role.equals("SUPER_ADMIN") ||
                        role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER_ADMIN"));
    }

    private boolean hasSubscriberRole(Collection<String> roles) {
        return roles.stream()
                .map(String::toUpperCase)
                .anyMatch(role -> role.equals("SUBSCRIBER") || role.equals("ROLE_SUBSCRIBER") ||
                        role.equals("USER") || role.equals("ROLE_USER"));
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private void clearTemporaryOauthSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }
}
