package com.yasarbilgi.announcementtracker.service.impl;

import com.yasarbilgi.announcementtracker.dto.request.LoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;
import com.yasarbilgi.announcementtracker.entity.AdminUser;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.repository.AdminUserRepository;
import com.yasarbilgi.announcementtracker.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PasswordEncoder passwordEncoder;

    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    private record SessionInfo(AdminUserDto userDto, LocalDateTime expiresAt) {}

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        log.info("SuperAdmin login attempt for username: {}", request.getUsername());

        // This endpoint is an explicitly enabled local break-glass login. Keycloak users
        // authenticate through the authorization-code SSO flow, never by password grant.
        AdminUser admin = adminUserRepository.findByUsername(request.getUsername()).orElse(null);
        if (admin != null && admin.getPasswordHash() != null) {
            boolean valid = passwordEncoder.matches(request.getPassword(), admin.getPasswordHash());
            if (valid) {
                log.info("Authenticated Admin '{}' via verified BCrypt DB password.", request.getUsername());
                AdminUserDto dto = AdminUserDto.builder()
                        .username(admin.getUsername())
                        .fullName(admin.getFullName())
                        .lastLoginAt(LocalDateTime.now())
                        .build();

                String sessionToken = "SA-TOKEN-" + UUID.randomUUID();
                activeSessions.put(sessionToken, new SessionInfo(dto, LocalDateTime.now().plusHours(24)));

                return LoginResponseDto.builder()
                        .token(sessionToken)
                        .username(admin.getUsername())
                        .fullName(admin.getFullName())
                        .build();
            }
        }

        throw new ScrapingException("Geçersiz kullanıcı adı veya şifre.");
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
    public LoginResponseDto createOidcSession(String username) {
        AdminUser admin = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new ScrapingException("OIDC kullanıcısı yerel yönetici kaydıyla eşleşmiyor."));
        admin.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(admin);

        String sessionToken = "SA-TOKEN-" + UUID.randomUUID();
        AdminUserDto dto = mapToDto(admin);
        activeSessions.put(sessionToken, new SessionInfo(dto, LocalDateTime.now().plusHours(24)));

        return LoginResponseDto.builder()
                .token(sessionToken)
                .username(admin.getUsername())
                .fullName(admin.getFullName())
                .build();
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
