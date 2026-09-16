package com.yasarbilgi.announcementtracker.service.impl;

import com.yasarbilgi.announcementtracker.dto.request.LoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;
import com.yasarbilgi.announcementtracker.entity.AdminUser;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.repository.AdminUserRepository;
import com.yasarbilgi.announcementtracker.service.AuthService;
import com.yasarbilgi.announcementtracker.util.PasswordEncoderHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoderHelper passwordEncoderHelper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:announcement-tracker-realm}")
    private String keycloakRealm;

    @Value("${keycloak.client-id:announcement-tracker-app}")
    private String keycloakClientId;

    // In-memory active session tokens map (Token -> AdminUserDto)
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    private record SessionInfo(AdminUserDto userDto, LocalDateTime expiresAt) {}

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        log.info("SuperAdmin login attempt for username: {}", request.getUsername());

        // 1. Try Keycloak Direct Access Grant Token endpoint if Keycloak is reachable
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakServerUrl, keycloakRealm);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "password");
            params.add("client_id", keycloakClientId);
            params.add("username", request.getUsername());
            params.add("password", request.getPassword());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().containsKey("access_token")) {
                String token = (String) response.getBody().get("access_token");
                log.info("Successfully authenticated SuperAdmin '{}' via Keycloak OAuth2.", request.getUsername());

                AdminUser admin = adminUserRepository.findByUsername(request.getUsername()).orElse(null);
                String fullName = admin != null ? admin.getFullName() : request.getUsername();

                AdminUserDto dto = AdminUserDto.builder()
                        .username(request.getUsername())
                        .fullName(fullName)
                        .lastLoginAt(LocalDateTime.now())
                        .build();

                activeSessions.put(token, new SessionInfo(dto, LocalDateTime.now().plusHours(24)));

                return LoginResponseDto.builder()
                        .token(token)
                        .username(request.getUsername())
                        .fullName(fullName)
                        .build();
            }
        } catch (Exception e) {
            log.debug("Keycloak authentication server unavailable or failed, falling back to local database auth: {}", e.getMessage());
        }

        // 2. Fallback to local DB authentication
        AdminUser admin = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ScrapingException("Geçersiz kullanıcı adı veya şifre"));

        if (!passwordEncoderHelper.matches(request.getPassword(), admin.getPasswordHash())) {
            log.warn("Invalid password attempt for username: {}", request.getUsername());
            throw new ScrapingException("Geçersiz kullanıcı adı veya şifre");
        }

        // Update last login timestamp
        admin.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(admin);

        // Generate session token
        String token = "SA-TOKEN-" + UUID.randomUUID();
        AdminUserDto dto = mapToDto(admin);

        // Session valid for 24 hours
        activeSessions.put(token, new SessionInfo(dto, LocalDateTime.now().plusHours(24)));

        log.info("SuperAdmin '{}' logged in successfully via local auth.", admin.getUsername());

        return LoginResponseDto.builder()
                .token(token)
                .username(admin.getUsername())
                .fullName(admin.getFullName())
                .build();
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) {
            activeSessions.remove(token);
            log.info("Session token invalidated.");
        }
    }

    @Override
    public AdminUserDto validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ScrapingException("Oturum jetonu bulunamadı. Lütfen giriş yapın.");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        SessionInfo session = activeSessions.get(token);
        if (session != null && session.expiresAt().isAfter(LocalDateTime.now())) {
            return session.userDto();
        }

        // If it's a JWT token (e.g. from Keycloak)
        if (token.contains(".")) {
            return AdminUserDto.builder()
                    .username("admin")
                    .fullName("Super Admin")
                    .lastLoginAt(LocalDateTime.now())
                    .build();
        }

        if (session != null) {
            activeSessions.remove(token);
        }
        throw new ScrapingException("Oturum süreniz doldu. Lütfen tekrar giriş yapın.");
    }

    private AdminUserDto mapToDto(AdminUser admin) {
        return AdminUserDto.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .fullName(admin.getFullName())
                .lastLoginAt(admin.getLastLoginAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}
