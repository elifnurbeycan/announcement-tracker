package com.yasarbilgi.announcementtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:announcement-tracker-realm}")
    private String keycloakRealm;

    /**
     * Obtains an Admin Access Token from Keycloak's master realm using admin credentials.
     */
    public String getAdminAccessToken() {
        try {
            String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", keycloakServerUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "password");
            params.add("client_id", "admin-cli");
            params.add("username", "admin");
            params.add("password", "admin");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to obtain Keycloak Admin access token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Creates or updates a subscriber user in Keycloak with their password.
     */
    public boolean createOrUpdateUserInKeycloak(String email, String password, String fullName) {
        String adminToken = getAdminAccessToken();
        if (adminToken == null) {
            log.warn("Cannot sync user '{}' to Keycloak: Admin token unavailable.", email);
            return false;
        }

        try {
            String searchUrl = String.format("%s/admin/realms/%s/users?email=%s", keycloakServerUrl, keycloakRealm, email);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> searchEntity = new HttpEntity<>(headers);
            ResponseEntity<List> searchResponse = restTemplate.exchange(searchUrl, HttpMethod.GET, searchEntity, List.class);

            if (searchResponse.getStatusCode().is2xxSuccessful() && searchResponse.getBody() != null && !searchResponse.getBody().isEmpty()) {
                // User exists in Keycloak -> Reset Password
                Map userMap = (Map) searchResponse.getBody().get(0);
                String userId = (String) userMap.get("id");
                
                String resetPasswordUrl = String.format("%s/admin/realms/%s/users/%s/reset-password", keycloakServerUrl, keycloakRealm, userId);
                Map<String, Object> passwordPayload = Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                );
                HttpEntity<Map<String, Object>> resetEntity = new HttpEntity<>(passwordPayload, headers);
                restTemplate.exchange(resetPasswordUrl, HttpMethod.PUT, resetEntity, Void.class);
                log.info("Successfully updated Keycloak password for user: {}", email);
                return true;
            } else {
                // Create New User in Keycloak
                String createUserUrl = String.format("%s/admin/realms/%s/users", keycloakServerUrl, keycloakRealm);
                
                String firstName = email.trim().toLowerCase();
                String lastName = "";

                if (fullName != null && !fullName.isBlank()) {
                    String trimmed = fullName.trim();
                    int lastSpace = trimmed.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        firstName = trimmed.substring(0, lastSpace).trim();
                        lastName = trimmed.substring(lastSpace + 1).trim();
                    } else {
                        firstName = trimmed;
                        lastName = "";
                    }
                }

                Map<String, Object> userPayload = Map.of(
                        "username", email.trim().toLowerCase(),
                        "email", email.trim().toLowerCase(),
                        "enabled", true,
                        "emailVerified", true,
                        "firstName", firstName,
                        "lastName", lastName,
                        "credentials", List.of(Map.of(
                                "type", "password",
                                "value", password,
                                "temporary", false
                        )),
                        "realmRoles", List.of("ROLE_SUBSCRIBER")
                );

                HttpEntity<Map<String, Object>> createEntity = new HttpEntity<>(userPayload, headers);
                ResponseEntity<Void> createResponse = restTemplate.postForEntity(createUserUrl, createEntity, Void.class);

                if (createResponse.getStatusCode().is2xxSuccessful() || createResponse.getStatusCode() == HttpStatus.CREATED) {
                    log.info("Successfully created new subscriber in Keycloak: {}", email);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync user '{}' to Keycloak: {}", email, e.getMessage());
        }
        return false;
    }

    /**
     * Deletes a user from Keycloak by email address (ignoring 'admin').
     */
    public boolean deleteUserInKeycloak(String email) {
        if (email == null || email.isBlank() || "admin".equalsIgnoreCase(email.trim())) {
            return false;
        }

        String adminToken = getAdminAccessToken();
        if (adminToken == null) {
            log.warn("Cannot delete user '{}' from Keycloak: Admin token unavailable.", email);
            return false;
        }

        try {
            String searchUrl = String.format("%s/admin/realms/%s/users?email=%s", keycloakServerUrl, keycloakRealm, email.trim());
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> searchEntity = new HttpEntity<>(headers);
            ResponseEntity<List> searchResponse = restTemplate.exchange(searchUrl, HttpMethod.GET, searchEntity, List.class);

            if (searchResponse.getStatusCode().is2xxSuccessful() && searchResponse.getBody() != null && !searchResponse.getBody().isEmpty()) {
                Map userMap = (Map) searchResponse.getBody().get(0);
                String userId = (String) userMap.get("id");

                String deleteUrl = String.format("%s/admin/realms/%s/users/%s", keycloakServerUrl, keycloakRealm, userId);
                restTemplate.exchange(deleteUrl, HttpMethod.DELETE, searchEntity, Void.class);
                log.info("Successfully deleted user '{}' from Keycloak.", email);
                return true;
            }
        } catch (Exception e) {
            log.warn("Failed to delete user '{}' from Keycloak: {}", email, e.getMessage());
        }
        return false;
    }
}
