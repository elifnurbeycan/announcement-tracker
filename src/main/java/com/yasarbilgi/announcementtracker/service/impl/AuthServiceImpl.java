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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoderHelper passwordEncoderHelper;

    // In-memory active session tokens map (Token -> AdminUserDto)
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    private record SessionInfo(AdminUserDto userDto, LocalDateTime expiresAt) {}

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        log.info("SuperAdmin login attempt for username: {}", request.getUsername());

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

        log.info("SuperAdmin '{}' logged in successfully.", admin.getUsername());

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
        if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
            if (session != null) {
                activeSessions.remove(token);
            }
            throw new ScrapingException("Oturum süreniz doldu. Lütfen tekrar giriş yapın.");
        }

        return session.userDto();
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
