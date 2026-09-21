package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.exception.IdentityProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class KeycloakAdminService {

    private static final String SUBSCRIBER_ROLE = "ROLE_SUBSCRIBER";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:announcement-tracker-realm}")
    private String keycloakRealm;

    @Value("${keycloak.admin.enabled:false}")
    private boolean enabled;

    @Value("${keycloak.admin.client-id:announcement-tracker-admin}")
    private String adminClientId;

    @Value("${keycloak.admin.client-secret:}")
    private String adminClientSecret;

    public boolean isEnabled() {
        return enabled;
    }

    @PostConstruct
    void validateConfiguration() {
        if (enabled && (adminClientSecret == null || adminClientSecret.isBlank())) {
            throw new IllegalStateException(
                    "Keycloak synchronization is enabled but KEYCLOAK_ADMIN_CLIENT_SECRET is missing.");
        }
    }

    public void provisionSubscriber(String email, String fullName, boolean active) {
        if (!enabled) {
            return;
        }

        try {
            String normalizedEmail = normalizeEmail(email);
            String token = getAdminAccessToken();
            Optional<Map<String, Object>> existing = findUserByEmail(normalizedEmail, token);
            String userId;

            if (existing.isPresent()) {
                userId = String.valueOf(existing.get().get("id"));
                updateUser(userId, normalizedEmail, fullName, active, token);
            } else {
                userId = createUser(normalizedEmail, fullName, active, token);
            }

            assignRealmRole(userId, SUBSCRIBER_ROLE, token);
            log.info("Keycloak subscriber synchronized: {}", normalizedEmail);
        } catch (IdentityProviderException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IdentityProviderException("Keycloak abone hesabı oluşturulamadı veya güncellenemedi.", exception);
        }
    }

    public void updateSubscriber(String previousEmail, String email, String fullName, boolean active) {
        if (!enabled) {
            return;
        }

        try {
            String normalizedEmail = normalizeEmail(email);
            String token = getAdminAccessToken();
            Optional<Map<String, Object>> existing = findUserByEmail(normalizeEmail(previousEmail), token);
            if (existing.isEmpty() && !previousEmail.equalsIgnoreCase(normalizedEmail)) {
                existing = findUserByEmail(normalizedEmail, token);
            }

            if (existing.isEmpty()) {
                provisionSubscriber(normalizedEmail, fullName, active);
                return;
            }

            String userId = String.valueOf(existing.get().get("id"));
            updateUser(userId, normalizedEmail, fullName, active, token);
            assignRealmRole(userId, SUBSCRIBER_ROLE, token);
            log.info("Keycloak subscriber updated: {} -> {}", previousEmail, normalizedEmail);
        } catch (IdentityProviderException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IdentityProviderException("Keycloak abone hesabı güncellenemedi.", exception);
        }
    }

    public void setSubscriberPassword(String email, String password, String fullName) {
        if (!enabled) {
            return;
        }

        try {
            provisionSubscriber(email, fullName, true);
            String token = getAdminAccessToken();
            Map<String, Object> user = findUserByEmail(normalizeEmail(email), token)
                    .orElseThrow(() -> new IdentityProviderException("Keycloak hesabı oluşturulamadı: " + email));
            String userId = String.valueOf(user.get("id"));
            String url = adminUrl("users/" + userId + "/reset-password");
            Map<String, Object> payload = Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", false
            );
            restTemplate.exchange(url, HttpMethod.PUT, jsonEntity(payload, token), Void.class);
            log.info("Keycloak subscriber credential initialized: {}", normalizeEmail(email));
        } catch (IdentityProviderException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IdentityProviderException("Keycloak kullanıcı parolası ayarlanamadı.", exception);
        }
    }

    public void deleteUserInKeycloak(String email) {
        if (!enabled || email == null || email.isBlank()) {
            return;
        }

        try {
            String normalizedEmail = normalizeEmail(email);
            String token = getAdminAccessToken();
            Optional<Map<String, Object>> existing = findUserByEmail(normalizedEmail, token);
            if (existing.isEmpty()) {
                return;
            }

            String userId = String.valueOf(existing.get().get("id"));
            restTemplate.exchange(adminUrl("users/" + userId), HttpMethod.DELETE,
                    new HttpEntity<>(bearerHeaders(token)), Void.class);
            log.info("Keycloak subscriber deleted: {}", normalizedEmail);
        } catch (IdentityProviderException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IdentityProviderException("Keycloak abone hesabı silinemedi.", exception);
        }
    }

    String getAdminAccessToken() {
        if (adminClientSecret == null || adminClientSecret.isBlank()) {
            throw new IdentityProviderException("Keycloak yönetim istemcisi secret değeri tanımlanmamış.");
        }

        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "client_credentials");
            params.add("client_id", adminClientId);
            params.add("client_secret", adminClientSecret);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(params, headers),
                    new ParameterizedTypeReference<>() {});
            Object accessToken = response.getBody() != null ? response.getBody().get("access_token") : null;
            if (accessToken == null) {
                throw new IdentityProviderException("Keycloak yönetim erişim anahtarı alınamadı.");
            }
            return String.valueOf(accessToken);
        } catch (IdentityProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IdentityProviderException("Keycloak yönetim bağlantısı kurulamadı.", exception);
        }
    }

    public String getKeycloakPasswordResetUrl() {
        return keycloakServerUrl + "/realms/" + keycloakRealm + "/login-actions/reset-credentials";
    }

    public void triggerKeycloakResetPasswordEmail(String email) {
        if (!enabled) return;
        try {
            String token = getAdminAccessToken();
            Optional<Map<String, Object>> userOpt = findUserByEmail(normalizeEmail(email), token);
            if (userOpt.isPresent()) {
                String userId = String.valueOf(userOpt.get().get("id"));
                String url = adminUrl("users/" + userId + "/execute-actions-email");
                restTemplate.exchange(url, HttpMethod.PUT, jsonEntity(List.of("UPDATE_PASSWORD"), token), Void.class);
                log.info("Keycloak execute-actions-email triggered for: {}", email);
            }
        } catch (Exception e) {
            log.warn("Could not trigger Keycloak execute-actions-email directly: {}", e.getMessage());
        }
    }

    public boolean authenticateUser(String email, String password) {
        if (!enabled) {
            return true;
        }
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakServerUrl, keycloakRealm);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "password");
            params.add("client_id", adminClientId != null && !adminClientId.isBlank() ? "announcement-tracker-app" : "announcement-tracker-app");
            params.add("username", normalizeEmail(email));
            params.add("password", password);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().containsKey("access_token");
        } catch (Exception e) {
            log.warn("Keycloak user password authentication failed for {}: {}", email, e.getMessage());
            return false;
        }
    }

    private String createUser(String email, String fullName, boolean active, String token) {
        NameParts name = splitName(fullName, email);
        Map<String, Object> payload = Map.of(
                "username", email,
                "email", email,
                "enabled", active,
                "emailVerified", true,
                "firstName", name.firstName(),
                "lastName", name.lastName()
        );
        ResponseEntity<Void> response = restTemplate.postForEntity(
                adminUrl("users"), jsonEntity(payload, token), Void.class);
        URI location = response.getHeaders().getLocation();
        if (location != null && location.getPath() != null) {
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        }
        return findUserByEmail(email, token)
                .map(user -> String.valueOf(user.get("id")))
                .orElseThrow(() -> new IdentityProviderException("Keycloak kullanıcısı oluşturuldu ancak kimliği alınamadı."));
    }

    private void updateUser(String userId, String email, String fullName, boolean active, String token) {
        NameParts name = splitName(fullName, email);
        Map<String, Object> payload = Map.of(
                "email", email,
                "enabled", active,
                "emailVerified", true,
                "firstName", name.firstName(),
                "lastName", name.lastName()
        );
        putUser(userId, payload, token);
    }

    private void putUser(String userId, Map<String, Object> payload, String token) {
        restTemplate.exchange(adminUrl("users/" + userId), HttpMethod.PUT,
                jsonEntity(payload, token), Void.class);
    }

    private Optional<Map<String, Object>> findUserByEmail(String email, String token) {
        String url = UriComponentsBuilder.fromUriString(adminUrl("users"))
                .queryParam("email", email)
                .queryParam("exact", true)
                .build()
                .encode()
                .toUriString();
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {});
        if (response.getBody() == null) {
            return Optional.empty();
        }
        return response.getBody().stream()
                .filter(user -> email.equalsIgnoreCase(String.valueOf(user.get("email"))))
                .findFirst();
    }

    private void assignRealmRole(String userId, String roleName, String token) {
        ResponseEntity<Map<String, Object>> roleResponse = restTemplate.exchange(
                adminUrl("roles/" + roleName),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {});
        Map<String, Object> role = roleResponse.getBody();
        if (role == null) {
            throw new IdentityProviderException("Keycloak rolü bulunamadı: " + roleName);
        }
        restTemplate.postForEntity(
                adminUrl("users/" + userId + "/role-mappings/realm"),
                jsonEntity(List.of(role), token), Void.class);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpEntity<Object> jsonEntity(Object body, String token) {
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String adminUrl(String resource) {
        return keycloakServerUrl + "/admin/realms/" + keycloakRealm + "/" + resource;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Keycloak kullanıcı e-postası boş olamaz.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private NameParts splitName(String fullName, String fallback) {
        if (fullName == null || fullName.isBlank()) {
            return new NameParts(fallback.substring(0, fallback.indexOf('@')), "");
        }
        String normalized = fullName.trim();
        int splitIndex = normalized.lastIndexOf(' ');
        if (splitIndex < 1) {
            return new NameParts(normalized, "");
        }
        return new NameParts(normalized.substring(0, splitIndex), normalized.substring(splitIndex + 1));
    }

    private record NameParts(String firstName, String lastName) {
    }
}
